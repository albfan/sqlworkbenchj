/*
 * WbListTables.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.SQLException;

import java.util.List;
import workbench.console.ConsoleSettings;
import workbench.console.RowDisplay;
import workbench.db.TableIdentifier;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.storage.DataStore;
import workbench.util.ArgumentParser;
import workbench.util.StringUtil;

/**
 *
 * @author  support@sql-workbench.net
 */
public class WbListTables extends SqlCommand
{
	public static final String VERB = "WBLIST";

	public WbListTables()
	{
		cmdLine = new ArgumentParser();
		cmdLine.addArgument("objects");
		cmdLine.addArgument("types");
	}
	public String getVerb() { return VERB; }

	public StatementRunnerResult execute(String aSql)
		throws SQLException
	{
		String options = getCommandLine(aSql);

		String[] types = new String[]
			{
				currentConnection.getMetadata().getTableTypeName(),
				currentConnection.getMetadata().getMViewTypeName()
			};

		StatementRunnerResult result = new StatementRunnerResult();
		ConsoleSettings.getInstance().setNextRowDisplay(RowDisplay.SingleLine);

		cmdLine.parse(options);

		String objects = options;

		if (cmdLine.hasArguments())
		{
			if (cmdLine.hasUnknownArguments())
			{
				result.addMessage(ResourceMgr.getString("ErrListWrongArgs"));
				result.setFailure();
				return result;
			}

			objects = cmdLine.getValue("objects");

			List<String> typeList = cmdLine.getListValue("types");
			if (typeList.size() > 0)
			{
				types = new String[typeList.size()];
				typeList.toArray(types);
			}
		}

		DataStore resultList = null;

		if (StringUtil.isBlank(objects))
		{
			objects = "%";
		}

		List<String> objectFilters = StringUtil.stringToList(objects);

		for (String filter : objectFilters)
		{
			TableIdentifier tbl = new TableIdentifier(filter);
			String schema = tbl.getSchema();
			String catalog = tbl.getCatalog();
			String tname = tbl.getTableName();
			DataStore ds = currentConnection.getMetadata().getTables(schema, catalog, tname, types);
			if (resultList == null)
			{
				resultList = ds;
			}
			else
			{
				resultList.copyFrom(ds);
			}
		}

		result.addDataStore(resultList);
		return result;
	}

}
