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

import java.util.List;
import workbench.db.TableIdentifier;
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

		String schema = null;
		String catalog = null;
		String tables = null;
		String[] types = new String[] 
			{
				currentConnection.getMetadata().getTableTypeName(),
				currentConnection.getMetadata().getMViewTypeName()
			};

		cmdLine.parse(options);
		String tableFilter = null;
		if (cmdLine.hasArguments())
		{
			tableFilter = cmdLine.getValue("objects");
			List<String> typeList = cmdLine.getListValue("types");
			if (typeList.size() > 0)
			{
				types = new String[typeList.size()];
				typeList.toArray(types);
			}
		}
		else
		{
			tableFilter = options;
		}
		
		if (StringUtil.isNonBlank(tableFilter))
		{
			TableIdentifier tbl = new TableIdentifier(tableFilter);
			schema = tbl.getSchema();
			catalog = tbl.getCatalog();
			tables = tbl.getTableName();
		}

		StatementRunnerResult result = new StatementRunnerResult();
		
		DataStore ds = currentConnection.getMetadata().getTables(schema, catalog, tables, types);
		
		result.addDataStore(ds);
		return result;
	}

}
