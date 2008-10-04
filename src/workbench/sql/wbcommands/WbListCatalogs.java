/*
 * WbListCatalogs.java
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

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.storage.DataStore;

/**
 *
 * @author  support@sql-workbench.net
 */
public class WbListCatalogs extends SqlCommand
{
	private final String VERB;

	public static final WbListCatalogs LISTDB = new WbListCatalogs("WBLISTDB");
	public static final WbListCatalogs LISTCAT = new WbListCatalogs("WBLISTCAT");

	private WbListCatalogs(String verb)
	{
		super();
		this.VERB = verb;
	}

	public String getVerb() { return VERB; }

	public StatementRunnerResult execute(String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		DataStore ds = currentConnection.getMetadata().getCatalogInformation();
		result.addDataStore(ds);
		result.setSuccess();
		return result;
	}

}
