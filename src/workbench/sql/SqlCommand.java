/*
 * SqlCommand.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.StringTokenizer;

import workbench.db.WbConnection;
import workbench.exception.ExceptionUtil;
import workbench.interfaces.ResultLogger;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.storage.DataStore;
import workbench.storage.RowActionMonitor;
import workbench.util.StringUtil;

/**
 *
 * @author  info@sql-workbench.net
 */
public class SqlCommand
{
	protected Statement currentStatement;
	protected WbConnection currentConnection;
	protected boolean isCancelled = false;
	private boolean consumerWaiting = false;
	protected RowActionMonitor rowMonitor;
	protected boolean isUpdatingCommand = false;
	protected ResultLogger resultLogger;
	protected StatementRunner runner;
	protected int queryTimeout = 0;

	/**
	 *	Checks if the verb of the given SQL script
	 *	is the same as registered for this SQL command.
	 */
	protected boolean checkVerb(String aSql)
		throws Exception
	{
		StringTokenizer tok = new StringTokenizer(aSql, " ");
		String verb = null;
		if (tok.hasMoreTokens()) verb = tok.nextToken();
		String thisVerb = this.getVerb();
		if (!thisVerb.equalsIgnoreCase(verb)) throw new Exception("Syntax error! " + thisVerb + " expected");
		return true;
	}

	public void setRowMonitor(RowActionMonitor monitor)
	{
		this.rowMonitor = monitor;
	}

	public void setResultLogger(ResultLogger logger)
	{
		this.resultLogger = logger;
	}

	protected void appendSuccessMessage(StatementRunnerResult result)
	{
		result.addMessage(this.getVerb() + " " + ResourceMgr.getString("MsgKnownStatementOK"));
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
		try
		{
			// some DBMS return warnings on the connection rather then on the
			// statement. We need to check them here as well. Then some of
			// the DBMS return the same warnings on the Statement AND the
			// Connection object.
			// For this we keep a list of warnings which have been added
			// from the statement. They will not be added when the Warnings from
			// the connection are retrieved
			ArrayList added = new ArrayList();

			String s = null;
			SQLWarning warn = aStmt.getWarnings();
			boolean hasWarnings = warn != null;
			while (warn != null)
			{
				s = warn.getMessage();
				if (s != null && s.length() > 0)
				{
					msg.append(s);
					if (!s.endsWith("\n")) msg.append('\n');
				}
				warn = warn.getNextWarning();
				added.add(s);
			}

			// if the statement has been cancelled Oracle
			// does not seem to like the getting of the
			// output messages...
			if (!this.isCancelled)
			{
				if (hasWarnings) msg.append('\n');

				s = aConn.getOutputMessages();
				if (s.length() > 0)
				{
					msg.append(s);
					if (!s.endsWith("\n")) msg.append("\n");
					hasWarnings = true;
				}
			}
			warn = aConn.getSqlConnection().getWarnings();
			hasWarnings = hasWarnings || (warn != null);
			while (warn != null)
			{
				s = warn.getMessage();
				if (!added.contains(s))
				{
					msg.append(s);
					if (!s.endsWith("\n")) msg.append('\n');
				}
				warn = warn.getNextWarning();
			}

			// make sure the warnings are cleared from both objects!
			aStmt.clearWarnings();
			aConn.clearWarnings();

			return hasWarnings;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	public void cancel()
		throws SQLException
	{
		this.isCancelled = true;
		if (this.currentStatement != null)
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

			if (this.currentConnection != null && this.currentConnection.cancelNeedsReconnect())
			{
				LogMgr.logInfo(this, "Cancelling needs a reconnect to the database for this DBMS...");
				this.currentConnection.reconnect();
			}
		}
	}

	public void done()
	{
		if (this.currentStatement != null)
		{
			try { this.currentStatement.close(); } catch (Throwable th) {}
			if (!this.isCancelled)
			{
				try { this.currentStatement.clearWarnings(); } catch (Throwable th) {}
				try { this.currentStatement.clearBatch(); } catch (Throwable th) {}
			}
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
		StatementRunnerResult result = new StatementRunnerResult(aSql);
		ResultSet rs = null;
		this.currentStatement = aConnection.createStatement();
		this.currentConnection = aConnection;
		this.isCancelled = false;

		try
		{
			boolean hasResult = this.currentStatement.execute(aSql);

			// Postgres obviously clears the warnings if the getMoreResults()
			// and stuff is called, so we add the warnings right at the beginning
			// this shouldn't affect other DBMSs (hopefully :-)
			StringBuffer warnings = new StringBuffer();
			if (appendWarnings(aConnection, this.currentStatement, warnings))
			{
				result.addMessage(warnings.toString());
			}
			int updateCount = -1;

			// fallback hack for JDBC drivers which do not reset the value
			// returned by getUpdateCount() after it is called
			int maxLoops = 25;
			int loopcounter = 0;

			DataStore ds = null;

			if (hasResult)
			{
				rs = this.currentStatement.getResultSet();
				ds = new DataStore(rs, aConnection);
				result.addDataStore(ds);
			}
			else
			{
				updateCount = this.currentStatement.getUpdateCount();
				//result.addUpdateCount(updateCount);
				if (updateCount > -1)
				{
					result.addMessage(updateCount + " " + ResourceMgr.getString(ResourceMgr.MSG_ROWS_AFFECTED));
				}
			}
			// we are not checking for further results as we
			// won't support them anyway :)

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

	public void setMaxRows(int maxRows) { }
	public boolean isResultSetConsumer() { return false; }
	public void consumeResult(StatementRunnerResult aResult) {}

}