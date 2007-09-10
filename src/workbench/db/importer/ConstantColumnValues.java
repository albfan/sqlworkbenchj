/*
 * ConstantColumnValues.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.importer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.util.ConverterException;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.ValueConverter;

/**
 * A class to parse column constants for the DataImporter
 *
 * @author support@sql-workbench.net
 */
public class ConstantColumnValues
{
	// I'm using two arraylists to ensure that the
	// order of the columns is always maintained.
	private List<ColumnIdentifier> columns;
	private List<Object> data;

	/**
	 * Parses a parameter value for column value definitions.
	 * e.g. description=something,firstname=arthur
	 * The values from the Commandline are converted to the correct
	 * datatype in the targettable.
	 * @throws SQLException if the target table was not found
	 * @throws ConverterException if a value could not be converted to the target data type
	 */
	public ConstantColumnValues(String parameterValue, WbConnection con, String tablename, ValueConverter converter)
		throws SQLException, ConverterException
	{
		List<ColumnIdentifier> tableColumns = con.getMetadata().getTableColumns(new TableIdentifier(tablename));
		if (tableColumns.size() == 0) throw new SQLException("Table '" + tablename + "' not found!");
		init(parameterValue, tableColumns, converter);
	}
	
	/**
	 * For Unit-Testing without a Database Connection
	 */
	ConstantColumnValues(String parameterValue, List<ColumnIdentifier> targetColumns)
		throws SQLException, ConverterException
	{
		init(parameterValue, targetColumns, new ValueConverter());
	}
	
	
	protected void init(String parameterValue, List<ColumnIdentifier> tableColumns, ValueConverter converter)
		throws SQLException, ConverterException
	{
		if (parameterValue == null) return;
		
		List<String> entries = StringUtil.stringToList(parameterValue, ",", true, true, false);
		if (entries.size() == 0) return;
		
		this.columns = new ArrayList<ColumnIdentifier>(entries.size());
		this.data = new ArrayList<Object>(entries.size());
		for (String entry : entries)
		{
			String[] parts = entry.split("=");
			if (parts.length == 2 && parts[0] != null && parts[1] != null)
			{
				String colname = parts[0];
				ColumnIdentifier col = findColumn(tableColumns, colname);
				if (col != null)
				{
					String value = parts[1];
					if (!StringUtil.isEmptyString(value))
					{
						if (value.startsWith("${"))
						{
							// DBMS Function call
							data.add(value.trim());
						}
						else
						{
							if (SqlUtil.isCharacterType(col.getDataType()))
							{
								if (value.charAt(0) == '\'' && value.charAt(value.length() - 1) == '\'')
								{
									value = value.substring(1, value.length() - 1);
								}
							}
							data.add(converter.convertValue(value, col.getDataType()));
						}
					}
					else
					{
						data.add(null);
						LogMgr.logWarning("ConstanColumnValues.init()", "Empty value for column '" + col + "' assumed as NULL");
					}
					columns.add(col);
				}
				else
				{
					throw new SQLException("Column '" + colname + "' not found in target table!");
				}
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
		if (!this.isFunctionCall(index)) return null;
		String value = (String)this.getValue(index);
		
		// The function call is enclosed in ${...}
		return value.substring(2, value.length() - 1);
	}
	
	public boolean isFunctionCall(int index)
	{
		Object value = this.getValue(index);
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
		if (this.columns == null) return 0;
		return this.columns.size();
	}
	
	public ColumnIdentifier getColumn(int index)
	{
		return this.columns.get(index);
	}
	
	public Object getValue(int index)
	{
		return this.data.get(index);
	}
	
	public boolean removeColumn(ColumnIdentifier col)
	{
		if (this.columns == null) return false;
		int index = this.columns.indexOf(col);
		if (index > -1)
		{
			this.columns.remove(index);
			this.data.remove(index);
		}
		return (index > -1);
	}
	
	public void setParameter(PreparedStatement pstmt, int statementIndex, int columnIndex)
		throws SQLException
	{
		ColumnIdentifier col = getColumn(columnIndex);
		Object value = getValue(columnIndex);
		int type = col.getDataType();
		
		// If the column value is a function call, this will not
		// be used in a prepared statement. It is expected that the caller
		// (that prepared the statement) inserted the literal value of the
		// function call into the SQL instead of a ? placeholder
		if (!isFunctionCall(columnIndex))
		{
			pstmt.setObject(statementIndex, value);
		}
	}
}
