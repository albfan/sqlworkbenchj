/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016 Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0 (the "License")
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.sql-workbench.net/manual/license.html
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

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.util.List;

import workbench.interfaces.ScriptGenerationMonitor;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.db.ObjectScripter;
import workbench.db.TableIdentifier;

import workbench.storage.RowActionMonitor;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.EncodingUtil;
import workbench.util.FileUtil;
import workbench.util.StringBuilderOutput;
import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 *
 * @author Thomas Kellerer
 */
public class WbGenerateFKScript
  extends SqlCommand
	implements ScriptGenerationMonitor
{
	public static final String VERB = "WbGenerateFKScript";
  private ObjectScripter scripter;

  public WbGenerateFKScript()
  {
    super();
    this.isUpdatingCommand = false;
    cmdLine = new ArgumentParser();
    cmdLine.addArgument(CommonArgs.ARG_TABLES, ArgumentType.TableArgument);
    cmdLine.addArgument(CommonArgs.ARG_EXCLUDE_TABLES, ArgumentType.TableArgument);
    cmdLine.addArgument(CommonArgs.ARG_FILE, ArgumentType.Filename);
    CommonArgs.addEncodingParameter(cmdLine);
  }

  @Override
  public StatementRunnerResult execute(String sql)
    throws SQLException, Exception
  {
		StatementRunnerResult result = new StatementRunnerResult();
		String args = getCommandLine(sql);
		cmdLine.parse(args);

    if (displayHelp(result))
    {
      return result;
    }

    String tableNames = cmdLine.getValue(CommonArgs.ARG_TABLES);
		String excludeTables = cmdLine.getValue(CommonArgs.ARG_EXCLUDE_TABLES);

		if (StringUtil.isBlank(tableNames))
		{
			tableNames = "%";
		}

		String[] types = currentConnection.getMetadata().getTableTypesArray();
		SourceTableArgument parser = new SourceTableArgument(tableNames, excludeTables, null, types, currentConnection);

		List<TableIdentifier> tables = parser.getTables();

		WbFile output = evaluateFileArgument(cmdLine.getValue(CommonArgs.ARG_FILE));

		scripter = new ObjectScripter(tables, currentConnection);
		scripter.setUseSeparator(cmdLine.getBoolean(WbGenerateScript.ARG_USE_SEPARATOR, false));

		if (this.rowMonitor != null)
		{
			rowMonitor.saveCurrentType("genfkscript");
			rowMonitor.setMonitorType(RowActionMonitor.MONITOR_PROCESS_TABLE);
		}
		scripter.setProgressMonitor(this);

    StringBuilderOutput script = new StringBuilderOutput(tables.size() * 100);
    scripter.setTextOutput(script);

		try
		{
			scripter.generateForeignKeys();
		}
		finally
		{
			if (rowMonitor != null)
			{
				rowMonitor.restoreType("genfkscript");
				rowMonitor.jobFinished();
			}
		}

		if (isCancelled)
		{
			result.setWarning();
			return result;
		}

		result.setSuccess();


		if (output != null)
		{
      String encoding = cmdLine.getValue(CommonArgs.ARG_ENCODING, EncodingUtil.getDefaultEncoding());
			Writer writer = null;
			try
			{
				writer = EncodingUtil.createWriter(output, encoding, false);
				writer.write(script.toString());
				result.addMessage(ResourceMgr.getFormattedString("MsgScriptWritten", output.getAbsolutePath()));
			}
			catch (IOException io)
			{
				LogMgr.logError("WbGenerateFKScript.execute()", "Could not write outputfile", io);
				result.setFailure();
				result.addMessage(io.getLocalizedMessage());
			}
			finally
			{
				FileUtil.closeQuietely(writer);
			}
		}
		else
		{
			result.addMessage(script.toString());
		}
    return result;
  }

	@Override
	public void cancel()
		throws SQLException
	{
		super.cancel();
		if (scripter != null)
		{
			scripter.cancel();
		}
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

	@Override
	public void done()
	{
		scripter = null;
	}

	@Override
	public void setCurrentObject(String anObject, int current, int count)
	{
		if (this.rowMonitor != null)
		{
      rowMonitor.setCurrentObject(anObject, current, count);
		}
	}

	@Override
	public boolean isWbCommand()
	{
		return true;
	}

}
