/*
 * WbFeedback.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.sql.wbcommands;

import java.sql.SQLException;

import workbench.log.LogMgr;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;

/**
 * Control the level of feedback during script execution.
 *
 * @author Thomas Kellerer
 */
public class WbFeedback
	extends SqlCommand
{
	public static final String VERB = "WbFeedback";
	private final String command;

	public WbFeedback()
	{
		this(VERB);
	}

	public WbFeedback(String verb)
	{
		super();
		this.command = verb;
		this.cmdLine = new ArgumentParser(false);
		this.cmdLine.addArgument("on", ArgumentType.BoolSwitch);
		this.cmdLine.addArgument("off", ArgumentType.BoolSwitch);
		this.cmdLine.addArgument("quiet", ArgumentType.BoolSwitch);
		this.cmdLine.addArgument("traceon", ArgumentType.BoolSwitch);
		this.cmdLine.addArgument("traceoff", ArgumentType.BoolSwitch);
	}

	@Override
	public String getVerb()
	{
		return command;
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
		StatementRunnerResult result = new StatementRunnerResult();
		result.setSuccess();

		cmdLine.parse(getCommandLine(sql));

		boolean quiet = cmdLine.getBoolean("quiet");

		if (cmdLine.getBoolean("off"))
		{
			this.runner.setVerboseLogging(false);
			if (quiet)
			{
				LogMgr.logInfo("WbFeedbac.execute()", "Feedback disabled");
			}
			else
			{
				result.addMessageByKey("MsgFeedbackDisabled");
			}
		}
		else if (cmdLine.getBoolean("on"))
		{
			this.runner.setVerboseLogging(true);
			if (quiet)
			{
				LogMgr.logInfo("WbFeedbac.execute()", "Feedback enabled");
			}
			else
			{
				result.addMessageByKey("MsgFeedbackEnabled");
			}
		}
		else if (cmdLine.getBoolean("traceon"))
		{
			result.setSuccess();
			this.runner.setTraceStatements(true);
			if (!quiet) result.addMessageByKey("MsgTraceOn");
		}
		else if (cmdLine.getBoolean("traceoff"))
		{
			result.setSuccess();
			this.runner.setTraceStatements(false);
			if (!quiet) result.addMessageByKey("MsgTraceOff");
		}
		else if (cmdLine.hasUnknownArguments())
		{
			result.addErrorMessageByKey("ErrFeedbackWrongParameter");
		}
		else
		{
			// no parameter, show the current status
			if (runner.getVerboseLogging())
			{
				result.addMessageByKey("MsgFeedbackEnabled");
			}
			else
			{
				result.addMessageByKey("MsgFeedbackDisabled");
			}
			result.setSuccess();
		}
		return result;
	}

	@Override
	public boolean isWbCommand()
	{
		return true;
	}

}
