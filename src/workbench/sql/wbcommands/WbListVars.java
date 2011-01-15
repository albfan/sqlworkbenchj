/*
 * WbListVars.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.SQLException;

import workbench.console.ConsoleSettings;
import workbench.console.RowDisplay;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.VariablePool;
import workbench.sql.StatementRunnerResult;
import workbench.storage.DataStore;

/**
 * Display all variables defined through {@link WbDefineVar}
 *
 * @author Thomas Kellerer
 */
public class WbListVars extends SqlCommand
{
	public static final String VERB = "WBVARLIST";

	public String getVerb() { return VERB; }

	protected boolean isConnectionRequired() { return false; }

	public StatementRunnerResult execute(String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		ConsoleSettings.getInstance().setNextRowDisplay(RowDisplay.SingleLine);

		DataStore ds = VariablePool.getInstance().getVariablesDataStore();
		ds.setResultName(ResourceMgr.getString("TxtVariables"));
		result.addDataStore(ds);
		result.setSuccess();
		return result;
	}

}
