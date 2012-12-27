/*
 * WbListTriggers.java
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

import workbench.console.ConsoleSettings;
import workbench.console.RowDisplay;
import workbench.db.TriggerReader;
import workbench.db.TriggerReaderFactory;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.storage.DataStore;
import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;

/**
 * List all triggers defined for the current schema.
 * <br>
 * This is the same information as displayed in the DbExplorer's "Triggers" tab.
 *
 * @see workbench.db.TriggerReader#getTriggers(java.lang.String, java.lang.String)
 * @author Thomas Kellerer
 */
public class WbListTriggers
	extends SqlCommand
{
	public static final String VERB = "WBLISTTRIGGERS";
	public static final String FORMATTED_VERB = "WbListTriggers";

	public WbListTriggers()
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

		String options = getCommandLine(aSql);

		cmdLine.parse(options);

		ConsoleSettings.getInstance().setNextRowDisplay(RowDisplay.SingleLine);

		TriggerReader reader = TriggerReaderFactory.createReader(this.currentConnection);

		String schema = cmdLine.getValue(CommonArgs.ARG_SCHEMA, currentConnection.getCurrentSchema());
		String catalog = cmdLine.getValue(CommonArgs.ARG_CATALOG, currentConnection.getMetadata().getCurrentCatalog());
		DataStore ds = reader.getTriggers(catalog, schema);
		
		ds.setResultName(ResourceMgr.getString("TxtDbExplorerTriggers"));
		result.addDataStore(ds);
		return result;
	}
}
