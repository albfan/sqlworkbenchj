/*
 * StatementHookFactory.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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

import workbench.db.WbConnection;
import workbench.db.mssql.SqlServerStatementHook;
import workbench.db.mssql.SqlServerUtil;
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
		if (conn.getMetadata().isSqlServer() && SqlServerUtil.isSqlServer2008(conn))
		{
			// The hack for the MERGE statement is only necessary for SQL Server 2008 and above
			return new SqlServerStatementHook();
		}
		return DEFAULT_HOOK;
	}
}
