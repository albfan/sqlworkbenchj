/*
 * WbXslt.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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

import java.io.File;
import java.sql.SQLException;
import java.util.Map;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
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
	public static final String VERB = "WbXslt";
	public static final String ARG_STYLESHEET = "stylesheet";
	public static final String ARG_OUTPUT = "xsltOutput";
	public static final String ARG_INPUT = "inputFile";
	public static final String ARG_PARAMETERS = "xsltParameters";

	public WbXslt()
	{
		super();
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(ARG_STYLESHEET, ArgumentType.Filename);
		cmdLine.addArgument(ARG_OUTPUT, ArgumentType.Filename);
		cmdLine.addArgument(ARG_INPUT, ArgumentType.Filename);
		cmdLine.addArgument(ARG_PARAMETERS, ArgumentType.Repeatable);
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
	public StatementRunnerResult execute(String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		String parm = SqlUtil.stripVerb(aSql);

		cmdLine.parse(parm);

		WbFile inputFile = evaluateFileArgument(cmdLine.getValue(ARG_INPUT));
		WbFile outputFile = evaluateFileArgument(cmdLine.getValue(ARG_OUTPUT));
		WbFile xsltFile = evaluateFileArgument(cmdLine.getValue(ARG_STYLESHEET));

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

		if (!inputFile.exists())
		{
			result.addMessage(ResourceMgr.getFormattedString("ErrFileNotFound", cmdLine.getValue(ARG_INPUT)));
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

		if (!xsltFile.exists())
		{
			result.addMessage(ResourceMgr.getFormattedString("ErrFileNotFound", cmdLine.getValue(ARG_STYLESHEET)));
			result.setFailure();
			return result;
		}

		Map<String, String> params = cmdLine.getMapValue(ARG_PARAMETERS);

		XsltTransformer transformer = new XsltTransformer();

		try
		{
			transformer.setSaveSystemOutMessages(true);
			transformer.setXsltBaseDir(new File(getBaseDir()));

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

	@Override
	public boolean isWbCommand()
	{
		return true;
	}

}
