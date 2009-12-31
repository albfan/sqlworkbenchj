/*
 * WbConfirm.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.SQLException;
import workbench.interfaces.ExecutionController;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.StringUtil;

/**
 * A SQL Statement to pause a script and confirm execution by the user.
 * <br>
 * When running in batch mode, this command has no effect. Technically this is
 * caused because no {@link ExecutionController} is available in batch mode.
 * 
 * @author Thomas Kellerer
 */
public class WbConfirm
	extends SqlCommand
{
	public static final String VERB = "WBCONFIRM";

	public WbConfirm()
	{
		super();
		this.isUpdatingCommand = false;
	}

	public String getVerb()
	{
		return VERB;
	}

	protected boolean isConnectionRequired()
	{
		return false;
	}

	public StatementRunnerResult execute(String sql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		result.setStopScript(false);
		result.setSuccess();

		ExecutionController controller = runner.getExecutionController();
		if (controller != null)
		{
			String msg = StringUtil.trimQuotes(getCommandLine(sql));

			if (StringUtil.isEmptyString(msg))
			{
				msg = ResourceMgr.getString("MsgConfirmContinue");
			}

			boolean continueScript = controller.confirmExecution(msg);
			
			if (!continueScript)
			{
				result.setStopScript(true);
			}
		}
		return result;
	}

}
