/*
 * WbListTriggers.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.SQLException;

import workbench.console.ConsoleSettings;
import workbench.console.RowDisplay;
import workbench.db.TriggerReader;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.storage.DataStore;

/**
 * List all triggers defined for the current schema.
 * <br>
 * This is the same information as displayed in the DbExplorer's "Triggers" tab.
 *
 * @see workbench.db.TriggerReader#getTriggers(java.lang.String, java.lang.String) 
 * @author Thomas Kellerer
 */
public class WbListTriggers extends SqlCommand
{
	public static final String VERB = "WBLISTTRIGGERS";
	public static final String FORMATTED_VERB = "WbListTriggers";

	public WbListTriggers()
	{
	}
	public String getVerb() { return VERB; }

	public StatementRunnerResult execute(String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		ConsoleSettings.getInstance().setNextRowDisplay(RowDisplay.SingleLine);

		TriggerReader reader = new TriggerReader(this.currentConnection);

		DataStore ds = reader.getTriggers(currentConnection.getMetadata().getCurrentCatalog(), currentConnection.getCurrentSchema());
		ds.setResultName(ResourceMgr.getString("TxtDbExplorerTriggers"));
		result.addDataStore(ds);
		return result;
	}

}
