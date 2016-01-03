/*
 * PostgresMergeGenerator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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

import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class Postgres95MergeGenerator
	implements MergeGenerator
{
	private SqlLiteralFormatter formatter;

	public Postgres95MergeGenerator()
	{
		this.formatter = new SqlLiteralFormatter(SqlLiteralFormatter.ANSI_DATE_LITERAL_TYPE);
	}

	@Override
	public String generateMergeStart(RowDataContainer data)
	{
    TableIdentifier tbl = data.getUpdateTable();
		String result = "INSERT INTO ";
		result += tbl.getTableExpression();
		result += "\n  (";
		ResultInfo info = data.getResultInfo();
		for (int col=0; col < info.getColumnCount(); col ++)
		{
			if (col > 0) result += ", ";
			result += info.getColumnName(col);
		}
		result += ")\nVALUES\n";
    return result;
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
		String sql = "\nON CONFLICT (";
		ResultInfo info = data.getResultInfo();
    int colNr = 0;
    for (ColumnIdentifier col : info.getColumnList())
		{
      if (!col.isPkColumn()) continue;
			if (colNr > 0) sql += ", ";
      sql += SqlUtil.quoteObjectname(col.getColumnName());
      colNr ++;
		}
		sql += ") DO UPDATE\n  SET ";
    colNr = 0;
    for (ColumnIdentifier col : info.getColumnList())
		{
      if (col.isPkColumn()) continue;

			if (colNr > 0) sql += ",\n      ";
      String colname = SqlUtil.quoteObjectname(col.getColumnName());
      sql += colname + " = EXCLUDED." + colname;
      colNr ++;
		}
    return sql;
	}

	@Override
	public String generateMerge(RowDataContainer data)
	{
		StringBuilder sql = new StringBuilder(data.getRowCount() * 20);
		sql.append(generateMergeStart(data));
    for (int i=0; i < data.getRowCount(); i++)
    {
      RowData row = data.getRow(i);
      appendValues(sql, row, data.getResultInfo(), i);
    }
    sql.append(generateMergeEnd(data));
		return sql.toString();
	}

	private void appendValues(StringBuilder sql, RowData rd, ResultInfo info, long rowNumber)
	{
		if (rowNumber > 0) sql.append(",\n");
		sql.append("  (");
		for (int col=0; col < info.getColumnCount(); col++)
		{
			if (col > 0) sql.append(", ");
			ColumnData cd = new ColumnData(rd.getValue(col), info.getColumn(col));
			sql.append(formatter.getDefaultLiteral(cd));
		}
		sql.append(')');
	}

}
