/*
 * WbListProfiles.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.StringUtil;


/**
 * List all defined profiles
 *
 * @author Thomas Kellerer
 */
public class WbDefineMacro
	extends SqlCommand
{
	public static final String VERB = "WBDEFINEMACRO";

	public static final String ARG_NAME = "name";
	public static final String ARG_TEXT = "text";
	public static final String ARG_FILE = "file";

	public WbDefineMacro()
	{
		super();
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(ARG_NAME);
		cmdLine.addArgument(ARG_TEXT);
		cmdLine.addArgument(ARG_FILE, ArgumentType.Filename);
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

		cmdLine.parse(getCommandLine(sql));
		String macroText = cmdLine.getValue(ARG_TEXT);
		String fname = cmdLine.getValue(ARG_FILE);
		String macroName = cmdLine.getValue(ARG_NAME);

		if (StringUtil.isBlank(macroName))
		{
			result.setFailure();
			result.addMessage("ErrMacroNameReq");
			return result;
		}
		result.setSuccess();
		return result;
	}

}
