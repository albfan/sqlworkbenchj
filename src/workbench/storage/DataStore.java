/*
 * DataStore.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.Collator;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.exporter.HtmlRowDataConverter;
import workbench.db.exporter.RowDataConverter;
import workbench.db.exporter.SqlRowDataConverter;
import workbench.db.exporter.XmlRowDataConverter;
import workbench.gui.WbSwingUtilities;
import workbench.interfaces.JobErrorHandler;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.CsvLineParser;
import workbench.util.SqlUtil;
import workbench.util.StrBuffer;
import workbench.util.ValueConverter;



/**
 * A class to cache the result of a database query.
 * If the underlying SELECT used only one table, then the
 * DataStore can be updated and the changes can be saved back
 * to the database. For inserting new records key columns are not
 * required, otherwise the base table needs to have a key defined.
 * @author  support@sql-workbench.net
 */
public class DataStore
{

	// Needed for the status display in the table model
	// as RowData is only package visible. Thus we need to provide the objects here
	public static final Integer ROW_MODIFIED = new Integer(RowData.MODIFIED);
	public static final Integer ROW_NEW = new Integer(RowData.NEW);
	public static final Integer ROW_ORIGINAL = new Integer(RowData.NOT_MODIFIED);

	private RowActionMonitor rowActionMonitor;

	private boolean modified;
	private boolean pkColumnsRead = false;
	private int realColumns;

	private RowDataList data;
	protected RowDataList deletedRows;

	// The SQL statement (SELECT) that produced this DataStore
	private String sql;

	private ResultInfo resultInfo;
	private String updateTable;

	private WbConnection originalConnection;
	private DecimalFormat defaultNumberFormatter;
	
	private boolean allowUpdates = false;
	private boolean escapeHtml = true;

	private ValueConverter converter = new ValueConverter();

	private boolean cancelRetrieve = false;
	private boolean cancelUpdate = false;
	private boolean cancelImport = false;
	private int reportInterval = Settings.getInstance().getIntProperty("workbench.gui.data.reportinterval", 10);

	public DataStore(String[] aColNames, int[] colTypes)
	{
		this(aColNames, colTypes, null);
	}
	/**
	 *	Create a DataStore which is not based on a result set
	 *	and contains the columns defined in the given array
	 *	The column types need to be SQL types from java.sql.Types
	 */
	public DataStore(String[] colNames, int[] colTypes, int[] colSizes)
	{
		this.data = createData();
		this.setColumnSizes(colSizes);
		this.resultInfo = new ResultInfo(colNames, colTypes, colSizes);
	}

	/**
	 *	Create a DataStore based on the contents of the given	ResultSet.
	 */
  public DataStore(ResultSet aResultSet, WbConnection aConn)
		throws SQLException
  {
		if (aResultSet == null) return;
		this.originalConnection = aConn;
		this.initData(aResultSet);
  }

	public DataStore(ResultInfo metaData)
	{
		this.resultInfo = metaData;
		this.data = createData();
	}

	public DataStore(ResultSet aResult)
		throws SQLException
	{
		this(aResult, false);
	}

	public DataStore(ResultSet aResult, boolean readData)
		throws SQLException
	{
		this(aResult, readData, null, -1, null);
	}

	/**
	 *	Create a DataStore based on the given ResultSet.
	 *	@param aResult the result set to use
	 *  @param readData if true the data from the ResultSet should be read into memory, otherwise only MetaData information is read
	 *  @param maxRows limit number of rows to maxRows if the JDBC driver does not already limit them
	 */
	public DataStore(ResultSet aResult, boolean readData, int maxRows)
		throws SQLException
	{
		this(aResult, readData, null, maxRows, null);
	}

	/**
	 *	Create a DataStore based on the given ResultSet.
	 *	@param aResult the result set to use
	 *  @param readData if true the data from the ResultSet should be read into memory, otherwise only MetaData information is read
	 *  @param aMonitor if not null, the loading process is displayed through this monitor
	 */
	public DataStore(ResultSet aResult, boolean readData, RowActionMonitor aMonitor)
		throws SQLException
	{
		this(aResult, readData, aMonitor, -1, null);
	}


	/**
	 *	Create a DataStore based on the given ResultSet.
	 *	@param aResult the result set to use
	 *  @param readData if true the data from the ResultSet should be read into memory, otherwise only MetaData information is read
	 *  @param aMonitor if not null, the loading process is displayed through this monitor
	 *  @param maxRows limit number of rows to maxRows if the JDBC driver does not already limit them
	 *  @param conn the connection that was used to retrieve the result set
	 */
	public DataStore(ResultSet aResult, boolean readData, RowActionMonitor aMonitor, int maxRows, WbConnection conn)
		throws SQLException
	{
		this.rowActionMonitor = aMonitor;
		this.originalConnection = conn;
		if (readData)
		{
			this.initData(aResult, maxRows);
		}
		else
		{
			ResultSetMetaData metaData = aResult.getMetaData();
			this.initMetaData(metaData);
			this.data = createData();
		}
	}

	/**
	 * Create an empty DataStore based on the information given in the MetaData
	 * object. The DataStore can be populated with the {@link #addRow(ResultSet)} method.
	 */
	public DataStore(ResultSetMetaData metaData, WbConnection aConn)
		throws SQLException
	{
		this.originalConnection = aConn;
		this.initMetaData(metaData);
		this.reset();
	}

	public WbConnection getOriginalConnection()
	{
		return this.originalConnection;
	}

	public void setSourceConnection(WbConnection aConn)
	{
		this.originalConnection = aConn;
	}

	public void setColumnSizes(int[] sizes)
	{
		if (this.resultInfo == null) return;
		this.resultInfo.setColumnSizes(sizes);
	}

	public void setAllowUpdates(boolean aFlag)
	{
		this.allowUpdates = aFlag;
	}

	private RowDataList createData(int size)
	{
		return new RowDataList(size);
	}
	private RowDataList createData()
	{
		return new RowDataList();
	}

	public int duplicateRow(int aRow)
	{
		if (aRow < 0 || aRow >= this.getRowCount()) return -1;
		RowData oldRow = this.getRow(aRow);
		RowData newRow = oldRow.createCopy();
		int newIndex = aRow + 1;
		if (newIndex >= this.getRowCount()) newIndex = this.getRowCount();
		this.data.add(newIndex, newRow);
		this.modified = true;
		return newIndex;
	}

  public int getRowCount() { return this.data.size(); }
	public int getColumnCount() { return this.resultInfo.getColumnCount(); }

	/**
	 *	Returns the total number of modified, new or deleted rows
	 */
	public int getModifiedCount()
	{
		if (!this.isModified()) return 0;
		int count = this.getRowCount();
		int modifiedCount = 0;
		for (int i=0; i < count; i++)
		{
			if (this.isRowModified(i)) modifiedCount++;
		}
		if (this.deletedRows != null)
		{
			count = this.deletedRows.size();
			for (int i=0; i < count; i++)
			{
				RowData data = this.deletedRows.get(i);
				if (!data.isNew()) modifiedCount++;
			}
		}
		return modifiedCount;
	}

	public int getColumnType(int aColumn)
		throws IndexOutOfBoundsException
	{
		return this.resultInfo.getColumnType(aColumn);
	}

	public String getColumnClassName(int aColumn)
	{
		return this.resultInfo.getColumnClassName(aColumn);
	}

	public Class getColumnClass(int aColumn)
	{
		return this.resultInfo.getColumnClass(aColumn);
	}

	/**
	 *	Deletes the given row and saves it in the delete buffer
	 *	in order to be able to generate a DELETE statement if
	 *	this DataStore needs updating
	 */
	public void deleteRow(int aRow)
		throws IndexOutOfBoundsException
	{
		RowData row = this.data.get(aRow);
		// new rows (not read from the database)
		// do not need to be put into the deleted buffer
		if (row.isNew())
		{
			this.data.remove(aRow);
		}
		else
		{
			if (this.deletedRows == null) this.deletedRows = createData();
			this.deletedRows.add(row);
			this.data.remove(aRow);
			this.modified = true;
		}
	}

	/**
	 *	Adds the next row from the result set
	 *	to the DataStore. No check will be done
	 *	if the ResultSet matches the current
	 *	column structure!!
	 *	@return int - the new row number
	 *	The new row will be marked as "Not modified".
	 */
	public int addRow(ResultSet data)
		throws SQLException
	{
		int cols = this.resultInfo.getColumnCount();
		RowData row = new RowData(cols);
		this.data.add(row);
		for (int i=0; i < cols; i++)
		{
			Object value = data.getObject(i + 1);
			if (data.wasNull() || value == null)
			{
				row.setNull(i, this.resultInfo.getColumnType(i));
			}
			else
			{
				row.setValue(i, value);
			}
		}
		row.resetStatus();
		return this.getRowCount() - 1;
	}

	/**
	 *	Adds a new empty row to the DataStore.
	 *	The new row will be marked as Modified
	 *	@return int - the new row number
	 */
	public int addRow()
	{
		RowData row = new RowData(this.resultInfo.getColumnCount());
		this.data.add(row);
		this.modified = true;
		return this.getRowCount() - 1;
	}

	public int addRow(RowData row)
	{
		this.data.add(row);
		this.modified = true;
		return this.getRowCount() - 1;
	}

	/**
	 *	Inserts a row after the given row number.
	 *	If the new Index is greater then the current
	 *	row count or the new index is < 0 the new
	 *	row will be added at the end.
	 *	@return int - the new row number
	 */
	public int insertRowAfter(int anIndex)
	{
		RowData row = new RowData(this.resultInfo.getColumnCount());
		anIndex ++;
		int newIndex = -1;

		if (anIndex > this.data.size() || anIndex < 0)
		{
			this.data.add(row);
			newIndex = this.getRowCount();
		}
		else
		{
			this.data.add(anIndex, row);
			newIndex = anIndex;
		}
		this.modified = true;
		return newIndex;
	}

	public boolean useUpdateTableFromSql(String aSql)
	{
		return this.useUpdateTableFromSql(aSql, false);
	}

	public boolean useUpdateTableFromSql(String aSql, boolean retrievePk)
	{
		this.updateTable = null;

		if (aSql == null) return false;
		List tables = SqlUtil.getTables(aSql);
		if (tables.size() != 1) return false;

		String table = (String)tables.get(0);
		this.useUpdateTable(table);
		if (retrievePk)
		{
			try
			{
				this.updatePkInformation(this.originalConnection);
			}
			catch (Exception e)
			{
				return false;
			}
			return (this.resultInfo.hasPkColumns());
		}
		return true;
	}

	public void useUpdateTable(String aTablename)
	{
		this.useUpdateTable(aTablename, false);
	}
	public void useUpdateTable(String aTablename, boolean updatePk)
	{
		// A connection-less update table is used to
		// create INSERT statements regardless if the
		// table actually exists, or if it really is a table.
		// this is used e.g. for creating scripts from result sets
		this.updateTable = aTablename;
		int colCount = this.resultInfo.getColumnCount();
		for (int i=0; i < colCount; i++)
		{
			this.resultInfo.setIsPkColumn(i, true);
		}
		if (updatePk)
		{
			try
			{
				this.updatePkInformation(this.originalConnection);
			}
			catch (Exception e)
			{
				LogMgr.logError("DataStore.useUpdateTable()", "Could not retrieve PK information", e);
			}
		}
	}

	public void setUpdateTable(String aTablename)
	{
		this.setUpdateTable(aTablename, this.originalConnection);
	}

	/**
	 * Sets the table to be updated for this DataStore.
	 * Upon setting the table, the column definition for the table
	 * will be retrieved using {@link workbench.db.DbMetadata}
	 * @param aTablename
	 * @param aConn
	 */
	public void setUpdateTable(String aTablename, WbConnection aConn)
	{
		setUpdateTable(new TableIdentifier(aTablename), aConn);
	}
	
	public void setUpdateTable(TableIdentifier tbl)
	{
		setUpdateTable(tbl, this.originalConnection);
	}
	
	public void setUpdateTable(TableIdentifier tbl, WbConnection aConn)
	{
		if (tbl == null)
		{
			this.updateTable = null;
		}
		else if (!tbl.getTableExpression().equalsIgnoreCase(this.updateTable) && aConn != null)
		{
			this.updateTable = null;
			// check the columns which are in that table
			// so that we can refuse any changes to columns
			// which do not derive from that table
			// note that this does not work, if the
			// columns were renamed via an alias in the
			// select statement
			try
			{
				DbMetadata meta = aConn.getMetadata();
				if (meta == null) return;

				this.updateTable = tbl.getTableExpression(aConn);
				
				//TableIdentifier tbl = new TableIdentifier(aTablename, aConn);
				ColumnIdentifier[] columns = meta.getColumnIdentifiers(tbl);
				if (columns == null || columns.length == 0)
				{
					LogMgr.logWarning("Datastore.setUpdateTable()", "No columns found for table " + tbl.getTableExpression() + "! Assuming columns from result set belong to update table", null);
				}
				else
				{
					for (int i=0; i < columns.length; i++)
					{
						int index = this.findColumn(columns[i].getColumnName());
						if (index > -1)
						{
							this.resultInfo.setUpdateable(index, true);
							this.resultInfo.setIsPkColumn(index, columns[i].isPkColumn());
						}
					}
				}
			}
			catch (Exception e)
			{
				this.updateTable = null;
				LogMgr.logError("DataStore.setUpdateTable()", "Could not read table definition", e);
			}
		}
		if (this.updateTable == null)
		{
			this.resultInfo.setUpdateTable(null);
		}
		else
		{
			String schema = this.getUpdateTableSchema(aConn);
			this.resultInfo.setUpdateTable(new TableIdentifier(schema, this.updateTable));
		}
	}

	/**
	 * Returns the current table to be updated
	 * @return The current update table
	 */
	public String getUpdateTable()
	{
		return this.updateTable;
	}

	/**
	 * Return the name of the given column
	 * @param aColumn The index of the column in this DataStore. The first column index is 0
	 * @return The name of the column
	 */
	public String getColumnName(int aColumn)
		throws IndexOutOfBoundsException
	{
		return this.resultInfo.getColumnName(aColumn);
	}

	public int getColumnDisplaySize(int aColumn)
		throws IndexOutOfBoundsException
	{
		return this.resultInfo.getColumnSize(aColumn);
	}

	protected Object getOriginalValue(int aRow, int aColumn)
	{
		RowData row = this.getRow(aRow);
		return row.getOriginalValue(aColumn);
	}

	public Object getValue(int aRow, int aColumn)
		throws IndexOutOfBoundsException
	{
		RowData row = this.getRow(aRow);
		return row.getValue(aColumn);
	}

	/**
	 * Returns the value of the given row/column as a String.
	 * The value's toString() method is used to convert the value to a String value.
	 * @return Null if the column is null, or the column's value as a String
	 */
	public String getValueAsString(int aRow, int aColumn)
		throws IndexOutOfBoundsException
	{
		RowData row = this.getRow(aRow);
		Object value = row.getValue(aColumn);
    if (value == null || value instanceof NullValue)
		{
      return null;
		}
    else
		{
			if (value instanceof Clob)
			{
				try
				{
					Clob lob = (Clob)value;
					long len = lob.length();
					return lob.getSubString(1, (int)len);
				}
				catch (Exception e)
				{
					return null;
				}
			}
			else
			{
				return value.toString();
			}
		}
	}


	/**
	 * Return the value of a column as an int value.
	 * If the object stored in the DataStore is an instance of Number
	 * the intValue() of that object will be returned, otherwise the String value
	 * of the column will be converted to an integer.
	 * If it cannot be converted to an int, the default value will be returned
	 * @param aRow The row
	 * @param aColumn The column to be returned
	 * @param aDefault The default value that will be returned if the the column's value cannot be converted to an int
	 */
	public int getValueAsInt(int aRow, int aColumn, int aDefault)
	{
		RowData row = this.getRow(aRow);
		Object value = row.getValue(aColumn);
    if (value == null || value instanceof NullValue)
		{
      return aDefault;
		}
    else if (value instanceof Number)
		{
			return ((Number)value).intValue();
		}
		else
		{
			int result = aDefault;
			try { result = Integer.parseInt(value.toString()); } catch (Exception e) {}
			return result;
		}
	}

	/**
	 * Return the value of a column as an long value.
	 * If the object stored in the DataStore is an instance of Number
	 * the longValue() of that object will be returned, otherwise the String value
	 * of the column will be converted to a long.
	 * If it cannot be converted to an long, the default value will be returned
	 * @param aRow The row
	 * @param aColumn The column to be returned
	 * @param aDefault The default value that will be returned if the the column's value cannot be converted to a long
	 */
	public long getValueAsLong(int aRow, int aColumn, long aDefault)
	{
		RowData row = this.getRow(aRow);
		Object value = row.getValue(aColumn);
    if (value == null || value instanceof NullValue)
      return aDefault;
    else if (value instanceof Number)
		{
      //return value.toString();
			return ((Number)value).longValue();
		}
		else
		{
			long result = aDefault;
			try { result = Long.parseLong(value.toString()); } catch (Exception e) {}
			return result;
		}
	}

	/**
	 *	Set a value received from a user input. This 
	 *  will convert the given value to an object of the
	 *  correct class
	 */
	public void setInputValue(int row, int col, Object value)
		throws Exception
	{
		Object realValue = this.convertCellValue(value, col);
		this.setValue(row, col, realValue);
	}
	
	/**
	 * Set the value for the given column. This will change the internal state of the DataStore to modified.
	 * @param aRow
	 * @param aColumn
	 * @param aValue The value to be set
	 */
	public void setValue(int aRow, int aColumn, Object aValue)
		throws IndexOutOfBoundsException
	{
		// do not allow setting the value for columns
		// which do not have a name. Those columns cannot
		// be saved to the database (because most likely they
		// are computed columns like count(*) etc)
		if (this.resultInfo.getColumnName(aColumn) == null) return;

		// If an updatetable is defined, we only accept
		// values for columns in that table
		RowData row = this.getRow(aRow);
		if (aValue == null)
			row.setNull(aColumn, this.resultInfo.getColumnType(aColumn));
		else
			row.setValue(aColumn,aValue);
		this.modified = row.isModified();
	}

	/**
	 * Set the given column to null. This is the same as calling setValue(aRow, aColumn, null).
	 * @param aRow
	 * @param aColumn
	 * @see #setValue(int, int, Object)
	 */
	public void setNull(int aRow, int aColumn)
	{
		NullValue nul = NullValue.getInstance(this.resultInfo.getColumnType(aColumn));
		this.setValue(aRow, aColumn, nul);
	}

	/**
	 * Returns the index of the column with the given name.
	 * @param aName The column's name to search for
	 * @return The column's index (first column starts at 0)
	 */
	public int getColumnIndex(String aName)
		throws SQLException
	{
		return this.findColumn(aName);
	}

	/**
	 * Returns true if the given row has been modified.
	 * A new row is considered modified only if setValue() has been called at least once.
	 * @param aRow The row to check
	 */
	public boolean isRowModified(int aRow)
	{
		RowData row = this.getRow(aRow);
		return (row.isNew() && row.isModified() || row.isModified());
	}

	/**
	 * Restore the original values as retrieved from the database.
	 * This will have no effect if {@link #isModified()} returns <code>false</code>
	 * @see #setValue(int, int, Object)
	 */
	public void restoreOriginalValues()
	{
		RowData row;
		if (this.deletedRows != null)
		{
			for (int i=0; i < this.deletedRows.size(); i++)
			{
				row = this.deletedRows.get(i);
				this.data.add(row);
			}
			this.deletedRows = null;
		}
		for (int i=0; i < this.data.size(); i++)
		{
			row = this.getRow(i);
			row.restoreOriginalValues();
		}
		this.resetStatus();
	}

	public boolean isRowNew(int aRow)
		throws IndexOutOfBoundsException
	{
		RowData row = this.getRow(aRow);
		return row.isNew();
	}

	public StringBuffer getRowDataAsString(int aRow)
	{
		String delimit = Settings.getInstance().getDefaultTextDelimiter();
		return this.getRowDataAsString(aRow, delimit);
	}

	public StringBuffer getRowDataAsString(int aRow, String aDelimiter)
	{
		RowData row = this.getRow(aRow);
		DecimalFormat formatter = Settings.getInstance().getDefaultDecimalFormatter();
		return row.getDataAsString(aDelimiter, formatter);
	}

	public StringBuffer getRowDataAsString(int aRow, String aDelimiter, boolean[] columns)
	{
		RowData row = this.getRow(aRow);
		DecimalFormat formatter = Settings.getInstance().getDefaultDecimalFormatter();
		return row.getDataAsString(aDelimiter, formatter, columns);
	}

	/**
	 * Return a StringBuffer with the names of the columns of this DataStore
	 * @param aFieldDelimiter the delimiter to be used
	 * @return a string containing the names of all columns
	 */
	public StringBuffer getHeaderString(String aFieldDelimiter)
	{
		return getHeaderString(aFieldDelimiter, null);
	}

	/**
	 * Return a StringBuffer with the names of the columns present in the 
	 * given List.
	 * @param aFieldDelimiter the delimiter to be used
	 * @param columns the columns to include. If null, all columns are included
	 * @return a string containing the names of the selected columns
	 */
	public StringBuffer getHeaderString(String aFieldDelimiter, List columns)
	{
		int cols = this.resultInfo.getColumnCount();
		StringBuffer result = new StringBuffer(cols * 30);
		int numCols = 0;
		for (int i=0; i < cols; i++)
		{
			if (columns != null)
			{
				if (!columns.contains(this.resultInfo.getColumn(i))) continue;
			}
			String colName = this.getColumnName(i);
			if (numCols > 0) result.append(aFieldDelimiter);

			if (colName == null || colName.trim().length() == 0) colName = "Col" + i;
			result.append(colName);
			numCols ++;
		}
		return result;
	}

	public String getDataString(String aLineTerminator, boolean includeHeaders)
	{
		return this.getDataString(Settings.getInstance().getDefaultTextDelimiter(), aLineTerminator, includeHeaders, null);
	}

	public String getDataString(String aLineTerminator, boolean includeHeaders, List columns)
	{
		return this.getDataString(Settings.getInstance().getDefaultTextDelimiter(), aLineTerminator, includeHeaders, columns);
	}

	public String getDataString(String aFieldDelimiter, String aLineTerminator, boolean includeHeaders)
	{
		return this.getDataString(aFieldDelimiter, aLineTerminator, includeHeaders, null);
	}

	public String getDataString(String aFieldDelimiter, String aLineTerminator, boolean includeHeaders, List columns)
	{
		int count = this.getRowCount();
		StringWriter result = new StringWriter(count * 250);
		try
		{
			this.writeDataString(result, aFieldDelimiter, aLineTerminator, includeHeaders, columns);
		}
		catch (Exception e)
		{
			LogMgr.logError("DataStore.getDataString()", "Error when writing ASCII data to StringWriter", e);
			return "";
		}
		return result.toString();
	}

	public void writeDataString(Writer out, String aFieldDelimiter, String aLineTerminator, boolean includeHeaders)
		throws IOException
	{
		this.writeDataString(out, aFieldDelimiter, aLineTerminator, includeHeaders, (List)null);
	}
	/**
	 *	WriteDataString writes the contents of this datastore into the passed Writer.
	 *	This can be used to write the contents directly to disk without the need
	 *  to build a complete buffer in memory
	 */
	public void writeDataString(Writer out, String aFieldDelimiter, String aLineTerminator, boolean includeHeaders, List columns)
		throws IOException
	{
		int count = this.getRowCount();
		if (includeHeaders)
		{
			out.write(this.getHeaderString(aFieldDelimiter, columns).toString());
			out.write(aLineTerminator);
		}
		boolean[] includeColumns = columnListToBool(columns);
		for (int i=0; i < count; i++)
		{
			out.write(this.getRowDataAsString(i, aFieldDelimiter, includeColumns).toString());
			out.write(aLineTerminator);
		}
	}

	private boolean[] columnListToBool(List columns)
	{
		int colCount = this.getColumnCount();
		boolean[] includeColumns = new boolean[colCount];
		for (int i=0; i < colCount; i++)
		{
			if (columns != null)
			{
				includeColumns[i] = columns.contains(this.resultInfo.getColumn(i));
			}
			else
			{
				includeColumns[i] = true;
			}
		}
		return includeColumns;
	}
	
	public void writeDataString(Writer out, String aFieldDelimiter, String aLineTerminator, boolean includeHeaders, int[] rows)
		throws IOException
	{
		writeDataString(out, aFieldDelimiter, aLineTerminator, includeHeaders, rows, null);
	}

	/**
	 *	Write the contents of the datastore into the writer but only the rows
	 *  that have been passed in the rows[] parameter
	 */
	public void writeDataString(Writer out, String aFieldDelimiter, String aLineTerminator, boolean includeHeaders, int[] rows, List columns)
		throws IOException
	{
		if (includeHeaders)
		{
			out.write(this.getHeaderString(aFieldDelimiter, columns).toString());
			out.write(aLineTerminator);
		}
		int count = rows.length;
		for (int i=0; i < count; i++)
		{
			out.write(this.getRowDataAsString(rows[i], aFieldDelimiter, columnListToBool(columns)).toString());
			out.write(aLineTerminator);
		}
	}

	/**
	 *	Remove all data from the DataStore
	 */
	public void reset()
	{
		this.data.reset();
		if (this.deletedRows != null)
		{
			this.deletedRows.reset();
			this.deletedRows = null;
		}
		this.modified = false;
	}

	public boolean hasUpdateableColumns()
	{
		return this.resultInfo.hasUpdateableColumns();
	}

	/**
	 *	Returns true if at least one row has been updated.
	 */
	public boolean hasUpdatedRows()
	{
		if (!this.isModified()) return false;
		int count = this.getRowCount();
		for (int i=0; i < count; i++)
		{
			RowData row = this.getRow(i);
			if (row.isModified() && !row.isNew()) return true;
		}
		return false;
	}

	/**
	 *	Returns true if at least one row has been deleted
	 */
	public boolean hasDeletedRows()
	{
		return (this.deletedRows != null && this.deletedRows.size() > 0);
	}

	/**
	 * Returns true if key columns are needed to save the changes
	 * to the database. If only inserted rows are present, then no 
	 * key is needed. For updated or deleted rows a key is needed
	 */
	public boolean needPkForUpdate()
	{
		if (!this.isModified()) return false;
		return (this.hasDeletedRows() || this.hasUpdatedRows());
	}

	public boolean isModified() { return this.modified;  }

	public boolean isUpdateable()
	{
		if (this.allowUpdates) return true;
		return (this.updateTable != null && this.hasUpdateableColumns());
	}

	private int findColumn(String name)
	{
		return this.resultInfo.findColumn(name);
	}

	public RowData getRow(int aRow)
		throws IndexOutOfBoundsException
	{
		return this.data.get(aRow);
	}

	private void initMetaData(ResultSetMetaData metaData)
		throws SQLException
	{
		this.resultInfo = new ResultInfo(metaData, this.originalConnection);
	}

	public void initData(ResultSet aResultSet)
		throws SQLException
	{
		this.initData(aResultSet, -1);
	}

	/**
	 * Read the column definitions from the result set's meta data 
	 * and store the data from the ResultSet in this DataStore (up to maxRows)
	 *
	 * @param aResultSet the ResultSet to read
	 * @param maxRows max. number of rows to read. Zero or lower to read all rows
	 */
	public void initData(ResultSet aResultSet, int maxRows)
		throws SQLException
	{
		try
		{
			ResultSetMetaData metaData = aResultSet.getMetaData();
			this.initMetaData(metaData);
		}
		catch (SQLException e)
		{
			LogMgr.logError("DataStore.initData()", "Error while retrieving ResultSetMetaData", e);
			throw e;
		}

		if (this.rowActionMonitor != null)
		{
			this.rowActionMonitor.setMonitorType(RowActionMonitor.MONITOR_LOAD);
		}
		this.cancelRetrieve = false;

		try
		{
			int rowCount = 0;
			int cols = this.resultInfo.getColumnCount();
			this.data = createData(500);
			while (!this.cancelRetrieve && aResultSet.next())
			{
				rowCount ++;

				if (this.rowActionMonitor != null && rowCount % reportInterval == 0)
				{
					this.rowActionMonitor.setCurrentRow(rowCount, -1);
				}

				RowData row = new RowData(cols);
				row.read(aResultSet, this.resultInfo);
				this.data.add(row);
				if (this.cancelRetrieve) break;
				if (maxRows > 0 && rowCount > maxRows) break;
			}
		}
		catch (SQLException e)
		{
			if (this.cancelRetrieve)
			{
				// some JDBC drivers will throw an exception when cancel() is called
				// as we silently want to use the data that has been retrieved so far
				// the Exception should not be passed to the caller
				LogMgr.logInfo("DataStore.initData()", "Retrieve cancelled");
			}
			else
			{
				throw e;
			}
		}
		catch (Exception e)
		{
			throw new SQLException(e.getMessage());
		}
		finally
		{
			this.modified = false;
			this.cancelRetrieve = false;
		}
	}

	/**	
	 * Define the (SELECT) statement that was used to produce this 
	 * DataStore's result set. This is used to find the update table later
	 */
	public void setOriginalStatement(String aSql)
	{
		this.sql = aSql;
	}

	public boolean checkUpdateTable(WbConnection aConn)
	{
		if (this.sql == null) return false;
		return this.checkUpdateTable(this.sql, aConn);
	}

	public boolean checkUpdateTable()
	{
		if (this.sql == null) return false;
		if (this.originalConnection == null) return false;
		return this.checkUpdateTable(this.sql, this.originalConnection);
	}

	public boolean checkUpdateTable(String aSql, WbConnection aConn)
	{
		if (aSql == null) return false;
		List tables = SqlUtil.getTables(aSql);
		if (tables.size() != 1) return false;
		String table = (String)tables.get(0);
		this.setUpdateTable(table, aConn);
		return true;
	}

	/**
	 *	Return the table that should be used for INSERTs
	 *	Normally this is the update table. If no update table
	 *	is defined, the table from the SQL statement will be used
	 *	but no checking for key columns takes place (which might take long)
	 */
	private String getInsertTable()
	{
		if (this.updateTable != null) return this.updateTable;
		if (this.sql == null) return null;
		if (!this.sqlHasUpdateTable()) return null;
		List tables = SqlUtil.getTables(this.sql);
		if (tables.size() != 1) return null;
		String table = (String)tables.get(0);
		return table;
	}

//	/**
//	 * Write the contents of this DataStore as XML into the  
//	 * supplied Writer
//	 * @param pw Thw writer to which the XML is written.
//	 * @see #writeXmlData(Writer, boolean)
//	 */
//	public void writeXmlData(Writer pw)
//		throws IOException
//	{
//		this.writeXmlData(pw, false);
//	}
//
//	/**
//	 * Write the contents of this DataStore as XML into the  
//	 * supplied Writer
//	 *
//	 * @param pw The writer to which the XML is written.
//	 * @param useCdata if true all character data is put into a CDATA sectioni
//	 * @see workbench.db.exporter.XmlRowDataConverter
//	 */
//	public void writeXmlData(Writer pw, boolean useCdata)
//		throws IOException
//	{
//		int count = this.getRowCount();
//		if (count == 0) return;
//
//		XmlRowDataConverter converter = new XmlRowDataConverter(this.resultInfo);
//		converter.setUseCDATA(useCdata);
//		this.writeConverterData(converter, pw);
//	}
//
//	/**
//	 * Write the contents of this DataStore as HTML into the  
//	 * supplied Writer
//	 *
//	 * @param pw The writer to which the HTML is written.
//	 * @see workbench.db.exporter.HtmlRowDataConverter
//	 */
//	public void writeHtmlData(Writer pw)
//		throws IOException
//	{
//		HtmlRowDataConverter converter = new HtmlRowDataConverter(this.resultInfo);
//		converter.setEscapeHtml(this.escapeHtml);
//		converter.setCreateFullPage(true);
//		String sql = SqlUtil.makeCleanSql(this.sql, false, false, '\'');
//		if (sql.length() > 60) sql = sql.substring(0, 60);
//		converter.setPageTitle(sql);
//		this.writeConverterData(converter, pw);
//	}

	/**
	 * Writes all rows using the supplied {@link workbench.db.exporter.RowDataConverter}
	 * to the supplied Writer.
	 */
	private void writeConverterData(RowDataConverter converter, Writer pw)
		throws IOException
	{
		int count = this.getRowCount();
		if (count == 0) return;
		converter.setDefaultTimestampFormat(this.converter.getTimestampPattern());
		converter.setDefaultNumberFormatter(this.defaultNumberFormatter);
		converter.setDefaultDateFormat(this.converter.getDatePattern());
		converter.getStart().writeTo(pw);
		for (int row=0; row < count; row++)
		{
			RowData data = this.getRow(row);
			StrBuffer rowData = converter.convertRowData(data, row);
			rowData.writeTo(pw);
		}
		converter.getEnd(count).writeTo(pw);
	}

	public void setEscapeExportValues(boolean aFlag)
	{
		this.escapeHtml = aFlag;
	}
	/**
	 *	Returns true if the current data can be converted to SQL INSERT statements.
	 *	The data can be saved as SQL INSERTs if an update table is defined.
	 *	If no update table is defined, then this method will call {@link #checkUpdateTable()}
	 *  and try to determine the table from the used SQL statement.
	 *
	 *  @return true if an update table is defined
	 */
	public boolean canSaveAsSqlInsert()
	{
		return (this.getInsertTable() != null);
	}

	/**
	 * Checks if the underlying SQL statement references only one table.
	 * @return true if only one table is found in the SELECT statement
	 */
	public boolean sqlHasUpdateTable()
	{
		if (this.updateTable != null) return true;
		if (this.sql == null) return false;
		List tables = SqlUtil.getTables(this.sql);
		if (tables.size() != 1) return false;
		return true;
	}

	public String getDataAsSqlDeleteInsert(int rows[], List columns)
		throws Exception, SQLException
	{
		return this.getDataAsSqlInsert("\n", null, null, rows, columns, true);
	}
	
	public String getDataAsSqlInsert(int[] rows, List columns)
		throws Exception, SQLException
	{
		return this.getDataAsSqlInsert("\n", null, null, rows, columns);
	}

	public String getDataAsSqlInsert(String aLineTerminator, String aCharFunc, String aConcatString, int[] rows, List columns)
		throws Exception, SQLException
	{
		return getDataAsSqlInsert(aLineTerminator, aCharFunc, aConcatString, rows, columns, false);
	}

	public String getDataAsSqlInsert(String aLineTerminator, String aCharFunc, String aConcatString, int[] rows, List columns, boolean includeDelete)
		throws Exception, SQLException
	{
		if (!this.canSaveAsSqlInsert()) return "";

		StringWriter script = new StringWriter(this.getRowCount() * 150);
		try
		{
			this.writeDataAsSqlInsert(script, aLineTerminator, aCharFunc, aConcatString, rows, columns, includeDelete);
		}
		catch (Exception e)
		{
			LogMgr.logError("DataStore.getDataAsSqlInsert()", "Error writing script to StringWriter", e);
			return "";
		}
		return script.toString();
	}

	public void writeDataAsSqlInsert(Writer out, String aLineTerminator, String aCharFunc, String aConcatString, int[] rows, List columns, boolean includeDelete)
		throws IOException
	{
		if (!this.canSaveAsSqlInsert()) return;
		int count;

		if (rows == null) count = this.getRowCount();
		else count = rows.length;

		if (includeDelete && !this.hasPkColumns())
		{
			includeDelete = false;
		}
		SqlRowDataConverter converter = new SqlRowDataConverter(this.resultInfo);
		converter.setIncludeTableOwner(Settings.getInstance().getIncludeOwnerInSqlExport());
		converter.setOriginalConnection(this.originalConnection);
		converter.setLineTerminator(aLineTerminator);
		converter.setColumnsToExport(columns);
		if (aCharFunc != null)
		{
			converter.setChrFunction(aCharFunc);
			converter.setConcatString(aConcatString);
		}
		if (includeDelete)
		{
			converter.setCreateInsertDelete();
		}
		else
		{
			converter.setCreateInsert();
		}
		TableIdentifier tbl = new TableIdentifier(this.getInsertTable());
		converter.setAlternateUpdateTable(tbl);

		for (int row = 0; row < count; row ++)
		{
			RowData data;
			int rowIndex = -1;
			if (rows == null) rowIndex = row;
			else rowIndex = rows[row];

			data = this.getRow(rowIndex);
			StrBuffer sql = converter.convertRowData(data, rowIndex);
			sql.writeTo(out);
		}
	}

	// =========== SQL Update generation ================
	public String getDataAsSqlUpdate(int[] rows, List columns)
	{
		return this.getDataAsSqlUpdate("\n", null, null, rows, columns);
	}

	public String getDataAsSqlUpdate(String aLineTerminator, String aCharFunc, String aConcatString, int[] rows, List columns)
	{
		if (!this.canSaveAsSqlInsert()) return "";
		StringWriter script = new StringWriter(this.getRowCount() * 150);
		try
		{
			this.writeDataAsSqlUpdate(script, aLineTerminator, aCharFunc, aConcatString, rows, columns);
		}
		catch (Exception e)
		{
			LogMgr.logError("DataStore.getDataAsSqlInsert()", "Error writing script to StringWriter", e);
			return "";
		}
		return script.toString();
	}

	public void writeDataAsSqlUpdate(Writer out, String aLineTerminator, String aCharFunc, String aConcatString, int[] rows, List columns)
		throws IOException
	{
		if (!this.pkColumnsRead)
		{
			try
			{
				this.updatePkInformation(this.originalConnection);
			}
			catch (SQLException e)
			{
				LogMgr.logError("DataStore.writeDataAsSqlUpdate()", "Could not retrieve PK columns",e);
				return;
			}
		}
		if (!this.resultInfo.hasPkColumns())
		{
			LogMgr.logError("DataStore.writeDataAsSqlUpdate()", "No PK columns found. Cannot write as SQL Update", null);
			return;
		}

		int count = 0;
		if (rows != null) count = rows.length;
		else count = this.getRowCount();

		StatementFactory factory = new StatementFactory(this.resultInfo);
		factory.setIncludeTableOwner(Settings.getInstance().getIncludeOwnerInSqlExport());
		factory.setCurrentConnection(this.originalConnection);
		for (int row = 0; row < count; row ++)
		{
			RowData data;
			if (rows == null) data = this.getRow(row);
			else data = this.getRow(rows[row]);

			DmlStatement stmt = factory.createUpdateStatement(data, true, aLineTerminator, columns);
			if (aCharFunc != null)
			{
				stmt.setChrFunction(aCharFunc);
				stmt.setConcatString(aConcatString);
			}
			String sql = stmt.getExecutableStatement(this.originalConnection.getSqlConnection());
			out.write(sql);
			out.write(";");
			out.write(aLineTerminator);
			out.write(aLineTerminator);
		}
	}

	/**
	 * Set all values in the given row to NULL
	 */
	public void setRowNull(int aRow)
	{
		for (int i=0; i < this.resultInfo.getColumnCount(); i++)
		{
			this.setNull(aRow, i);
		}
	}

	public void cancelUpdate()
	{
		this.cancelUpdate = true;
	}

	/**
	 * 	Cancel a running import
	 */
	public void cancelImport()
	{
		this.cancelImport = true;
	}

	public void cancelRetrieve()
	{
		this.cancelRetrieve = true;
	}

	/**
	 *	Import a text file into this DataStore.
	 * @param aFilename - The text file to import
	 * @param hasHeader - wether the text file has a header row
	 * @param aColSeparator - the separator for column data
	 */
	public void importData(String aFilename
	                     , boolean hasHeader
											 , String aColSeparator
											 , String aQuoteChar
											 , JobErrorHandler errorHandler)
		throws FileNotFoundException
	{
		File f = new File(aFilename);
		long fileSize = f.length();
		BufferedReader in = new BufferedReader(new FileReader(aFilename),1024*512);
		String line;
		Object colData;
		int row;
		this.cancelImport = false;

		if ("\\t".equals(aColSeparator))
    {
      aColSeparator = "\t";
    }

		try
		{
			line = in.readLine();
			if (hasHeader) line = in.readLine();
		}
		catch (IOException e)
		{
			line = null;
		}

		if (this.rowActionMonitor != null)
		{
			this.rowActionMonitor.setMonitorType(RowActionMonitor.MONITOR_INSERT);
		}

		// if the data store is empty, we trie to initialize the
		// data array to an approx. size. As we don't know how many lines
		// we really have in the file, we take the length of the first line
		// as the average, and calculate the expected number of lines from
		// this length.
		// Even if we don't get the number of lines correct, this method should be better
		// then not initializing the array at all.
		if (line != null && this.data.size() == 0)
		{
			int initialSize = (int)(fileSize / line.length());
			this.data = createData(initialSize);
		}

		CsvLineParser tok = new CsvLineParser(aColSeparator.charAt(0), '"');
		int importRow = 0;
		boolean ignoreError = false;

		while (line != null)
		{
			tok.setLine(line);

			row = this.addRow();
			importRow ++;

			this.updateProgressMonitor(importRow, -1);

			this.setRowNull(row);

			for (int col=0; col < this.resultInfo.getColumnCount(); col++)
			{
				if (col > -1)
				{
					String value = null;
					try
					{

						if (tok.hasNext()) value = tok.getNext();

						if (value == null || value.length() == 0)
						{
							this.setNull(row, col);
						}
						else
						{
							colData = this.convertCellValue(value, col);
							this.setValue(row, col, colData);
						}
					}
					catch (Exception e)
					{
						LogMgr.logWarning("DataStore.importData()","Error reading line #" + row + ",col #" + col + ",colValue=" + value, e);
						if (errorHandler != null && !ignoreError)
						{
							String colname = this.getColumnName(col);
							int choice = errorHandler.getActionOnError(row, colname, (value == null ? null : value.toString()), "");
							if (choice == JobErrorHandler.JOB_ABORT)
							{
								this.cancelImport = true;
								break;
							}
							else if (choice == JobErrorHandler.JOB_IGNORE_ALL)
							{
								ignoreError = true;
							}
						}
					}
				}
			}

			Thread.yield();
			if (this.cancelImport) break;

			try
			{
				line = in.readLine();
			}
			catch (IOException e)
			{
				line = null;
			}
		}

		try { in.close(); } catch (IOException e) {}
	}

	private void updateProgressMonitor(int currentRow, int totalRows)
	{
		if (this.rowActionMonitor != null)
		{
			this.rowActionMonitor.setCurrentRow(currentRow, totalRows);
		}
	}

	/**
	 * Returns a List of {@link workbench.storage.DmlStatement}s which
	 * would be executed in order to store the current content
	 * of the DataStore.
	 */
	public List getUpdateStatements(WbConnection aConnection)
		throws SQLException
	{
		if (this.updateTable == null) throw new NullPointerException("No update table defined!");
		this.updatePkInformation(aConnection);

		ArrayList stmt = new ArrayList(this.getModifiedCount());
		this.resetUpdateRowCounters();
		DmlStatement dml = null;
		RowData row = null;

		StatementFactory factory = new StatementFactory(this.resultInfo);
		factory.setIncludeTableOwner(aConnection.getMetadata().needSchemaInDML(resultInfo.getUpdateTable()));

		row = this.getNextDeletedRow();
		while (row != null)
		{
			dml = factory.createDeleteStatement(row);
			stmt.add(dml);
			row = this.getNextDeletedRow();
		}

		row = this.getNextChangedRow();
		while (row != null)
		{
			dml = factory.createUpdateStatement(row);
			stmt.add(dml);
			row = this.getNextChangedRow();
		}

		row = this.getNextInsertedRow();
		while (row != null)
		{
			dml = factory.createInsertStatement(row, false, "\n");
			stmt.add(dml);
			row = this.getNextInsertedRow();
		}
		this.resetUpdateRowCounters();
		return stmt;
	}

	public synchronized int updateDb(WbConnection aConnection)
		throws SQLException
	{
		return this.updateDb(aConnection, null);
	}

	private boolean ignoreAllUpdateErrors = false;

	private int executeGuarded(WbConnection aConnection, DmlStatement dml, JobErrorHandler errorHandler, int row)
		throws SQLException
	{
		int rowsUpdated = 0;
		try
		{
			rowsUpdated = dml.execute(aConnection);
		}
		catch (SQLException e)
		{
			if (!this.ignoreAllUpdateErrors)
			{
				boolean abort = true;
				int choice = JobErrorHandler.JOB_ABORT;
				if (errorHandler != null)
				{
					choice = errorHandler.getActionOnError(row, null, dml.getExecutableStatement(), e.getMessage());
				}
				if (choice == JobErrorHandler.JOB_CONTINUE)
				{
					abort = false;
				}
				else if (choice == JobErrorHandler.JOB_IGNORE_ALL)
				{
					abort = false;
					this.ignoreAllUpdateErrors = true;
				}
				if (abort) throw e;
			}
			else
			{
				LogMgr.logError("DataStore.executeGuarded()", "Error executing statement " + dml.getExecutableStatement() + " for row = " + row + ", error: " + e.getMessage(), null);
			}
		}
		return rowsUpdated;
	}

	/**
	 * Save the changes to this DataStore to the database.
	 * The changes are applied in the following order
	 * <ul>
	 * <li>Delete statements</li>
	 * <li>Insert statements</li>
	 * <li>Update statements</li>
	 * </ul>
	 */
	public synchronized int updateDb(WbConnection aConnection, JobErrorHandler errorHandler)
		throws SQLException
	{
		int rows = 0;
		RowData row = null;
		this.cancelUpdate = false;
		this.updatePkInformation(aConnection);
		int totalRows = this.getModifiedCount();
		int currentRow = 0;
		if (this.rowActionMonitor != null)
		{
			this.rowActionMonitor.setMonitorType(RowActionMonitor.MONITOR_UPDATE);
		}

		this.ignoreAllUpdateErrors = false;

		StatementFactory factory = new StatementFactory(this.resultInfo);
		factory.setIncludeTableOwner(aConnection.getMetadata().needSchemaInDML(resultInfo.getUpdateTable()));
		
		try
		{
			this.resetUpdateRowCounters();
			row = this.getNextDeletedRow();
			DmlStatement dml = null;
			while (row != null)
			{
				currentRow ++;
				this.updateProgressMonitor(currentRow, totalRows);
				if (!row.isDmlSent())
				{
					dml = factory.createDeleteStatement(row);
					rows += this.executeGuarded(aConnection, dml, errorHandler, -1);
					row.setDmlSent(true);
				}
				Thread.yield();
				if (this.cancelUpdate) return rows;
				row = this.getNextDeletedRow();
			}

			row = this.getNextChangedRow();
			while (row != null)
			{
				currentRow ++;
				this.updateProgressMonitor(currentRow, totalRows);
				if (!row.isDmlSent())
				{
					dml = factory.createUpdateStatement(row, false, "\r\n");
					rows += this.executeGuarded(aConnection, dml, errorHandler, currentUpdateRow);
					row.setDmlSent(true);
				}
				Thread.yield();
				if (this.cancelUpdate) return rows;
				row = this.getNextChangedRow();
			}

			row = this.getNextInsertedRow();
			while (row != null)
			{
				currentRow ++;
				this.updateProgressMonitor(currentRow, totalRows);
				if (!row.isDmlSent())
				{
					dml = factory.createInsertStatement(row, false);
					rows += this.executeGuarded(aConnection, dml, errorHandler, currentInsertRow);
					row.setDmlSent(true);
				}
				Thread.yield();
				if (this.cancelUpdate) return rows;
				row = this.getNextInsertedRow();
			}

			if (!aConnection.getAutoCommit() && rows > 0) aConnection.commit();
			this.resetStatus();
		}
		catch (SQLException e)
		{
			if (!aConnection.getAutoCommit())
			{
				aConnection.rollback();
			}
			LogMgr.logError("DataStore.updateDb()", "Error when saving data for row=" + currentRow + ", error: " + e.getMessage(), null);
			throw e;
		}

		return rows;
	}

	/**
	 * Clears the flag for all modified rows that the update
	 * has already been sent to the database
	 */
	public void resetDmlSentStatus()
	{
		int rows = this.getRowCount();
		for (int i=0; i < rows; i++)
		{
			RowData row = this.getRow(i);
			row.setDmlSent(false);
		}
		if (this.deletedRows != null)
		{
			rows = this.deletedRows.size();
			for (int i=0; i < rows; i++)
			{
				RowData row = this.deletedRows.get(i);
			row.setDmlSent(false);
			}
		}
	}

	/**
	 * Clears the modified status for those rows where
	 * the update has been sent to the database
	 */
	public void resetStatusForSentRows()
	{
		int rows = this.getRowCount();
		for (int i=0; i < rows; i++)
		{
			RowData row = this.getRow(i);
			if (row.isDmlSent())
			{
				row.resetStatus();
			}
		}
		if (this.deletedRows != null)
		{
			RowDataList newDeleted = createData(this.deletedRows.size());
			rows = this.deletedRows.size();
			for (int i=0; i < rows; i++)
			{
				RowData row = this.deletedRows.get(i);
				if (!row.isDmlSent())
				{
					newDeleted.add(row);
				}
			}
			this.deletedRows.reset();
			this.deletedRows = newDeleted;
		}
	}

	/**
	 * Reset all rows to not modified. After this a call 
	 * to #isModified() will return false.
	 */
	public void resetStatus()
	{
		this.deletedRows = null;
		this.modified = false;
		for (int i=0; i < this.data.size(); i++)
		{
			RowData row = this.getRow(i);
			row.resetStatus();
		}
		this.resetUpdateRowCounters();
	}

	public void sortByColumn(int col, boolean ascending)
	{
		synchronized (this.data)
		{
			RowDataListSorter sorter = new RowDataListSorter(col, ascending);
			sorter.sort(this.data);
		}
	}

	public String getDefaultDateFormat()
	{
		return this.converter.getDatePattern();
	}

	public void setDefaultTimestampFormat(String aFormat)
	{
		if (aFormat == null) return;
		this.converter.setDefaultTimestampFormat(aFormat);
	}
	
	public void setDefaultDateFormat(String aFormat)
	{
		if (aFormat == null) return;
		this.converter.setDefaultDateFormat(aFormat);
	}

	public String getDefaultNumberFormat()
	{
		if (this.defaultNumberFormatter == null) return null;
		return defaultNumberFormatter.toPattern();
	}

	public void setDefaultNumberFormat(String aFormat)
	{
		if (aFormat == null) return;
		try
		{
			this.defaultNumberFormatter = new DecimalFormat(aFormat);
			DecimalFormatSymbols symb = this.defaultNumberFormatter.getDecimalFormatSymbols();
			this.converter.setDecimalCharacter(symb.getDecimalSeparator());
		}
		catch (Exception e)
		{
			this.defaultNumberFormatter = null;
			LogMgr.logWarning("DataStore.setDefaultDateFormat()", "Could not create decimal formatter for format " + aFormat);
		}
	}

	/**
	 * Convert the value to the approriate class instance 
	 * for the given column
	 *
	 * @param aValue the value as entered by the user
	 * @param aColumn the column for which this value should be converted
	 * @return an Object of the needed class for the column
	 * @see ValueConverter#convertValue(Object, int)
	 */
	public Object convertCellValue(Object aValue, int aColumn)
		throws Exception
	{
		int type = this.getColumnType(aColumn);
		if (aValue == null)
		{
			return NullValue.getInstance(type);
		}

		return converter.convertValue(aValue, type);
	}
	
	/**
	 * Return the status object for the given row.
	 * The status is one of
	 * <ul>
	 * <li>{@link #ROW_ORIGINAL}</li>
	 * <li>{@link #ROW_MODIFIED}</li>
	 * <li>{@link #ROW_NEW}</li>
	 * </ul>
	 * The status object is used by the {@link workbench.gui.renderer.RowStatusRenderer} 
	 * in the result table to display the approriate icon.
	 * @param aRow the row for which the status should be returned
	 * @return an Integer identifying the status
	 */
	public Integer getRowStatus(int aRow)
		throws IndexOutOfBoundsException
	{
		RowData row = this.getRow(aRow);
		if (row.isOriginal())
		{
			return ROW_ORIGINAL;
		}
		else if (row.isNew())
		{
			return ROW_NEW;
		}
		else if (row.isModified())
		{
			return ROW_MODIFIED;
		}
		else
		{
			return ROW_ORIGINAL;
		}
	}

	public Map getPkValues(int aRow)
	{
		return this.getPkValues(this.originalConnection, aRow);
	}

	public Map getPkValues(WbConnection aConnection, int aRow)
	{
		if (aConnection == null) return Collections.EMPTY_MAP;
		try
		{
			this.updatePkInformation(this.originalConnection);
		}
		catch (SQLException e)
		{
			return Collections.EMPTY_MAP;
		}

		if (!this.resultInfo.hasPkColumns()) return Collections.EMPTY_MAP;

		RowData data = this.getRow(aRow);
		if (data == null) return Collections.EMPTY_MAP;

		int count = this.resultInfo.getColumnCount();
		HashMap result = new HashMap(count);
		for (int j=0; j < count ; j++)
		{
			if (this.resultInfo.isPkColumn(j))
			{
				String name = this.getColumnName(j);
				Object value = data.getValue(j);
				if (value instanceof NullValue)
				{
					result.put(name, null);
				}
				else
				{
					result.put(name, value);
				}
			}
		}
		return result;
	}

	private int currentUpdateRow = 0;
	private int currentInsertRow = 0;
	private int currentDeleteRow = 0;

	protected void resetUpdateRowCounters()
	{
		currentUpdateRow = 0;
		currentInsertRow = 0;
		currentDeleteRow = 0;
	}

	private RowData getNextChangedRow()
	{
		if (this.currentUpdateRow >= this.getRowCount()) return null;
		RowData row = null;

		int count = this.getRowCount();

		while (this.currentUpdateRow < count)
		{
			row = this.getRow(this.currentUpdateRow);
			this.currentUpdateRow ++;

			if (row.isModified() && !row.isNew()) return row;
		}
		return null;
	}

	protected RowData getNextDeletedRow()
	{
		if (this.deletedRows == null || this.deletedRows.size() == 0) return null;
		int count = this.deletedRows.size();

		if (this.currentDeleteRow > count) return null;

		RowData row = null;

		while (this.currentDeleteRow < count)
		{
			row = this.deletedRows.get(this.currentDeleteRow);
			this.currentDeleteRow ++;
			return row;
		}
		return null;
	}

	private RowData getNextInsertedRow()
	{
		int count = this.getRowCount();
		if (this.currentInsertRow >= count) return null;

		RowData row = null;

		while (this.currentInsertRow < count)
		{
			row = this.getRow(this.currentInsertRow);
			this.currentInsertRow ++;
			if (row.isNew() && row.isModified())
			{
				return row;
			}
		}
		return null;
	}

	public String getUpdateTableSchema()
	{
		return this.getUpdateTableSchema(this.originalConnection);
	}

	public String getUpdateTableSchema(WbConnection aConnection)
	{
		if (this.updateTable == null) return null;
		int pos = this.updateTable.indexOf(".");
		String schema = null;
		if (pos > -1)
		{
			schema = this.updateTable.substring(0, pos);
		}

		if (schema == null || schema.trim().length() == 0)
		{
			try
			{
				schema = aConnection.getMetadata().findSchemaForTable(this.updateTable);
			}
			catch (Exception e)
			{
				schema = null;
			}
		}
		return schema;
	}

	public void setPKColumns(ColumnIdentifier[] pkColumns)
	{
		this.resultInfo.setPKColumns(pkColumns);
	}

	public ResultInfo getResultInfo()
	{
		return this.resultInfo;
	}

	public ColumnIdentifier[] getColumns()
	{
		return this.resultInfo.getColumns();
	}

	public boolean hasPkColumns()
	{
		return this.resultInfo.hasPkColumns() && this.resultInfo.hasRealPkColumns();
	}

	private void updatePkInformation(WbConnection aConnection)
		throws SQLException
	{
		if (this.resultInfo.hasPkColumns()) return;
		if (this.updateTable == null)
		{
			this.checkUpdateTable();
		}
		this.resultInfo.readPkDefinition(aConnection);
	}

	/**
	 *	Check if the currently defined updateTable
	 *	has any Primary keys defined in the database.
	 *
	 *	If it has, a subsequent call to hasPkColumns() returns true
	 */
	public void checkDefinedPkColumns()
		throws SQLException
	{
		if (this.originalConnection == null) return;
		if (this.updateTable == null)
		{
			this.checkUpdateTable();
		}
		this.resultInfo.readPkDefinition(this.originalConnection, false);
	}

	private boolean isPkColumn(int col)
	{
		return this.resultInfo.isPkColumn(col);
	}

	/** Getter for property progressMonitor.
	 * @return Value of property progressMonitor.
	 *
	 */
	public RowActionMonitor getProgressMonitor()
	{
		return this.rowActionMonitor;
	}

	/** Setter for property progressMonitor.
	 * @param aMonitor New value of property progressMonitor.
	 *
	 */
	public void setProgressMonitor(RowActionMonitor aMonitor)
	{
		this.rowActionMonitor = aMonitor;
	}

}
