/*
 * WbToggleDisplay.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
package workbench.sql.wbcommands.console;

import java.sql.SQLException;

import workbench.RunMode;
import workbench.console.ConsoleSettings;
import workbench.console.RowDisplay;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

/**
 * A SQL command to control the output format in console mode.
 *
 * @author  Thomas Kellerer
 */
public class WbToggleDisplay extends SqlCommand
{
	public static final String VERB = "WbToggleDisplay";

	@Override
	public StatementRunnerResult execute(String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();

		RowDisplay current = ConsoleSettings.getInstance().getRowDisplay();
		RowDisplay newDisplay = null;
		if (current == RowDisplay.Form)
		{
			newDisplay = RowDisplay.SingleLine;
			result.addMessageByKey("MsgDispChangeRow");
		}
		else
		{
			newDisplay = RowDisplay.Form;
			result.addMessageByKey("MsgDispChangeForm");
		}
		ConsoleSettings.getInstance().setRowDisplay(newDisplay);
		result.setSuccess();
		return result;
	}

	@Override
	protected boolean isConnectionRequired()
	{
		return false;
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

	@Override
	public boolean isModeSupported(RunMode mode)
	{
		return mode != RunMode.GUI;
	}

	@Override
	public boolean isWbCommand()
	{
		return true;
	}

}
