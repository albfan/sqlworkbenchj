/*
 * SelectCommand.java
 *
 * Created on 16. November 2002, 16:40
 */

package workbench.sql.commands;

import java.sql.ResultSet;
import java.sql.SQLException;

import workbench.WbManager;
import workbench.db.WbConnection;
import workbench.exception.ExceptionUtil;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.storage.DataStore;

/**
 *
 * @author  workbench@kellerer.org
 */
public class SelectCommand extends SqlCommand
{

	public static final String VERB = "SELECT";
	private int maxRows = 0;

	public SelectCommand()
	{
	}

	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult(aSql);
		try
		{
			this.currentConnection = aConnection;
			this.currentStatement = aConnection.createStatement();
			try
			{
				this.currentStatement.setMaxRows(this.maxRows);
			}
			catch (Exception e)
			{
				LogMgr.logWarning("SelectCommand.execute()", "The JDBC driver does not support the setMaxRows() function! (" +e.getMessage() + ")");
			}
			int fetchSize = 0;
			try
			{
				fetchSize = WbManager.getSettings().getDefaultFetchSize();
			}
			catch (Exception e)
			{
				fetchSize = 0;
			}
			if (fetchSize > 0 && fetchSize < this.maxRows)
			{
				try
				{
					this.currentStatement.setFetchSize(fetchSize);
				}
				catch (Throwable th)
				{
					LogMgr.logWarning("SelectCommand.execute()", "The JDBC driver does not support the setFetchSize() function! (" +th.getMessage() + ")");
				}
			}

			this.isCancelled = false;
			ResultSet rs = this.currentStatement.executeQuery(aSql);

			if (rs != null)
			{
				// if a ResultSetConsumer is waiting, we have to store the
				// result set, so that not all the data is read into memory
				// when exporting data
				// If the result set is not consume, we can create the DataStore
				// right away. This is necessary, because with Oracle, the stream to
				// read LONG columns would be closed, if any other statement
				// is executed before the result set is retrieved.
				// (The result set itself can be retrieved but access to the LONG columns
				// would cause an error)
				if (this.getConsumerWaiting())
				{
					result.addResultSet(rs);
				}
				else
				{
					DataStore ds = new DataStore(rs, true, this.rowMonitor, maxRows);
					result.addDataStore(ds);
					try
					{
						rs.close();
					}
					catch (Exception e)
					{
						LogMgr.logError("SelectCommand.execute()", "Error closing result set", e);
					}
				}

				StringBuffer warnings = new StringBuffer();

				this.appendSuccessMessage(result);
				if (this.appendWarnings(aConnection, this.currentStatement, warnings))
				{
					result.addMessage(warnings.toString());
				}
				if (this.isCancelled) result.addMessage(ResourceMgr.getString("MsgStatementCancelled"));
				result.setSuccess();
			}
			else if (this.isCancelled)
			{
				result.addMessage(ResourceMgr.getString("MsgStatementCancelled"));
				result.setFailure();
			}
			else
			{
				throw new Exception(ResourceMgr.getString("MsgReceivedNullResultSet"));
			}
		}
		catch (Throwable e)
		{
			result.clear();
			result.addMessage(ResourceMgr.getString("MsgExecuteError"));
			result.addMessage(ExceptionUtil.getDisplay(e));

			StringBuffer warnings = new StringBuffer();
			if (this.appendWarnings(aConnection, this.currentStatement, warnings))
			{
				result.addMessage(warnings.toString());
			}
			LogMgr.logDebug("SelectCommand.execute()", "Error executing statement", e);
			result.setFailure();
		}

		// we cannot close the statement here, as the result will be processed
		// somewhere else! (e.g. Export command, DwPanel to display the result!

		return result;
	}

	public String getVerb()
	{
		return VERB;
	}

	public boolean supportsMaxRows()
	{
		return true;
	}

	public void setMaxRows(int max)
	{
		if (max >= 0)
			this.maxRows = max;
		else
			this.maxRows = 0;
	}

}