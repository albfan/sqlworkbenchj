/*
 * PostgresMergeGenerator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.storage;

import java.util.List;
import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.util.CollectionUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresMergeGenerator
	implements MergeGenerator
{
	private SqlLiteralFormatter formatter;
	private WbConnection dbConn;
	public PostgresMergeGenerator(WbConnection con)
	{
		this.dbConn = con;
		this.formatter = new SqlLiteralFormatter(con);
	}

	@Override
	public List<String> generateMerge(DataStore data, int[] rows, int chunkSize)
	{
		StringBuilder sql = new StringBuilder(rows == null ? data.getRowCount() : rows.length * 100);
		generateCte(sql, data, rows);
		appendUpdate(sql, data, rows);
		appendInsert(sql, data, rows);
		return CollectionUtil.arrayList(sql.toString());
	}

	private void generateCte(StringBuilder sql, DataStore data, int[] rows)
	{
		sql.append("with merge_data (");
		ResultInfo info = data.getResultInfo();
		for (int col=0; col < info.getColumnCount(); col ++)
		{
			if (col > 0) sql.append(", ");
			sql.append(info.getColumnName(col));
		}
		sql.append(") as \n(\n  values\n");
		if (rows == null)
		{
			for (int row=0; row < data.getRowCount(); row++)
			{
				if (row > 0) sql.append(",\n");
				appendValues(sql, data, row);
			}
		}
		else
		{
			for (int i=0; i < rows.length; i++)
			{
				if (i > 0) sql.append(",\n");
				appendValues(sql, data, rows[i]);
			}
		}
		sql.append("\n)");
	}

	private void appendValues(StringBuilder sql, DataStore ds, int row)
	{
		ResultInfo info = ds.getResultInfo();
		sql.append("    (");
		RowData rd = ds.getRow(row);
		for (int col=0; col < info.getColumnCount(); col++)
		{
			if (col > 0) sql.append(',');
			ColumnData cd = new ColumnData(rd.getValue(col), info.getColumn(col));
			sql.append(formatter.getDefaultLiteral(cd));
		}
		sql.append(')');
	}

	private void appendUpdate(StringBuilder sql, DataStore data, int[] rows)
	{
		TableIdentifier tbl = data.getUpdateTable();

		sql.append(",\nupsert as\n(\n");
		sql.append("  update ");
		sql.append(tbl.getTableExpression(dbConn));
		sql.append(" m\n");
		ResultInfo info = data.getResultInfo();

		for (int col=0; col < info.getColumnCount(); col ++)
		{
			if (col == 0) sql.append("     set ");
			if (col > 0) sql.append(",\n         ");
			sql.append("m.");
			sql.append(info.getColumnName(col));
			sql.append(" = md.");
			sql.append(info.getColumnName(col));
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

	private void appendInsert(StringBuilder sql, DataStore data, int[] rows)
	{
		TableIdentifier tbl = data.getUpdateTable();

		sql.append("\ninsert into ");
		sql.append(tbl.getTableExpression(dbConn));
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
		sql.append(")");
	}
}
