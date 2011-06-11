/*
 * WbStoreProfile.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands.console;

import java.sql.SQLException;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.DbDriver;
import workbench.gui.profiles.ProfileKey;
import workbench.resource.ResourceMgr;
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
	public static final String VERB = "WBSTOREPROFILE";
	public static final String ARG_PROFILE_NAME = "name";
	public static final String ARG_SAVE_PASSWORD = "savePassword";
	public WbStoreProfile()
	{
		super();
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
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(ARG_PROFILE_NAME);
		cmdLine.addArgument(ARG_SAVE_PASSWORD, ArgumentType.BoolArgument);

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
			result.addMessageByKey("TxtNotConnected");
			result.setFailure();
			return result;
		}

		if (StringUtil.isBlank(name))
		{
			result.addMessageByKey("ErrNoProfile");
			result.setFailure();
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
		// a new driver entry if no matching driver was found. In that case it is marked as "internal"
		if (drv.isInternal())
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


}
