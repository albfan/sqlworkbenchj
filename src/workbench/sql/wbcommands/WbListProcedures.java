/*
 * WbListProcedures.java
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

import java.sql.SQLException;

import workbench.console.ConsoleSettings;
import workbench.console.RowDisplay;
import workbench.resource.ResourceMgr;

import workbench.db.DbObject;
import workbench.db.TableIdentifier;

import workbench.storage.DataStore;

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

	public WbListProcedures()
	{
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(CommonArgs.ARG_SCHEMA, ArgumentType.SchemaArgument);
		cmdLine.addArgument(CommonArgs.ARG_CATALOG, ArgumentType.CatalogArgument);
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

	@Override
	public StatementRunnerResult execute(String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		ConsoleSettings.getInstance().setNextRowDisplay(RowDisplay.SingleLine);

		String args = getCommandLine(aSql);

		cmdLine.parse(args);

		String schema = null;
		String catalog = null;
		String name = null;

		if (cmdLine.hasArguments())
		{
			schema = cmdLine.getValue(CommonArgs.ARG_SCHEMA);
			catalog = cmdLine.getValue(CommonArgs.ARG_CATALOG);
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
		ds.setResultName(ResourceMgr.getString("TxtDbExplorerProcs"));
		ds.setGeneratingSql(aSql);
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
