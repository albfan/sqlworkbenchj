/*
 * WbDescribeTable.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.SQLException;
import java.util.StringTokenizer;

import workbench.db.WbConnection;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.storage.DataStore;

/**
 *
 * @author  support@sql-workbench.net
 */
public class WbDescribeTable extends SqlCommand
{
	public static final String VERB = "DESC";
  public static final String VERB_LONG = "DESCRIBE";

	public WbDescribeTable()
	{
	}

	public String getVerb() { return VERB; }

	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		StringTokenizer tok = new StringTokenizer(aSql.trim(), " ");
		String verb = tok.nextToken();
		if (!VERB.equalsIgnoreCase(verb) &&
        !VERB_LONG.equalsIgnoreCase(verb)) throw new SQLException("Wrong syntax. " + VERB + " expected!");
		String table = null;
		if (tok.hasMoreTokens()) table = tok.nextToken();
		String schema = null;
		int pos = table.indexOf('.');
		if (pos > -1)
		{
			schema = table.substring(0, pos);
			table = table.substring(pos + 1);
		}

    if (schema == null && aConnection.getMetadata().isOracle())
    {
      schema = aConnection.getMetadata().getUserName();
    }
		DataStore ds = aConnection.getMetadata().getTableDefinition(null, schema, table);
    if (ds == null || ds.getRowCount() == 0)
    {
      result.setFailure();
      String msg = ResourceMgr.getString("ErrorTableOrViewNotFound");
      msg = msg.replaceAll("%name%", table);
      result.addMessage(msg);
    }
    else
    {
      result.setSuccess();
  		result.addDataStore(ds);
    }
		return result;
	}

}
