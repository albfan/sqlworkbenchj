/*
 * WbConnect.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;


/**
 * Restore the original connection in a script.
 * <br>
 * @author Thomas Kellerer
 */
public class WbRestoreConnection
	extends SqlCommand
{
	public static final String VERB = "WbRestoreConnection";

	public WbRestoreConnection()
	{
		super();
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

	@Override
	protected boolean isConnectionRequired()
	{
		return false;
	}

	@Override
	public StatementRunnerResult execute(String aSql)
		throws SQLException, Exception
	{
		StatementRunnerResult result = new StatementRunnerResult();
    boolean restored = runner.restoreMainConnection();
    if (restored)
    {
      result.addMessageByKey("MsgConnectedTo", runner.getConnection().getDisplayString());
    }
    else
    {
      result.addMessageByKey("MsgConnNotRestored", runner.getConnection().getDisplayString());
    }
    result.setSuccess();

		return result;
	}

	@Override
	public boolean isWbCommand()
	{
		return true;
	}

}
