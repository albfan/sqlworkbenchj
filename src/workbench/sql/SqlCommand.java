/*
 * SqlCommand.java
 *
 * Created on 16. November 2002, 15:31
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
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.storage.DataStore;
import workbench.storage.RowActionMonitor;
import workbench.util.StringUtil;

/**
 *
 * @author  workbench@kellerer.org
 */
public class SqlCommand
{
	protected Statement currentStatement;
	protected WbConnection currentConnection;
	protected boolean isCancelled = false;
	private boolean consumerWaiting = false;
	protected RowActionMonitor rowMonitor;

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
			if (hasWarnings) msg.append('\n');
			s = aConn.getOutputMessages();
			if (s.length() > 0)
			{
				msg.append(s);
				if (!s.endsWith("\n")) msg.append("\n");
				hasWarnings = true;
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
		if (this.currentStatement != null)
		{
			this.isCancelled = true;
			this.currentStatement.cancel();
			this.currentStatement.close();
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
			try { this.currentStatement.clearWarnings(); } catch (Throwable th) {}
			try { this.currentStatement.clearBatch(); } catch (Throwable th) {}
			try { this.currentStatement.close(); } catch (Throwable th) {}
		}
		this.currentStatement = null;
		this.isCancelled = false;
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
			LogMgr.logDebug("SqlCommand.execute()", "Error executing sql statement", e);
			result.clear();
			StringBuffer msg = new StringBuffer(50);
			msg.append(ResourceMgr.getString("MsgExecuteError") + ":\n----------\n");
			int maxLen = 80;
			if (aSql.trim().length() > maxLen)
			{
				msg.append(aSql.trim().substring(0, maxLen));
				msg.append(" ...");
			}
			else
			{
				msg.append(aSql.trim());
			}
			msg.append("\n----------\n");
			result.addMessage(msg.toString());
			result.addMessage(ExceptionUtil.getDisplay(e));
			result.setFailure();
		}
		finally
		{
			this.done();
		}
		return result;
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

	public boolean getConsumerWaiting()
	{
		return this.consumerWaiting;
	}

	public void setMaxRows(int maxRows) { }
	public boolean isResultSetConsumer() { return false; }
	public void consumeResult(StatementRunnerResult aResult) {}

}