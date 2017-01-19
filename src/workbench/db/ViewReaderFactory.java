/*
 * ViewReaderFactory.java
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

import workbench.db.hana.HanaViewReader;
import workbench.db.mssql.SqlServerViewReader;
import workbench.db.mysql.MySQLViewReader;
import workbench.db.oracle.OracleViewReader;
import workbench.db.postgres.PostgresViewReader;

/**
 *
 * @author Thomas Kellerer
 */
public class ViewReaderFactory
{
  public static ViewReader createViewReader(WbConnection con)
  {
    if (con.getMetadata().isPostgres())
    {
      return new PostgresViewReader(con);
    }
    if (con.getMetadata().isMySql())
    {
      return new MySQLViewReader(con);
    }
    if (con.getMetadata().isOracle())
    {
      return new OracleViewReader(con);
    }
    if (con.getMetadata().isSqlServer())
    {
      return new SqlServerViewReader(con);
    }
    if (DBID.HANA.isDB(con))
    {
      return new HanaViewReader(con);
    }
    return new DefaultViewReader(con);
  }
}
