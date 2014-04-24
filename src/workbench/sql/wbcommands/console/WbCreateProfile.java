/*
 * WbStoreProfile.java
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

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;

/**
 *
 * @author Thomas Kellerer
 */
public class WbCreateProfile
	extends SqlCommand
{
	public static final String VERB = "WbCreateProfile";

	public WbCreateProfile()
	{
		super();
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(WbStoreProfile.ARG_PROFILE_NAME);
		cmdLine.addArgument(WbStoreProfile.ARG_SAVE_PASSWORD, ArgumentType.BoolArgument);
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException, Exception
	{
		StatementRunnerResult result = new StatementRunnerResult();
		result.addMessage("Not yet implemented");
		result.setFailure();
		return result;
	}

}
