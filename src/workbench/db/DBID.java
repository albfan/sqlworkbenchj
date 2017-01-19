/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017 Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0 (the "License")
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db;

/**
 *
 * @author Thomas Kellerer
 */
public enum DBID
{
  Oracle("oracle"),
  Postgres("postgresql"),
  SQL_Server("microsoft_sql_server"),
  Vertica("vertica_database"),
  MySQL("mysql"),
  Firebird("firebird"),
  DB2_LUW("db2"),  // Linux, Unix, Windows
  DB2_ISERIES("db2i"),  // AS/400 iSeries
  DB2_ZOS("db2h"),  // z/OS
  SQLite("sqlite"),
  SQL_Anywhere("sql_anywhere"),
  Teradata("teradata"),
  H2("h2"),
  HSQLDB("hsql_database_engine"),
  Derby("apache_derby"),
  OPENEDGE("openedge"),
  HANA("hdb"),
  Cubrid("cubrid"),
  Informix("informix_dynamic_server"),
  Exasol("exasolution");

  private String dbid;

  private DBID(String id)
  {
    dbid = id;
  }

  public String getId()
  {
    return dbid;
  }

  public boolean isDB(String id)
  {
    return this.dbid.equalsIgnoreCase(id);
  }

  public boolean isDB(WbConnection conn)
  {
    if (conn == null) return false;
    return this.dbid.equalsIgnoreCase(conn.getDbId());
  }
}
