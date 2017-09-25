/*
 * HsqlConstraintReader.java
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
package workbench.db.hsqldb;

import workbench.db.AbstractConstraintReader;
import workbench.db.ColumnIdentifier;
import workbench.db.JdbcUtils;
import workbench.db.TableDefinition;
import workbench.db.WbConnection;

import workbench.util.StringUtil;

/**
 * Constraint reader for HSQLDB
 * @author  Thomas Kellerer
 */
public class HsqlConstraintReader
  extends AbstractConstraintReader
{
  private String sql;

  public HsqlConstraintReader(WbConnection dbConnection)
  {
    super(dbConnection.getDbId());

    this.sql =
      "select chk.constraint_name, chk.check_clause \n" +
      "from information_schema.system_check_constraints chk" +
      "  join  information_schema.system_table_constraints cons on chk.constraint_name = cons.constraint_name  \n" +
      "where cons.constraint_type = 'CHECK' \n" +
      "and cons.table_name = ?; \n";

    if (JdbcUtils.hasMinimumServerVersion(dbConnection, "1.9"))
    {
      this.sql = sql.replace("system_check_constraints", "check_constraints");
      this.sql = sql.replace("system_table_constraints", "table_constraints");
    }
  }

  @Override
  public String getColumnConstraintSql()
  {
    return null;
  }

  @Override
  public String getTableConstraintSql()
  {
    return this.sql;
  }

  @Override
  public boolean isSystemConstraintName(String name)
  {
    if (StringUtil.isBlank(name)) return false;
    return name.startsWith("SYS_");
  }

  @Override
  protected boolean shouldIncludeTableConstraint(String constraintName, String constraint, TableDefinition table)
  {
    if (constraint == null) return false;
    if (!constraint.toUpperCase().endsWith("IS NOT NULL")) return true;

    int pos = constraint.indexOf(' ');
    if (pos < 0) return true;

    String colname = constraint.substring(0,pos);
    int pos2 = colname.lastIndexOf('.');
    if (pos2 < 0) return true;

    colname = colname.substring(pos2 + 1);
    ColumnIdentifier col = findColumn(table, colname);
    if (col != null && !col.isNullable())
    {
      if (isSystemConstraintName(constraintName))
      {
        // the constraint name is system generated and the column is already marked
        // as NOT NULL, so there is no need to include this constraint here
        return false;
      }
    }

    return true;
  }

  private ColumnIdentifier findColumn(TableDefinition table, String columnName)
  {
    for (ColumnIdentifier col : table.getColumns())
    {
      if (col.getColumnName().equalsIgnoreCase(columnName))
      {
        return col;
      }
    }
    return null;
  }

}
