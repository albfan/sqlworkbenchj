/*
 * WbConfirm.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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
package workbench.sql.wbcommands;

import java.sql.SQLException;

import workbench.interfaces.ExecutionController;
import workbench.resource.ResourceMgr;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.ArgumentParser;
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
	public static final String VERB = "WbConfirm";
	public static final String PARAM_MSG = "message";
	public static final String PARAM_YES = "yesText";
	public static final String PARAM_NO = "noText";

	public WbConfirm()
	{
		super();
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(PARAM_MSG);
		cmdLine.addArgument(PARAM_YES);
		cmdLine.addArgument(PARAM_NO);
    ConditionCheck.addParameters(cmdLine);
		this.isUpdatingCommand = false;
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
		throws SQLException
	{
		String args = getCommandLine(sql);
		cmdLine.parse(args);

		String msg = null;
		String yes = null;
		String no = null;

    StatementRunnerResult result = new StatementRunnerResult();
		if (cmdLine.hasArguments())
		{
			msg = cmdLine.getValue(PARAM_MSG);
			yes = cmdLine.getValue(PARAM_YES);
			no = cmdLine.getValue(PARAM_NO);

      if (!checkConditions(result))
      {
        return result;
      }
    }
		else
		{
			msg = StringUtil.trimQuotes(args);
		}
		result.setStopScript(false);
		result.setSuccess();

		ExecutionController controller = runner.getExecutionController();
		if (controller != null)
		{
			if (StringUtil.isBlank(msg))
			{
				msg = ResourceMgr.getString("MsgConfirmContinue");
			}

			boolean continueScript = controller.confirmExecution(msg, yes, no);

			if (!continueScript)
			{
				result.setStopScript(true);
			}
		}
		return result;
	}

	@Override
	public boolean isWbCommand()
	{
		return true;
	}

}
