/*
 * WbXslt.java
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

import java.io.File;
import java.sql.SQLException;
import java.util.Map;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.ArgumentParser;
import workbench.util.ExceptionUtil;
import workbench.util.SqlUtil;
import workbench.util.WbFile;
import workbench.util.XsltTransformer;

/**
 *
 * @author  Thomas Kellerer
 */
public class WbXslt
	extends SqlCommand
{
	public static final String VERB = "WBXSLT";
	public static final String ARG_STYLESHEET = "stylesheet";
	public static final String ARG_OUTPUT = "xsltOutput";
	public static final String ARG_INPUT = "inputFile";
	public static final String ARG_PARAMETERS = "xsltParameters";

	public WbXslt()
	{
		super();
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(ARG_STYLESHEET);
		cmdLine.addArgument(ARG_OUTPUT);
		cmdLine.addArgument(ARG_INPUT);
		cmdLine.addArgument(ARG_PARAMETERS);
	}

	public String getVerb()
	{
		return VERB;
	}

	protected boolean isConnectionRequired() { return false; }

	public StatementRunnerResult execute(String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		if (!aSql.trim().toUpperCase().startsWith(this.getVerb()))
		{
			result.addMessage("Wrong command! " + this.getVerb() + " expected.");
			result.setFailure();
			return result;
		}

		String parm = SqlUtil.stripVerb(aSql);

		cmdLine.parse(parm);

		String inputFile = cmdLine.getValue(ARG_INPUT);
		String outputFile = cmdLine.getValue(ARG_OUTPUT);
		String xsltFile = cmdLine.getValue(ARG_STYLESHEET);

		if (!cmdLine.hasArguments())
		{
			result.addMessage(ResourceMgr.getString("ErrXsltWrongParameter"));
			result.setFailure();
			return result;
		}

		if (inputFile == null)
		{
			result.addMessage(ResourceMgr.getString("ErrXsltMissingInputFile"));
			result.setFailure();
			return result;
		}

		if (outputFile == null)
		{
			result.addMessage(ResourceMgr.getString("ErrXsltMissingOutputFile"));
			result.setFailure();
			return result;
		}

		if (xsltFile == null)
		{
			result.addMessage(ResourceMgr.getString("ErrXsltMissingStylesheet"));
			result.setFailure();
			return result;
		}

		Map<String, String> params = cmdLine.getMapValue(ARG_PARAMETERS);
		
		XsltTransformer transformer = new XsltTransformer();
		
		try
		{
			transformer.setSaveSystemOutMessages(true);
			transformer.setXsltBaseDir(new File(runner.getBaseDir()));

			transformer.transform(inputFile, outputFile, xsltFile, params);

			String msg = transformer.getAllOutputs();
			if (msg.length() != 0)
			{
				result.addMessage(msg);
				result.addMessage(""); // create newline
			}

			WbFile xsltUsed = new WbFile(transformer.getXsltUsed());
			WbFile userXslt = new WbFile(xsltFile);
			if (xsltUsed != null && !userXslt.equals(xsltUsed))
			{
				// If the xslt file has been "automatically" found, inform the user about this
				result.addMessage(ResourceMgr.getFormattedString("MsgXsltUsed", xsltUsed.getFullPath()));
			}
			result.addMessage(ResourceMgr.getFormattedString("MsgXsltSuccessful", outputFile));
			result.setSuccess();
		}
		catch (Exception e)
		{
			LogMgr.logError("WbXslt.execute()", "Error when transforming '" + inputFile + "' to '" + outputFile + "' using " + xsltFile, e);
			String msg = transformer.getAllOutputs(e);
			LogMgr.logError("WbXslt.execute()", msg, null);
			result.addMessage(msg);
		}
		return result;
	}

}
