/*
 * WbListProcedures.java
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

import java.sql.SQLException;
import java.sql.Types;

import workbench.console.ConsoleSettings;
import workbench.console.RowDisplay;
import workbench.resource.ResourceMgr;

import workbench.db.DbObject;
import workbench.db.JdbcProcedureReader;
import workbench.db.ProcedureReader;
import workbench.db.TableIdentifier;

import workbench.storage.DataStore;
import workbench.storage.ResultInfo;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.StringUtil;


/**
 * List all procedures and functions available to the current user.
 * <br>
 * This is the same information as displayed in the DbExplorer's "Procedure" tab.
 *
 * @see workbench.db.ProcedureReader
 * @author Thomas Kellerer
 */
public class WbListProcedures
	extends SqlCommand
{
	public static final String VERB = "WbListProcs";
	public static final String ALTERNATE_VERB = "WbListProcedures";

	public WbListProcedures()
	{
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(CommonArgs.ARG_SCHEMA, ArgumentType.SchemaArgument);
		cmdLine.addArgument(CommonArgs.ARG_CATALOG, ArgumentType.CatalogArgument);
		cmdLine.addArgument("package");
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

  @Override
  public String getAlternateVerb()
  {
    return ALTERNATE_VERB;
  }

	@Override
	public StatementRunnerResult execute(String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		ConsoleSettings.getInstance().setNextRowDisplay(RowDisplay.SingleLine);

		String args = getCommandLine(aSql);

		cmdLine.parse(args);
    if (displayHelp(result))
    {
      return result;
    }

		String schema = null;
		String catalog = null;
		String name = null;

		if (cmdLine.hasArguments())
		{
			schema = cmdLine.getValue(CommonArgs.ARG_SCHEMA);
			catalog = cmdLine.getValue(CommonArgs.ARG_CATALOG);
			if (StringUtil.isBlank(catalog))
			{
				catalog = cmdLine.getValue("package");
			}
		}
		else if (StringUtil.isNonBlank(args))
		{
			DbObject db = new TableIdentifier(args, currentConnection);
			schema = db.getSchema();
			catalog = db.getCatalog();
			name = db.getObjectName();
		}

		if (schema == null)
		{
			schema = currentConnection.getCurrentSchema();
		}

		if (catalog == null)
		{
			catalog = currentConnection.getCurrentCatalog();
		}

		DataStore ds = currentConnection.getMetadata().getProcedureReader().getProcedures(catalog, schema, name);

		// Displaying this DataStore will not use the ProcStatusRenderer to display the
		// "ResultType" column. So we just convert the integer value to a string
		int count = ds.getRowCount();
		for (int row=0; row < count; row++)
		{
			int type = ds.getValueAsInt(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE, -1);
			String typeName = JdbcProcedureReader.convertProcTypeToSQL(type);
			ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE, typeName);
		}

		// adjust the type info, just be sure
		ResultInfo info = ds.getResultInfo();
		info.getColumn(ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE).setDataType(Types.VARCHAR);
		ds.setResultName(ResourceMgr.getString("TxtDbExplorerProcs"));
		ds.setGeneratingSql(aSql);
		ds.resetStatus();

		result.addDataStore(ds);
		result.setSuccess();
		return result;
	}

	@Override
	public boolean isWbCommand()
	{
		return true;
	}
}
