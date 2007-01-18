/*
 * WbFeedback.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.SQLException;
import workbench.db.WbConnection;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.ArgumentParser;
import workbench.util.SqlUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class WbFeedback
	extends SqlCommand
{
	public static final String VERB = "WBFEEDBACK";
	
	public WbFeedback()
	{
		this.cmdLine = new ArgumentParser(false);
		this.cmdLine.addArgument("on");
		this.cmdLine.addArgument("off");
	}

	public String getVerb() { return VERB; }
	
	protected boolean isConnectionRequired() { return false; }
	
	public StatementRunnerResult execute(WbConnection aConnection, String sql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		result.setSuccess();
		String parm = SqlUtil.stripVerb(sql);
		if (parm == null)
		{
			result.setFailure();
			result.addMessage(ResourceMgr.getString("ErrFeedbackWrongParameter"));
			return result;
		}
		
		if ("off".equalsIgnoreCase(parm) || "false".equalsIgnoreCase(parm))
		{
			this.runner.setVerboseLogging(false);
			result.addMessage(ResourceMgr.getString("MsgFeedbackDisabled"));
		}
		else if ("on".equalsIgnoreCase(parm) || "true".equalsIgnoreCase(parm))
		{
			this.runner.setVerboseLogging(true);
			result.addMessage(ResourceMgr.getString("MsgFeedbackEnabled"));
		}
		else
		{
			result.setFailure();
			result.addMessage(ResourceMgr.getString("ErrFeedbackWrongParameter"));
		}
		return result;
	}
	
}
