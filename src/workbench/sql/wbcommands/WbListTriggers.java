/*
 * WbListTables.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.SQLException;

import workbench.db.TriggerReader;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.storage.DataStore;

/**
 *
 * @author  support@sql-workbench.net
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
		TriggerReader reader = new TriggerReader(this.currentConnection);

		DataStore ds = reader.getTriggers(currentConnection.getMetadata().getCurrentCatalog(), currentConnection.getCurrentSchema());
		
		result.addDataStore(ds);
		return result;
	}

}
