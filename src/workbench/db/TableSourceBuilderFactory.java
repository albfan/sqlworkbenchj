/*
 * TableSourceBuilderFactory.java
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
package workbench.db;

import workbench.db.derby.DerbyTableSourceBuilder;
import workbench.db.h2database.H2TableSourceBuilder;
import workbench.db.hsqldb.HsqlTableSourceBuilder;
import workbench.db.ibm.Db2TableSourceBuilder;
import workbench.db.ibm.InformixTableSourceBuilder;
import workbench.db.mssql.SqlServerTableSourceBuilder;
import workbench.db.mysql.MySQLTableSourceBuilder;
import workbench.db.oracle.OracleTableSourceBuilder;
import workbench.db.postgres.PostgresTableSourceBuilder;

/**
 * A factory to create a TableSourceBuilder.
 *
 * @author Thomas Kellerer
 */
public class TableSourceBuilderFactory
{

	public static TableSourceBuilder getBuilder(WbConnection con)
	{
		if (con.getMetadata().isPostgres())
		{
			return new PostgresTableSourceBuilder(con);
		}
		else if (con.getMetadata().isApacheDerby())
		{
			return new DerbyTableSourceBuilder(con);
		}
		else if (con.getMetadata().isOracle())
		{
			return new OracleTableSourceBuilder(con);
		}
		else if (con.getMetadata().isH2())
		{
			return new H2TableSourceBuilder(con);
		}
		else if (con.getMetadata().isMySql())
		{
			return new MySQLTableSourceBuilder(con);
		}
		else if (con.getMetadata().isSqlServer())
		{
			return new SqlServerTableSourceBuilder(con);
		}
		else if (con.getMetadata().isHsql())
		{
			return new HsqlTableSourceBuilder(con);
		}
		else if (con.getDbId().equals("db2"))
		{
			return new Db2TableSourceBuilder(con);
		}
		if (con.getDbId().equals("informix_dynamic_server"))
		{
			return new InformixTableSourceBuilder(con);
		}
		return new TableSourceBuilder(con);
	}

}
