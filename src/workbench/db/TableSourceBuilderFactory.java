/*
 * TableSourceBuilderFactory.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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
package workbench.db;

import workbench.db.derby.DerbyTableSourceBuilder;
import workbench.db.exasol.ExasolTableSourceBuilder;
import workbench.db.firebird.FirebirdTableSourceBuilder;
import workbench.db.h2database.H2TableSourceBuilder;
import workbench.db.hana.HanaTableSourceBuilder;
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
    else if (DBID.DB2_LUW.isDB(con))
    {
      return new Db2TableSourceBuilder(con);
    }
    if (DBID.Informix.isDB(con))
    {
      return new InformixTableSourceBuilder(con);
    }
    if (con.getMetadata().isFirebird())
    {
      return new FirebirdTableSourceBuilder(con);
    }
    if (DBID.HANA.isDB(con))
    {
      return new HanaTableSourceBuilder(con);
    }
    if (DBID.Exasol.isDB(con))
    {
      return new ExasolTableSourceBuilder(con);
    }
    return new TableSourceBuilder(con);
  }

}
