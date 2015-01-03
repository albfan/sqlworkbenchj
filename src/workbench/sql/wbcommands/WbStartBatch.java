/*
 * WbStartBatch.java
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
package workbench.sql.wbcommands;

import java.sql.SQLException;
import java.sql.Statement;

import workbench.resource.ResourceMgr;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

/**
 * @author  Thomas Kellerer
 */
public class WbStartBatch
	extends SqlCommand
{
	public static final String VERB = "WbStartBatch";
	private Statement batch;

	@Override
	public String getVerb()
	{
		return VERB;
	}

	@Override
	public StatementRunnerResult execute(String aSql)
		throws SQLException, Exception
	{
		StatementRunnerResult result = new StatementRunnerResult(aSql);
		if (!currentConnection.getMetadata().supportsBatchUpdates())
		{
			this.batch = null;
			result.setFailure();
			result.addMessage(ResourceMgr.getString("ErrJdbcBatchUpdateNotSupported"));
		}
		else
		{
			this.batch = currentConnection.createStatement();
			result.setSuccess();
			result.addMessage(ResourceMgr.getString("MsgJdbcBatchProcessingStarted"));
		}
		return result;
	}

	public void addStatement(String sql)
		throws SQLException
	{
		if (this.currentStatement == null) throw new SQLException("Batch mode not supported");
		this.batch.addBatch(sql);
	}

	public StatementRunnerResult executeBatch()
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult("ExecuteBatch");
		if (this.batch == null)
		{
			result.setFailure();
			result.addMessage(ResourceMgr.getString("ErrJdbcBatchUpdateNotSupported"));
			return result;
		}
		long totalRows = 0;
		result.setSuccess();
		result.addMessage(ResourceMgr.getString("MsgJdbcBatchProcessingEnded"));

		int[] rows = this.batch.executeBatch();
		if (rows == null || rows.length == 0)
		{
			result.addMessage(ResourceMgr.getString("MsgJdbcBatchStatementNoInfo"));
		}
		else
		{
			for (int i=0; i < rows.length; i++)
			{
				if (rows[i] != Statement.EXECUTE_FAILED)
				{
					String msg = ResourceMgr.getString("MsgJdbcBatchStatementFailed");
					result.addMessage(msg.replace("%num%", Integer.toString(i)));
				}
				else if (rows[i] == Statement.SUCCESS_NO_INFO)
				{
					String msg = ResourceMgr.getString("MsgJdbcBatchStatementNoStatementInfo");
					result.addMessage(msg.replace("%num%", Integer.toString(i)));
				}
				else
				{
					totalRows += rows[i];
				}
			}
			String msg = ResourceMgr.getString("MsgJdbcBatchTotalRows") + " " + Long.toString(totalRows);
			result.addMessage(msg);
		}
		return result;
	}

	@Override
	public boolean isWbCommand()
	{
		return true;
	}

}
