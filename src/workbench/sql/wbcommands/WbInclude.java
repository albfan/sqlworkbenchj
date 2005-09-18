/*
 * WbInclude.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.io.File;
import java.sql.SQLException;
import workbench.db.WbConnection;
import workbench.util.ExceptionUtil;
import workbench.resource.ResourceMgr;
import workbench.sql.BatchRunner;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.ArgumentParser;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.resource.Settings;

/**
 * @author  support@sql-workbench.net
 */
public class WbInclude
	extends SqlCommand
{
	public static final WbInclude INCLUDE_LONG = new WbInclude("WBINCLUDE");
	public static final WbInclude INCLUDE_SHORT = new WbInclude("@");
	public static final WbInclude INCLUDE_FB = new WbInclude("INPUT");

	private final String verb;
	private BatchRunner batchRunner;
	private ArgumentParser cmdLine;

	private WbInclude(String aVerb)
	{
		this.verb = aVerb;
		cmdLine = new ArgumentParser();
		cmdLine.addArgument("file");
		cmdLine.addArgument("continueonerror");
		cmdLine.addArgument("checkescapedquotes");
		cmdLine.addArgument("delimiter");
		cmdLine.addArgument("verbose");
		cmdLine.addArgument("encoding");
	}

	public String getVerb() { return verb; }

	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		result.setSuccess();

		String clean = SqlUtil.makeCleanSql(aSql, false, '"');
		int pos = clean.indexOf(' ');
		if (pos > -1)
			clean = clean.substring(pos);
		else
			clean = "";

		String file = null;

		cmdLine.parse(clean);
		if (cmdLine.isArgPresent("file"))
		{
			file = cmdLine.getValue("file");
		}
		else
		{
			file = this.getFilename(aSql);
		}

		if (file == null || file.length() == 0)
		{
			String msg = ResourceMgr.getString("ErrorIncludeWrongParameter");
			result.addMessage(msg);
			result.setFailure();
			return result;
		}

		File f = new File(file);
		if (!f.exists())
		{
			result.setFailure();
			String msg = ResourceMgr.getString("ErrorIncludeFileNotFound");
			msg = StringUtil.replace(msg, "%filename%", file);
			result.addMessage(msg);
			return result;
		}

		boolean continueOnError = cmdLine.getBoolean("continueonerror", true);
		boolean checkEscape = cmdLine.getBoolean("checkescapedquotes", Settings.getInstance().getCheckEscapedQuotes());
		boolean verbose = cmdLine.getBoolean("verbose", false);
		String encoding = cmdLine.getValue("encoding");

		String delim = cmdLine.getValue("delimiter");
		try
		{
			batchRunner = new BatchRunner(file);
			batchRunner.setConnection(aConnection);
			if (delim != null) batchRunner.setDelimiter(delim);
			batchRunner.setResultLogger(this.resultLogger);
			batchRunner.setVerboseLogging(verbose);
			batchRunner.setRowMonitor(this.rowMonitor);
			batchRunner.setAbortOnError(!continueOnError);
			batchRunner.setCheckEscapedQuotes(checkEscape);
			batchRunner.setShowTiming(false);
			batchRunner.setEncoding(encoding);
			batchRunner.execute();
			result.setSuccess();
		}
		catch (Throwable th)
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

	private String getFilename(String parameter)
	{
		String name = parameter.substring(this.verb.length()).trim();
		name = StringUtil.trimQuotes(name).trim();
		return name;
	}

}