/*
 * StatementHookFactory.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql;

import workbench.db.JdbcUtils;
import workbench.db.WbConnection;
import workbench.db.mssql.SqlServerStatementHook;
import workbench.db.oracle.OracleStatementHook;

/**
 *
 * @author Thomas Kellerer
 */
public class StatementHookFactory
{

	public static StatementHook DEFAULT_HOOK = new DefaultStatementHook();

	public static StatementHook getStatementHook(StatementRunner runner)
	{
		WbConnection conn = runner.getConnection();
		if (conn == null) return DEFAULT_HOOK;

		if (conn.getMetadata().isOracle())
		{
			return new OracleStatementHook();
		}
		if (conn.getMetadata().isSqlServer() && JdbcUtils.hasMinimumServerVersion(conn, "10.0"))
		{
			// The hack for the MERGE statement is only necessary for SQL Server 2008 and above
			return new SqlServerStatementHook();
		}
		return DEFAULT_HOOK;
	}
}
