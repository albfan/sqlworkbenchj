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



import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
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
	public String generateMergeStart(RowDataContainer data)
	{
		StringBuilder result = new StringBuilder(100);
		return result.toString();
	}

	@Override
	public String addRow(ResultInfo info, RowData row, long rowIndex)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String generateMergeEnd(RowDataContainer data)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String generateMerge(RowDataContainer data)
	{
		StringBuilder sql = new StringBuilder(data.getRowCount());

		generateStart(sql, data, true);
		appendJoin(sql, data);
		appendUpdate(sql, data);
		appendInsert(sql, data);
		return sql.toString();
	}

	private void generateStart(StringBuilder sql, RowDataContainer data, boolean withData)
	{
		TableIdentifier tbl = data.getUpdateTable();
		sql.append("merge into ");
		sql.append(tbl.getTableExpression(this.dbConn));
		sql.append(" ut\nusing\n(\n");
		if (withData)
		{
			ResultInfo info = data.getResultInfo();
			for (int row=0; row < data.getRowCount(); row++)
			{
				if (row > 0) sql.append("\n  union all\n");
				appendValues(sql, info, data.getRow(row), row == 0);
			}
			sql.append("\n)");
		}
	}

	private void appendJoin(StringBuilder sql, RowDataContainer data)
	{
		ResultInfo info = data.getResultInfo();
		sql.append(" md on (");
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

	private void appendValues(StringBuilder sql, ResultInfo info, RowData rd, boolean useAlias)
	{
		sql.append("  select ");

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

	private void appendUpdate(StringBuilder sql, RowDataContainer data)
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

	private void appendInsert(StringBuilder sql, RowDataContainer data)
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
		sql.append(");\n");
	}

}

