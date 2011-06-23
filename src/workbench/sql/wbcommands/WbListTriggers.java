/*
 * WbListTriggers.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
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
		cmdLine.addArgument("schema", ArgumentType.SchemaArgument);
		cmdLine.addArgument("catalog", ArgumentType.CatalogArgument);
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

		TriggerReader reader = TriggerReaderFactory.createReader(this.currentConnection);

		DataStore ds = reader.getTriggers(currentConnection.getMetadata().getCurrentCatalog(), currentConnection.getCurrentSchema());
		ds.setResultName(ResourceMgr.getString("TxtDbExplorerTriggers"));
		result.addDataStore(ds);
		return result;
	}
}
