/*
 * WbRun.java
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
import workbench.sql.DelimiterDefinition;
import workbench.util.ExceptionUtil;
import workbench.resource.ResourceMgr;
import workbench.sql.BatchRunner;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.StringUtil;
import workbench.resource.Settings;
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
		DelimiterDefinition delim = Settings.getInstance().getAlternateDelimiter(currentConnection);
		String encoding = Settings.getInstance().getDefaultEncoding();

		try
		{
			BatchRunner batchRunner = new BatchRunner(file.getCanonicalPath());
			String dir = file.getCanonicalFile().getParent();
			batchRunner.setBaseDir(dir);
			batchRunner.setConnection(currentConnection);
			batchRunner.setDelimiter(delim);
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
