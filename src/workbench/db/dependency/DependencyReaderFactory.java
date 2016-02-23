/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.dependency;

import workbench.db.DbMetadata;
import workbench.db.JdbcUtils;
import workbench.db.WbConnection;
import workbench.db.firebird.FirebirdDependencyReader;
import workbench.db.hana.HanaDependencyReader;
import workbench.db.hsqldb.HsqlDependencyReader;
import workbench.db.mssql.SqlServerDependencyReader;
import workbench.db.mssql.SqlServerUtil;
import workbench.db.oracle.OracleDependencyReader;
import workbench.db.postgres.PostgresDependencyReader;

/**
 *
 * @author Thomas Kellerer
 */
public class DependencyReaderFactory
{
  public static DependencyReader getReader(WbConnection connection)
  {
    if (connection == null) return null;

    if (connection.getMetadata().isOracle())
    {
      return new OracleDependencyReader();
    }

    if (connection.getMetadata().isPostgres() && JdbcUtils.hasMinimumServerVersion(connection, "8.4"))
    {
      return new PostgresDependencyReader();
    }

    if (connection.getMetadata().isSqlServer() && SqlServerUtil.isSqlServer2008(connection))
    {
      return new SqlServerDependencyReader();
    }

    if (connection.getMetadata().isFirebird())
    {
      return new FirebirdDependencyReader();
    }

    if (connection.getMetadata().isHsql())
    {
      return new HsqlDependencyReader();
    }

    if (connection.getDbId().equals(DbMetadata.DBID_HANA))
    {
      return new HanaDependencyReader();
    }
    
    return null;
  }
}
