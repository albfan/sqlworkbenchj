/*
 * DummySelect.java
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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import workbench.resource.Settings;

import workbench.sql.formatter.WbSqlFormatter;

import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class DummySelect
  implements DbObject
{
  private TableIdentifier table;
  private List<ColumnIdentifier> columns;

  public DummySelect(TableIdentifier tbl)
  {
    this.table = tbl;
  }

  public DummySelect(TableIdentifier tbl, List<ColumnIdentifier> cols)
  {
    this.table = tbl;
    this.columns = new ArrayList<>(cols);
  }

  @Override
  public String getComment()
  {
    return null;
  }

  @Override
  public void setComment(String c)
  {
  }

  @Override
  public String getCatalog()
  {
    return null;
  }

  @Override
  public String getFullyQualifiedName(WbConnection conn)
  {
    return getObjectExpression(conn);
  }

  @Override
  public String getObjectExpression(WbConnection conn)
  {
    return null;
  }

  @Override
  public String getObjectName()
  {
    return null;
  }

  @Override
  public String getObjectName(WbConnection conn)
  {
    return null;
  }

  @Override
  public String getDropStatement(WbConnection con, boolean cascade)
  {
    return null;
  }

  @Override
  public String getObjectNameForDrop(WbConnection con)
  {
    return null;
  }

  @Override
  public String getObjectType()
  {
    return "SELECT";
  }

  @Override
  public String getSchema()
  {
    return null;
  }

  @Override
  public CharSequence getSource(WbConnection con)
    throws SQLException
  {
    DbMetadata meta = con.getMetadata();
    String nl = Settings.getInstance().getInternalEditorLineEnding();

    List<ColumnIdentifier> cols = columns;
    if (cols == null) cols = meta.getTableColumns(table, false);

    if (cols.isEmpty())
    {
      return StringUtil.EMPTY_STRING;
    }

    int colCount = cols.size();

    StringBuilder sql = new StringBuilder(colCount * 80);

    sql.append("SELECT ");
    for (int i = 0; i < colCount; i++)
    {
      String column = cols.get(i).getColumnName();
      if (i > 0)
      {
        sql.append(',');
        sql.append(nl);
        sql.append("       ");
      }

      sql.append(column);
    }
    sql.append(nl);
    sql.append("FROM ");
    sql.append(table.getTableExpression(con));
    sql.append(';');
    sql.append(nl);

    WbSqlFormatter formatter = new WbSqlFormatter(sql, con.getDbId());
    try
    {
      return formatter.getFormattedSql();
    }
    catch (Exception e)
    {
      return sql.toString();
    }
  }

  @Override
  public boolean supportsGetSource()
  {
    return true;
  }

}
