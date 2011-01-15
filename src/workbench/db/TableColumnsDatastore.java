/*
 * TableColumnsDatastore.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import workbench.storage.DataStore;
import workbench.util.StringUtil;

/**
 * A DataStore to display a List of ColumnIdentifier objects.
 *
 * @author Thomas Kellerer
 * @see DbMetadata#getTableDefinition(workbench.db.TableIdentifier)
 * @see DbMetadata#getTableColumns(workbench.db.TableIdentifier) 
 */
public class TableColumnsDatastore
	extends DataStore
{
	/** The column index for a {@link workbench.storage.DataStore} returned
	 *  by {@link DbMetadata#getTableDefinition(TableIdentifier)} that holds
	 *  the column name
	 */
	public final static int COLUMN_IDX_TABLE_DEFINITION_COL_NAME = 0;

	/** The column index for a {@link workbench.storage.DataStore} returned
	 *  by {@link DbMetadata#getTableDefinition(TableIdentifier)} that holds
	 *  the DBMS specific data type string
	 */
	public final static int COLUMN_IDX_TABLE_DEFINITION_DATA_TYPE = 1;

	/** The column index for a {@link workbench.storage.DataStore} returned
	 *  by {@link DbMetadata#getTableDefinition(TableIdentifier)} that holds
	 *  the primary key flag
	 */
	public final static int COLUMN_IDX_TABLE_DEFINITION_PK_FLAG = 2;

	/** The column index for a {@link workbench.storage.DataStore} returned
	 *  by {@link DbMetadata#getTableDefinition(TableIdentifier)} that holds
	 *  the nullable flag
	 */
	public final static int COLUMN_IDX_TABLE_DEFINITION_NULLABLE = 3;

	/** The column index for a {@link workbench.storage.DataStore} returned
	 *  by {@link DbMetadata#getTableDefinition(TableIdentifier)} that holds
	 *  the default value for this column
	 */
	public final static int COLUMN_IDX_TABLE_DEFINITION_DEFAULT = 4;

	/** The column index for a {@link workbench.storage.DataStore} returned
	 *  by {@link DbMetadata#getTableDefinition(TableIdentifier)} that holds
	 *  the auto increment flag for this column
	 */
	public final static int COLUMN_IDX_TABLE_DEFINITION_AUTO_INC = 5;

	/** The column index for a {@link workbench.storage.DataStore} returned
	 *  by {@link DbMetadata#getTableDefinition(TableIdentifier)} that holds
	 *  the remark for this column
	 */
	public final static int COLUMN_IDX_TABLE_DEFINITION_REMARKS = 6;

	/** The column index for a {@link workbench.storage.DataStore} returned
	 *  by {@link DbMetadata#getTableDefinition(TableIdentifier)} that holds
	 *  the integer value of the java datatype from {@link java.sql.Types}
	 */
	public final static int COLUMN_IDX_TABLE_DEFINITION_JAVA_SQL_TYPE = 7;

	/** The column index for a {@link workbench.storage.DataStore} returned
	 *  by {@link DbMetadata#getTableDefinition(TableIdentifier)} that holds
	 *  the integer value of siez of the column
	 */
	public final static int COLUMN_IDX_TABLE_DEFINITION_SIZE = 8;

	/** The column index for a {@link workbench.storage.DataStore} returned
	 *  by {@link DbMetadata#getTableDefinition(TableIdentifier)} that holds
	 *  the number of digits for the column
	 */
	public final static int COLUMN_IDX_TABLE_DEFINITION_DIGITS = 9;

	/** The column index for a {@link workbench.storage.DataStore} returned
	 *  by {@link DbMetadata#getTableDefinition(TableIdentifier)} that holds
	 *  the ordinal position of the column
	 */
	public final static int COLUMN_IDX_TABLE_DEFINITION_POSITION = 10;

	public static final String JAVA_SQL_TYPE_COL_NAME = "JDBC Type";

	public static final String[] TABLE_DEFINITION_COLS = {"COLUMN_NAME", "DATA_TYPE", "PK", "NULLABLE", "DEFAULT", "AUTOINCREMENT", "REMARKS", JAVA_SQL_TYPE_COL_NAME, "SCALE/SIZE", "PRECISION", "POSITION"};
	private static final int[] TYPES = {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.INTEGER};
	private static final int[] SIZES = {20, 18, 5, 8, 10, 10, 25, 18, 2, 2, 2};

	public TableColumnsDatastore(TableDefinition table)
	{
		super(TABLE_DEFINITION_COLS, TYPES, SIZES);
		List<ColumnIdentifier> columns = table.getColumns();
		if (columns != null)
		{
			for (ColumnIdentifier col : columns)
			{
				int row = addRow();
				setValue(row, COLUMN_IDX_TABLE_DEFINITION_COL_NAME, col.getColumnName());
				setValue(row, COLUMN_IDX_TABLE_DEFINITION_JAVA_SQL_TYPE, Integer.valueOf(col.getDataType()));
				setValue(row, COLUMN_IDX_TABLE_DEFINITION_PK_FLAG, col.isPkColumn() ? "YES" : "NO");
				setValue(row, COLUMN_IDX_TABLE_DEFINITION_SIZE, Integer.valueOf(col.getColumnSize()));
				setValue(row, COLUMN_IDX_TABLE_DEFINITION_DIGITS, Integer.valueOf(col.getDecimalDigits()));
				setValue(row, COLUMN_IDX_TABLE_DEFINITION_NULLABLE, col.isNullable() ? "YES" : "NO");
				setValue(row, COLUMN_IDX_TABLE_DEFINITION_AUTO_INC, col.isAutoincrement() ? "YES" : "NO");
				setValue(row, COLUMN_IDX_TABLE_DEFINITION_POSITION, Integer.valueOf(col.getPosition()));
				setValue(row, COLUMN_IDX_TABLE_DEFINITION_DATA_TYPE, col.getDbmsType());
				setValue(row, COLUMN_IDX_TABLE_DEFINITION_DEFAULT, col.getDefaultValue());
				setValue(row, COLUMN_IDX_TABLE_DEFINITION_REMARKS, col.getComment());
			}
		}
		this.resetStatus();
	}

	/**
	 * Convert the contents of a DataStore back to a List of ColumnIdentifier
	 *
	 * @param meta the DbMetadata for the current connection (used to quote names correctly)
	 * @param ds the datastore to be converted
	 * @return the ColumnIdentifiers
	 */
	public static List<ColumnIdentifier> createColumnIdentifiers(DbMetadata meta, DataStore ds)
	{
		int count = ds.getRowCount();
		List<ColumnIdentifier> result = new ArrayList<ColumnIdentifier>(count);
		for (int i=0; i < count; i++)
		{
			String col = ds.getValueAsString(i, COLUMN_IDX_TABLE_DEFINITION_COL_NAME);
			int type = ds.getValueAsInt(i, COLUMN_IDX_TABLE_DEFINITION_JAVA_SQL_TYPE, Types.OTHER);
			String dbmstype = ds.getValueAsString(i, COLUMN_IDX_TABLE_DEFINITION_DATA_TYPE);
			boolean pk = "YES".equals(ds.getValueAsString(i, COLUMN_IDX_TABLE_DEFINITION_PK_FLAG));
			ColumnIdentifier ci = new ColumnIdentifier(meta.quoteObjectname(col), meta.getDataTypeResolver().fixColumnType(type, dbmstype), pk);
			int size = ds.getValueAsInt(i, COLUMN_IDX_TABLE_DEFINITION_SIZE, 0);
			int digits = ds.getValueAsInt(i, COLUMN_IDX_TABLE_DEFINITION_DIGITS, -1);
			String nullable = ds.getValueAsString(i, COLUMN_IDX_TABLE_DEFINITION_NULLABLE);
			int position = ds.getValueAsInt(i, COLUMN_IDX_TABLE_DEFINITION_POSITION, 0);
			String comment = ds.getValueAsString(i, COLUMN_IDX_TABLE_DEFINITION_REMARKS);
			String def = ds.getValueAsString(i, COLUMN_IDX_TABLE_DEFINITION_DEFAULT);
			ci.setColumnSize(size);
			ci.setDecimalDigits(digits);
			ci.setIsNullable(StringUtil.stringToBool(nullable));
			ci.setDbmsType(dbmstype);
			ci.setComment(comment);
			ci.setDefaultValue(def);
			ci.setPosition(position);
			result.add(ci);
		}
		return result;
	}

}
