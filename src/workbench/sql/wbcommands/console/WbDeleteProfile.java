/*
 * WbDeleteProfile.java
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

import workbench.interfaces.ExecutionController;
import workbench.resource.ResourceMgr;

import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;

import workbench.gui.profiles.ProfileKey;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class WbDeleteProfile
	extends SqlCommand
{
	public static final String VERB = "WbDeleteProfile";

	public WbDeleteProfile()
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
		String name = getCommandLine(sql);

		if (StringUtil.isBlank(name))
		{
			result.addMessageByKey("ErrNoProfile");
			result.setFailure();
			return result;
		}

		ProfileKey key = new ProfileKey(name);

		ConnectionProfile prof = ConnectionMgr.getInstance().getProfile(key);
		if (prof != null)
		{
			key = prof.getKey();
			boolean doDelete = true;
			ExecutionController controller = runner.getExecutionController();
			if (controller != null)
			{
				String prompt = ResourceMgr.getFormattedString("MsgConfirmProfDel", key.toString());
				doDelete = controller.confirmExecution(prompt, null, null);
			}
			if (doDelete)
			{
				ConnectionMgr.getInstance().removeProfile(prof);
				ConnectionMgr.getInstance().saveProfiles();
				result.addMessage(ResourceMgr.getFormattedString("MsgProfileDeleted", key.toString()));
			}
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
