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

import workbench.AppArguments;
import workbench.db.ConnectionProfile;
import workbench.sql.BatchRunner;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.StringUtil;

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
		cmdLine.addArgument(AppArguments.ARG_CONN_AUTOCOMMIT);
		cmdLine.addArgument(AppArguments.ARG_CONN_DRIVER_CLASS);
		cmdLine.addArgument(AppArguments.ARG_CONN_PWD);
		cmdLine.addArgument(AppArguments.ARG_CONN_URL);
		cmdLine.addArgument(AppArguments.ARG_CONN_USER);
		cmdLine.addArgument(AppArguments.ARG_CONN_FETCHSIZE);
		cmdLine.addArgument(AppArguments.ARG_CONN_EMPTYNULL);
		cmdLine.addArgument(AppArguments.ARG_WORKSPACE);
		cmdLine.addArgument(AppArguments.ARG_ALT_DELIMITER);
		cmdLine.addArgument(AppArguments.ARG_CONN_SEPARATE);
		cmdLine.addArgument(AppArguments.ARG_CONN_TRIM_CHAR);
		cmdLine.addArgument(AppArguments.ARG_CONN_REMOVE_COMMENTS);
		cmdLine.addArgument(AppArguments.ARG_READ_ONLY);
		cmdLine.addArgument(AppArguments.ARG_CONN_PROPS);
		cmdLine.addArgument("driverName");
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

		cmdLine.parse(getCommandLine(sql));
		String name = cmdLine.getValue(WbStoreProfile.ARG_PROFILE_NAME);
		if (StringUtil.isBlank(name))
		{
			result.addMessage("Profile name required");
			result.setFailure();
			return result;
		}

		ConnectionProfile profile = BatchRunner.createCmdLineProfile(cmdLine);
		if (profile == null)
		{
			result.addMessage("Invalid arguments");
			result.setFailure();
			return result;
		}

		profile.setTemporaryProfile(false);
		profile.setName(name);
		boolean savePwd = cmdLine.getBoolean(WbStoreProfile.ARG_SAVE_PASSWORD, true);
		profile.setStorePassword(savePwd);

		result.addMessage("Not yet implemented");
		result.setFailure();
		return result;
	}

}
