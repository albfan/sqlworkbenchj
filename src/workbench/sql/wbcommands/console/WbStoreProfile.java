/*
 * WbStoreProfile.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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

import workbench.resource.ResourceMgr;

import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.DbDriver;

import workbench.gui.profiles.ProfileKey;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class WbStoreProfile
	extends SqlCommand
{
	public static final String VERB = "WbStoreProfile";
	public static final String ARG_PROFILE_NAME = "name";
	public static final String ARG_SAVE_PASSWORD = "savePassword";
	public WbStoreProfile()
	{
		super();
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(ARG_PROFILE_NAME);
		cmdLine.addArgument(ARG_SAVE_PASSWORD, ArgumentType.BoolArgument);
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
		String arguments = getCommandLine(sql);
		cmdLine.parse(arguments);

		String name = null;
		boolean storePwd = false;

		if (cmdLine.hasArguments())
		{
			name = cmdLine.getValue(ARG_PROFILE_NAME);
			storePwd = cmdLine.getBoolean(ARG_SAVE_PASSWORD, false);
		}
		else
		{
			name = arguments;
		}

		if (this.currentConnection == null)
		{
			result.addErrorMessageByKey("TxtNotConnected");
			return result;
		}

		if (StringUtil.isBlank(name))
		{
			result.addErrorMessageByKey("ErrNoProfile");
			return result;
		}

		ProfileKey key = new ProfileKey(name);

		ConnectionProfile profile = this.currentConnection.getProfile().createCopy();
		profile.setName(key.getName());
		profile.setGroup(key.getGroup());
		if (storePwd)
		{
			profile.setStorePassword(true);
		}
		else
		{
			profile.setPassword(null);
			profile.setStorePassword(false);
		}
		profile.setWorkspaceFile(null);

		ConnectionMgr.getInstance().addProfile(profile);
		ConnectionMgr.getInstance().saveProfiles();
		result.addMessage(ResourceMgr.getFormattedString("MsgProfileAdded", key.toString()));

		DbDriver drv = ConnectionMgr.getInstance().findDriver(profile.getDriverclass());

		// if a profile was created from the commandline, this will implicitely also create
		// a new driver entry if no matching driver was found. In that case it is marked as "temporary"
		if (drv.isTemporaryDriver())
		{
			DbDriver newDrv = drv.createCopy();
			String drvName = currentConnection.getSqlConnection().getMetaData().getDriverName();
			newDrv.setName(drvName);
			newDrv.setSampleUrl(profile.getUrl());
			profile.setDriverName(drvName);
			ConnectionMgr.getInstance().getDrivers().add(newDrv);
			ConnectionMgr.getInstance().saveDrivers();
			result.addMessage(ResourceMgr.getFormattedString("MsgDriverAdded", drvName));
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
