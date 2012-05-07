/*
 * JdbcTableDefinitionReader
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import workbench.log.LogMgr;
import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class JdbcTableDefinitionReader
	implements TableDefinitionReader
{
	/**
	 * Return the definition of the given table.
	 * <br/>
	 * To display the columns for a table in a DataStore create an
	 * instance of {@link TableColumnsDatastore}.
	 *
	 * @param table The table for which the definition should be retrieved
	 * @param primaryKeyColumns the primary keys of <tt>table</tt>, may not be null
	 * @param dbConnection the connection to be used
	 * @param typeResolver the DataTypeResolver to be used. If null, it will be taken from the connection
	 *
	 * @throws SQLException
	 * @return the definition of the table.
	 * @see TableColumnsDatastore
	 */
	@Override
	public List<ColumnIdentifier> getTableColumns(TableIdentifier table, WbConnection dbConnection, DataTypeResolver typeResolver)
		throws SQLException
	{
		DbSettings dbSettings = dbConnection.getDbSettings();
		DbMetadata dbmeta = dbConnection.getMetadata();

		String tablename = StringUtil.trimQuotes(table.getTableName());
		String schema = StringUtil.trimQuotes(table.getSchema());
		String catalog = StringUtil.trimQuotes(table.getCatalog());

		if (dbConnection.getDbSettings().supportsMetaDataWildcards())
		{
			tablename = SqlUtil.escapeUnderscore(tablename, dbConnection);
			schema = SqlUtil.escapeUnderscore(schema, dbConnection);
			catalog = SqlUtil.escapeUnderscore(catalog, dbConnection);
		}

		ResultSet rs = null;
		List<ColumnIdentifier> columns = new ArrayList<ColumnIdentifier>();

		PkDefinition primaryKey = table.getPrimaryKey();
		Set<String> primaryKeyColumns = CollectionUtil.caseInsensitiveSet();

		LogMgr.logDebug("JdbcTableDefinitionReader.getTableColumns()", "PK for " + table.getTableName() + ": " + primaryKey);
		if (primaryKey != null)
		{
			primaryKeyColumns.addAll(primaryKey.getColumns());
		}

		try
		{
			rs = dbmeta.getJdbcMetaData().getColumns(catalog, schema, tablename, "%");

			ResultSetMetaData rsmeta = rs.getMetaData();
			boolean jdbc4 = false;

			if (rsmeta.getColumnCount() > 22)
			{
				String name = rsmeta.getColumnName(23);

				// HSQLDB 1.8 returns 23 columns, but is not JDBC4, so I need to check for the name as well.
				jdbc4 = name.equals("IS_AUTOINCREMENT");
			}

			while (rs != null && rs.next())
			{
				String colName = StringUtil.trim(rs.getString("COLUMN_NAME"));
				int sqlType = rs.getInt("DATA_TYPE");
				String typeName = rs.getString("TYPE_NAME");
				ColumnIdentifier col = new ColumnIdentifier(dbmeta.quoteObjectname(colName), typeResolver.fixColumnType(sqlType, typeName));

				int size = rs.getInt("COLUMN_SIZE");
				int digits = -1;
				try
				{
					digits = rs.getInt("DECIMAL_DIGITS");
				}
				catch (Exception e)
				{
					digits = -1;
				}
				if (rs.wasNull()) digits = -1;

				String remarks = rs.getString("REMARKS");
				String defaultValue = rs.getString("COLUMN_DEF");
				if (defaultValue != null && dbSettings.trimDefaults())
				{
					defaultValue = defaultValue.trim();
				}

				int position = -1;
				try
				{
					position = rs.getInt("ORDINAL_POSITION");
				}
				catch (SQLException e)
				{
					LogMgr.logWarning("DbMetadata", "JDBC driver does not suport ORDINAL_POSITION column for getColumns()", e);
					position = -1;
				}

				String nullable = rs.getString("IS_NULLABLE");
				String increment = jdbc4 ? rs.getString("IS_AUTOINCREMENT") : "NO";
				boolean autoincrement = StringUtil.stringToBool(increment);

				String display = typeResolver.getSqlTypeDisplay(typeName, sqlType, size, digits);

				col.setDbmsType(display);
				col.setIsAutoincrement(autoincrement);
				col.setIsPkColumn(primaryKeyColumns.contains(colName));
				col.setIsNullable("YES".equalsIgnoreCase(nullable));
				col.setDefaultValue(defaultValue);
				col.setComment(remarks);
				col.setColumnSize(size);
				col.setDecimalDigits(digits);
				col.setPosition(position);
				columns.add(col);
			}
		}
		finally
		{
			SqlUtil.closeResult(rs);
		}

		return columns;
	}
}
