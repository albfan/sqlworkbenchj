/*
 * WbDisplay.java
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
package workbench.sql.wbcommands.console;

import java.sql.SQLException;

import workbench.RunMode;
import workbench.console.ConsoleSettings;
import workbench.resource.Settings;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.StringUtil;

/**
 * A SQL command to control the maximum output length for values in console mode.
 *
 * @author  Thomas Kellerer
 */
public class WbSetDisplaySize
	extends SqlCommand
{
	public static final String VERB = "WbSetDisplaySize";

	@Override
	public StatementRunnerResult execute(String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		String param = getCommandLine(aSql);
    if (StringUtil.isBlank(param))
    {
      String size = Settings.getInstance().getProperty(ConsoleSettings.PROP_MAX_DISPLAY_SIZE, "");
      result.addMessage("Maximum display length is: " + size);
    }
    else if (StringUtil.isNumber(param))
    {
      Settings.getInstance().setProperty(ConsoleSettings.PROP_MAX_DISPLAY_SIZE, param);
      result.addMessage("Maximum display length set to: " + param);
    }
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
