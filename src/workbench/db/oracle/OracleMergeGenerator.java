/*
 * OracleMergeGenerator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.oracle;

import java.util.List;
import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.storage.ColumnData;
import workbench.storage.DataStore;
import workbench.storage.MergeGenerator;
import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.storage.SqlLiteralFormatter;
import workbench.util.CollectionUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleMergeGenerator
	implements MergeGenerator
{
	private SqlLiteralFormatter formatter;
	private WbConnection dbConn;

	public OracleMergeGenerator(WbConnection con)
	{
		this.dbConn = con;
		this.formatter = new SqlLiteralFormatter(con);
	}

	@Override
	public List<String> generateMerge(DataStore data, int[] rows, int chunkSize)
	{
		StringBuilder sql = new StringBuilder(rows == null ? data.getRowCount() : rows.length * 100);

		generateStart(sql, data, rows);
		appendUpdate(sql, data, rows);
		appendInsert(sql, data, rows);
		return CollectionUtil.arrayList(sql.toString());
	}

	private void generateStart(StringBuilder sql, DataStore data, int[] rows)
	{
		TableIdentifier tbl = data.getUpdateTable();
		ResultInfo info = data.getResultInfo();
		sql.append("merge into ");
		sql.append(tbl.getTableExpression(this.dbConn));
		sql.append(" ut\nusing\n(\n");
		if (rows == null)
		{
			for (int row=0; row < data.getRowCount(); row++)
			{
				if (row > 0) sql.append("\n  union all\n");
				appendValues(sql, data, row, row == 0);
			}
		}
		else
		{
			for (int i=0; i < rows.length; i++)
			{
				if (i > 0) sql.append("union all\n");
				appendValues(sql, data, rows[i], i == 0);
			}
		}
		sql.append("\n) md on (");
		int pkCount = 0;
		for (int col=0; col < info.getColumnCount(); col ++)
		{
			ColumnIdentifier colid = info.getColumn(col);
			if (!colid.isPkColumn()) continue;
			if (pkCount > 0)  sql.append(" and ");
			sql.append("ut.");
			sql.append(info.getColumnName(col));
			sql.append(" = md.");
			sql.append(info.getColumnName(col));
			pkCount ++;
		}
		sql.append(")");
	}

	private void appendValues(StringBuilder sql, DataStore ds, int row, boolean useAlias)
	{
		ResultInfo info = ds.getResultInfo();
		sql.append("  select ");
		RowData rd = ds.getRow(row);
		for (int col=0; col < info.getColumnCount(); col++)
		{
			if (col > 0) sql.append(", ");
			ColumnData cd = new ColumnData(rd.getValue(col), info.getColumn(col));
			sql.append(formatter.getDefaultLiteral(cd));
			if (useAlias)
			{
				sql.append(" as ");
				sql.append(info.getColumnName(col));
			}
		}
		sql.append(" from dual");
	}

	private void appendUpdate(StringBuilder sql, DataStore data, int[] rows)
	{
		sql.append("\nwhen matched then update");
		ResultInfo info = data.getResultInfo();

		int colCount = 0;
		for (int col=0; col < info.getColumnCount(); col ++)
		{
			ColumnIdentifier id = info.getColumn(col);
			if (id.isPkColumn()) continue;
			if (colCount == 0) sql.append("\n     set ");
			if (colCount > 0) sql.append(",\n         ");
			sql.append("ut.");
			sql.append(info.getColumnName(col));
			sql.append(" = md.");
			sql.append(info.getColumnName(col));
			colCount ++;
		}
	}

	private void appendInsert(StringBuilder sql, DataStore data, int[] rows)
	{
		sql.append("\nwhen not matched then\n  insert (");
		ResultInfo info = data.getResultInfo();
		StringBuilder columns = new StringBuilder(info.getColumnCount() * 10);
		for (int col=0; col < info.getColumnCount(); col ++)
		{
			if (col > 0)
			{
				sql.append(", ");
				columns.append(", ");
			}
			sql.append("ut.");
			sql.append(info.getColumnName(col));
			columns.append("md.");
			columns.append(info.getColumnName(col));
		}
		sql.append(")\n");
		sql.append("  values (");
		sql.append(columns);
		sql.append(");");
	}

}

