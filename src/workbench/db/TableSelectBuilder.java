/*
 * TableSelectBuilder.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.SQLException;
import java.util.List;
import workbench.log.LogMgr;

/**
 *
 * @author support@sql-workbench.net
 */
public class TableSelectBuilder
{
	public static final String COLUMN_PLACEHOLDER = "${column}";
	
	private WbConnection dbConnection;

	public TableSelectBuilder(WbConnection source)
	{
		this.dbConnection = source;
	}

	public String getSelectForTable(TableIdentifier table)
		throws SQLException
	{
		List<ColumnIdentifier> cols = dbConnection.getMetadata().getTableColumns(table);
		return getSelectForColumns(table, cols);
	}

	public String getSelectForColumns(TableIdentifier table, List<ColumnIdentifier> columns)
	{
		int colCount = columns.size();
		if (colCount == 0) 
		{
			try
			{
				columns = dbConnection.getMetadata().getTableColumns(table);
			}
			catch (SQLException e)
			{
				return "";
			}
		}
		
		colCount = columns.size();
		
		StringBuilder sql = new StringBuilder(colCount * 80);

		sql.append("SELECT ");
		for (int i=0; i < colCount; i++)
		{
			String expr = getColumnExpression(columns.get(i));
			if (i > 0 && i < colCount) sql.append(",\n");
			if (i > 0) sql.append("       ");
			sql.append(expr);
		}
		sql.append("\nFROM ");
		sql.append(table.getTableExpression(this.dbConnection));
		return sql.toString();
	}

	private String getColumnExpression(ColumnIdentifier col)
	{
		String colname = dbConnection.getMetadata().quoteObjectname(col.getColumnName());
		DbSettings db = dbConnection.getDbSettings();
		if (db == null) return colname;
		
		String type = cleanDataType(col.getDbmsType());
		String expr = db.getDataTypeExpression(type);
		if (expr == null)
		{
			return colname;
		}
		
		// If an expression without the placeholder was defined
		// ignore the definition
		if (expr.indexOf(COLUMN_PLACEHOLDER) == -1)
		{
			LogMgr.logError("TableSelectBuilder.getColumnExpression()", "Expression without " + COLUMN_PLACEHOLDER + " specified for datatype '" + type + "': " + expr, null);
			return colname;
		}

		expr = expr.replace(COLUMN_PLACEHOLDER, colname);
		
		return expr;
	}

	protected String cleanDataType(String dbmsType)
	{
		if (dbmsType == null) return null;
		int pos = dbmsType.indexOf('(');
		if (pos == -1) return dbmsType;
		return dbmsType.substring(0, pos);
	}
}
