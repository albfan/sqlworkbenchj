/*
 * WbInclude.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.SQLException;
import workbench.AppArguments;
import workbench.sql.DelimiterDefinition;
import workbench.util.ArgumentType;
import workbench.util.ExceptionUtil;
import workbench.resource.ResourceMgr;
import workbench.sql.BatchRunner;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.ArgumentParser;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.resource.Settings;
import workbench.util.WbFile;

/**
 * @author  Thomas Kellerer
 */
public class WbInclude
	extends SqlCommand
{
	public static final String VERB = "WBINCLUDE";
	public static final String ORA_INCLUDE = "@";

	private BatchRunner batchRunner;

	public WbInclude()
	{
		super();
		cmdLine = new ArgumentParser();
		cmdLine.addArgument("file");
		cmdLine.addArgument(CommonArgs.ARG_CONTINUE, ArgumentType.BoolArgument);
		cmdLine.addArgument(AppArguments.ARG_DISPLAY_RESULT, ArgumentType.BoolArgument);
		cmdLine.addArgument("checkEscapedQuotes", ArgumentType.BoolArgument);
		cmdLine.addArgument("delimiter",StringUtil.stringToList("';','/','GO:nl'"));
		cmdLine.addArgument("verbose", ArgumentType.BoolArgument);
		cmdLine.addArgument(AppArguments.ARG_IGNORE_DROP, ArgumentType.BoolArgument);
		cmdLine.addArgument(WbImport.ARG_USE_SAVEPOINT, ArgumentType.BoolArgument);
		CommonArgs.addEncodingParameter(cmdLine);
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

	@Override
	public String getAlternateVerb()
	{
		return ORA_INCLUDE;
	}

	@Override
	protected boolean isConnectionRequired()
	{
		return false;
	}

	@Override
	public StatementRunnerResult execute(String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult(aSql);
		result.setSuccess();

		String clean = SqlUtil.makeCleanSql(aSql, false, false);
		boolean checkParms = true;

		WbFile file = null;

		if (clean.charAt(0) == '@')
		{
			clean = clean.substring(1);
			file = evaluateFileArgument(clean);
			String ext = file.getExtension();
			if (ext != null && !ext.toLowerCase().equals("sql"))
			{
				String fullname = file.getFullPath() + ".sql";
				file = new WbFile(fullname);
			}
			checkParms = false;
		}
		else
		{
			clean = getCommandLine(aSql);
			cmdLine.parse(clean);
			file = evaluateFileArgument(cmdLine.getValue("file"));
			if (file == null)
			{
				// support a short version of WbInclude that simply specifies the filename
				file = evaluateFileArgument(clean);
				checkParms = false;
			}
		}

		if (file == null)
		{
			String msg = ResourceMgr.getString("ErrIncludeWrongParameter").replace("%default_encoding%", Settings.getInstance().getDefaultEncoding());
			result.addMessage(msg);
			result.setFailure();
			return result;
		}

		if (!file.exists())
		{
			result.setFailure();
			String msg = ResourceMgr.getString("ErrIncludeFileNotFound");
			msg = StringUtil.replace(msg, "%filename%", file.getFullPath());
			result.addMessage(msg);
			return result;
		}

		boolean continueOnError = false;
		boolean checkEscape = Settings.getInstance().getCheckEscapedQuotes();
		boolean verbose = true;
		boolean defaultIgnore = (currentConnection == null ? false : currentConnection.getProfile().getIgnoreDropErrors());
		boolean ignoreDrop = defaultIgnore;
		DelimiterDefinition delim = null;
		String encoding = null;

		if (checkParms)
		{
			continueOnError = cmdLine.getBoolean(CommonArgs.ARG_CONTINUE, false);
			checkEscape = cmdLine.getBoolean("checkescapedquotes", Settings.getInstance().getCheckEscapedQuotes());
			verbose = cmdLine.getBoolean("verbose", false);
			defaultIgnore = (currentConnection == null ? false : currentConnection.getProfile().getIgnoreDropErrors());
			ignoreDrop = cmdLine.getBoolean(AppArguments.ARG_IGNORE_DROP, defaultIgnore);
			encoding = cmdLine.getValue(CommonArgs.ARG_ENCODING);
			delim = DelimiterDefinition.parseCmdLineArgument(cmdLine.getValue("delimiter"));
			setUnknownMessage(result, cmdLine, null);
		}

		if (encoding == null)
		{
			encoding = Settings.getInstance().getDefaultEncoding();
		}

		try
		{
			batchRunner = new BatchRunner(file.getCanonicalPath());
			String dir = file.getCanonicalFile().getParent();
			batchRunner.setBaseDir(dir);
			batchRunner.setConnection(currentConnection);
			batchRunner.setDelimiter(delim);
			batchRunner.setResultLogger(this.resultLogger);
			batchRunner.setVerboseLogging(verbose);
			batchRunner.setRowMonitor(this.rowMonitor);
			batchRunner.setAbortOnError(!continueOnError);
			batchRunner.setCheckEscapedQuotes(checkEscape);
			batchRunner.setShowTiming(false);
			batchRunner.setEncoding(encoding);
			batchRunner.setParameterPrompter(this.prompter);
			batchRunner.setExecutionController(runner.getExecutionController());
			batchRunner.setIgnoreDropErrors(ignoreDrop);
			boolean showResults = cmdLine.getBoolean(AppArguments.ARG_DISPLAY_RESULT, false);
			batchRunner.showResultSets(showResults);
			batchRunner.setOptimizeColWidths(showResults);
			if (cmdLine.isArgPresent(WbImport.ARG_USE_SAVEPOINT))
			{
				batchRunner.setUseSavepoint(cmdLine.getBoolean(WbImport.ARG_USE_SAVEPOINT));
			}
			if (showResults)
			{
				batchRunner.setShowProgress(false);
			}
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

	@Override
	public void done()
	{
		// nothing to do
	}

	@Override
	public void cancel()
		throws SQLException
	{
		super.cancel();
		if (batchRunner != null)
		{
			batchRunner.cancel();
		}
	}
}
