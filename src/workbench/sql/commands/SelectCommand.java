/*
 * SelectCommand.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.commands;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import workbench.db.WbConnection;
import workbench.util.ExceptionUtil;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

/**
 * @author  support@sql-workbench.net
 */
public class SelectCommand extends SqlCommand
{
	public static final String VERB = "SELECT";

	public SelectCommand()
	{
	}

	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException
	{
		this.isCancelled = false;

		StatementRunnerResult result = new StatementRunnerResult(aSql);
		result.setWarning(false);

		try
		{
			this.currentConnection = aConnection;
			boolean isPrepared = false;

			if (Settings.getInstance().getCheckPreparedStatements()
				  && aConnection.getPreparedStatementPool().isRegistered(aSql))
			{
				this.currentStatement = aConnection.getPreparedStatementPool().prepareStatement(aSql);
				if (this.currentStatement != null)
				{
					isPrepared = true;
				}
			}
			
			if (this.currentStatement == null)
			{
				this.currentStatement = aConnection.createStatementForQuery();
			}
			
			try 
			{ 
				if (this.queryTimeout >= 0 && aConnection.supportsQueryTimeout())
				{
					this.currentStatement.setQueryTimeout(this.queryTimeout); 
				}
			} 
			catch (Throwable th) 
			{
				LogMgr.logWarning("SelectCommand.execute()", "Error when setting query timeout", th);
			}

			try
			{
				this.currentStatement.setMaxRows(this.maxRows);
			}
			catch (Exception e)
			{
				LogMgr.logWarning("SelectCommand.execute()", "The JDBC driver does not support the setMaxRows() function! (" +e.getMessage() + ")");
			}

			ResultSet rs = null;
			if (isPrepared)
			{
				rs = ((PreparedStatement)this.currentStatement).executeQuery();
			}
			else
			{
				rs = this.currentStatement.executeQuery(aSql);
			}

			if (rs != null)
			{
				// if a ResultSetConsumer is waiting, we have to store the
				// result set, so that not all the data is read into memory
				// when exporting data
				// If the result set is not consumed, we can create the DataStore
				// right away. This is necessary, because with Oracle, the stream to
				// read LONG columns would be closed, if any other statement
				// is executed before the result set is retrieved.
				// (The result set itself can be retrieved but access to the LONG columns
				// would cause an error)
				if (this.isConsumerWaiting())
				{
					result.addResultSet(rs);
					StringBuilder warnings = new StringBuilder();
					if (this.appendWarnings(aConnection, this.currentStatement, warnings))
					{
						result.addMessage(warnings);
					}
				}
				else
				{
					processResults(result, true, rs);
				}

				if (!isCancelled)
				{
					this.appendSuccessMessage(result);
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
		catch (Exception e)
		{
			result.clear();
			result.addMessage(ResourceMgr.getString("MsgExecuteError"));
			result.addMessage(ExceptionUtil.getAllExceptions(e));

			StringBuilder warnings = new StringBuilder();
			if (this.appendWarnings(aConnection, this.currentStatement, warnings))
			{
				result.addMessage(warnings.toString());
			}
			LogMgr.logSqlError("SelectCommand.execute()", aSql, e);
			result.setFailure();
		}

		return result;
	}

	public String getVerb()
	{
		return VERB;
	}

	public void setMaxRows(int max)
	{
		if (max >= 0)
			this.maxRows = max;
		else
			this.maxRows = 0;
	}

}
