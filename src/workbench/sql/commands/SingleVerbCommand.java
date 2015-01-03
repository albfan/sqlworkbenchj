/*
 * SingleVerbCommand.java
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

import java.sql.SQLException;

import workbench.log.LogMgr;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

/**
 * Handles COMMIT and ROLLBACK
 *
 * @author Thomas Kellerer
 */
public class SingleVerbCommand extends SqlCommand
{
	public static final String COMMIT_VERB = "COMMIT";
	public static final String ROLLBACK_VERB = "ROLLBACK";

	public static SqlCommand getCommit()
	{
		return new SingleVerbCommand(COMMIT_VERB);
	}

	public static SqlCommand getRollback()
	{
		return new SingleVerbCommand(ROLLBACK_VERB);
	}

	private String verb;

	private SingleVerbCommand(String sqlVerb)
	{
		super();
		this.verb = sqlVerb;
		this.isUpdatingCommand = COMMIT_VERB.equalsIgnoreCase(this.verb);
	}

	@Override
	public StatementRunnerResult execute(String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult(aSql);
		try
		{
			if (currentConnection.useJdbcCommit())
			{
				if (COMMIT_VERB.equals(this.verb))
				{
					currentConnection.getSqlConnection().commit();
				}
				else if (ROLLBACK_VERB.equals(this.verb))
				{
					currentConnection.getSqlConnection().rollback();
				}
			}
			else
			{
				this.currentStatement = currentConnection.createStatement();
				this.currentStatement.execute(verb);
			}

			appendSuccessMessage(result);
			result.setSuccess();
			processMoreResults(aSql, result, false);
		}
		catch (Exception e)
		{
			addErrorInfo(result, aSql, e);
			LogMgr.logUserSqlError("SingleVerbCommand.execute()", aSql, e);
		}
		finally
		{
			done();
		}

		return result;
	}

	@Override
	public String getVerb()
	{
		return verb;
	}

}
