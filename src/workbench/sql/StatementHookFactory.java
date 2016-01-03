/*
 * StatementHookFactory.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql;

import workbench.resource.Settings;

import workbench.db.WbConnection;
import workbench.db.firebird.FirebirdStatementHook;
import workbench.db.mssql.SqlServerStatementHook;
import workbench.db.mssql.SqlServerUtil;
import workbench.db.oracle.OracleStatementHook;
import workbench.db.postgres.PostgresStatementHook;

/**
 *
 * @author Thomas Kellerer
 */
public class StatementHookFactory
{
	public static final StatementHook DEFAULT_HOOK = new DefaultStatementHook();

	public static StatementHook getStatementHook(StatementRunner runner)
	{
		WbConnection conn = runner.getConnection();
		if (conn == null) return DEFAULT_HOOK;
		if (conn.getMetadata() == null) return DEFAULT_HOOK;

		if (conn.getMetadata().isOracle())
		{
			return new OracleStatementHook();
		}
		if (conn.getMetadata().isSqlServer() && SqlServerUtil.isSqlServer2008(conn))
		{
			// The hack for the MERGE statement is only necessary for SQL Server 2008 and above
			return new SqlServerStatementHook();
		}
		if (conn.getMetadata().isFirebird())
		{
			return new FirebirdStatementHook(conn);
		}
		if (conn.getMetadata().isPostgres() && Settings.getInstance().getBoolProperty("workbench.db.postgresql.enable.listen", true))
		{
			return new PostgresStatementHook(conn);
		}
		return DEFAULT_HOOK;
	}
}
