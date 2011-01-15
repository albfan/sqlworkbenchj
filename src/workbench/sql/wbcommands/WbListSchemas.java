/*
 * WbListCatalogs.java
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

import java.sql.Types;
import java.util.List;
import workbench.WbManager;
import workbench.console.ConsoleSettings;
import workbench.console.RowDisplay;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.storage.DataStore;
import workbench.util.StringUtil;

/**
 *
 * @author  Thomas Kellerer
 */
public class WbListSchemas
	extends SqlCommand
{
	public static final String VERB = "WBLISTSCHEMAS";

	public WbListSchemas()
	{
		super();
	}

	public String getVerb() { return VERB; }

	public StatementRunnerResult execute(String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		ConsoleSettings.getInstance().setNextRowDisplay(RowDisplay.SingleLine);

		List<String> schemas = currentConnection.getMetadata().getSchemas();
		String schemaName = StringUtil.capitalize(currentConnection.getMetadata().getSchemaTerm());
		String[] cols = { schemaName };
		int[] types = { Types.VARCHAR };
		int[] sizes = { 10 };

		DataStore ds = new DataStore(cols, types, sizes);
		for (String cat : schemas)
		{
			int row = ds.addRow();
			ds.setValue(row, 0, cat);
		}
		ds.resetStatus();
		if (!WbManager.getInstance().isConsoleMode())
		{
			ds.setResultName(schemaName);
		}
		result.addDataStore(ds);
		result.setSuccess();
		return result;
	}

}
