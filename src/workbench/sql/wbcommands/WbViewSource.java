/*
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

import workbench.db.DropType;
import workbench.db.TableIdentifier;
import workbench.db.ViewReader;
import workbench.db.ViewReaderFactory;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.EncodingUtil;
import workbench.util.FileUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 * Display the source code of a view.
 *
 * @author Thomas Kellerer
 */
public class WbViewSource
	extends SqlCommand
{

	public static final String VERB = "WbViewSource";
  public static final String ARG_VIEWNAME = "view";

	public WbViewSource()
	{
		super();
    this.isUpdatingCommand = false;

    cmdLine = new ArgumentParser();
    cmdLine.addArgument(ARG_VIEWNAME);
    cmdLine.addArgument(CommonArgs.ARG_FILE, ArgumentType.Filename);
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
    String viewName = null;
    WbFile outputfile = null;
    String encoding = null;

    if (displayHelp(result))
    {
      return result;
    }

    if (cmdLine.hasArguments())
    {
      viewName = cmdLine.getValue(ARG_VIEWNAME);
      outputfile = evaluateFileArgument(cmdLine.getValue(CommonArgs.ARG_FILE));
      encoding = cmdLine.getValue(CommonArgs.ARG_ENCODING, EncodingUtil.getDefaultEncoding());
    }
    else
    {
      viewName = args;
    }

		TableIdentifier object = new TableIdentifier(viewName, currentConnection);
		TableIdentifier tbl = currentConnection.getMetadata().findObject(object);

		CharSequence source = null;
		if (tbl != null)
		{
			ViewReader reader = ViewReaderFactory.createViewReader(currentConnection);
			source = reader.getExtendedViewSource(tbl, DropType.none);
		}

		if (StringUtil.isNonEmpty(source))
		{
      if (outputfile != null)
      {
        try
        {
          FileUtil.writeString(outputfile, source.toString(), encoding, false);
          result.addMessageByKey("MsgScriptWritten", outputfile.getFullPath());
          result.setSuccess();
        }
        catch (IOException io)
        {
          result.addErrorMessage(io.getLocalizedMessage());
        }
      }
      else
      {
  			result.addMessage(source);
        result.setSuccess();
      }
		}
		else
		{
			result.addErrorMessageByKey("ErrViewNotFound", object.getObjectExpression(currentConnection));
		}

		return result;
	}

	@Override
	public boolean isWbCommand()
	{
		return true;
	}
}
