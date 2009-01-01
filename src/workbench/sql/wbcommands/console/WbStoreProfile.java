/*
 * WbStoreProfile.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
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
import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 *
 * @author support@sql-workbench.net
 */
public class WbStoreProfile
	extends SqlCommand
{
	public static final String VERB = "WBSTOREPROFILE";

	public WbStoreProfile()
	{
		super();
	}

	public String getVerb()
	{
		return VERB;
	}

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException, Exception
	{
		StatementRunnerResult result = new StatementRunnerResult();
		String name = getCommandLine(sql);

		if (this.currentConnection == null)
		{
			result.addMessageByKey("TxtNotConnected");
			result.setFailure();
			return result;
		}

		if (StringUtil.isBlank(name))
		{
			result.addMessage("ErrNoProfile");
			result.setFailure();
			return result;
		}

		ProfileKey key = new ProfileKey(name);
		
		ConnectionProfile profile = this.currentConnection.getProfile().createCopy();
		profile.setName(key.getName());
		profile.setGroup(key.getGroup());
		profile.setPassword(null);
		profile.setStorePassword(false);
		profile.setWorkspaceFile(null);
		
		ConnectionMgr.getInstance().addProfile(profile);
		ConnectionMgr.getInstance().saveProfiles();
		result.addMessage(ResourceMgr.getFormattedString("MsgProfileAdded", key.toString()));

		DbDriver drv = ConnectionMgr.getInstance().findDriver(profile.getDriverclass());
		if (drv.isInternal())
		{
			DbDriver newDrv = drv.createCopy();
			String drvName = currentConnection.getSqlConnection().getMetaData().getDriverName();
			newDrv.setName(drvName);
			newDrv.setSampleUrl(profile.getUrl());
			WbFile f = new WbFile(drv.getLibrary());
			newDrv.setLibrary(f.getFullPath());
			profile.setDriverName(drvName);

			ConnectionMgr.getInstance().getDrivers().add(newDrv);
			ConnectionMgr.getInstance().saveDrivers();
			result.addMessage(ResourceMgr.getFormattedString("MsgDriverAdded", drvName));
		}

		result.setSuccess();
		
		return result;
	}


}
