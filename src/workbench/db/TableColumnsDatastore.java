/*
 * TableColumnsDatastore.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

	public final static int COLUMN_IDX_TABLE_DEFINITION_COMPUTED = 6;

	/** The column index for a {@link workbench.storage.DataStore} returned
	 *  by {@link DbMetadata#getTableDefinition(TableIdentifier)} that holds
	 *  the remark for this column
	 */
	public final static int COLUMN_IDX_TABLE_DEFINITION_REMARKS = 7;

	/** The column index for a {@link workbench.storage.DataStore} returned
	 *  by {@link DbMetadata#getTableDefinition(TableIdentifier)} that holds
	 *  the integer value of the java datatype from {@link java.sql.Types}
	 */
	public final static int COLUMN_IDX_TABLE_DEFINITION_JAVA_SQL_TYPE = 8;

	/** The column index for a {@link workbench.storage.DataStore} returned
	 *  by {@link DbMetadata#getTableDefinition(TableIdentifier)} that holds
	 *  the integer value of siez of the column
	 */
	public final static int COLUMN_IDX_TABLE_DEFINITION_SIZE = 9;

	/** The column index for a {@link workbench.storage.DataStore} returned
	 *  by {@link DbMetadata#getTableDefinition(TableIdentifier)} that holds
	 *  the number of digits for the column
	 */
	public final static int COLUMN_IDX_TABLE_DEFINITION_DIGITS = 10;

	/** The column index for a {@link workbench.storage.DataStore} returned
	 *  by {@link DbMetadata#getTableDefinition(TableIdentifier)} that holds
	 *  the ordinal position of the column
	 */
	public final static int COLUMN_IDX_TABLE_DEFINITION_POSITION = 11;

	public static final String JAVA_SQL_TYPE_COL_NAME = "JDBC Type";

	public static final String[] TABLE_DEFINITION_COLS = {"COLUMN_NAME", "DATA_TYPE", "PK", "NULLABLE", "DEFAULT", "AUTOINCREMENT", "COMPUTED", "REMARKS", JAVA_SQL_TYPE_COL_NAME, "SCALE/SIZE", "PRECISION", "POSITION"};
	private static final int[] TYPES = {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.INTEGER};
	private static final int[] SIZES = {20, 18, 5, 8, 10, 10, 25, 18, 2, 2, 2, 2};

	private TableIdentifier sourceTable;

	public TableColumnsDatastore(TableDefinition table)
	{
		super(TABLE_DEFINITION_COLS, TYPES, SIZES);
		this.sourceTable = table.getTable();
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
				boolean isComputed = !col.isAutoGenerated() && StringUtil.isNonEmpty(col.getComputedColumnExpression());
				setValue(row, COLUMN_IDX_TABLE_DEFINITION_COMPUTED, isComputed ? "YES" : "NO");
				setValue(row, COLUMN_IDX_TABLE_DEFINITION_POSITION, Integer.valueOf(col.getPosition()));
				setValue(row, COLUMN_IDX_TABLE_DEFINITION_DATA_TYPE, col.getDbmsType());
				setValue(row, COLUMN_IDX_TABLE_DEFINITION_DEFAULT, col.getDefaultValue());
				setValue(row, COLUMN_IDX_TABLE_DEFINITION_REMARKS, col.getComment());
				getRow(row).setUserObject(col);
			}
		}
		this.resetStatus();
	}

	public TableIdentifier getSourceTable()
	{
		return sourceTable;
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
		List<ColumnIdentifier> result = new ArrayList<>(count);
		for (int i=0; i < count; i++)
		{
			ColumnIdentifier ci = (ColumnIdentifier)ds.getRow(i).getUserObject();
			if (ci == null)
			{
				String col = ds.getValueAsString(i, COLUMN_IDX_TABLE_DEFINITION_COL_NAME);
				int type = ds.getValueAsInt(i, COLUMN_IDX_TABLE_DEFINITION_JAVA_SQL_TYPE, Types.OTHER);
				String dbmstype = ds.getValueAsString(i, COLUMN_IDX_TABLE_DEFINITION_DATA_TYPE);
				boolean pk = "YES".equals(ds.getValueAsString(i, COLUMN_IDX_TABLE_DEFINITION_PK_FLAG));
				ci = new ColumnIdentifier(meta.quoteObjectname(col), meta.getDataTypeResolver().fixColumnType(type, dbmstype), pk);
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
			}
			result.add(ci);
		}
		return result;
	}

}
