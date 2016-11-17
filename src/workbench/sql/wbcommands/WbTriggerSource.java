/*
 * WbTriggerSource.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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

import java.io.IOException;
import java.sql.SQLException;

import workbench.db.TableIdentifier;
import workbench.db.TriggerDefinition;
import workbench.db.TriggerReader;
import workbench.db.TriggerReaderFactory;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.EncodingUtil;
import workbench.util.FileUtil;
import workbench.util.WbFile;

/**
 * Display the source code of a trigger.
 * @see workbench.db.TriggerReader#getTriggerSource(workbench.db.TriggerDefinition, boolean)
 *
 * @author Thomas Kellerer
 */
public class WbTriggerSource
	extends SqlCommand
{
	public static final String VERB = "WbTriggerSource";
  public static final String ARG_TRIGGER_NAME = "trigger";
  public static final String ARG_INCLUDE_DEPS = "includeDependent";

	public WbTriggerSource()
	{
		super();
    cmdLine = new ArgumentParser();
    cmdLine.addArgument(CommonArgs.ARG_FILE);
    cmdLine.addArgument(ARG_TRIGGER_NAME);
    cmdLine.addArgument(ARG_INCLUDE_DEPS, ArgumentType.BoolArgument);
    CommonArgs.addEncodingParameter(cmdLine);
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		String args = getCommandLine(sql);

    cmdLine.parse(args);
    if (displayHelp(result))
    {
      return result;
    }

    boolean includeDeps = true;
    String triggerName = null;

    if (cmdLine.hasArguments())
    {
      triggerName = cmdLine.getValue(ARG_TRIGGER_NAME);
      includeDeps = cmdLine.getBoolean(ARG_INCLUDE_DEPS, true);
    }
    else
    {
      triggerName = args;
    }

    TableIdentifier object = new TableIdentifier(triggerName, currentConnection);
    object.adjustCatalogAndSchema(currentConnection);

		TriggerReader reader = TriggerReaderFactory.createReader(currentConnection);
		TriggerDefinition trg = reader.findTrigger(object.getCatalog(), object.getSchema(), object.getObjectName());

		String source = null;
		if (trg != null)
		{
			source = reader.getTriggerSource(trg, includeDeps);
		}

		if (source != null)
		{
			result.setSuccess();
      String fname = cmdLine.getValue(CommonArgs.ARG_FILE);
      WbFile outFile = evaluateFileArgument(fname);
      if (outFile != null)
      {
        try
        {
          String encoding = cmdLine.getValue(CommonArgs.ARG_ENCODING, EncodingUtil.getDefaultEncoding());
          FileUtil.writeString(outFile, source, encoding, false);
          result.addMessageByKey("MsgScriptWritten", outFile.getAbsolutePath());
        }
        catch (IOException io)
        {
          result.setFailure();
          result.addMessage(io.getLocalizedMessage());
        }
      }
      else
      {
        result.addMessage(source);
      }
		}
		else
		{
			result.addMessageByKey("ErrTrgNotFound", object.getObjectExpression(currentConnection));
			result.setFailure();
		}

		return result;
	}

	@Override
	public boolean isWbCommand()
	{
		return true;
	}
}
