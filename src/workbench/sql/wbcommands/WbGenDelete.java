/*
 * WbGenDelete.java
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
import java.util.ArrayList;
import java.util.List;

import workbench.interfaces.ScriptGenerationMonitor;
import workbench.resource.ResourceMgr;

import workbench.db.ColumnIdentifier;
import workbench.db.CommitType;
import workbench.db.DeleteScriptGenerator;
import workbench.db.TableDeleter;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.storage.ColumnData;
import workbench.storage.RowActionMonitor;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.CollectionUtil;
import workbench.util.ExceptionUtil;
import workbench.util.FileUtil;
import workbench.util.WbFile;

/**
 * A SqlCommand to create a script of DELETE statement to delete specific rows from a table respecting FK constraints.
 *
 * @author Thomas Kellerer
 */
public class WbGenDelete
	extends SqlCommand
	implements ScriptGenerationMonitor
{
	public static final String VERB = "WbGenerateDelete";

	public static final String ARG_TABLE = "table";
	public static final String ARG_COLUMN_VAL = "columnValue";
	public static final String ARG_DO_FORMAT = "formatSql";
	public static final String ARG_INCLUDE_COMMIT = "includeCommit";
	public static final String ARG_APPEND = "appendFile";
	public static final String ARG_SHOW_FK_NAMES = "showConstraints";
	public static final String ARG_EXCLUDE_TABLES = "excludeTables";
	public static final String ARG_USE_TRUNCATE = "useTruncate";

	private DeleteScriptGenerator generator;
	private TableDeleter simpleGenerator;

	public WbGenDelete()
	{
		super();
		this.isUpdatingCommand = true;
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(CommonArgs.ARG_OUTPUT_FILE, ArgumentType.Filename);
		cmdLine.addArgument(ARG_DO_FORMAT, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_TABLE, ArgumentType.TableArgument);
		cmdLine.addArgument(ARG_COLUMN_VAL, ArgumentType.Repeatable);
		cmdLine.addArgument(ARG_INCLUDE_COMMIT, ArgumentType.BoolSwitch);
		cmdLine.addArgument(ARG_APPEND, ArgumentType.BoolSwitch);
		cmdLine.addArgument(ARG_SHOW_FK_NAMES, ArgumentType.BoolSwitch);
		cmdLine.addArgument(ARG_EXCLUDE_TABLES, ArgumentType.TableArgument);
	}

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException, Exception
	{
		StatementRunnerResult result = new StatementRunnerResult();
		String args = getCommandLine(sql);
		cmdLine.parse(args);

		if (cmdLine.hasUnknownArguments())
		{
			setUnknownMessage(result, cmdLine, ResourceMgr.getString("ErrGenDeleteWrongParam"));
			return result;
		}

		if (!cmdLine.hasArguments())
		{
			result.addErrorMessageByKey("ErrGenDropWrongParam");
			return result;
		}

    String[] types = currentConnection.getMetadata().getTableTypesArray();
    SourceTableArgument tableArg = new SourceTableArgument(cmdLine.getValue(ARG_TABLE), cmdLine.getValue(ARG_EXCLUDE_TABLES), null, types, currentConnection);
    List<TableIdentifier> tables = tableArg.getTables();
    if (CollectionUtil.isEmpty(tables))
		{
			result.addErrorMessageByKey("ErrTableNotFound", cmdLine.getValue(ARG_TABLE));
			return result;
		}

		List<String> cols = cmdLine.getList(ARG_COLUMN_VAL);
		List<ColumnData> values = new ArrayList<>();
		for (String def : cols)
		{
			String[] pair = def.split(":");
			if (pair.length == 2)
			{
				String column = pair[0];
				String value = pair[1];
				ColumnData data = new ColumnData(value, new ColumnIdentifier(column,ColumnIdentifier.NO_TYPE_INFO));
				values.add(data);
			}
			else
			{
				result.addErrorMessage("Illegal column specification: " + def);
				return result;
			}
		}

    if (tables.size() > 1 && CollectionUtil.isNonEmpty(values))
    {
      result.addErrorMessage("Deletion based on column value not supported for multiple tables");
      return result;
    }

		generator = new DeleteScriptGenerator(this.currentConnection);

    String script = null;
    CommitType commit = CommitType.never;
    if (cmdLine.getBoolean(ARG_INCLUDE_COMMIT))
    {
      commit = CommitType.once;
    }

    if (tables.size() == 1 && CollectionUtil.isNonEmpty(values))
    {
      SourceTableArgument exclude = new SourceTableArgument(cmdLine.getValue(ARG_EXCLUDE_TABLES), currentConnection);
      generator.setTable(tables.get(0));
      generator.setExcludedTables(exclude.getTables());
      generator.setShowConstraintNames(cmdLine.getBoolean(ARG_SHOW_FK_NAMES, false));
      generator.setFormatSql(cmdLine.getBoolean(ARG_DO_FORMAT, false));
      script = generator.getScriptForValues(values, commit);

      if (this.rowMonitor != null)
      {
        rowMonitor.setMonitorType(RowActionMonitor.MONITOR_PROCESS_TABLE);
        generator.setProgressMonitor(this);
      }
    }
    else
    {
      simpleGenerator = new TableDeleter(currentConnection);
      if (this.rowMonitor != null)
      {
        rowMonitor.setMonitorType(RowActionMonitor.MONITOR_PROCESS_TABLE);
        simpleGenerator.setScriptMonitor(this);
      }
      script = simpleGenerator.generateSortedScript(tables, commit, cmdLine.getBoolean(ARG_USE_TRUNCATE, false), true);
    }

    result.setSuccess();

    if (isCancelled)
    {
      result.addMessageByKey("MsgStatementCancelled");
      result.setWarning();
    }

		if (this.rowMonitor != null)
		{
			rowMonitor.jobFinished();
		}

		WbFile output = evaluateFileArgument(cmdLine.getValue(CommonArgs.ARG_OUTPUT_FILE));
		if (output != null && script != null)
		{
			boolean append = cmdLine.getBoolean(ARG_APPEND, false);
			try
			{
				FileUtil.writeString(output, script, append);
				result.addMessage(ResourceMgr.getFormattedString("MsgScriptWritten", output.getFullPath()));
			}
			catch (IOException io)
			{
				result.addErrorMessageByKey("ErrFileCreate", ExceptionUtil.getDisplay(io));
			}
		}
		else
		{
			result.addMessage(script);
		}
		return result;
	}


	@Override
	public void cancel()
		throws SQLException
	{
		super.cancel();
		if (generator != null)
		{
			generator.cancel();
		}
    if (simpleGenerator != null)
    {
      simpleGenerator.cancel();
    }
	}

	@Override
	public void done()
	{
		super.done();
		generator = null;
	}

	@Override
	public boolean isUpdatingCommand(WbConnection con, String sql)
	{
		return false;
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

	@Override
	public void setCurrentObject(String anObject, int current, int count)
	{
		if (this.rowMonitor != null)
		{
			if (anObject.indexOf(' ') > -1)
			{
				try
				{
					rowMonitor.saveCurrentType("genDel");
					rowMonitor.setMonitorType(RowActionMonitor.MONITOR_PLAIN);
					rowMonitor.setCurrentObject(anObject, current, count);
				}
				finally
				{
					rowMonitor.restoreType("genDel");
				}
			}
			else
			{
				rowMonitor.setCurrentObject(anObject, current, count);
			}
		}
	}

	@Override
	public boolean isWbCommand()
	{
		return true;
	}

}
