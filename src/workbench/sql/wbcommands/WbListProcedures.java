/*
 * WbListProcedures.java
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
import workbench.db.DbObject;
import workbench.db.TableIdentifier;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.storage.DataStore;
import workbench.util.StringUtil;


/**
 *
 * @author  support@sql-workbench.net
 */
public class WbListProcedures
	extends SqlCommand
{
	public static final String VERB = "WBLISTPROCS";

	public WbListProcedures()
	{
	}

	public String getVerb()
	{
		return VERB;
	}

	public StatementRunnerResult execute(String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		String args = getCommandLine(aSql);

		String schema = null;
		String catalog = null;
		String name = null;

		if (StringUtil.isNonBlank(args))
		{
			DbObject db = new TableIdentifier(args);
			schema = db.getSchema();
			catalog = db.getCatalog();
			name = db.getObjectName();
		}

		DataStore ds = currentConnection.getMetadata().getProcedureReader().getProcedures(catalog, schema, name);
		result.addDataStore(ds);
		result.setSuccess();
		return result;
	}
}
