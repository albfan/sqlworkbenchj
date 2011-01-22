/*
 * TableSelectBuilder.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.SQLException;
import java.util.List;
import workbench.log.LogMgr;
import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class TableSelectBuilder
{
	public static final String COLUMN_PLACEHOLDER = "${column}";

	private WbConnection dbConnection;
	private boolean excludeLobColumns;
	private boolean useColumnAlias;

	public TableSelectBuilder(WbConnection source)
	{
		this.dbConnection = source;
	}

	public void setExcludeLobColumns(boolean flag)
	{
		this.excludeLobColumns = flag;
	}

	public void setUseColumnAlias(boolean flag)
	{
		this.useColumnAlias = flag;
	}

	public String getSelectForTable(TableIdentifier table)
		throws SQLException
	{
		TableIdentifier tbl = null;

		if (!table.getNeverAdjustCase() || table.getType() == null || table.getSchema() == null)
		{
			// if neverAdjustCase() has been set, the TableIdentifier was system generated
			// if no type or schema is present, it is most probably a user supplied value
			// thus we need to resolve synonyms
			// This is not done by default because resolving the synonym is quite costly especially on Oracle!
			tbl = dbConnection.getMetadata().getSynonymTable(table);
		}
		TableDefinition def = dbConnection.getMetadata().getTableDefinition(tbl == null ? table : tbl);
		return getSelectForColumns(def.getTable(), def.getColumns());
	}

	public String getSelectForColumns(TableIdentifier table, List<ColumnIdentifier> columns)
	{
		int colCount = columns.size();
		if (colCount == 0) return null;

		StringBuilder sql = new StringBuilder(colCount * 30 + 50);

		sql.append("SELECT ");
		int colsInList = 0;
		for (int i=0; i < colCount; i++)
		{
			String expr = null;
			int type  = columns.get(i).getDataType();
			String dbmsType = columns.get(i).getDbmsType();
			if (dbmsType == null)
			{
				dbmsType = SqlUtil.getTypeName(type);
			}

			if (!excludeLobColumns || dbConnection.getDbSettings().isSearchable(dbmsType))
			{
				expr = getColumnExpression(columns.get(i));
			}
			else if (!SqlUtil.isBlobType(type) && !SqlUtil.isClobType(type))
			{
				expr = getColumnExpression(columns.get(i));
			}

			if (expr != null)
			{
				if (colsInList > 0) sql.append(",\n");
				if (colsInList > 0) sql.append("       ");
				sql.append(expr);
				colsInList ++;
			}
		}
		sql.append("\nFROM ");
		sql.append(table.getTableExpression(this.dbConnection));
		return sql.toString();
	}

	public String getColumnExpression(ColumnIdentifier col)
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

		// If an expression without the placeholder was defined ignore the definition
		if (expr.indexOf(COLUMN_PLACEHOLDER) == -1)
		{
			LogMgr.logError("TableSelectBuilder.getColumnExpression()", "Expression without " + COLUMN_PLACEHOLDER + " specified for datatype '" + type + "': " + expr, null);
			return colname;
		}

		expr = expr.replace(COLUMN_PLACEHOLDER, colname);
		if (useColumnAlias)
		{
			expr = expr + " AS " + colname;
		}
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
