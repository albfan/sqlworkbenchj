/*
 * H2MergeGenerator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
package workbench.db.h2database;

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
public class H2MergeGenerator
	implements MergeGenerator
{
	private SqlLiteralFormatter formatter;

	public H2MergeGenerator()
	{
		this.formatter = new SqlLiteralFormatter(SqlLiteralFormatter.ANSI_DATE_LITERAL_TYPE);
	}

	@Override
	public String generateMergeStart(RowDataContainer data)
	{
		StringBuilder result = new StringBuilder(100);
		generateStart(result, data, false);
		return result.toString();
	}

	@Override
	public String addRow(ResultInfo info, RowData row, long rowIndex)
	{
		StringBuilder sql = new StringBuilder(100);
		if (rowIndex > 0) sql.append(",\n  ");
		sql.append('(');
		appendValues(sql, info, row);
		sql.append(')');
		return sql.toString();
	}

	@Override
	public String generateMergeEnd(RowDataContainer data)
	{
		return ";\n";
	}

	@Override
	public String generateMerge(RowDataContainer data)
	{
		StringBuilder sql = new StringBuilder(data.getRowCount());
		generateStart(sql, data, true);
		sql.append(";\n");
		return sql.toString();
	}

	private void generateStart(StringBuilder sql, RowDataContainer data, boolean withData)
	{
		TableIdentifier tbl = data.getUpdateTable();
		sql.append("MERGE INTO ");
		sql.append(tbl.getTableExpression(data.getOriginalConnection()));
		sql.append(" (");

		ResultInfo info = data.getResultInfo();
		for (int col=0; col < info.getColumnCount(); col ++)
		{
			if (col > 0) sql.append(", ");
			sql.append(info.getColumnName(col));
		}
		sql.append(")\n  KEY (");
		int pkCount = 0;
		for (int col=0; col < info.getColumnCount(); col ++)
		{
			if (info.getColumn(col).isPkColumn())
			{
				if (pkCount > 0) sql.append(", ");
				sql.append(info.getColumnName(col));
				pkCount ++;
			}
		}
		sql.append(")\nVALUES\n  ");
		if (withData)
		{
			for (int row=0; row < data.getRowCount(); row++)
			{
				if (row > 0) sql.append(",\n  ");
				sql.append('(');
				appendValues(sql, info, data.getRow(row));
				sql.append(')');
			}
		}
	}

	private void appendValues(StringBuilder sql, ResultInfo info, RowData rd)
	{
		for (int col=0; col < info.getColumnCount(); col++)
		{
			if (col > 0) sql.append(", ");
			ColumnData cd = new ColumnData(rd.getValue(col), info.getColumn(col));
			sql.append(formatter.getDefaultLiteral(cd));
		}
	}

}

