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
import java.util.Set;

import workbench.RunMode;
import workbench.console.ConsoleSettings;
import workbench.console.RowDisplay;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.CollectionUtil;
import workbench.util.StringUtil;

/**
 * A SQL command to control the output format in console mode.
 *
 * @author  Thomas Kellerer
 */
public class WbDisplay
	extends SqlCommand
{
	public static final String VERB = "WbDisplay";
  private final Set<String> tabularKeywords = CollectionUtil.caseInsensitiveSet("tab", "tabular", "row");
  private final Set<String> recordKeywords = CollectionUtil.caseInsensitiveSet("record", "form", "single");

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		String param = StringUtil.trim(getCommandLine(sql));

    if (StringUtil.isBlank(param))
    {
			RowDisplay current = ConsoleSettings.getInstance().getRowDisplay();
			String currentDisp = "tab";

			if (current == RowDisplay.Form)
			{
				currentDisp = "record";
			}

			result.setSuccess();
			String msg = ResourceMgr.getFormattedString("ErrDispWrongArgument", currentDisp);
			result.addMessage(msg);
    }
    else if (tabularKeywords.contains(param))
		{
			result.setSuccess();
			ConsoleSettings.getInstance().setRowDisplay(RowDisplay.SingleLine);
			result.addMessageByKey("MsgDispChangeRow");
		}
		else if (recordKeywords.contains(param))
		{
			ConsoleSettings.getInstance().setRowDisplay(RowDisplay.Form);
			result.addMessageByKey("MsgDispChangeForm");
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
