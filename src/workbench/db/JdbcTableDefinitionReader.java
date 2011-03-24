/*
 * JdbcTableDefinitionReader
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
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
import workbench.log.LogMgr;
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
	public List<ColumnIdentifier> getTableColumns(TableIdentifier table, List<String> primaryKeyColumns, WbConnection dbConnection, DataTypeResolver typeResolver)
		throws SQLException
	{
		DbSettings dbSettings = dbConnection.getDbSettings();
		DbMetadata dbmeta = dbConnection.getMetadata();

		String catalog = StringUtil.trimQuotes(table.getCatalog());
		String schema = StringUtil.trimQuotes(table.getSchema());
		String tablename = StringUtil.trimQuotes(table.getTableName());

		ResultSet rs = null;

		List<ColumnIdentifier> columns = new ArrayList<ColumnIdentifier>();

		TableIdentifier requested = new TableIdentifier(catalog, schema, tablename);
		boolean hasWildcards = hasSQLWildcard(tablename) || hasSQLWildcard(schema);

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
				String colTable = rs.getString("TABLE_NAME");
				String colSchema = rs.getString("TABLE_SCHEM");
				String colCatalog = rs.getString("TABLE_CAT");
				if (hasWildcards && !isTableColumn(requested, new TableIdentifier(colCatalog, colSchema, colTable), dbConnection))
				{
					continue;
				}
				String colName = rs.getString("COLUMN_NAME");
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
				col.setIsPkColumn(primaryKeyColumns.contains(colName.toLowerCase()));
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

	private boolean hasSQLWildcard(String input)
	{
		if (input == null)
		{
			return false;
		}
		return input.indexOf('_') > -1 || input.indexOf('%') > -1;
	}
	private boolean isTableColumn(TableIdentifier requestedTable, TableIdentifier retrievedTable, WbConnection dbConnection)
	{
		if (!requestedTable.getTableName().equals(retrievedTable.getTableName()))
		{
			return false;
		}

		String fullName1 = requestedTable.getTableExpression(dbConnection);
		String fullName2 = retrievedTable.getTableExpression(dbConnection);

		return fullName1.equals(fullName2);
	}

}
