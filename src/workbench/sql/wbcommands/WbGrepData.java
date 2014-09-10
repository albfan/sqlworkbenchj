/*
 * WbGrepData.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
import java.util.ArrayList;
import java.util.List;

import workbench.interfaces.ExecutionController;
import workbench.interfaces.TableSearchConsumer;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.JdbcUtils;
import workbench.db.TableIdentifier;
import workbench.db.search.ClientSideTableSearcher;

import workbench.storage.DataStore;
import workbench.storage.RowActionMonitor;
import workbench.storage.filter.ColumnComparator;
import workbench.storage.filter.ContainsComparator;
import workbench.storage.filter.IsNullComparator;
import workbench.storage.filter.RegExComparator;
import workbench.storage.filter.StartsWithComparator;
import workbench.storage.filter.StringEqualsComparator;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.CollectionUtil;
import workbench.util.StringUtil;

/**
 * A class to search for text in all columns of all tables.
 * <br/>
 * This SQL commands parses the input and then uses {@link workbench.db.search.ClientSideTableSearcher}
 * to perform the search.
 *
 * @author Thomas Kellerer
 */
public class WbGrepData
	extends SqlCommand
	implements TableSearchConsumer
{
	public static final String VERB = "WbGrepData";
	public static final String PARAM_TABLES = "tables";
	public static final String PARAM_EXPRESSION = "searchValue";
	public static final String PARAM_EXCLUDE_LOBS = "excludeLobs";
	public static final String PARAM_IGNORE_CASE = "ignoreCase";

	public static final String PARAM_COMPARATOR = "compareType";
	public static final String PARAM_COLUMNS = "columns";

	private ClientSideTableSearcher searcher;
	private StatementRunnerResult searchResult;
	private int foundTables;
	private List<String> searchedTables;

	public WbGrepData()
	{
		super();
		this.isUpdatingCommand = false;

		cmdLine = new ArgumentParser();
		cmdLine.addArgument(PARAM_TABLES, ArgumentType.TableArgument);
		cmdLine.addArgument(CommonArgs.ARG_EXCLUDE_TABLES, ArgumentType.TableArgument);
		cmdLine.addArgument(CommonArgs.ARG_TYPES, ArgumentType.ObjectTypeArgument);
		cmdLine.addArgument(PARAM_EXCLUDE_LOBS, ArgumentType.BoolArgument);
		cmdLine.addArgument(PARAM_IGNORE_CASE, ArgumentType.BoolArgument);
		cmdLine.addArgument(PARAM_EXPRESSION);
		cmdLine.addArgument(PARAM_COLUMNS);
		cmdLine.addArgument(PARAM_COMPARATOR, CollectionUtil.arrayList("equals", "startsWith", "contains", "matches", "isNull"));
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
		searchResult = new StatementRunnerResult();
		String args = getCommandLine(sql);
		cmdLine.parse(args);

		if (cmdLine.hasUnknownArguments())
		{
			setUnknownMessage(searchResult, cmdLine, ResourceMgr.getString("ErrDataSearchWrongParms"));
			searchResult.setFailure();
			return searchResult;
		}

		String searchValue = cmdLine.getValue(PARAM_EXPRESSION);
		boolean needSearchValue = true;

		String tableNames = cmdLine.getValue(PARAM_TABLES);
		String excludeTables = cmdLine.getValue(CommonArgs.ARG_EXCLUDE_TABLES);
		List<TableIdentifier> tables = null;

		if (StringUtil.isBlank(tableNames))
		{
			tableNames = "%";
		}

		String[] types = SourceTableArgument.parseTypes(cmdLine.getValue(CommonArgs.ARG_TYPES), currentConnection) ;
		SourceTableArgument parser = new SourceTableArgument(tableNames, excludeTables, null, types, currentConnection);

		tables = parser.getTables();

		searcher = new ClientSideTableSearcher();
		searcher.setExcludeLobColumns(cmdLine.getBoolean(PARAM_EXCLUDE_LOBS, false));
		searcher.setConnection(currentConnection);
		searcher.setTableNames(tables);

		boolean ignoreCase = cmdLine.getBoolean(PARAM_IGNORE_CASE, true);
		String comparatorType = cmdLine.getValue(PARAM_COMPARATOR);
		if (StringUtil.isBlank(comparatorType))
		{
			comparatorType = "contains";
		}

		ColumnComparator comp = null;
		if ("equals".equalsIgnoreCase(comparatorType))
		{
			comp = new StringEqualsComparator();
		}
		else if ("startsWith".equalsIgnoreCase(comparatorType))
		{
			comp = new StartsWithComparator();
		}
		else if ("matches".equalsIgnoreCase(comparatorType))
		{
			comp = new RegExComparator();
		}
		else if ("isnull".equalsIgnoreCase(comparatorType))
		{
			comp = new IsNullComparator();
			needSearchValue = false;
		}
		else
		{
			comp = new ContainsComparator();
		}

		if (needSearchValue && StringUtil.isBlank(searchValue))
		{
			searchResult.addMessage(ResourceMgr.getString("ErrDataSearchValueReq"));
			searchResult.addMessage(ResourceMgr.getString("ErrDataSearchWrongParms"));
			searchResult.setFailure();
			return searchResult;
		}

		List<String> columns = cmdLine.getListValue(PARAM_COLUMNS);
		searcher.setComparator(comp);
		searcher.setConsumer(this);
		searcher.setCriteria(searchValue, ignoreCase, columns);

		if (Settings.getInstance().getBoolProperty("workbench.searchdata.warn.buffer", true) &&
				JdbcUtils.driverMightBufferResults(currentConnection))
		{
			ExecutionController controller = this.runner.getExecutionController();
			boolean goOn = controller.confirmExecution(ResourceMgr.getString("MsgTableSearchBuffered"), null, null);
			if (!goOn)
			{
				searchResult.setFailure();
				searchResult.addMessageByKey("MsgStatementCancelled");
				return searchResult;
			}
		}

		if (rowMonitor != null)
		{
			rowMonitor.setMonitorType(RowActionMonitor.MONITOR_PROCESS);
		}

		searchResult.setSuccess();
		searcher.search();

		StringBuilder summary = new StringBuilder(searchedTables.size() * 20);
		summary.append(ResourceMgr.getString("MsgSearchedTables"));
		for (String table : searchedTables)
		{
			summary.append("\n  ");
			summary.append(table);
		}
		summary.append('\n');
		searchResult.addMessage(summary.toString());
		String msg = ResourceMgr.getFormattedString("MsgSearchDataFinished", searchedTables.size(), foundTables);
		searchResult.addMessage(msg);

		return searchResult;
	}

	@Override
	public void cancel()
		throws SQLException
	{
		super.cancel();
		if (searcher != null)
		{
			searcher.cancelSearch();
		}
	}

	@Override
	public void done()
	{
		super.done();
		searcher = null;
	}

	@Override
	public void setCurrentTable(String tableName, String query, long number, long totalObjects)
	{
		if (rowMonitor != null)
		{
			rowMonitor.setCurrentObject(tableName, number, totalObjects);
		}
	}

	@Override
	public void error(String msg)
	{
		searchResult.addMessage(msg);
		searchResult.setFailure();
	}

	@Override
	public void tableSearched(TableIdentifier table, DataStore result)
	{
		searchedTables.add(table.getTableName());
		if (result != null && result.getRowCount() > 0)
		{
			result.resetStatus();
			result.setGeneratingFilter(searcher.getSearchExpression());
			searchResult.addDataStore(result);
			foundTables ++;
		}
	}

	@Override
	public void setStatusText(String message)
	{
		if (rowMonitor != null)
		{
			rowMonitor.setCurrentObject(message, -1, -1);
		}
	}

	@Override
	public void searchStarted()
	{
		if (rowMonitor != null)
		{
			// clear the status display
			rowMonitor.jobFinished();
		}
		searchedTables = new ArrayList<>(50);
		foundTables = 0;
	}

	@Override
	public void searchEnded()
	{
		if (rowMonitor != null)
		{
			rowMonitor.jobFinished();
		}
	}

	@Override
	public boolean isWbCommand()
	{
		return true;
	}

}
