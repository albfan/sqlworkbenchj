/*
 * WbGenDelete.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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

import java.sql.SQLException;
import java.util.List;

import workbench.db.DummyInsert;
import workbench.resource.ResourceMgr;

import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.importer.TableDependencySorter;
import workbench.interfaces.ScriptGenerationMonitor;


import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.storage.RowActionMonitor;

import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;

/**
 * A SqlCommand to create a script of DELETE statement to delete specific rows from a table respecting FK constraints.
 *
 * @author Thomas Kellerer
 */
public class WbGenInsert
	extends SqlCommand
	implements ScriptGenerationMonitor
{
	public static final String VERB = "WBGENERATEINSERT";

	public static final String PARAM_TABLES = "tables";
	public static final String PARAM_FILE = "outputFile";
	public static final String PARAM_DO_FORMAT = "formatSql";
	public static final String PARAM_APPEND = "appendFile";

	private TableDependencySorter tableSorter;

	public WbGenInsert()
	{
		super();
		this.isUpdatingCommand = true;
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(PARAM_FILE, ArgumentType.StringArgument);
		cmdLine.addArgument(PARAM_DO_FORMAT, ArgumentType.BoolArgument);
		cmdLine.addArgument(PARAM_TABLES, ArgumentType.TableArgument);
		cmdLine.addArgument(PARAM_APPEND, ArgumentType.BoolSwitch);
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
			result.setFailure();
			return result;
		}

		if (!cmdLine.hasArguments())
		{
			result.addMessage(ResourceMgr.getString("ErrGenDropWrongParam"));
			result.setFailure();
			return result;
		}

		String names = cmdLine.getValue(PARAM_TABLES);
		SourceTableArgument tableArgs = new SourceTableArgument(names, currentConnection);

		List<TableIdentifier> tables = tableArgs.getTables();

		if (CollectionUtil.isEmpty(tables))
		{
			result.addMessage(ResourceMgr.getFormattedString("ErrTableNotFound", names));
			result.setFailure();
			return result;
		}

		tableSorter = new TableDependencySorter(this.currentConnection);

		if (this.rowMonitor != null)
		{
			rowMonitor.setMonitorType(RowActionMonitor.MONITOR_PROCESS_TABLE);
			tableSorter.setProgressMonitor(this);
		}

		List<TableIdentifier> sorted = tableSorter.sortForInsert(tables);

		if (this.rowMonitor != null)
		{
			rowMonitor.jobFinished();
		}

		for (TableIdentifier table : sorted)
		{
			DummyInsert insert = new DummyInsert(table);
			insert.setFormatSql(false);
			String source = insert.getSource(currentConnection).toString();
			result.addMessage(SqlUtil.makeCleanSql(source,false));
			result.setSuccess();
		}
		return result;
	}


	@Override
	public void cancel()
		throws SQLException
	{
		super.cancel();
		if (tableSorter != null)
		{
			tableSorter.cancel();
		}
	}

	@Override
	public void done()
	{
		super.done();
		tableSorter = null;
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

}
