/*
 * WbDescribeTable.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.SQLException;
import workbench.db.TableIdentifier;

import workbench.db.WbConnection;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;

/**
 *
 * @author  support@sql-workbench.net
 */
public class WbDescribeTable extends SqlCommand
{
	private static final String VERB = "DESC";

	public WbDescribeTable()
	{
	}

	public String getVerb() { return VERB; }

	public StatementRunnerResult execute(String sql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		String table = SqlUtil.stripVerb(SqlUtil.makeCleanSql(sql, false, false, '\''));
		
		TableIdentifier tbl = new TableIdentifier(table);
		
		DataStore ds = currentConnection.getMetadata().getTableDefinition(tbl);
    if (ds == null || ds.getRowCount() == 0)
    {
      result.setFailure();
      String msg = ResourceMgr.getString("ErrTableOrViewNotFound");
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
