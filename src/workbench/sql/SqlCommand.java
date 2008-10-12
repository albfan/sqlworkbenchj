/*
 * SqlCommand.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import workbench.WbManager;
import workbench.db.ConnectionProfile;
import workbench.db.DbSettings;
import workbench.db.WbConnection;
import workbench.interfaces.ParameterPrompter;
import workbench.util.ArgumentParser;
import workbench.util.ExceptionUtil;
import workbench.interfaces.ResultLogger;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.storage.DataStore;
import workbench.storage.RowActionMonitor;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 * A single SQL command. This class is used if no special class was found
 * for a given SQL verb. The execute method checks if the command
 * returned a result set or simply an update count.
 *
 * An instance of a SQL command should always be executed in a separate Thread
 * to allow cancelling of the running statement.
 *
 * @author  support@sql-workbench.net
 */
public class SqlCommand
{
	protected Statement currentStatement;
	protected WbConnection currentConnection;
	protected boolean isCancelled = false;
	private boolean consumerWaiting = false;
	protected RowActionMonitor rowMonitor;
	protected boolean isUpdatingCommand = false;
	protected boolean reportFullStatementOnError = false;
	protected ResultLogger resultLogger;
	protected StatementRunner runner;
	protected int queryTimeout = 0;
	protected int maxRows = 0;
	protected DataStore currentRetrievalData;
	protected ParameterPrompter prompter;
	protected ArgumentParser cmdLine;
	protected boolean errorMessagesOnly = false;

	public void setRowMonitor(RowActionMonitor monitor)
	{
		this.rowMonitor = monitor;
	}

	public void setResultLogger(ResultLogger logger)
	{
		this.resultLogger = logger;
	}

	public ArgumentParser getArgumentParser()
	{
		return this.cmdLine;
	}

	public boolean getFullErrorReporting() { return reportFullStatementOnError; }
	public void setFullErrorReporting(boolean flag) { reportFullStatementOnError = flag; }
	public void setReturnOnlyErrorMessages(boolean flag) { this.errorMessagesOnly = flag; }

	protected String getDefaultSuccessMessage()
	{
		return this.getVerb() + " " + ResourceMgr.getString("MsgKnownStatementOK");
	}
	
	protected void appendSuccessMessage(StatementRunnerResult result)
	{
		result.addMessage(getDefaultSuccessMessage());
	}

	public void setParameterPrompter(ParameterPrompter p)
	{
		this.prompter = p;
	}

	protected void appendOutput(StatementRunnerResult result)
	{
		String s = this.currentConnection.getOutputMessages();
		if (!StringUtil.isBlank(s))
		{
			if (result.hasMessages())
			{
				result.addMessageNewLine();
			}
			result.addMessage(ResourceMgr.getString("TxtServerOutput"));
			result.addMessage(s);
			result.addMessageNewLine();
		}
	}

	/**
	 *	Append any warnings from the given Statement and Connection to the given
	 *	StringBuilder. If the connection is a connection to Oracle
	 *	then any messages written with dbms_output are appended as well
	 *  This behaviour is then similar to MS SQL Server where any messages
	 *  displayed using the PRINT function are returned in the Warnings as well.
	 *
	 *  @see workbench.util.SqlUtil#getWarnings(WbConnection, Statement)
	 */
	protected boolean appendWarnings(StatementRunnerResult result)
	{
		if (this.runner.getHideWarnings()) return false;

		CharSequence warn = SqlUtil.getWarnings(this.currentConnection, this.currentStatement);
		boolean hasWarning = false;
		if (warn != null && warn.length() > 0)
		{
			hasWarning = true;
			if (result.hasMessages()) result.addMessageNewLine();

			// Only add the "Warnings:" text if the message returned from the
			// server does not already start with "Warning"
			if (warn.length() > 7 && !warn.subSequence(0, 7).toString().equalsIgnoreCase("Warning"))
			{
				result.addMessage(ResourceMgr.getString("TxtWarnings"));
				result.addMessageNewLine();
			}
			result.addMessage(warn);
			result.setWarning(true);
		}
		return hasWarning;
	}

	protected void setUnknownMessage(StatementRunnerResult result, ArgumentParser cmdline, String help)
	{
		StringBuilder msg = new StringBuilder(ResourceMgr.getString("ErrUnknownParameter"));
		msg.append(cmdLine.getUnknownArguments());
		result.addMessage(msg.toString());
		if (!WbManager.getInstance().isBatchMode())
		{
			result.addMessageNewLine();
			result.addMessage(help);
		}
		result.setFailure();
	}
	/**
	 * Cancels this statements execution. Cancelling is done by
	 * calling <tt>cancel</tt> on the current JDBC Statement object. This requires
	 * that the JDBC driver actually supports cancelling of statements <b>and</b>
	 * that this method is called from a differen thread.
	 *
	 * It also sets the internal cancelled flag so that <tt>SqlCommand</tt>s
	 * that process data in a loop can check this flag and exit the loop
	 * (e.g. {@link workbench.sql.wbcommands.WbExport})
	 *
	 */
	public void cancel()
		throws SQLException
	{
		this.isCancelled = true;
		if (this.currentRetrievalData != null)
		{
			this.currentRetrievalData.cancelRetrieve();
		}
		else if (this.currentStatement != null)
		{
			try
			{
				LogMgr.logDebug("SqlCommand.cancel()", "Cancelling statement execution...");
				this.currentStatement.cancel();
				LogMgr.logDebug("SqlCommand.cancel()", "Cancelled.");
			}
			catch (Exception th)
			{
				LogMgr.logWarning("SqlCommand.cancel()", "Error when cancelling statement", th);
			}
		}
	}

	/**
	 * This method should be called, once the caller is finished with running
	 * the SQL command. This releases any database resources that were
	 * obtained during the execution of the statement (especially it
	 * closes the JDBC statement object that was used to run this command).
	 *
	 * If this statement has been cancelled a rollback() is sent to the server.
	 */
	public void done()
	{
		if (this.currentStatement != null)
		{
			if (!this.isCancelled)
			{
				try { this.currentStatement.clearWarnings(); } catch (Exception th) {}
				try { this.currentStatement.clearBatch(); } catch (Exception th) {}
			}
			try { this.currentStatement.close(); } catch (Exception th) {}
		}
		if (this.isCancelled)
		{
			try { this.currentConnection.rollback(); } catch (Exception th) {}
		}
		try { currentConnection.clearWarnings(); } catch (Exception e) {}
		this.currentStatement = null;
		this.isCancelled = false;
		currentRetrievalData = null;
	}

	/**
	 * Define the StatementRunner that runs this statement.
	 * This is needed e.g. to determine the base directory for
	 * relative file paths (WbInclude, WbImport, etc)
	 *
	 * @param r The runner running this statement.
	 */
	public void setStatementRunner(StatementRunner r)
	{
		this.runner = r;
	}

	/**
	 * Checks if this statement needs a connection to a database to run.
	 * This should be overridden by an ancestor not requiring a connection.
	 *
	 * @return true
	 * @see workbench.sql.wbcommands.WbCopy#isConnectionRequired()
	 */
	protected boolean isConnectionRequired()
	{
		return true;
	}

	/**
	 *	Should be overridden by a specialised SqlCommand.
	 * setConnection should have been called before calling execute()
	 */
	public StatementRunnerResult execute(String aSql)
		throws SQLException, Exception
	{
		StatementRunnerResult result = new StatementRunnerResult(aSql);

		this.currentStatement = this.currentConnection.createStatement();
		this.isCancelled = false;

		runner.setSavepoint();
		try
		{
			boolean hasResult = this.currentStatement.execute(aSql);
			result.setSuccess();
			processResults(result, hasResult);
			runner.releaseSavepoint();
		}
		catch (Exception e)
		{
			runner.rollbackSavepoint();
			addErrorInfo(result, aSql, e);
			LogMgr.logDebug("SqlCommand.execute()", "Error executing sql statement: " + aSql + "\nError:" + ExceptionUtil.getDisplay(e), null);
		}
		finally
		{
			this.done();
		}
		return result;
	}

	/**
	 * Tries to process any "pending" results from the last statement that was
	 * executed, but only if the current DBMS supports multiple SQL statements
	 * in a single execute() call. In all other cases the current SqlCommand
	 * will process results properly.
	 * If the DBMS does not support "batched" statements, then only possible
	 * warnings are stored in the result object
	 *
	 * @see #processResults(StatementRunnerResult, boolean)
	 * @see #appendWarnings(StatementRunnerResult)
	 */
	protected void processMoreResults(String sql, StatementRunnerResult result, boolean hasResult)
		throws SQLException
	{
		if (this.isMultiple(sql))
		{
			processResults(result, hasResult);
		}
		else
		{
			appendWarnings(result);
		}
	}

	protected void processResults(StatementRunnerResult result, boolean hasResult)
		throws SQLException
	{
		processResults(result, hasResult, null);
	}

	/**
	 * Process any ResultSets or updatecounts generated by the last statement
	 * execution.
	 */
	protected void processResults(StatementRunnerResult result, boolean hasResult, ResultSet queryResult)
		throws SQLException
	{
		if (result == null) return;

		appendOutput(result);

		// Postgres obviously clears the warnings if the getMoreResults() is called,
		// so we add the warnings before calling getMoreResults(). This doesn't seem
		// to do any harm for other DBMS as well.
		appendWarnings(result);

		int updateCount = -1;
		boolean moreResults = false;

		if (!hasResult)
		{
			// the first "result" is an updateCount
			try
			{
				updateCount = this.currentStatement.getUpdateCount();
			}
			catch (Exception e)
			{
				LogMgr.logError("SqlCommand.processResults()", "Error when calling getUpdateCount()", e);
				updateCount = -1;
			}

			try
			{
				moreResults = this.currentStatement.getMoreResults();
			}
			catch (Exception e)
			{
				// Some drivers throw errors if no result is available. In this case
				// simply assume there are no more results.
				LogMgr.logError("SqlCommand.processResults()", "Error when calling getMoreResults()", e);
				moreResults = false;
			}
		}
		else
		{
			moreResults = true;
		}

		ResultSet rs = null;
		boolean multipleUpdateCounts = (this.currentConnection != null ? this.currentConnection.getDbSettings().allowsMultipleGetUpdateCounts() : false);

		int counter = 0;
		while (moreResults || updateCount > -1)
		{
			if (updateCount > -1)
			{
				result.addUpdateCountMsg(updateCount);
			}

			if (moreResults)
			{
				if (queryResult != null)
				{
					rs = queryResult;
					queryResult = null;
				}
				else
				{
					rs = this.currentStatement.getResultSet();
				}

				if (this.isConsumerWaiting() && rs != null)
				{
					result.addResultSet(rs);
					// only one resultSet can be exported
					// if we call getMoreResults() another time, the previous ResultSet will be closed!
					break;
				}

				if (rs != null)
				{
					// we have to use an instance variable for the retrieval, otherwise the retrieval
					// cannot be cancelled!
					this.currentRetrievalData = new DataStore(rs, false, this.rowMonitor, maxRows, this.currentConnection);
					try
					{
						// Not reading the data in the constructor enables us
						// to cancel the retrieval of the data from the ResultSet
						// without using statement.cancel()
						// The DataStore checks for the cancel flag during processing
						// of the ResulSet
						this.currentRetrievalData.setGeneratingSql(result.getSourceCommand());
						this.currentRetrievalData.initData(rs, maxRows);
					}
					catch (SQLException e)
					{
						// Some JDBC driver throw an exception when a statement is
						// cancelled. But in this case, we do not want to throw away the
						// data that was retrieved until now. We only add a warning
						if (this.currentRetrievalData != null && this.currentRetrievalData.isCancelled())
						{
							result.addMessage(ResourceMgr.getString("MsgErrorDuringRetrieve"));
							result.addMessage(ExceptionUtil.getAllExceptions(e));
							result.setWarning(true);
						}
						else
						{
							// if the statement was not cancelled, make sure
							// the error is displayed to the user.
							throw e;
						}
					}
					finally
					{
						SqlUtil.closeResult(rs);
					}
					result.addDataStore(this.currentRetrievalData);
				}
			}

			try
			{
				moreResults = this.currentStatement.getMoreResults();
			}
			catch (Throwable th)
			{
				// Some older Postgres drivers throw a NPE when getMoreResults() is called multiple
				// times. This exception is simply ignored, so that processing can proceed normally
				LogMgr.logError("SqlCommand.processResults()", "Error when calling getMoreResults()", th);
				break;
			}

			if (multipleUpdateCounts)
			{
				try
				{
					updateCount = this.currentStatement.getUpdateCount();
				}
				catch (Exception e)
				{
					LogMgr.logWarning("SqlCommand.processResult()", "Error when calling getUpdateCount(): " + ExceptionUtil.getDisplay(e));
					updateCount = -1;
					multipleUpdateCounts = false;
				}
			}
			else
			{
				updateCount = -1;
			}

			counter ++;

			// some JDBC drivers do not implement getMoreResults() and getUpdateCount()
			// correctly, so this is a safety to prevent an endless loop
			if (counter > 50) break;
		}

		this.currentRetrievalData = null;
	}

	public void setConnection(WbConnection conn)
	{
		this.currentConnection = conn;
	}

	/**
	 *	Should be overridden by a specialised SqlCommand
	 */
	public String getVerb()
	{
		return StringUtil.EMPTY_STRING;
	}

	public String getAlternateVerb()
	{
		return null;
	}
	
	/**
	 * 	The commands producing a result set need this flag.
	 * 	If no consumer is waiting, the can directly produce a DataStore
	 * 	for the result.
	 * @see #isConsumerWaiting()
	 */
	public void setConsumerWaiting(boolean flag)
	{
		this.consumerWaiting = flag;
	}

	/**
	 * Checks if a consumer is waiting for the result of this command.
	 * @see #setConsumerWaiting(boolean)
	 */
	public boolean isConsumerWaiting()
	{
		return this.consumerWaiting;
	}

	/**
	 * Check if this verb for this statement is considered an updating command in
	 * all circumstances.
	 */
	public boolean isUpdatingCommand()
	{
		return this.isUpdatingCommand;
	}

	/**
	 * Check if this statement needs a confirmation by the user
	 * to be run.
	 *
	 * @param con the current connection
	 * @param sql the statement to be executed
	 * @return true if the user should confirm to run this statement
	 * @see #getModificationTarget(workbench.db.WbConnection, java.lang.String)
	 * @see workbench.db.ConnectionProfile#getConfirmUpdates()
	 */
	public boolean needConfirmation(WbConnection con, String sql)
	{
		if (con == null || con.isClosed()) return false;
		ConnectionProfile prof = getModificationTarget(con, sql);
		if (prof == null) return false;
		if (isUpdatingCommand(con, sql))
		{
			return prof.getConfirmUpdates();
		}
		return false;
	}

	/**
	 * Check if the given SQL statement may update the database.
	 * This method will also consider additional keywords
	 * added by the user (globally or DBMS specific) and check if
	 * the SELECT statement is a disguised CREATE TABLE.
	 *
	 * @param con the database connection to check against
	 * @param sql the SQL statement to check
	 * @return true if the SQL might update the database
	 *
	 * @see workbench.db.DbSettings#isUpdatingCommand(java.lang.String)
	 * @see workbench.db.DbMetadata#isSelectIntoNewTable(java.lang.String)
	 */
	public boolean isUpdatingCommand(WbConnection con, String sql)
	{
		if (con == null) return isUpdatingCommand;
		if (con.isClosed()) return isUpdatingCommand;
		if (this.isUpdatingCommand) return true;
		String verb = SqlUtil.getSqlVerb(sql);
		boolean updating = con.getDbSettings().isUpdatingCommand(verb);
		if (updating) return true;
		return con.getMetadata().isSelectIntoNewTable(sql);
	}

	/**
	 * Returns the profile that is attached to the connection where the
	 * data would be modified.
	 * The default implementation returns the profile attached to the
	 * passed connection. WbCopy will check the commandline and will
	 * return the profile specified in the -targetProfile parameter
	 *
	 * @param con the connection to check
	 * @param sql the SQL statement to check (ignored)
	 * @return the profile attached to the connection
	 *
	 * @see workbench.sql.wbcommands.WbCopy#getModificationTarget(workbench.db.WbConnection, java.lang.String)
	 */
	public ConnectionProfile getModificationTarget(WbConnection con, String sql)
	{
		if (con == null) return null;
		return con.getProfile();
	}

	/**
	 * Check if the current profile allows running of the given SQL. This
	 * will check for the read-only option and will disallowe statements
	 * where {@link #isUpdatingCommand(workbench.db.WbConnection, java.lang.String)}
	 * returns true.
	 *
	 * @param con
	 * @param sql
	 * @return true if the profile is not read-only, or if the statement does not update anything
	 * @see #isUpdatingCommand(workbench.db.WbConnection, java.lang.String)
	 */
	public boolean isModificationAllowed(WbConnection con, String sql)
	{
		if (con == null || con.isClosed()) return true;
		ConnectionProfile prof = getModificationTarget(con, sql);
		if (prof == null) return true;
		if (isUpdatingCommand(con, sql))
		{
			if (prof.isReadOnly()) return false;
		}
		return true;
	}

	public void setQueryTimeout(int timeout)
	{
		this.queryTimeout = timeout;
	}

	public void setMaxRows(int max)
	{
		maxRows = max;
	}
	public boolean isResultSetConsumer() { return false; }
	public void consumeResult(StatementRunnerResult aResult) {}

	/**
	 * Get a "clean" version of the sql with the verb stripped off
	 * and all comments and newlines removed for processing the
	 * parameters to a Workbench command
	 *
	 * @param sql the sql to "clean"
	 * @return the sql with the verb, comments and newlines removed
	 * @see workbench.util.SqlUtil#makeCleanSql(String, boolean, boolean, char)
	 * @see workbench.util.SqlUtil#getSqlVerb(java.lang.CharSequence)
	 */
	protected String getCommandLine(String sql)
	{
		return SqlUtil.stripVerb(SqlUtil.makeCleanSql(sql,false,false,'\''));
	}

	/**
	 * Assumes the given parameter is a filename supplied by the end user.
	 * If the filename is absolute, a file object with that path is returned.
	 * If the filename does not contain a full path, the current baseDir of the
	 * StatementRunner is added to the returned file.
	 * @param fileName
	 * @return a File object pointing to the file indicated by the user.
	 * @see workbench.sql.StatementRunner#getBaseDir()
	 */
	protected WbFile evaluateFileArgument(String fileName)
	{
		if (StringUtil.isEmptyString(fileName)) return null;

		String fname = StringUtil.trimQuotes(fileName);
		WbFile f  = new WbFile(fname);
		if (f.isAbsolute()) return f;

		// Use the "current" directory of the StatementRunner
		// for the path of the file, if no path is specified.
		if (this.runner != null)
		{
			String dir = this.runner.getBaseDir();
			if (!StringUtil.isEmptyString(dir))
			{
				f = new WbFile(dir, fname);
			}
		}
		return f;
	}

	protected void addErrorInfo(StatementRunnerResult result, String sql, Throwable e)
	{
		result.clear();

		StringBuilder msg = new StringBuilder(150);
		if (!errorMessagesOnly)
		{
			msg.append(ResourceMgr.getString("MsgExecuteError") + "\n");
			if (reportFullStatementOnError)
			{
				msg.append(sql);
			}
			else
			{
				msg.append(StringUtil.getMaxSubstring(sql.trim(), 150));
			}
			result.addMessage(msg);
			result.addMessageNewLine();
		}
		result.addMessage(ExceptionUtil.getAllExceptions(e));
		result.setFailure();
	}

	/**
	 * Check if the passed SQL is a "batched" statement.
	 *
	 * Returns true if the passed SQL string could be a "batched"
	 * statement that actually contains more than one statement.
	 * SQL Server supports these kind of "batches". If this is the case
	 * affected rows will always be shown, because we cannot know
	 * if the statement did not update anything or if it actually
	 * updated only 0 rows (for some reason SQL Server seems to
	 * return 0 as the updatecount even if no update was involved).
	 *
	 * Currently this is only checking for newlines in the passed string.
	 *
	 * @param sql the statement/script to check
	 * @return true if the passed sql could contain more than one (independent) statements
	 */
	protected boolean isMultiple(String sql)
	{
		if (this.currentConnection == null) return false;
		DbSettings settings = currentConnection.getDbSettings();
		if (settings.supportsBatchedStatements())
		{
			// TODO: analyze the statement properly to find out if it is really a batched statement.
			return (sql.indexOf('\n') > -1);
		}
		return false;
	}

}
