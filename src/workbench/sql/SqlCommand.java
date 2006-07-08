/*
 * SqlCommand.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
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
import workbench.util.ExceptionUtil;
import workbench.interfaces.ResultLogger;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.storage.DataStore;
import workbench.storage.RowActionMonitor;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
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

	public void setRowMonitor(RowActionMonitor monitor)
	{
		this.rowMonitor = monitor;
	}

	public void setResultLogger(ResultLogger logger)
	{
		this.resultLogger = logger;
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
	 *	StringBuffer. If the connection is a connection to Oracle
	 *	then any messages written with dbms_output are appended as well
	 *  This behaviour is then similar to MS SQL Server where any messages
	 *  displayed using the PRINT function are returned in the Warnings as well.
	 */
	protected boolean appendWarnings(WbConnection aConn, Statement aStmt, StringBuffer msg)
	{
		String warn = SqlUtil.getWarnings(aConn, aStmt, !this.isCancelled);
		boolean hasWarning = false;
		if (warn != null && warn.trim().length() > 0)
		{
			hasWarning = true;
			msg.append(warn);
		}
		return hasWarning;
	}

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

//			if (this.currentConnection != null && this.currentConnection.cancelNeedsReconnect())
//			{
//				LogMgr.logInfo(this, "Cancelling needs a reconnect to the database for this DBMS...");
//				this.currentConnection.reconnect();
//			}
		}
	}

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

	/**
	 *	Should be overridden by a specialised SqlCommand
	 */
	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException, Exception
	{
		String clean = SqlUtil.makeCleanSql(aSql,false,false,'\'');
		StatementRunnerResult result = new StatementRunnerResult(aSql);
		if (clean.length() == 0) 
		{
			result.addMessage(ResourceMgr.getString("MsgWarningEmptySqlIgnored"));
			result.setWarning(true);
			result.setSuccess();
			return result;
		}
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
			StringBuffer msg = new StringBuffer(150);
			msg.append(ResourceMgr.getString("MsgExecuteError") + "\n");
			String s = StringUtil.getMaxSubstring(aSql.trim(), 150);
			msg.append(s);
			msg.append("\n");
			result.addMessage(msg.toString());
			String er = ExceptionUtil.getDisplay(e);
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
		StringBuffer warnings = new StringBuffer();
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
					}
					catch (SQLException e)
					{
						// Errors during loading should not throw away the
						// rows retrieved until then
						if (this.currentRetrievalData != null && this.currentRetrievalData.getRowCount() > 0)
						{
							result.addMessage(ResourceMgr.getString("MsgErrorDuringRetrieve"));
							result.addMessage(ExceptionUtil.getDisplay(e));
							result.setWarning(true);
						}
					}
					result.addDataStore(this.currentRetrievalData);
				}
			}
			else if (updateCount > -1)
			{			
				result.addUpdateCountMsg(updateCount);
			}
			moreResults = this.currentStatement.getMoreResults();
			updateCount = this.currentStatement.getUpdateCount();
			
			counter ++;
			// some JDBC drivers do not implement getMoreResults() and getUpdateCount()
			// correctly, so this is a safety to prevent an endless loop
			if (counter > 50) break;
			try { rs.close(); } catch (Throwable th) {}
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
	public void preConsume(SqlCommand producer) {}
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
