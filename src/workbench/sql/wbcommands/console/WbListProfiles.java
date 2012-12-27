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
		// will sort the list which is not possible with an unmodifieable List
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
