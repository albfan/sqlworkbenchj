/*
 * WbDisconnect.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands.console;

import java.sql.SQLException;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.gui.profiles.ProfileKey;
import workbench.interfaces.ExecutionController;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.StringUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class WbDeleteProfile
	extends SqlCommand
{
	public static final String VERB = "WBDELETEPROFILE";

	public WbDeleteProfile()
	{
		super();
	}

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
				doDelete = controller.confirmExecution(prompt);
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


}
