/*
 * TableSelectBuilder.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.SQLException;
import java.util.List;
import workbench.db.sqltemplates.TemplateHandler;
import workbench.log.LogMgr;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class TableSelectBuilder
{
	public static final String COLUMN_PLACEHOLDER = "${column}";

	private WbConnection dbConnection;
	private boolean includLobColumns = true;
	private boolean useColumnAlias;
	private String sqlTemplate;

	public TableSelectBuilder(WbConnection source)
	{
		this(source, null);
	}

	public TableSelectBuilder(WbConnection source, String templateKey)
	{
		this.dbConnection = source;
		if (StringUtil.isNonBlank(templateKey))
		{
			sqlTemplate = source.getDbSettings().getTableSelectTemplate(templateKey.toLowerCase());
		}

		if (StringUtil.isBlank(sqlTemplate))
		{
			sqlTemplate = "SELECT " + MetaDataSqlManager.COLUMN_LIST_PLACEHOLDER + "\nFROM " + MetaDataSqlManager.TABLE_NAME_PLACEHOLDER;
		}
	}

	/**
	 * For testing purposes only.
	 * @param template
	 */
	void setSqlTemplate(String template)
	{
		this.sqlTemplate = template;
	}

	public void setExcludeLobColumns(boolean flag)
	{
		this.includLobColumns = !flag;
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
			// If neverAdjustCase() has been set, the TableIdentifier was system generated.
			// If no type or schema is present, it is most probably a user supplied value and thus we need to resolve synonyms
			// This is not done by default because resolving the synonym is quite costly especially on Oracle!
			tbl = dbConnection.getMetadata().getSynonymTable(table);
		}
		TableDefinition def = dbConnection.getMetadata().getTableDefinition(tbl == null ? table : tbl);
		return getSelectForColumns(def.getTable(), def.getColumns());
	}

	public String getSelectForColumns(TableIdentifier table, List<ColumnIdentifier> columns)
	{
		if (table == null)
		{
			LogMgr.logWarning("TableSelectBuilder.getSelectForColumns()", "Not table supplied!");
			return null;
		}

		StringBuilder selectCols = new StringBuilder(columns.size() * 30);

		if (columns.isEmpty())
		{
			String tbl = table.getTableExpression(this.dbConnection);
			LogMgr.logWarning("TableSelectBuilder.getSelectForColumns()", "No columns available for table " + tbl  + ". Using \"SELECT *\" instead");
			selectCols.append("*");
		}
		else
		{
			int colsInList = 0;

			for (ColumnIdentifier column : columns)
			{
				String expr = null;
				int type  = column.getDataType();
				String dbmsType = column.getDbmsType();
				if (dbmsType == null)
				{
					dbmsType = SqlUtil.getTypeName(type);
				}

				if (includLobColumns || dbConnection.getDbSettings().isSearchable(dbmsType))
				{
					expr = getColumnExpression(column);
				}
				else if (!SqlUtil.isBlobType(type) && !SqlUtil.isClobType(type))
				{
					expr = getColumnExpression(column);
				}

				if (expr != null)
				{
					if (colsInList > 0) selectCols.append(",\n");
					if (colsInList > 0) selectCols.append("       ");
					selectCols.append(expr);
					colsInList ++;
				}
			}
		}


		String fqTableName = SqlUtil.fullyQualifiedName(dbConnection, table);

		String select = sqlTemplate.replace(MetaDataSqlManager.COLUMN_LIST_PLACEHOLDER, selectCols);
		select = TemplateHandler.replacePlaceHolder(select, MetaDataSqlManager.FQ_TABLE_NAME_PLACEHOLDER, fqTableName);

		if (sqlTemplate.contains(MetaDataSqlManager.TABLE_NAME_PLACEHOLDER))
		{
			// do not call getTableExpression() if not necessary.
			// this might trigger a SELECT to the database to get the current schema
			// to avoid unnecessary calls, this is only done if really needed
			select = TemplateHandler.replacePlaceHolder(select, MetaDataSqlManager.TABLE_NAME_PLACEHOLDER, table.getTableExpression(this.dbConnection));
		}
		return select;
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
