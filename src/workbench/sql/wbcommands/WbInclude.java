/*
 * WbInclude.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
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
 * @author  support@sql-workbench.net
 */
public class WbInclude
	extends SqlCommand
{
	public static final String VERB = "WBINCLUDE";
	public static final WbInclude INCLUDE_LONG = new WbInclude(VERB);
	public static final WbInclude INCLUDE_SHORT = new WbInclude("@");
	public static final WbInclude INCLUDE_FB = new WbInclude("INPUT");

	private final String verb;
	private BatchRunner batchRunner;

	private WbInclude(String aVerb)
	{
		this.verb = aVerb;
		cmdLine = new ArgumentParser();
		cmdLine.addArgument("file");
		cmdLine.addArgument("continueOnError", ArgumentType.BoolArgument);
		cmdLine.addArgument("checkEscapedQuotes", ArgumentType.BoolArgument);
		cmdLine.addArgument("delimiter",StringUtil.stringToList("';','/',<char>"));
		cmdLine.addArgument("verbose", ArgumentType.BoolArgument);
		cmdLine.addArgument(AppArguments.ARG_IGNORE_DROP, ArgumentType.BoolArgument);
		CommonArgs.addEncodingParameter(cmdLine);
		this.isUpdatingCommand = true;
	}

	public String getVerb() { return verb; }

	protected boolean isConnectionRequired() { return false; }
	
	public StatementRunnerResult execute(String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		result.setSuccess();
		
		String clean = SqlUtil.makeCleanSql(aSql, false, '"');
		boolean isShortInclude = false;
		
		if (clean.charAt(0) == '@')
		{
			clean = clean.substring(1);
			isShortInclude = true;
		}
		else
		{
			clean = SqlUtil.stripVerb(clean);
		}
		
		WbFile file = null;

		if (isShortInclude)
		{
			file = evaluateFileArgument(clean);
		}
		else
		{
			cmdLine.parse(clean);
			file = evaluateFileArgument(cmdLine.getValue("file"));
		}

		if (file == null || file.length() == 0)
		{
			String msg = ResourceMgr.getString("ErrIncludeWrongParameter");
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

		boolean continueOnError = cmdLine.getBoolean("continueonerror", false);
		boolean checkEscape = cmdLine.getBoolean("checkescapedquotes", Settings.getInstance().getCheckEscapedQuotes());
		boolean verbose = cmdLine.getBoolean("verbose", false);
		boolean defaultIgnore = currentConnection.getProfile().getIgnoreDropErrors();
		boolean ignoreDrop = cmdLine.getBoolean(AppArguments.ARG_IGNORE_DROP, defaultIgnore);
		String encoding = cmdLine.getValue("encoding");

		String delim = cmdLine.getValue("delimiter");
		try
		{
			batchRunner = new BatchRunner(file.getCanonicalPath());
			String dir = file.getCanonicalFile().getParent();
			batchRunner.setBaseDir(dir);
			batchRunner.setConnection(currentConnection);
			if (delim != null) batchRunner.setDelimiter(DelimiterDefinition.parseCmdLineArgument(delim));
			batchRunner.setResultLogger(this.resultLogger);
			batchRunner.setVerboseLogging(verbose);
			batchRunner.setRowMonitor(this.rowMonitor);
			batchRunner.setAbortOnError(!continueOnError);
			batchRunner.setCheckEscapedQuotes(checkEscape);
			batchRunner.setShowTiming(false);
			batchRunner.setEncoding(encoding);
			batchRunner.setParameterPrompter(this.prompter);
			batchRunner.setIgnoreDropErrors(ignoreDrop);
			batchRunner.execute();
			if (batchRunner.isSuccess())
			{
				result.setSuccess();
			}
			else
			{
				result.setFailure();
			}
		}
		catch (Exception th)
		{
			result.setFailure();
			result.addMessage(ExceptionUtil.getDisplay(th));
		}
		return result;
	}

	public void done()
	{
		// nothing to do
	}

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
