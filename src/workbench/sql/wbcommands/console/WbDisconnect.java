/*
 * WbDisconnect.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.sql.wbcommands.console;

import java.sql.SQLException;

import workbench.RunMode;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

/**
 *
 * @author Thomas Kellerer
 */
public class WbDisconnect
	extends SqlCommand
{
	public static final String VERB = "WbDisconnect";

	public WbDisconnect()
	{
		super();
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

	@Override
	public StatementRunnerResult execute(String aSql)
		throws SQLException, Exception
	{
		StatementRunnerResult result = new StatementRunnerResult();
		if (this.currentConnection != null)
		{
			//this.currentConnection.disconnect();
			// setConnection will call disconnect() on the "old" connection
			this.runner.setConnection(null);
			result.addMessageByKey("MsgDisconnected");
			result.setSuccess();
		}
		else
		{
			result.addMessageByKey("TxtNotConnected");
			result.setFailure();
		}
		return result;
	}

	@Override
	public boolean isModeSupported(RunMode mode)
	{
		return mode == RunMode.Console;
	}

	@Override
	public boolean isWbCommand()
	{
		return true;
	}

}
