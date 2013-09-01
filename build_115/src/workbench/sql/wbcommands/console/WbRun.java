/*
 * WbRun.java
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

import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.BatchRunner;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.ExceptionUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 * A SQL command that runs an external file and displays the result of the
 * query.
 *
 * @author  Thomas Kellerer
 */
public class WbRun
	extends SqlCommand
{
	public static final String VERB = "WBRUN";

	public WbRun()
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
		return true;
	}

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult(sql);
		result.setSuccess();

		String clean = getCommandLine(sql);
		if (StringUtil.isBlank(clean))
		{
			result.setFailure();
			result.addMessageByKey("ErrFileNameRqd");
			return result;
		}

		WbFile file = new WbFile(clean);

		if (StringUtil.isEmptyString(clean) || !file.exists())
		{
			result.setFailure();
			String msg = ResourceMgr.getString("ErrIncludeFileNotFound");
			msg = StringUtil.replace(msg, "%filename%", file.getFullPath());
			result.addMessage(msg);
			return result;
		}

		boolean checkEscape = Settings.getInstance().getCheckEscapedQuotes();
		boolean defaultIgnore = (currentConnection == null ? false : currentConnection.getProfile().getIgnoreDropErrors());
		String encoding = Settings.getInstance().getDefaultEncoding();

		try
		{
			BatchRunner batchRunner = new BatchRunner(file.getCanonicalPath());
			String dir = file.getCanonicalFile().getParent();
			batchRunner.setBaseDir(dir);
			batchRunner.setConnection(currentConnection);
			batchRunner.setResultLogger(this.resultLogger);
			batchRunner.setRowMonitor(null);
			batchRunner.setVerboseLogging(false);
			batchRunner.setIgnoreDropErrors(defaultIgnore);
			batchRunner.setAbortOnError(true);
			batchRunner.setCheckEscapedQuotes(checkEscape);
			batchRunner.setShowTiming(false);
			batchRunner.setEncoding(encoding);
			batchRunner.setParameterPrompter(this.prompter);
			batchRunner.setExecutionController(runner.getExecutionController());
			batchRunner.setOptimizeColWidths(true);
			batchRunner.showResultSets(true);
			batchRunner.setShowProgress(false);
			batchRunner.execute();
			if (batchRunner.isSuccess())
			{
				result.setSuccess();
			}
			else
			{
				result.setFailure();
			}

			if (this.rowMonitor != null)
			{
				this.rowMonitor.jobFinished();
			}
		}
		catch (Exception th)
		{
			result.setFailure();
			result.addMessage(ExceptionUtil.getDisplay(th));
		}
		return result;
	}

}
