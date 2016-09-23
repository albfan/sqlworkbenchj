/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql.wbcommands;

import java.sql.SQLException;

import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.EncodingUtil;
import workbench.util.FileUtil;
import workbench.util.WbFile;

/**
 *
 * @author Thomas Kellerer
 */
public class WbShowEncoding
  extends SqlCommand
{
  public static final String ARG_LIST = "list";
  public static final String ARG_FILE = "file";
  public static final String VERB = "WbShowEncoding";

  public WbShowEncoding()
  {
    cmdLine = new ArgumentParser();
    cmdLine.addArgument(ARG_LIST, ArgumentType.BoolSwitch);
    cmdLine.addArgument(ARG_FILE, ArgumentType.Filename);
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
  public StatementRunnerResult execute(String sql)
    throws SQLException
  {
    StatementRunnerResult result = new StatementRunnerResult();

    cmdLine.parse(getCommandLine(sql));

    if (displayHelp(result))
    {
      return result;
    }

    WbFile file = evaluateFileArgument(cmdLine.getValue(ARG_FILE));

    if (cmdLine.getBoolean(ARG_LIST))
    {
      result.addMessageByKey("MsgAvailableEncodings");
      result.addMessageNewLine();
      String[] encodings = EncodingUtil.getEncodings();
      for (String encoding : encodings)
      {
        result.addMessage(encoding);
      }
      result.addMessageNewLine();
    }
    else if (file != null)
    {
      String fMsg = "File " + file;
      if (file.exists())
      {
        String encoding = FileUtil.detectFileEncoding(file);
        if (encoding != null && encoding.toUpperCase().equals("UTF8"))
        {
          encoding += " with BOM";
        }
        result.addMessage(fMsg + " has encoding: " + encoding);
      }
      else
      {
        result.addErrorMessage(fMsg + " does not exist");
      }
    }

    String msg = ResourceMgr.getFormattedString("MsgDefaultEncoding", Settings.getInstance().getDefaultEncoding());
    result.addMessage(msg);
    result.setSuccess();
    return result;
  }

  @Override
  public boolean isWbCommand()
  {
    return true;
  }
}
