/*
 * WbXslt.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.SQLException;

import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.ArgumentParser;
import workbench.util.XsltTransformer;

/**
 *
 * @author  info@sql-workbench.net
 */
public class WbXslt
	extends SqlCommand
{
	private ArgumentParser cmdLine;
	public static final String VERB = "WBXSLT";
	public static final String ARG_STYLESHEET = "stylesheet";
	public static final String ARG_OUTPUT = "xsltoutput";
	public static final String ARG_INPUT = "inputfile";

	public WbXslt()
	{
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(ARG_STYLESHEET);
		cmdLine.addArgument(ARG_OUTPUT);
		cmdLine.addArgument(ARG_INPUT);
	}

	public String getVerb()
	{
		return VERB;
	}

	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult("XSLT");
		if (!aSql.trim().toUpperCase().startsWith(this.getVerb()))
		{
			result.addMessage("Wrong command! " + this.getVerb() + " expected.");
			result.setFailure();
			return result;
		}

		String parm = aSql.trim().substring(this.getVerb().length());

		cmdLine.parse(parm);

		String inputFile = cmdLine.getValue(ARG_INPUT);
		String outputFile = cmdLine.getValue(ARG_OUTPUT);
		String xsltFile = cmdLine.getValue(ARG_STYLESHEET);

		if (!cmdLine.hasArguments())
		{
			result.addMessage(ResourceMgr.getString("ErrorXsltWrongParameter"));
			result.setFailure();
			return result;
		}

		if (inputFile == null)
		{
			result.addMessage(ResourceMgr.getString("ErrorXsltMissingInputFile"));
			result.setFailure();
			return result;
		}
		if (outputFile == null)
		{
			result.addMessage(ResourceMgr.getString("ErrorXsltMissingOutputFile"));
			result.setFailure();
			return result;
		}
		if (xsltFile == null)
		{
			result.addMessage(ResourceMgr.getString("ErrorXsltMissingStylesheet"));
			result.setFailure();
			return result;
		}

		try
		{
			XsltTransformer.transformFile(inputFile, outputFile, xsltFile);
			result.addMessage(ResourceMgr.getString("MsgXsltSuccessful"));
			result.setSuccess();
		}
		catch (Exception e)
		{
			LogMgr.logError("WbXslt.execute()", "Error when transforming '" + inputFile + "' to '" + outputFile + "' using " + xsltFile, e);
			result.addMessage(e.getMessage());
		}
		return result;
	}

}
