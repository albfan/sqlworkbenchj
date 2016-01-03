/*
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.gui.components;

import java.util.ArrayList;
import java.util.List;

import workbench.db.ColumnIdentifier;

import workbench.storage.DataStore;

import org.dbunit.dataset.Column;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.ITableMetaData;
import org.dbunit.dataset.RowOutOfBoundsException;
import org.dbunit.dataset.datatype.DataType;

/**
 *
 * @author Brian Bonner
 */
public class DBUnitTableAdapter
	implements ITable
{

	private DataStore dataStore;

	public DBUnitTableAdapter(DataStore dataStore)
	{
		this.dataStore = dataStore;
	}

	@Override
	public Object getValue(int row, String column)
		throws DataSetException
	{
		if (row < 0 || row >= getRowCount())
		{
			throw new RowOutOfBoundsException();
		}
		return dataStore.getValue(row, column);
	}

	@Override
	public ITableMetaData getTableMetaData()
	{
		return new ITableMetaData()
		{

			@Override
			public String getTableName()
			{
				if (dataStore.getUpdateTable() != null)
				{
					return dataStore.getUpdateTable().getTableName();
				}
				else if (dataStore.getResultInfo() != null && dataStore.getResultInfo().getUpdateTable() != null)
				{
					return dataStore.getResultInfo().getUpdateTable().getTableName();
				}
				else if (dataStore.getInsertTable() != null)
				{
					return dataStore.getInsertTable();
				}
				else
				{
					return "UNKNOWN";
				}
			}

			@Override
			public Column[] getPrimaryKeys()
				throws DataSetException
			{
				if (!dataStore.hasPkColumns()) return null;

				ColumnIdentifier[] columns = dataStore.getColumns();

				if (columns == null) return null;

				List<Column> pkCols = new ArrayList<>(1);
				for (ColumnIdentifier col : columns)
				{
					if (col.isPkColumn())
					{
						DataType type = DataType.forSqlType(col.getDataType());
						Column pk = new Column(col.getColumnName(), type);
						pkCols.add(pk);
					}
				}
				if (pkCols.isEmpty()) return null;

				Column[] result = pkCols.toArray(new Column[]{});
				return result;
			}

			@Override
			public Column[] getColumns()
				throws DataSetException
			{
				List<Column> columns = new ArrayList<>();
				for (int i = 0; i < dataStore.getColumns().length; i++)
				{
					String columnName = dataStore.getColumnName(i);
					DataType columnType = DataType.forSqlType(dataStore.getColumnType(i));
					Column column = new Column(columnName, columnType);
					columns.add(column);
				}
				return columns.toArray(new Column[columns.size()]);
			}

			@Override
			public int getColumnIndex(String columnName)
				throws DataSetException
			{
				return dataStore.getColumnIndex(columnName);
			}

		};
	}

	@Override
	public int getRowCount()
	{
		return dataStore.getRowCount();
	}

}
