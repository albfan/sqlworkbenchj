/*
 * ConstantColumnValues.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
package workbench.db.importer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.storage.ColumnData;
import workbench.util.CollectionUtil;
import workbench.util.ConverterException;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.ValueConverter;

/**
 * A class to parse column constants for the DataImporter
 *
 * @author Thomas Kellerer
 */
public class ConstantColumnValues
{
	private List<ColumnData> columnValues;
	private Map<Integer, ValueStatement> selectStatements;

	/**
	 * Parses a parameter value for column value definitions.
	 * e.g. description=something,firstname=arthur
	 * The values from the Commandline are converted to the correct
	 * datatype in the targettable.
	 * @throws SQLException if the target table was not found
	 * @throws ConverterException if a value could not be converted to the target data type
	 *
	 */
	public ConstantColumnValues(List<String> entries, WbConnection con, String tablename, ValueConverter converter)
		throws SQLException, ConverterException
	{
		List<ColumnIdentifier> tableColumns = con.getMetadata().getTableColumns(new TableIdentifier(tablename, con));
		if (tableColumns.isEmpty()) throw new SQLException("Table '" + tablename + "' not found!");
		init(entries, tableColumns, converter);
	}

	/**
	 * For Unit-Testing without a Database Connection
	 */
	ConstantColumnValues(List<String> entries, List<ColumnIdentifier> targetColumns)
		throws SQLException, ConverterException
	{
		init(entries, targetColumns, new ValueConverter());
	}

	protected final void init(List<String> entries, List<ColumnIdentifier> tableColumns, ValueConverter converter)
		throws SQLException, ConverterException
	{

		columnValues = new ArrayList<>(entries.size());
		selectStatements = new HashMap<>();

		for (String entry : entries)
		{
			int pos = entry.indexOf('=');
			if (pos < 0) continue;
			String colname = entry.substring(0, pos);
			String value = entry.substring(pos + 1);

			ColumnIdentifier col = findColumn(tableColumns, colname);

			if (col != null)
			{
				Object data = null;
				if (StringUtil.isEmptyString(value))
				{
					LogMgr.logWarning("ConstanColumnValues.init()", "Empty value for column '" + col + "' assumed as NULL");
				}
				else
				{
					if (value.startsWith("${") || value.startsWith("$@{"))
					{
						// DBMS Function call
						data = value.trim();
					}
					else
					{
						if (SqlUtil.isCharacterType(col.getDataType()) &&
								value.charAt(0) == '\'' && value.charAt(value.length() - 1) == '\'')
						{
							value = value.substring(1, value.length() - 1);
						}
						data = converter.convertValue(value, col.getDataType());
					}
				}
				this.columnValues.add(new ColumnData(data, col));
			}
			else
			{
				throw new SQLException("Column '" + colname + "' not found in target table!");
			}

		}
	}

	private ColumnIdentifier findColumn(List<ColumnIdentifier> columns, String name)
	{
		for (ColumnIdentifier col : columns)
		{
			if (col.getColumnName().equalsIgnoreCase(name)) return col;
		}
		return null;
	}

	public String getFunctionLiteral(int index)
	{
		if (!isFunctionCall(index)) return null;
		String value = (String)this.getValue(index);

		// The function call is enclosed in ${...}
		return value.substring(2, value.length() - 1);
	}

	public List<String> getInputColumnsForFunction(int index)
	{
		String func = getFunctionLiteral(index);
		if (func == null) return null;
		List<String> args = SqlUtil.getFunctionParameters(func);
		List<String> result = CollectionUtil.arrayList();
		for (String f : args)
		{
			String arg = StringUtil.trimQuotes(f);
			if (arg.startsWith("$"))
			{
				result.add(arg.substring(1));
			}
		}
		return result;
	}

	public ValueStatement getStatement(int index)
	{
		ValueStatement stmt = selectStatements.get(index);
		if (stmt == null)
		{
			if (!isSelectStatement(index)) return null;
			String value = (String)getValue(index);
			String sql = value.substring(3, value.length() - 1);
			stmt = new ValueStatement(sql);
			selectStatements.put(index, stmt);
		}
		return stmt;
	}

	public boolean isSelectStatement(int index)
	{
		Object value = getValue(index);
		if (value == null) return false;

		if (value instanceof String)
		{
			String f = (String)value;
			return f.startsWith("$@{") && f.endsWith("}");
		}
		return false;
	}

	public boolean isFunctionCall(int index)
	{
		Object value = getValue(index);
		if (value == null) return false;

		if (value instanceof String)
		{
			String f = (String)value;
			return f.startsWith("${") && f.endsWith("}");
		}
		return false;
	}

	public int getColumnCount()
	{
		if (columnValues == null) return 0;
		return columnValues.size();
	}

	public ColumnIdentifier getColumn(int index)
	{
		return columnValues.get(index).getIdentifier();
	}

	public Object getValue(int index)
	{
		return columnValues.get(index).getValue();
	}

	public boolean removeColumn(ColumnIdentifier col)
	{
		if (columnValues == null) return false;
		if (col == null) return false;

		int index = -1;
		for (int i=0; i < columnValues.size(); i++)
		{
			if (columnValues.get(i).getIdentifier().equals(col))
			{
				index = i;
				break;
			}
		}

		if (index > -1)
		{
			this.columnValues.remove(index);
		}
		return (index > -1);
	}

	public void setParameter(PreparedStatement pstmt, int statementIndex, int columnIndex)
		throws SQLException
	{
		Object value = getValue(columnIndex);

		// If the column value is a function call, this will not
		// be used in a prepared statement. It is expected that the caller
		// (that prepared the statement) inserted the literal value of the
		// function call into the SQL instead of a ? placeholder
		if (!isFunctionCall(columnIndex))
		{
			pstmt.setObject(statementIndex, value);
		}
	}

	public void done()
	{
		for (ValueStatement stmt : selectStatements.values())
		{
			if (stmt != null) stmt.done();
		}
	}
}

