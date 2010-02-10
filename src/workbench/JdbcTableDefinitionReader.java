/*
 * JdbcTableDefinitionReader
 * 
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 * 
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import workbench.db.ColumnIdentifier;
import workbench.db.DataTypeResolver;
import workbench.db.DbMetadata;
import workbench.db.DbSettings;
import workbench.db.TableColumnsDatastore;
import workbench.db.TableDefinitionReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
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
	 * @param toRead The table for which the definition should be retrieved
	 *
	 * @throws SQLException
	 * @return the definition of the table.
	 * @see TableColumnsDatastore
	 */
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

		try
		{
			rs = dbmeta.getJdbcMetaData().getColumns(catalog, schema, tablename, "%");

			while (rs != null && rs.next())
			{
				// The columns should be retrieved (getXxx()) in the order
				// as they appear in the result set as some drivers
				// do not like an out-of-order processing of the columns
				String colName = rs.getString("COLUMN_NAME"); // index 4
				int sqlType = rs.getInt("DATA_TYPE"); // index 5
				ColumnIdentifier col = new ColumnIdentifier(dbmeta.quoteObjectname(colName), typeResolver.fixColumnType(sqlType));

				String typeName = rs.getString("TYPE_NAME");

				int size = rs.getInt("COLUMN_SIZE"); // index 7
				int digits = -1;
				try
				{
					digits = rs.getInt("DECIMAL_DIGITS"); // index 9
				}
				catch (Exception e)
				{
					digits = -1;
				}
				if (rs.wasNull()) digits = -1;

				String remarks = rs.getString("REMARKS"); // index 12
				String defaultValue = rs.getString("COLUMN_DEF"); // index 13
				if (defaultValue != null && dbSettings.trimDefaults())
				{
					defaultValue = defaultValue.trim();
				}

				int position = -1;
				try
				{
					position = rs.getInt("ORDINAL_POSITION"); // index 17
				}
				catch (SQLException e)
				{
					LogMgr.logWarning("DbMetadata", "JDBC driver does not suport ORDINAL_POSITION column for getColumns()", e);
					position = -1;
				}

				String nullable = rs.getString("IS_NULLABLE"); // index 18

				String display = typeResolver.getSqlTypeDisplay(typeName, sqlType, size, digits);

				col.setDbmsType(display);
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

}
