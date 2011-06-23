/*
 * WbListProcedures.java
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
import workbench.db.DbObject;
import workbench.db.TableIdentifier;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.storage.DataStore;
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
	public static final String VERB = "WBLISTPROCS";

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
			DbObject db = new TableIdentifier(args);
			schema = db.getSchema();
			catalog = db.getCatalog();
			name = db.getObjectName();
		}

		DataStore ds = currentConnection.getMetadata().getProcedureReader().getProcedures(catalog, schema, name);
		ds.setResultName(ResourceMgr.getString("TxtDbExplorerProcs"));
		result.addDataStore(ds);
		result.setSuccess();
		return result;
	}
}
