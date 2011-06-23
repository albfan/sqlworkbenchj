/*
 * WbListTables.java
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

import java.util.List;
import workbench.console.ConsoleSettings;
import workbench.console.RowDisplay;
import workbench.db.TableIdentifier;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.storage.DataStore;
import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.StringUtil;

/**
 * List all tables available to the current user.
 * <br>
 * This is the same information as displayed in the DbExplorer's "Objects" tab.
 *
 * @see workbench.db.DbMetadata#getObjects(String, String, String, String[])
 * @author Thomas Kellerer
 */
public class WbListTables extends SqlCommand
{
	public static final String VERB = "WBLIST";

	public WbListTables()
	{
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(CommonArgs.ARG_OBJECTS);
		cmdLine.addArgument(CommonArgs.ARG_TYPES, ArgumentType.ObjectTypeArgument);
		cmdLine.addArgument(CommonArgs.ARG_SCHEMA, ArgumentType.SchemaArgument);
		cmdLine.addArgument(CommonArgs.ARG_CATALOG, ArgumentType.SchemaArgument);
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
		String options = getCommandLine(aSql);

		String[] types = currentConnection.getMetadata().getTableTypesArray();

		StatementRunnerResult result = new StatementRunnerResult();
		ConsoleSettings.getInstance().setNextRowDisplay(RowDisplay.SingleLine);

		cmdLine.parse(options);

		String objects = options;
		String schema = null;
		String catalog = null;

		if (cmdLine.hasUnknownArguments())
		{
			result.addMessage(ResourceMgr.getString("ErrListWrongArgs"));
			result.setFailure();
			return result;
		}

		if (cmdLine.hasArguments())
		{

			objects = cmdLine.getValue("objects");

			List<String> typeList = cmdLine.getListValue("types");
			if (typeList.size() > 0)
			{
				types = new String[typeList.size()];
				typeList.toArray(types);
			}
			schema = cmdLine.getValue(CommonArgs.ARG_SCHEMA);
			catalog = cmdLine.getValue(CommonArgs.ARG_CATALOG);
		}

		if (StringUtil.isBlank(schema)) schema = currentConnection.getMetadata().getSchemaToUse();

		if (StringUtil.isBlank(catalog)) catalog = currentConnection.getMetadata().getCurrentCatalog();

		DataStore resultList = null;

		if (StringUtil.isBlank(objects))
		{
			objects = "%";
		}

		List<String> objectFilters = StringUtil.stringToList(objects);

		for (String filter : objectFilters)
		{
			// Create a tableidentifier for parsing e.g. parameters
			// like -tables=public.*
			TableIdentifier tbl = new TableIdentifier(filter);
			String tschema = tbl.getSchema();
			if (StringUtil.isBlank(tschema))
			{
				tschema = schema;
			}
			tschema = currentConnection.getMetadata().adjustSchemaNameCase(tschema);
			String tcatalog = tbl.getCatalog();
			if (StringUtil.isBlank(tcatalog))
			{
				tcatalog = catalog;
			}
			tcatalog = currentConnection.getMetadata().adjustObjectnameCase(tcatalog);

			String tname = tbl.getTableName();

			DataStore ds = currentConnection.getMetadata().getObjects(tcatalog, tschema, tname, types);
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
