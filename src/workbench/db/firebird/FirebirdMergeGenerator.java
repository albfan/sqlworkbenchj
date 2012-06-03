/*
 * FirebirdMergeGenerator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.firebird;

import workbench.storage.DmlStatement;
import workbench.storage.MergeGenerator;
import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.storage.RowDataContainer;
import workbench.storage.SqlLiteralFormatter;
import workbench.storage.StatementFactory;

/**
 *
 * @author Thomas Kellerer
 */
public class FirebirdMergeGenerator
	implements MergeGenerator
{
	private SqlLiteralFormatter formatter;
	private StatementFactory stmtFactory;

	public FirebirdMergeGenerator()
	{
		formatter = new SqlLiteralFormatter("firebird");
	}

	@Override
	public String generateMerge(RowDataContainer data)
	{
		StringBuilder result = new StringBuilder(data.getRowCount() * 100);
		ResultInfo info = data.getResultInfo();
		StatementFactory factory = new StatementFactory(info, data.getOriginalConnection());
		for (int row=0; row < data.getRowCount(); row++)
		{
			result.append(generateUpsert(factory, info, data.getRow(row)));
			result.append('\n');
		}
		return result.toString();
	}

	@Override
	public String generateMergeStart(RowDataContainer data)
	{
		stmtFactory = new StatementFactory(data.getResultInfo(), data.getOriginalConnection());
		return "";
	}

	@Override
	public String addRow(ResultInfo info, RowData row, long rowIndex)
	{
		if (stmtFactory == null)
		{
			stmtFactory = new StatementFactory(info, null);
		}
		return generateUpsert(stmtFactory, info, row);
	}

	@Override
	public String generateMergeEnd(RowDataContainer data)
	{
		stmtFactory = null;
		return "";
	}

	private String generateUpsert(StatementFactory factory, ResultInfo info, RowData row)
	{
		DmlStatement dml = factory.createInsertStatement(row, true, "\n");
		CharSequence sql =  dml.getExecutableStatement(formatter);
		StringBuilder result = new StringBuilder(sql.length() + 50);
		result.append("UPDATE OR ");
		result.append(sql);
		result.append("\nMATCHING (");
		int pkCount = 0;
		for (int col=0; col < info.getColumnCount(); col++)
		{
			if (info.getColumn(col).isPkColumn())
			{
				if (pkCount > 0) result.append(',');
				result.append(info.getColumn(0).getColumnName());
			}
		}
		result.append(");");
		return result.toString();
	}

}
