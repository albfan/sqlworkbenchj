/*
 * WbListProfiles.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands.console;

import java.sql.SQLException;
import java.util.List;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.ProfileGroupMap;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.CollectionUtil;

/**
 * List all defined profiles
 *
 * @author Thomas Kellerer
 */
public class WbListProfiles
	extends SqlCommand
{
	public static final String VERB = "WBLISTPROFILES";

	public WbListProfiles()
	{
		super();
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

		List<ConnectionProfile> prof = CollectionUtil.arrayList();

		// getProfiles() returns an unmodifiable List, but ProfileGroupMap
		// tries to sort the list...
		prof.addAll(ConnectionMgr.getInstance().getProfiles());
		ProfileGroupMap map = new ProfileGroupMap(prof);

		String userTxt = ResourceMgr.getString("TxtUser");
		for (String group : map.keySet())
		{
			List<ConnectionProfile> profiles = map.get(group);
			result.addMessage(group);
			for (ConnectionProfile profile : profiles)
			{
				result.addMessage("  " + profile.getName() + ", " + userTxt + "=" + profile.getUsername() + ", URL=" + profile.getUrl());
			}
		}
		result.setSuccess();
		return result;
	}

}
