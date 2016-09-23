/*
 * WbListProfiles.java
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

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.sql.macros.MacroDefinition;
import workbench.sql.macros.MacroManager;
import workbench.sql.macros.MacroStorage;

import workbench.util.ArgumentParser;
import workbench.util.StringUtil;


/**
 * Create a new macro.
 *
 * @author Thomas Kellerer
 */
public class WbDeleteMacro
	extends SqlCommand
{
	public static final String VERB = "WbDeleteMacro";

	public static final String ARG_NAME = "name";

	public WbDeleteMacro()
	{
		super();
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(ARG_NAME);
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
	public StatementRunnerResult execute(String sql)
		throws SQLException, Exception
	{
		StatementRunnerResult result = new StatementRunnerResult();

		String clean = getCommandLine(sql);
		cmdLine.parse(clean);

		String macroName = null;
		if (cmdLine.hasArguments())
		{
			macroName = cmdLine.getValue(ARG_NAME);
		}
		else
		{
			macroName = StringUtil.trim(clean);
		}

		if (StringUtil.isBlank(macroName))
		{
			result.setFailure();
			result.addMessageByKey("ErrMacroNameReq");
			return result;
		}

		MacroStorage storage = MacroManager.getInstance().getMacros(getMacroClientId());
		MacroDefinition macro = storage.getMacro(macroName);

		if (macro != null)
		{
			storage.removeMacro(macro);

			MacroManager.getInstance().save();
			result.addMessageByKey("MsgMacroDeleted", macro.getName());
			result.setSuccess();
		}
		else
		{
			result.addMessageByKey("MsgMacroNotFound", macroName);
			result.setFailure();
		}

		return result;
	}

	@Override
	public boolean isWbCommand()
	{
		return true;
	}

}
