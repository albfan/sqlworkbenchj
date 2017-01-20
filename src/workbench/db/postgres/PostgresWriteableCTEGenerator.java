/*
 * PostgresMergeGenerator.java
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
package workbench.db.postgres;

import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.storage.ColumnData;
import workbench.storage.MergeGenerator;
import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.storage.RowDataContainer;
import workbench.storage.SqlLiteralFormatter;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresWriteableCTEGenerator
  implements MergeGenerator
{
  private SqlLiteralFormatter formatter;

  public PostgresWriteableCTEGenerator()
  {
    this.formatter = new SqlLiteralFormatter(SqlLiteralFormatter.ANSI_DATE_LITERAL_TYPE);
  }

  @Override
  public String generateMergeStart(RowDataContainer data)
  {
    StringBuilder result = new StringBuilder(100);
    generateCte(result, data, false);
    return result.toString();
  }

  @Override
  public String addRow(ResultInfo info, RowData row, long rowIndex)
  {
    StringBuilder result = new StringBuilder(100);
    appendValues(result, row, info, rowIndex);
    return result.toString();
  }

  @Override
  public String generateMergeEnd(RowDataContainer data)
  {
    StringBuilder sql = new StringBuilder(100);
    appendUpdate(sql, data);
    appendInsert(sql, data);
    return sql.toString();
  }

  @Override
  public String generateMerge(RowDataContainer data)
  {
    StringBuilder sql = new StringBuilder(data.getRowCount());
    generateCte(sql, data, true);
    appendUpdate(sql, data);
    appendInsert(sql, data);
    return sql.toString();
  }

  private void generateCte(StringBuilder sql, RowDataContainer data, boolean withData)
  {
    sql.append("with merge_data (");
    ResultInfo info = data.getResultInfo();
    for (int col=0; col < info.getColumnCount(); col ++)
    {
      if (col > 0) sql.append(", ");
      sql.append(info.getColumnName(col));
    }
    sql.append(") as \n(\n  values\n");
    if (withData)
    {
      for (int row=0; row < data.getRowCount(); row++)
      {
        appendValues(sql, data.getRow(row), info, row);
      }
    }
  }

  private void appendValues(StringBuilder sql, RowData rd, ResultInfo info, long rowNumber)
  {
    if (rowNumber > 0) sql.append(",\n");
    sql.append("    (");
    for (int col=0; col < info.getColumnCount(); col++)
    {
      if (col > 0) sql.append(',');
      ColumnData cd = new ColumnData(rd.getValue(col), info.getColumn(col));
      sql.append(formatter.getDefaultLiteral(cd));
    }
    sql.append(')');
  }

  private void appendUpdate(StringBuilder sql, RowDataContainer data)
  {
    TableIdentifier tbl = data.getUpdateTable();

    sql.append("\n),\nupsert as\n(\n");
    sql.append("  update ");
    sql.append(tbl.getTableExpression(data.getOriginalConnection()));
    sql.append(" m\n");
    ResultInfo info = data.getResultInfo();

    int colCount = 0;
    for (int col=0; col < info.getColumnCount(); col ++)
    {
      if (info.getColumn(col).isPkColumn()) continue;
      if (colCount == 0) sql.append("     set ");
      if (colCount > 0) sql.append(",\n         ");
      sql.append("m.");
      sql.append(info.getColumnName(col));
      sql.append(" = md.");
      sql.append(info.getColumnName(col));
      colCount++;
    }

    sql.append("\n  from merge_data md\n");
    int pkCount = 0;
    for (int col=0; col < info.getColumnCount(); col ++)
    {
      ColumnIdentifier colid = info.getColumn(col);
      if (!colid.isPkColumn()) continue;
      if (pkCount == 0) sql.append("  where ");
      if (pkCount > 0)  sql.append("    and ");
      sql.append("m.");
      sql.append(info.getColumnName(col));
      sql.append(" = md.");
      sql.append(info.getColumnName(col));
      pkCount ++;
    }
    sql.append("\n  returning m.*");
    sql.append("\n)");
  }

  private void appendInsert(StringBuilder sql, RowDataContainer data)
  {
    TableIdentifier tbl = data.getUpdateTable();

    sql.append("\ninsert into ");
    sql.append(tbl.getTableExpression());
    sql.append(" (");
    ResultInfo info = data.getResultInfo();
    StringBuilder columns = new StringBuilder(info.getColumnCount() * 15);
    for (int col=0; col < info.getColumnCount(); col ++)
    {
      if (col > 0) columns.append(", ");
      columns.append(info.getColumnName(col));
    }
    sql.append(columns);
    sql.append(")\nselect ");
    sql.append(columns);
    sql.append("\nfrom merge_data\n");
    sql.append("where not exists (select 1\n");
    sql.append("                  from upsert up\n");
    int pkCount = 0;
    for (int col=0; col < info.getColumnCount(); col ++)
    {
      ColumnIdentifier colid = info.getColumn(col);
      if (!colid.isPkColumn()) continue;
      if (pkCount == 0) sql.append("                  where ");
      if (pkCount > 0)  sql.append("                    and ");
      sql.append("up.");
      sql.append(info.getColumnName(col));
      sql.append(" = md.");
      sql.append(info.getColumnName(col));
      pkCount ++;
    }
    sql.append(");\n");
  }
}
