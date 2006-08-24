/*
 * EchoCommand.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.commands;

import java.sql.SQLException;

import workbench.db.WbConnection;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.StringUtil;

/**
 * This class implements a wrapper for the ECHO command
 *
 * @author  support@sql-workbench.net
 */
public class EchoCommand extends SqlCommand
{
	public static final String VERB = "ECHO";

	public EchoCommand()
	{
	}

	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException
	{
		StatementRunnerResult result = null;

		String param = stripVerb(aSql);

		if (StringUtil.isEmptyString(param))
		{
				result.addMessage(ResourceMgr.getString("ErrEchoWrongParameter"));
				result.setFailure();
		}
		else if ("off".equalsIgnoreCase(param) || "false".equalsIgnoreCase(param))
		{
			this.runner.setVerboseLogging(false);
			result.addMessage(ResourceMgr.getString("MsgFeedbackDisabled"));
			result.setSuccess();
		}
		else if ("on".equalsIgnoreCase(param) || "true".equalsIgnoreCase(param))
		{
			this.runner.setVerboseLogging(true);
			result.addMessage(ResourceMgr.getString("MsgFeedbackEnabled"));
			result.setSuccess();
		}
		return result;
	}

	public String getVerb()
	{
		return VERB;
	}

}
