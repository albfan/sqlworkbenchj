/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014 Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import workbench.console.ConsoleSettings;
import workbench.console.RowDisplay;
import workbench.resource.ResourceMgr;

import workbench.db.DbMetadata;
import workbench.db.JdbcUtils;
import workbench.db.TableIdentifier;
import workbench.db.TableSelectBuilder;
import workbench.db.WbConnection;

import workbench.storage.DataStore;
import workbench.storage.RowActionMonitor;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.SqlUtil;

/**
 * A class to count rows from the specified tables.
 *
 * It's the commandline version of the DbExplorer's  "Count rows" feature.
 *
 * @author Thomas Kellerer
 */
public class WbRowCount
	extends SqlCommand
{
	public static final String VERB = "WbRowCount";

	public WbRowCount()
	{
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(CommonArgs.ARG_OBJECTS);
		cmdLine.addArgument(CommonArgs.ARG_TYPES, ArgumentType.ObjectTypeArgument);
		cmdLine.addArgument(CommonArgs.ARG_SCHEMA, ArgumentType.SchemaArgument);
		cmdLine.addArgument(CommonArgs.ARG_CATALOG, ArgumentType.CatalogArgument);
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

	public static DataStore buildResultDataStore(WbConnection connection)
	{
		String[] tableListColumns = connection.getMetadata().getTableListColumns();
		String[] columns = new String[tableListColumns.length];

		columns[0] = ResourceMgr.getString("TxtRowCnt").toUpperCase();
		columns[1] = tableListColumns[0];
		columns[2] = tableListColumns[1];
		columns[3] = tableListColumns[2];
		columns[4] = tableListColumns[3];

		DataStore ds = new DataStore(columns, new int[]	{ Types.BIGINT, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR});
		return ds;
	}

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException
	{
		String options = getCommandLine(sql);

		StatementRunnerResult result = new StatementRunnerResult();
		ConsoleSettings.getInstance().setNextRowDisplay(RowDisplay.SingleLine);

		cmdLine.parse(options);
		if (cmdLine.hasUnknownArguments())
		{
			result.addMessage(ResourceMgr.getString("ErrListWrongArgs"));
			result.setFailure();
			return result;
		}

		ObjectLister lister = new ObjectLister();
		if (this.rowMonitor != null)
		{
			rowMonitor.setMonitorType(RowActionMonitor.MONITOR_PLAIN);
			rowMonitor.setCurrentObject("Retrieving tables", -1, -1);
		}

		DataStore resultList = lister.getObjects(cmdLine, options, currentConnection);

		if (resultList == null)
		{
			result.setFailure();
			result.addMessage("Not tables found!");
		}

		DbMetadata meta = currentConnection.getMetadata();
		boolean useSavepoint = currentConnection.getDbSettings().useSavePointForDML();

		DataStore rowCounts = buildResultDataStore(currentConnection);
		TableSelectBuilder builder = new TableSelectBuilder(currentConnection, "tabledata");
		currentStatement = currentConnection.createStatementForQuery();

		int tableCount = resultList.getRowCount();
		for (int row=0; row < tableCount; row++)
		{
			ResultSet rs = null;
			try
			{
				TableIdentifier table = meta.buildTableIdentifierFromDs(resultList, row);

				String countQuery = builder.getSelectForCount(table);
				String msg = ResourceMgr.getFormattedString("MsgCalculatingRowCount", table.getTableExpression(), row + 1, tableCount);

				rowMonitor.setCurrentObject(msg, row + 1, tableCount);
				rs = JdbcUtils.runStatement(currentConnection, currentStatement, countQuery, false, useSavepoint);

				long rowCount = -1;
				if (rs != null && rs.next())
				{
					rowCount = rs.getLong(1);
				}

				int dsRow = rowCounts.addRow();
				rowCounts.setValue(dsRow, 0, Long.valueOf(rowCount));
				rowCounts.setValue(dsRow, 1, table.getTableName());
				rowCounts.setValue(dsRow, 2, table.getObjectType());
				rowCounts.setValue(dsRow, 3, table.getCatalog());
				rowCounts.setValue(dsRow, 4, table.getSchema());
			}
			finally
			{
				SqlUtil.closeResult(rs);
			}
		}
		rowCounts.resetStatus();
		result.addDataStore(rowCounts);

		return result;
	}
}
