/*
 * StatementHookFactory.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql;

import workbench.db.WbConnection;
import workbench.db.oracle.OracleStatementHook;

/**
 *
 * @author Thomas Kellerer
 */
public class StatementHookFactory
{

	public static StatementHook getStatementHook(StatementRunner runner)
	{
		WbConnection conn = runner.getConnection();
		if (conn != null && conn.getMetadata().isOracle())
		{
			return new OracleStatementHook();
		}
		return new DefaultStatementHook();
	}
}
