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
import workbench.db.WbConnection;
import workbench.exception.ExceptionUtil;
import workbench.exception.WbException;
import workbench.resource.ResourceMgr;
import workbench.storage.DataStore;
import workbench.util.LineTokenizer;

/**
 *
 * @author  workbench@kellerer.org
 */
public class SqlCommand
{
	protected Statement currentStatement;
	
	/**
	 *	Checks if the verb of the given SQL script 
	 *	is the same as registered for this SQL command.
	 */
	protected boolean checkVerb(String aSql)
		throws WbException
	{
		LineTokenizer tok = new LineTokenizer(aSql, " ");
		String verb = tok.nextToken(); 
		String thisVerb = this.getVerb();
		if (!thisVerb.equalsIgnoreCase(verb)) throw new WbException("Syntax error! " + thisVerb + " expected");
		return true;
	}
	
	protected void appendSuccessMessage(StatementRunnerResult result)
	{
		result.addMessage(this.getVerb() + " " + ResourceMgr.getString("MsgKnownStatementOK"));
	}
	
	/**
	 *	Append any warnings from the Statement to the given 
	 *	StringBuffer. If the connection is a connection to Oracle
	 *	then any messages written with dbms_output are appended as well
	 */
	protected boolean appendWarnings(WbConnection aConn, Statement aStmt, StringBuffer msg)
	{
		try
		{
			SQLWarning warn = aStmt.getWarnings();
			boolean warnings = warn != null;
			while (warn != null)
			{
				msg.append('\n');
				msg.append(warn.getMessage());
				warn = warn.getNextWarning();
			}
			if (warnings) msg.append('\n');
			String outMsg = aConn.getOutputMessages();
			if (outMsg.length() > 0)
			{
				msg.append(outMsg);
				msg.append("\n\n");
				warnings = true;
			}
			return warnings;
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
			this.currentStatement.cancel();
		}
	}

	public void done()
	{
		try 
		{ 
			this.currentStatement.close(); 
		} 
		catch (Throwable th) 
		{
		}
	}
	
	/**
	 *	Should be overridden by a specialised SqlCommand 
	 */
	public StatementRunnerResult execute(WbConnection aConnection, String aSql) 	
		throws SQLException, WbException
	{
		StatementRunnerResult result = new StatementRunnerResult(aSql);
		ResultSet rs = null;
		this.currentStatement = aConnection.createStatement();
		
		try
		{
			boolean hasResult = this.currentStatement.execute(aSql);
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
				result.addUpdateCount(updateCount);
			}

			boolean moreResults = false; 
			
			moreResults = this.currentStatement.getMoreResults();

			while ( (moreResults || (updateCount != -1)) && (loopcounter < maxLoops) )
			{
				if (moreResults)
				{
					rs  = this.currentStatement.getResultSet();
					ds = new DataStore(rs, aConnection);
					result.addDataStore(ds);
				}

				if (updateCount > -1)
				{
					result.addMessage(updateCount + " " + ResourceMgr.getString(ResourceMgr.MSG_ROWS_AFFECTED));
				}
				moreResults = this.currentStatement.getMoreResults();
				updateCount = this.currentStatement.getUpdateCount();
			}			

			// we add the last ResultSet to the result object
			// as all the previous ResultSets will be closed
			// after calling getMoreResults();
			result.setSuccess();
		}
		catch (Exception e)
		{
			result.clear();
			result.addMessage(ResourceMgr.getString("MsgExecuteError"));
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
		return ""; 
	}

	public void setMaxRows(int maxRows) { }
	public boolean isResultSetConsumer() { return false; }
	public void consumeResult(StatementRunnerResult aResult) {}
}
