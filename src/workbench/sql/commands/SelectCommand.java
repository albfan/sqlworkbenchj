/*
 * SelectCommand.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.sql.commands;

import java.sql.ResultSet;
import java.sql.SQLException;

import workbench.db.WbConnection;
import workbench.exception.ExceptionUtil;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.storage.DataStore;

/**
 *
 * @author  info@sql-workbench.net
 */
public class SelectCommand extends SqlCommand
{

	public static final String VERB = "SELECT";
	private int maxRows = 0;
	private DataStore data;

	public SelectCommand()
	{
	}

	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException
	{
		this.data = null;
		this.isCancelled = false;

		StatementRunnerResult result = new StatementRunnerResult(aSql);
		result.setWarning(false);

		try
		{
			this.currentConnection = aConnection;
			this.currentStatement = aConnection.createStatement();
			try { this.currentStatement.setQueryTimeout(this.queryTimeout); } catch (Throwable th) {}
			
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
				fetchSize = Settings.getInstance().getDefaultFetchSize();
			}
			catch (Exception e)
			{
				fetchSize = 0;
			}
			if (fetchSize > 0)
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
				if (this.isConsumerWaiting())
				{
					result.addResultSet(rs);
				}
				else
				{
					try
					{
						// An exception in the constructor should leead to a real error
						this.data = new DataStore(rs, false, this.rowMonitor, maxRows, this.currentConnection);
						try
						{
							// Not reading the data in the constructor enables us
							// to cancel the retrieval of the data from the ResultSet
							// without using statement.cancel()
							// The DataStore checks for the cancel flag during processing
							// of the ResulSet
							this.data.initData(rs, maxRows);
						}
						catch (SQLException e)
						{
							// Errors during loading should not throw away the
							// rows retrieved until then
							if (this.data != null && this.data.getRowCount() > 0)
							{
								result.addMessage(ResourceMgr.getString("MsgErrorDuringRetrieve"));
								result.addMessage(ExceptionUtil.getDisplay(e));
								result.setWarning(true);
							}
						}
					}
					finally
					{
						try { rs.close(); } catch (Throwable th) {}
					}

					if (data != null)
					{
						result.addDataStore(data);
					}
				}

				if (!isCancelled)
				{
					StringBuffer warnings = new StringBuffer();
					this.appendSuccessMessage(result);
					if (this.appendWarnings(aConnection, this.currentStatement, warnings))
					{
						result.addMessage(warnings.toString());
					}
				}
				else
				{
					result.addMessage(ResourceMgr.getString("MsgStatementCancelled"));
				}
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
			if (e instanceof SQLException)
			{
				LogMgr.logDebug("SelectCommand.execute()", "Error executing statement: " + ExceptionUtil.getDisplay(e));
			}
			else
			{
				LogMgr.logError("SelectCommand.execute()", "Error executing statement", e);
			}
			result.setFailure();
		}

		return result;
	}

	public void cancel()
		throws SQLException
	{
		if (this.data != null)
		{
			this.data.cancelRetrieve();
		}
		else
		{
			super.cancel();
		}
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