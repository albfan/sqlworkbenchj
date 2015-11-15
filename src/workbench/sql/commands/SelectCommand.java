/*
 * SelectCommand.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.commands;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.LowMemoryException;
import workbench.util.SqlUtil;

/**
 * Implementation of the SELECT statement.
 *
 * The result of the SELECT is passed back in the StatementRunnerResult object.
 * If a ResultSetConsumer is registered in the StatementRunner instance that is executing this
 * select, the ResultSet will be returned directly, otherwise the ResultSet will be completely
 * read into a DataStore (see {@link SqlCommand#processResults(workbench.sql.StatementRunnerResult, boolean)}.
 *
 * @author Thomas Kellerer
 *
 */
public class SelectCommand
	extends SqlCommand
{
	public static final String VERB = "SELECT";

	/**
	 * Runs the passed SQL statement using Statement.executeQuery()
	 *
	 * @param sql the statement to execute
	 * @return the result of the execution
	 * @throws java.sql.SQLException
	 */
	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException
	{
		this.isCancelled = false;

		StatementRunnerResult result = new StatementRunnerResult(sql);

		try
		{
			boolean isPrepared = false;

      if (runner.useSavepointForDML())
      {
        runner.setSavepoint();
      }

			if (Settings.getInstance().getCheckPreparedStatements() && currentConnection.getPreparedStatementPool().isRegistered(sql))
			{
				this.currentStatement = currentConnection.getPreparedStatementPool().prepareStatement(sql);
				if (this.currentStatement != null)
				{
					isPrepared = true;
				}
			}

			if (this.currentStatement == null)
			{
				this.currentStatement = currentConnection.createStatementForQuery();
			}

			try
			{
				if (this.queryTimeout > 0 && currentConnection.supportsQueryTimeout())
				{
					LogMgr.logTrace("SelectCommand.execute()", "Setting query timeout to: " + this.queryTimeout);
					this.currentStatement.setQueryTimeout(this.queryTimeout);
				}
			}
			catch (Exception th)
			{
				LogMgr.logWarning("SelectCommand.execute()", "Error when setting query timeout: " + th.getMessage(), null);
			}

			try
			{
//				LogMgr.logTrace("SelectCommand.execute()", "Setting maxrows to: " + maxRows);
				this.currentStatement.setMaxRows(this.maxRows);
			}
			catch (Exception e)
			{
				LogMgr.logWarning("SelectCommand.execute()", "The JDBC driver does not support the setMaxRows() function! (" +e.getMessage() + ")");
			}

			ResultSet rs = null;
			boolean hasResult = true;

			// we can safely remove the comments here, as the StatementRunnerResult was already
			// initialized with the "real" statement that contains the comments (and which potentially
			// can contain a "wbdoc" to identify the result tab
			sql = getSqlToExecute(sql);

			if (isPrepared)
			{
				rs = ((PreparedStatement)this.currentStatement).executeQuery();
			}
			else
			{
				if (currentConnection.getDbSettings().getUseGenericExecuteForSelect())
				{
					LogMgr.logDebug("SelectCommand.execute()", "Using execute() instead of executeQuery()");
					hasResult = this.currentStatement.execute(sql);
				}
				else
				{
					rs = this.currentStatement.executeQuery(sql);
				}
			}

			if (this.isCancelled)
			{
				result.addMessage(ResourceMgr.getString("MsgStatementCancelled"));
				result.setFailure();
			}
			else
			{
				processResults(result, hasResult, rs);

				if (isCancelled)
				{
					result.addMessage(ResourceMgr.getString("MsgStatementCancelled"));
				}
				else
				{
					this.appendSuccessMessage(result);
				}
				result.setSuccess();
			}

			this.runner.releaseSavepoint();
		}
		catch (LowMemoryException mem)
		{
			result.clear();
			result.setFailure();
			this.runner.rollbackSavepoint();
			throw mem;
		}
		catch (Exception e)
		{
			addErrorInfo(result, sql, e);

			// this config is only for MySQL because it repeats the error message
			// as a warning on the statement instance
			if (currentConnection.getDbSettings().addWarningsOnError())
			{
				appendWarnings(result, true);
			}
			else
			{
				SqlUtil.clearWarnings(currentConnection, currentStatement);
			}
			LogMgr.logUserSqlError("SelectCommand.execute()", sql, e);
			this.runner.rollbackSavepoint();
		}

		return result;
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

	@Override
	public void setMaxRows(int max)
	{
		if (max >= 0)
		{
			this.maxRows = max;
		}
		else
		{
			this.maxRows = 0;
		}
	}

}
