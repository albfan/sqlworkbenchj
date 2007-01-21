/*
 * SqlCommand.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import workbench.db.WbConnection;
import workbench.interfaces.ParameterPrompter;
import workbench.interfaces.StatementRunner;
import workbench.util.ArgumentParser;
import workbench.util.ExceptionUtil;
import workbench.interfaces.ResultLogger;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.storage.DataStore;
import workbench.storage.RowActionMonitor;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

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
	
	protected void appendSuccessMessage(StatementRunnerResult result)
	{
		result.addMessage(this.getVerb() + " " + ResourceMgr.getString("MsgKnownStatementOK"));
	}

	public void setParameterPrompter(ParameterPrompter p) 
	{
		this.prompter = p;
	}
	/**
	 *	Append any warnings from the given Statement and Connection to the given
	 *	StringBuilder. If the connection is a connection to Oracle
	 *	then any messages written with dbms_output are appended as well
	 *  This behaviour is then similar to MS SQL Server where any messages
	 *  displayed using the PRINT function are returned in the Warnings as well.
	 */
	protected boolean appendWarnings(WbConnection aConn, Statement aStmt, StringBuilder msg)
	{
		String warn = SqlUtil.getWarnings(aConn, aStmt, !this.isCancelled);
		boolean hasWarning = false;
		if (warn != null && warn.trim().length() > 0)
		{
			hasWarning = true;
			msg.append(warn.trim());
		}
		return hasWarning;
	}

	protected void setUnknownMessage(StatementRunnerResult result, ArgumentParser cmdline, String help)
	{
		StringBuilder msg = new StringBuilder(ResourceMgr.getString("ErrUnknownParameter"));
		msg.append(cmdLine.getUnknownArguments());
		result.addMessage(msg.toString());
		result.addMessage(""); // add empty line
		result.addMessage(help);
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
			catch (Throwable th)
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
				try { this.currentStatement.clearWarnings(); } catch (Throwable th) {}
				try { this.currentStatement.clearBatch(); } catch (Throwable th) {}
			}
			try { this.currentStatement.close(); } catch (Throwable th) {}
		}
		if (this.isCancelled)
		{
			try { this.currentConnection.rollback(); } catch (Throwable th) {}
		}
		this.currentStatement = null;
		this.isCancelled = false;
	}

	public void setStatementRunner(StatementRunner r)
	{
		this.runner = r;
	}

	protected boolean isConnectionRequired() { return true; }
	
	/**
	 *	Should be overridden by a specialised SqlCommand
	 */
	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException, Exception
	{
		StatementRunnerResult result = new StatementRunnerResult(aSql);
		
		this.currentStatement = aConnection.createStatement();
		setConnection(aConnection);
		this.isCancelled = false;
		

		try
		{
			boolean hasResult = this.currentStatement.execute(aSql);

			processResults(result, hasResult);
			
			result.setSuccess();
		}
		catch (Exception e)
		{
			result.clear();
			StringBuilder msg = new StringBuilder(150);
			msg.append(ResourceMgr.getString("MsgExecuteError") + "\n");
			String s = StringUtil.getMaxSubstring(aSql.trim(), 150);
			msg.append(s);
			msg.append("\n");
			result.addMessage(msg.toString());
			StringBuilder er = ExceptionUtil.getAllExceptions(e);
			result.addMessage(er);
			result.setFailure();
			LogMgr.logDebug("SqlCommand.execute()", "Error executing sql statement " + s + "\nError:" + er, null);
		}
		finally
		{
			this.done();
		}
		return result;
	}

	protected void processResults(StatementRunnerResult result, boolean hasResult)
		throws SQLException
	{
		processResults(result, hasResult, null);
	}
	
	protected void processResults(StatementRunnerResult result, boolean hasResult, ResultSet queryResult)
		throws SQLException
	{
		if (result == null) return;
		
		// Postgres obviously clears the warnings if the getMoreResults()
		// and stuff is called, so we add the warnings right at the beginning
		// this shouldn't affect other DBMSs (hopefully :-)
		StringBuilder warnings = new StringBuilder();
		if (appendWarnings(this.currentConnection, this.currentStatement, warnings))
		{
			result.addMessage(warnings.toString());
		}

		int updateCount = -1;
		boolean moreResults = false;

		if (hasResult == false) 
		{
			// the first "result" is an updateCount
			updateCount = this.currentStatement.getUpdateCount();
			moreResults = this.currentStatement.getMoreResults();
		}
		else
		{
			moreResults = true;
		}

		ResultSet rs = null;
		boolean multipleUpdateCounts = (this.currentConnection != null ? this.currentConnection.getDbSettings().allowsMultipleGetUpdateCounts() : true);

		int counter = 0;
		while (moreResults || updateCount > -1)
		{
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
						this.currentRetrievalData.initData(rs, maxRows);
						this.currentRetrievalData.setGeneratingSql(result.getSourceCommand());
					}
					catch (SQLException e)
					{
						// Some JDBC driver throw an exception when a statement is 
						// cancelled. But in this case, we do not want to throw away the 
						// data retrieved until now. We only add a warning
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
						try { rs.close(); } catch (Throwable th) {}
					}
					result.addDataStore(this.currentRetrievalData);
				}
			}
			else if (updateCount > -1)
			{			
				result.addUpdateCountMsg(updateCount);
			}
			moreResults = this.currentStatement.getMoreResults();
			
			if (multipleUpdateCounts)
			{
				try
				{
					updateCount = this.currentStatement.getUpdateCount();
				}
				catch (Exception e)
				{
					LogMgr.logWarning("SqlCommand.processResult()", "Error when calling getUpdateCount() " + ExceptionUtil.getDisplay(e));
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

	/**
	 * 	The commands producing a result set need this flag.
	 * 	If no consumer is waiting, the can directly produce a DataStore
	 * 	for the result.
	 */
	public void setConsumerWaiting(boolean flag)
	{
		this.consumerWaiting = flag;
	}


	public boolean isConsumerWaiting()
	{
		return this.consumerWaiting;
	}

	public boolean isUpdatingCommand()
	{
		return this.isUpdatingCommand;
	}

	public void setQueryTimeout(int timeout)
	{
		this.queryTimeout = timeout;
	}

	public void setMaxRows(int max) { maxRows = max; }
	public boolean isResultSetConsumer() { return false; }
	public void consumeResult(StatementRunnerResult aResult) {}

	protected String evaluateFileArgument(String fileName)
	{
		if (StringUtil.isEmptyString(fileName)) return fileName;
		
		String fname = StringUtil.trimQuotes(fileName);
		File f  = new File(fname);
		if (f.isAbsolute()) return fname;
		
		// Use the "current" directory of the StatementRunner
		// for the path of the file, if no path is specified.
		if (this.runner != null)
		{
			String dir = this.runner.getBaseDir();
			if (!StringUtil.isEmptyString(dir))
			{
				f = new File(dir, fname);
				try
				{
					fname = f.getCanonicalPath();
				}
				catch (Exception e)
				{
				}
			}
		}
		return fname;
	}
	
}
