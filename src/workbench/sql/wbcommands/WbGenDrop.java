/*
 * WbGenDrop.java
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

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import workbench.resource.ResourceMgr;

import workbench.db.DropScriptGenerator;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.ExceptionUtil;
import workbench.util.FileUtil;
import workbench.util.WbFile;

import static workbench.sql.wbcommands.WbGenDrop.*;

/**
 * A SqlCommand to create a DROP script for one or more tables that will drop referencing foreign keys
 * before dropping the table(s).
 *
 * This can be used as an alternative to DROP ... CASCADE
 *
 * @author Thomas Kellerer
 */
public class WbGenDrop
	extends SqlCommand
{
	public static final String VERB = "WbGenerateDrop";

	public static final String PARAM_TABLES = "tables";
	public static final String PARAM_FILE = "outputFile";
	public static final String PARAM_DIR = "outputDir";
	public static final String PARAM_INCLUDE_CREATE = "includeCreate";
	public static final String PARAM_DROP_FK_ONLY = "onlyForeignkeys";
	public static final String PARAM_SORT_BY_TYPE = "sortByType";
	public static final String PARAM_INCLUDE_COMMENTS = "includeMarkers";

	public WbGenDrop()
	{
		super();
		this.isUpdatingCommand = true;
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(PARAM_DIR, ArgumentType.DirName);
		cmdLine.addArgument(PARAM_FILE, ArgumentType.Filename);
		cmdLine.addArgument(PARAM_TABLES, ArgumentType.TableArgument);
		cmdLine.addArgument(PARAM_INCLUDE_CREATE, ArgumentType.BoolArgument);
		cmdLine.addArgument(PARAM_DROP_FK_ONLY, ArgumentType.BoolArgument);
		cmdLine.addArgument(PARAM_SORT_BY_TYPE, ArgumentType.BoolArgument);
		cmdLine.addArgument(PARAM_INCLUDE_COMMENTS, ArgumentType.BoolArgument);
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
			setUnknownMessage(result, cmdLine, ResourceMgr.getString("ErrGenDropWrongParam"));
			result.setFailure();
			return result;
		}
		if (!cmdLine.hasArguments())
		{
			result.addMessage(ResourceMgr.getString("ErrGenDropWrongParam"));
			result.setFailure();
			return result;
		}

		SourceTableArgument tableArg = new SourceTableArgument(cmdLine.getValue(PARAM_TABLES), currentConnection);
		List<TableIdentifier> tables = tableArg.getTables();

		if (tables.isEmpty())
		{
			result.addMessage(ResourceMgr.getFormattedString("ErrExportNoTablesFound", cmdLine.getValue(PARAM_TABLES)));
			result.setFailure();
			return result;
		}

		boolean includeCreate = cmdLine.getBoolean(PARAM_INCLUDE_CREATE, true);
		boolean onlyFk = cmdLine.getBoolean(PARAM_DROP_FK_ONLY, false);
		boolean sortByType = cmdLine.getBoolean(PARAM_SORT_BY_TYPE, true);
		boolean includeComments = cmdLine.getBoolean(PARAM_INCLUDE_COMMENTS, false);

		DropScriptGenerator gen = new DropScriptGenerator(currentConnection);
		gen.setIncludeRecreateStatements(includeCreate);
		gen.setIncludeComments(includeComments);

		if (onlyFk)
		{
			gen.setIncludeRecreateStatements(false);
			gen.setIncludeDropTable(false);
		}

		gen.setTables(tables);
		gen.setSortByType(sortByType);
		gen.setRowMonitor(this.rowMonitor);
		String dir = cmdLine.getValue(PARAM_DIR, null);
		String file = cmdLine.getValue(PARAM_FILE, null);

		gen.generateScript();

		List<TableIdentifier> processed = gen.getTables();

		if (dir != null)
		{
			WbFile dirFile = new WbFile(dir);
			if (!dirFile.isDirectory())
			{
				result.addMessage(ResourceMgr.getFormattedString("ErrExportOutputDirNotDir", dir));
				result.setFailure();
				return result;
			}

			int count = 0;
			for (TableIdentifier tbl : processed)
			{
				WbFile output = new WbFile(dirFile, "drop_" + tbl.getTableName().toLowerCase() + ".sql");
				try
				{
					FileUtil.writeString(output, gen.getScriptFor(tbl));
					count ++;
				}
				catch (IOException io)
				{
					result.addMessageByKey("ErrFileCreate");
					result.addMessage(ExceptionUtil.getDisplay(io));
					result.setFailure();
					return result;
				}
			}
			result.setSuccess();
			result.addMessage(ResourceMgr.getFormattedString("MsgDropScriptsWritten", Integer.valueOf(count), dirFile.getFullPath()));
		}
		else if (file != null)
		{
			WbFile output = new WbFile(file);
			try
			{
				FileUtil.writeString(output, gen.getScript());
				result.addMessage(ResourceMgr.getFormattedString("MsgScriptWritten", output.getFullPath()));
				result.setSuccess();
			}
			catch (IOException io)
			{
				result.addMessageByKey("ErrFileCreate");
				result.addMessage(ExceptionUtil.getDisplay(io));
				result.setFailure();
			}
		}
		else
		{
			result.setSuccess();
			result.addMessage(gen.getScript());
		}
		return result;
	}

	@Override
	public void cancel()
		throws SQLException
	{
		super.cancel();
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
	public boolean isWbCommand()
	{
		return true;
	}

}
