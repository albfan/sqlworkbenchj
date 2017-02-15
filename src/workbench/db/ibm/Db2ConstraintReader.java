/*
 * Db2ConstraintReader.java
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
package workbench.db.ibm;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import workbench.db.AbstractConstraintReader;
import workbench.db.DBID;
import workbench.db.WbConnection;

/**
 * Constraint reader for the Derby database
 * @author  Thomas Kellerer
 */
public class Db2ConstraintReader
  extends AbstractConstraintReader
{
  private final String HOST_TABLE_SQL =
    "select checkname, '('||checkcondition||')' \n" +
    "from  sysibm.syschecks \n" +
    "where tbname = ? " +
    "  and tbowner = ?";

  private final String DB2I_TABLE_SQL =
    "select chk.constraint_name, '('||chk.check_clause||')' \n" +
    "from  qsys2.syschkcst chk \n" +
    "  JOIN qsys2.syscst cons ON cons.constraint_schema = chk.constraint_schema AND cons.constraint_name = chk.constraint_name " +
    "where cons.table_name = ? " +
    "  and cons.table_schema = ?";

  private final String LUW_TABLE_SQL =
    "select cons.constname, '('||cons.text||')' \n" +
    "from syscat.checks cons \n" +
    "where type <> 'S' " +
    "  AND tabname = ? " +
    "  and tabschema = ?";

  private final DBID dbid;
  private final Pattern sysname = Pattern.compile("^SQL[0-9]+");
  private final char catalogSeparator;

  public Db2ConstraintReader(WbConnection conn)
  {
    super(conn.getDbId());
    dbid = DBID.fromConnection(conn);
    catalogSeparator = conn.getMetadata().getCatalogSeparator();
  }

  @Override
  public boolean isSystemConstraintName(String name)
  {
    if (name == null) return false;
    Matcher m = sysname.matcher(name);
    return m.matches();
  }

  @Override
  public String getColumnConstraintSql()
  {
    return null;
  }

  @Override
  public String getTableConstraintSql()
  {
    switch (dbid)
    {
      case DB2_ZOS:
        return HOST_TABLE_SQL;
      case DB2_ISERIES:
        return DB2I_TABLE_SQL.replace("qsys2.", "qsys2" + catalogSeparator);
      default:
        return LUW_TABLE_SQL;
    }
  }

  @Override
  public int getIndexForTableNameParameter()
  {
    return 1;
  }

  @Override
  public int getIndexForSchemaParameter()
  {
    return 2;
  }
}
