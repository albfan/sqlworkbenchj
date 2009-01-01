/*
 * WbListCatalogs.java
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

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.storage.DataStore;
import workbench.util.StringUtil;

/**
 *
 * @author  support@sql-workbench.net
 */
public class WbListCatalogs extends SqlCommand
{
	private final String VERB = "WBLISTDB";
	private final String VERB_ALTERNATE = "WBLISTCAT";

	public WbListCatalogs()
	{
		super();
	}

	public String getVerb() { return VERB; }
	public String getAlternateVerb() { return VERB_ALTERNATE; }

	public StatementRunnerResult execute(String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		DataStore ds = currentConnection.getMetadata().getCatalogInformation();
		String catName = StringUtil.capitalize(currentConnection.getMetadata().getCatalogTerm());
		ds.setResultName(catName);
		result.addDataStore(ds);
		result.setSuccess();
		return result;
	}

}
