/*
 * DataStore.java
 *
 * Created on 15. September 2001, 11:29
 */

package workbench.storage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.Collator;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import workbench.WbManager;
import workbench.interfaces.JobErrorHandler;
import workbench.db.DbMetadata;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.util.CsvLineParser;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.ValueConverter;
import workbench.util.WbStringTokenizer;



/**
 * @author  workbench@kellerer.org
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
	private int colCount;
	private int realColumns;

	private ArrayList data;
	private ArrayList pkColumns;
	private ArrayList deletedRows;
	private String sql;

	// Cached ResultSetMetaData information
	private int[] columnTypes;
  private int[] columnSizes;
	private int[] columnPrecision;
	private int[] columnScale;
	private String[] columnNames;
	private String[] columnClassNames;
	private String[] columnTypeNames;

	private String updateTable;
	private String updateTableSchema;
	private ArrayList updateTableColumns;

	private WbConnection originalConnection;

	private SimpleDateFormat defaultDateFormatter;
	private DecimalFormat defaultNumberFormatter;
	private SimpleDateFormat defaultTimestampFormatter;

	private ColumnComparator comparator;

	private String defaultExportDelimiter = "\t";
	private boolean allowUpdates = false;

	private ValueConverter converter = new ValueConverter();

	private static final Collator defaultCollator;
	static
	{
		String lang = System.getProperty("org.kellerer.sort.language", System.getProperty("user.language"));
		String country = System.getProperty("org.kellerer.sort.country", System.getProperty("user.country"));
		Locale l = new Locale(lang, country);
		defaultCollator = Collator.getInstance(l);
	}

	public DataStore(String[] aColNames, int[] colTypes)
	{
		this(aColNames, colTypes, null);
	}
	/**
	 *	Create a DataStore which is not based on a result set
	 *	and contains the columns defined in the given array
	 *	The column types need to be SQL types from java.sql.Types
	 */
	public DataStore(String[] aColNames, int[] colTypes, int[] colSizes)
	{
		this.data = new ArrayList();
		this.colCount = aColNames.length;
		this.columnNames = new String[this.colCount];
		this.columnTypes = new int[this.colCount];
		for (int i=0; i < this.colCount; i++)
		{
			this.columnNames[i] = aColNames[i];
			this.columnTypes[i] = colTypes[i];
		}
		this.setColumnSizes(colSizes);
	}

	/**
	 *	Create a DataStore based on the contents of the given	ResultSet.
	 */
  public DataStore(final ResultSet aResultSet, WbConnection aConn) throws SQLException
  {
		if (aResultSet == null) return;
		this.originalConnection = aConn;
		this.initData(aResultSet);
  }

	public DataStore(ResultSet aResult) throws SQLException
	{
		this(aResult, false);
	}

	public DataStore(ResultSet aResult, boolean readData)
		throws SQLException
	{
		this(aResult, readData, null, -1);
	}

	/**
	 *	Create a DataStore based on the given ResultSet.
	 *	@param aResult the result set to use
	 *  @param readData if true the data from the ResultSet should be read into memory, otherwise only MetaData information is read
	 *  @param maxRows limit number of rows to maxRows if the JDBC driver does not already limit them
	 */
	public DataStore(ResultSet aResult, boolean readData, int maxRows) throws SQLException
	{
		this(aResult, readData, null, maxRows);
	}

	/**
	 *	Create a DataStore based on the given ResultSet.
	 *	@param aResult the result set to use
	 *  @param readData if true the data from the ResultSet should be read into memory, otherwise only MetaData information is read
	 *  @param RowActionMonitor if not null, the loading process is displayed through this monitor
	 */
	public DataStore(ResultSet aResult, boolean readData, RowActionMonitor aMonitor)
		throws SQLException
	{
		this(aResult, readData, aMonitor, -1);
	}


	/**
	 *	Create a DataStore based on the given ResultSet.
	 *	@param aResult the result set to use
	 *  @param readData if true the data from the ResultSet should be read into memory, otherwise only MetaData information is read
	 *  @param RowActionMonitor if not null, the loading process is displayed through this monitor
	 *  @param maxRows limit number of rows to maxRows if the JDBC driver does not already limit them
	 */
	public DataStore(ResultSet aResult, boolean readData, RowActionMonitor aMonitor, int maxRows) throws SQLException
	{
		this.rowActionMonitor = aMonitor;
		if (readData)
		{
			this.originalConnection = null;
			this.initData(aResult, maxRows);
		}
		else
		{
			ResultSetMetaData metaData = aResult.getMetaData();
			this.initMetaData(metaData);
			this.data = new ArrayList(100);
		}
	}


	/**
	 * Create an empty DataStore based on the information given in the MetaData
	 * object. The DataStore can be populated with the {@link #addRow(ResultSet)} method.
	 */
	public DataStore(ResultSetMetaData metaData, WbConnection aConn) throws SQLException
	{
		this.originalConnection = aConn;
		this.initMetaData(metaData);
		this.reset();
	}

	public void setSourceConnection(WbConnection aConn)
	{
		this.originalConnection = aConn;
	}

	public void setColumnSizes(int[] sizes)
	{
		if (sizes == null) return;
		if (sizes.length != this.colCount) return;
		this.columnSizes = new int[this.colCount];
		for (int i=0; i < this.colCount; i++)
		{
			this.columnSizes[i] = sizes[i];
		}
	}

	public void setAllowUpdates(boolean aFlag)
	{
		this.allowUpdates = aFlag;
	}

	public int[] getColumnTypes() { return this.columnTypes; }

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

	public DataStore createCopy(boolean withData)
	{
		DataStore ds = new DataStore(this.columnNames, this.columnTypes, this.columnSizes);
		ds.updateTable = this.updateTable;
		ds.sql = this.sql;
		ds.columnClassNames = this.columnClassNames;
		ds.pkColumns = this.pkColumns;
		ds.originalConnection = this.originalConnection;
		ds.resetStatus();
		ds.updateTableColumns = (ArrayList)this.updateTableColumns.clone();
		if (withData)
		{
			this.copyData(ds, 0, this.getRowCount());
		}
		return ds;
	}

	public void copyData(DataStore target, int beginRow, int endRow)
	{
		for (int i=beginRow; i <= endRow; i++)
		{
			RowData rowData = this.getRow(i).createCopy();
			int row = target.addRow();
			target.data.set(row, rowData);
		}
	}

	public void copyData(DataStore target, int[] rows)
	{
		for (int i=0; i < rows.length; i++)
		{
			int sourceRow = rows[i];
			int targetRow = target.addRow();
			RowData data = this.getRow(sourceRow).createCopy();
			target.data.set(targetRow, data);
		}
	}

	public List getColumnValues(int aColumn)
	{
		int rowCount = this.getRowCount();
		ArrayList result = new ArrayList(rowCount);
		for (int i=0; i < rowCount; i++)
		{
			result.add(this.getValue(i, aColumn));
		}
		return result;
	}

  public int getRowCount() { return this.data.size(); }
	public int getColumnCount() { return this.colCount; }

	public void setExportDelimiter(String aDelimit)
	{
		this.defaultExportDelimiter = aDelimit;
	}

	public String getExportDelimiter()
	{
		return this.defaultExportDelimiter;
	}

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
				RowData data = (RowData)this.deletedRows.get(i);
				if (!data.isNew()) modifiedCount++;
			}
		}
		return modifiedCount;
	}

	public int getColumnType(int aColumn)
		throws IndexOutOfBoundsException
	{
		return this.columnTypes[aColumn];
	}

	public String getColumnClassName(int aColumn)
	{
		if (this.columnClassNames[aColumn] != null) return this.columnClassNames[aColumn];
		return this.getColumnClass(aColumn).getName();
	}
	public Class getColumnClass(int aColumn)
	{

		int type = this.getColumnType(aColumn);
		switch (type)
		{
			case Types.BIGINT:
			case Types.INTEGER:
				return BigInteger.class;
			case Types.SMALLINT:
				return Integer.class;
			case Types.NUMERIC:
			case Types.DECIMAL:
				return BigDecimal.class;
			case Types.DOUBLE:
				return Double.class;
			case Types.REAL:
			case Types.FLOAT:
				return Float.class;
			case Types.CHAR:
			case Types.VARCHAR:
				return String.class;
			case Types.DATE:
				return java.sql.Date.class;
			case Types.TIMESTAMP:
				return Timestamp.class;
			default:
				return Object.class;
		}
	}

	/**
	 *	Removes the row from the DataStore without putting
	 *	it into the delete buffer. So no DELETE statement
	 *	will be generated for that row, when updating the
	 *	DataStore.
	 *	The internal modification state will not be modified.
	 */
	public void discardRow(int aRow)
		throws IndexOutOfBoundsException
	{
		this.data.remove(aRow);
	}

	/**
	 *	Deletes the given row and saves it in the delete buffer
	 *	in order to be able to generate a DELETE statement if
	 *	this DataStore needs updating
	 */
	public void deleteRow(int aRow)
		throws IndexOutOfBoundsException
	{
		Object row = this.data.get(aRow);
		if (this.deletedRows == null) this.deletedRows = new ArrayList();
		this.deletedRows.add(row);
		this.data.remove(aRow);
		this.modified = true;
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
		RowData row = new RowData(this.colCount);
		this.data.add(row);
		for (int i=0; i < this.colCount; i++)
		{
			Object value = data.getObject(i + 1);
			if (data.wasNull() || value == null)
			{
				row.setNull(i, this.columnTypes[i]);
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
		RowData row = new RowData(this.colCount);
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
		RowData row = new RowData(this.colCount);
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

	public void setUpdateTable(String aTablename)
	{
		this.setUpdateTable(aTablename, this.originalConnection);
	}

	public boolean useUpdateTableFromSql(String aSql)
	{
		return this.useUpdateTableFromSql(aSql, false);
	}

	public boolean useUpdateTableFromSql(String aSql, boolean retrievePk)
	{
		this.updateTable = null;
		this.updateTableColumns = null;

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
			return (this.pkColumns != null);
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
		this.updateTableColumns = new ArrayList();
		for (int i=0; i < this.columnNames.length; i++)
		{
			this.updateTableColumns.add(this.columnNames[i]);
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

	/**
	 * Sets the table to be updated for this DataStore.
	 * Upon setting the table, the column definition for the table
	 * will be retrieved using {@link workbench.db.DbMetadata}
	 * @param aTablename
	 * @param aConn
	 */
	public void setUpdateTable(String aTablename, WbConnection aConn)
	{
		if (aTablename == null)
		{
			this.updateTable = null;
			this.updateTableColumns = null;
			this.pkColumns = null;
		}
		else if (!aTablename.equalsIgnoreCase(this.updateTable) && aConn != null)
		{
			this.pkColumns = null;
			this.updateTable = null;
			this.updateTableColumns = null;
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

				DataStore columns = meta.getTableDefinition(aTablename);
				if (columns == null)
				{
					return;
				}
				this.updateTable = aTablename;
				this.updateTableColumns = new ArrayList();
				for (int i=0; i < columns.getRowCount(); i++)
				{
					String column = columns.getValueAsString(i, DbMetadata.COLUMN_IDX_TABLE_DEFINITION_COL_NAME);
					this.updateTableColumns.add(column.toLowerCase());
				}
			}
			catch (Exception e)
			{
				this.pkColumns = null;
				this.updateTable = null;
				this.updateTableColumns = null;
				LogMgr.logError("DataStore.setUpdateTable()", "Could not read table definition", e);
			}
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
		return this.columnNames[aColumn];
	}

	public int getColumnDisplaySize(int aColumn)
		throws IndexOutOfBoundsException
	{
		return this.columnSizes[aColumn];
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
      return null;
    else
      return value.toString();
	}

	/**
	 * Return the column's value as a formatted String.
	 * Especially for Date objects this is different then getValueAsString()
	 * as a default formatter can be defined.
	 * @param aRow The requested row
	 * @param aColumn The column in aRow for which the value should be formatted
	 * @return The formatted value as a String
	 * @see #setDefaultDateFormatter(SimpleDateFormat)
	 * @see #setDefaultTimestampFormatter(SimpleDateFormat)
	 * @see #setDefaultNumberFormatter(SimpleDateFormat)
	 * @see #setDefaultDateFormat(String)
	 * @see #setDefaultTimestampFormat(String)
	 * @see #setDefaultNumberFormat(String)
	 */
	public String getValueAsFormattedString(int aRow, int aColumn)
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
			String result = null;
			if (value instanceof java.sql.Timestamp && this.defaultTimestampFormatter != null)
			{
				result = this.defaultTimestampFormatter.format(value);
			}
			else if (value instanceof java.util.Date && this.defaultDateFormatter != null)
			{
				result = this.defaultDateFormatter.format(value);
			}
			else if (value instanceof Number && this.defaultNumberFormatter != null)
			{
				result = this.defaultNumberFormatter.format(value);
			}
			else
			{
				result = value.toString();
			}
      return result;
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
		if (this.columnNames[aColumn] == null) return;

		// If an updatetable is defined, we only accept
		// values for columns in that table
		RowData row = this.getRow(aRow);
		if (aValue == null)
			row.setNull(aColumn, this.columnTypes[aColumn]);
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
		NullValue nul = NullValue.getInstance(this.columnTypes[aColumn]);
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
				row = (RowData)this.deletedRows.get(i);
				this.data.add(row);
			}
			this.deletedRows.clear();
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
		String delimit = WbManager.getSettings().getDefaultTextDelimiter();
		return this.getRowDataAsString(aRow, delimit);
	}

	public StringBuffer getRowDataAsString(int aRow, String aDelimiter)
	{
		int count = this.getColumnCount();
		StringBuffer result = new StringBuffer(count * 20);
		int start = 0;
		DecimalFormat formatter = WbManager.getSettings().getDefaultDecimalFormatter();
		for (int c=0; c < count; c++)
		{
			RowData row = this.getRow(aRow);
			Object value = row.getValue(c);
			if (value != null)
			{
				if (value instanceof Double ||
				    value instanceof Float ||
						value instanceof BigDecimal)
				{
					Number num = (Number)value;
					result.append(formatter.format(num.doubleValue()));
				}
				else
				{
					String v = value.toString();
					if (v.indexOf((char)0) > 0)
					{
						LogMgr.logWarning("DataStore.getRowDataAsString()", "Found a zero byte in the data! Replacing with space char.");
						byte[] d = v.getBytes();
						int len = d.length;
						for (int i=0; i < len; i++)
						{
							if (d[i] == 0) d[i] = 20;
						}
						v = new String(d);
					}
					result.append(v);
				}

			}
			if (c < count - 1) result.append(aDelimiter);
		}
		return result;
	}

	public StringBuffer getHeaderString()
	{
		return this.getHeaderString(this.defaultExportDelimiter);
	}

	public StringBuffer getHeaderString(String aFieldDelimiter)
	{
		StringBuffer result = new StringBuffer(this.colCount * 30);
		for (int i=0; i < colCount; i++)
		{
			String colName = this.getColumnName(i);
			if (colName == null || colName.trim().length() == 0) colName = "Col" + i;
			result.append(colName);
			if (i < colCount - 1) result.append(aFieldDelimiter);
		}
		return result;
	}

	public String getDataString(String aLineTerminator, boolean includeHeaders)
	{
		return this.getDataString(WbManager.getSettings().getDefaultTextDelimiter(), aLineTerminator, includeHeaders);
	}

	public String getDataString(String aFieldDelimiter, String aLineTerminator, boolean includeHeaders)
	{
		int count = this.getRowCount();
		StringWriter result = new StringWriter(count * 250);
		try
		{
			this.writeDataString(result, aFieldDelimiter, aLineTerminator, includeHeaders);
		}
		catch (Exception e)
		{
			LogMgr.logError("DataStore.getDataString()", "Error when writing ASCII data to StringWriter", e);
			return "";
		}
		return result.toString();
	}

	/**
	 *	WriteDataString writes the contents of this datastore into the passed Writer.
	 *	This can be used to write the contents directly to disk without the need
	 *  to build a complete buffer in memory
	 */
	public void writeDataString(Writer out, String aFieldDelimiter, String aLineTerminator, boolean includeHeaders)
		throws IOException
	{
		int count = this.getRowCount();
		if (includeHeaders)
		{
			out.write(this.getHeaderString(aFieldDelimiter).toString());
			out.write(aLineTerminator);
		}
		for (int i=0; i < count; i++)
		{
			out.write(this.getRowDataAsString(i, aFieldDelimiter).toString());
			out.write(aLineTerminator);
		}
	}

	public void reset()
	{
		this.reset(150);
	}

	public void reset(int initialCapacity)
	{
		this.data = new ArrayList(initialCapacity);
		this.deletedRows = null;
		this.modified = false;
	}

	public boolean hasUpdateableColumns()
	{
		if (this.updateTableColumns == null)
		{
			return (this.realColumns > 0);
		}
		else
		{
			return true;
		}
	}

	public boolean isModified() { return this.modified;  }
	public boolean isUpdateable()
	{
		if (this.allowUpdates) return true;
		return (this.updateTable != null && this.hasUpdateableColumns());
	}


	private int findColumn(String name)
		throws SQLException
	{
		if (name == null) throw new SQLException("Invalid column name");

		for (int i = 0; i < this.colCount; i++)
		{
			if (this.columnNames[i] != null && name.equalsIgnoreCase(this.columnNames[i]))
			{
				return i;
			}
		}

		throw new SQLException("Invalid column name");
	}

	private RowData getRow(int aRow)
		throws IndexOutOfBoundsException
	{
		return (RowData)this.data.get(aRow);
	}

	private void initMetaData(ResultSetMetaData metaData)
		throws SQLException
	{
		this.colCount = metaData.getColumnCount();
		int col = 0;
		this.columnTypes = new int[this.colCount];
		this.columnTypeNames = new String[this.colCount];
		this.columnSizes = new int[this.colCount];
		this.columnNames = new String[this.colCount];
		this.columnClassNames = new String[this.colCount];
		this.columnPrecision = new int[this.colCount];
		this.columnScale = new int[this.colCount];

		for (int i=0; i < this.colCount; i++)
		{
			String name = metaData.getColumnName(i + 1);
			this.columnTypes[i] = metaData.getColumnType(i + 1);
			this.columnTypeNames[i] = metaData.getColumnTypeName(i + 1);
			this.columnSizes[i] = metaData.getColumnDisplaySize(i + 1);

			if (name != null && name.trim().length() > 0)
			{
				this.realColumns ++;
				this.columnNames[i] = name;
			}
			else
			{
				this.columnNames[i] = "Col" + (i+1);
			}
		}

		// these methods might not work with certain drivers, so I'll put
		// the calls into a separate loop!
		for (int i=0; i < this.colCount; i++)
		{
			try
			{
				this.columnClassNames[i] = metaData.getColumnClassName(i + 1);
			}
			catch (Throwable e)
			{
				LogMgr.logWarning("DataStore.initMetaData()", "Error when retrieving class name for column " + i + " (" + e.getClass().getName() + ")");
				this.columnClassNames[i] = "java.lang.Object";
			}

			try
			{
				this.columnPrecision[i] = metaData.getPrecision(i + 1);
			}
			catch (Throwable e)
			{
				LogMgr.logWarning("DataStore.initMetaData()", "Error when retrieving precision for column " + i + " (" + e.getClass().getName() + ")");
				this.columnPrecision[i] = 0;
			}

			try
			{
				this.columnScale[i] = metaData.getScale(i + 1);
			}
			catch (Throwable e)
			{
				LogMgr.logWarning("DataStore.initMetaData()", "Error when retrieving scale for column " + i + " (" + e.getClass().getName() + ")");
				this.columnScale[i] = 0;
			}

		}
	}

	private void initData(ResultSet aResultSet)
		throws SQLException
	{
		this.initData(aResultSet, -1);
	}

	private void initData(ResultSet aResultSet, int maxRows)
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

		try
		{
			int rowCount = 0;
			this.data = new ArrayList(500);
			while (aResultSet.next())
			{
				rowCount ++;
				if (this.rowActionMonitor != null)
				{
					this.rowActionMonitor.setCurrentRow(rowCount, -1);
				}

				RowData row = new RowData(this.colCount);
				for (int i=0; i < this.colCount; i++)
				{
					Object value = null;
					try
					{
						value = aResultSet.getObject(i + 1);
					}
					catch (SQLException e)
					{
						LogMgr.logError("DataStore.initData()", "Error when retrieving column " + this.getColumnName(i) + " for row " + rowCount, e);
						value = null;
					}

					if (value == null)// || aResultSet.wasNull() )
					{
						row.setValue(i, NullValue.getInstance(this.columnTypes[i]));
					}
					else
					{
						row.setValue(i, value);
					}
				}
				row.resetStatus();
				this.data.add(row);
				if (maxRows > 0)
				{
					if (rowCount > maxRows) break;
				}
			}
			this.modified = false;
		}
		catch (SQLException e)
		{
			LogMgr.logError(this, "Error while retrieving ResultSet", e);
			throw e;
		}
		catch (Exception e)
		{
			LogMgr.logError(this, "Error while retrieving ResultSet", e);
			throw new SQLException(e.getMessage());
		}
	}

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

	public StringBuffer getXmlStart()
	{
		return this.getXmlStart("UTF-8");
	}

	/**
	 *	Return the opening root xml tag for the whole DataStore.
	 * A valid XML document can be created with <br>
	 * {@link #getXmlStart()} <br>
	 * {@link #getMetaDataAsXml(String)} <br>
	 * Call {@link #getRowDataAsXml(int)} for each row <br>
	 * {@link #getXmlEnd()} <br>
	 *
	 * @see #getMetaDataAsXml(String)
	 * @see #getDataAsXml()
	 * @see #getXmlEnd()
	 */
	public StringBuffer getXmlStart(String encoding)
	{
		StringBuffer xml = new StringBuffer(1000 + colCount * 50);
		xml.append("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>");
		xml.append(StringUtil.LINE_TERMINATOR);
		xml.append("<wb-export>");
		xml.append(StringUtil.LINE_TERMINATOR);
		xml.append(this.getMetaDataAsXml("  "));
		xml.append(StringUtil.LINE_TERMINATOR);
		xml.append("  <data>");
		xml.append(StringUtil.LINE_TERMINATOR);
		return xml;
	}

	/**
	 *	Return the closing root xml tag for the whole DataStore.
	 * A valid XML document can be created with
	 * {@link #getXmlStart()}
	 * {@link #getMetaDataAsXml()}
	 * Call {@link #getRowDataAsXml()} for each row
	 * {@link #getXmlEnd()}
	 *
	 * @see #getXmlStart()
	 * @see #getMetaDataAsXml(String)
	 * @see getDataAsXml()
	 */
	public StringBuffer getXmlEnd()
	{
		StringBuffer xml = new StringBuffer(100);
		xml.append("  </data>");
		xml.append(StringUtil.LINE_TERMINATOR);
		xml.append("</wb-export>");
		xml.append(StringUtil.LINE_TERMINATOR);
		return xml;
	}

	public StringBuffer getMetaDataAsXml()
	{
		return this.getMetaDataAsXml(null);
	}

	/**
	 * Return the metadata for this DataStore as an XML definition.
	 * A valid XML document can be created with <br>
	 * {@link #getXmlStart()} <br>
	 * {@link #getMetaDataAsXml()} <br>
	 * Call {@link #getRowDataAsXml(int)} for each row <br>
	 * {@link #getXmlEnd()} <br>
	 *
	 * @see #getDataAsXml()
	 * @see #getXmlStart()
	 * @see #getXmlEnd()
	 * @param anIndent Defines an indention to be used when creating the XML string
	 */
	public StringBuffer getMetaDataAsXml(String anIndent)
	{
		boolean indent = (anIndent != null && anIndent.length() > 0);
		StringBuffer result = new StringBuffer(this.colCount * 50);
		if (indent) result.append(anIndent);
		result.append("<meta-data>");
		result.append(StringUtil.LINE_TERMINATOR);

		if (this.sql != null)
		{
			if (indent) result.append(anIndent);
			result.append("  <generating-sql>");
			result.append(StringUtil.LINE_TERMINATOR);
			if (indent) result.append(anIndent);
			result.append("  <![CDATA[");
			result.append(StringUtil.LINE_TERMINATOR);
			result.append(sql);
			result.append(StringUtil.LINE_TERMINATOR);
			if (indent) result.append(anIndent);
			result.append("  ]]>");
			result.append(StringUtil.LINE_TERMINATOR);
			if (indent) result.append(anIndent);
			result.append("  </generating-sql>");
			result.append(StringUtil.LINE_TERMINATOR);
			result.append(StringUtil.LINE_TERMINATOR);
		}

		if (this.originalConnection != null)
		{
			result.append(this.originalConnection.getDatabaseInfoAsXml(anIndent));
			result.append(StringUtil.LINE_TERMINATOR);
		}

		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
		if (indent) result.append(anIndent);
		result.append("  <created>");
		result.append(df.format(new java.util.Date()));
		result.append("</created>" + StringUtil.LINE_TERMINATOR);
		result.append(StringUtil.LINE_TERMINATOR);

		if (indent) result.append(anIndent);
		result.append("</meta-data>");
		result.append(StringUtil.LINE_TERMINATOR);
		result.append(StringUtil.LINE_TERMINATOR);

		if (indent) result.append(anIndent);
		result.append("<table-def>");
		result.append(StringUtil.LINE_TERMINATOR);

		if (indent) result.append(anIndent);
		result.append("  <!-- The following information was retrieved from the JDBC driver's ResultSetMetaData -->");
		result.append(StringUtil.LINE_TERMINATOR);

		if (indent) result.append(anIndent);
		result.append("  <!-- name is retrieved from ResultSetMetaData.getColumnName() -->");
		result.append(StringUtil.LINE_TERMINATOR);

		if (indent) result.append(anIndent);
		result.append("  <!-- java-class is retrieved from ResultSetMetaData.getColumnClassName() -->");
		result.append(StringUtil.LINE_TERMINATOR);

		if (indent) result.append(anIndent);
		result.append("  <!-- java-sql-type-name is the constant's name from java.sql.Types -->");
		result.append(StringUtil.LINE_TERMINATOR);

		if (indent) result.append(anIndent);
		result.append("  <!-- java-sql-type is the constant's numeric value from java.sql.Types as returned from ResultSetMetaData.getColumnType() -->");
		result.append(StringUtil.LINE_TERMINATOR);

		if (indent) result.append(anIndent);
		result.append("  <!-- dbms-data-type is retrieved from ResultSetMetaData.getColumnTypeName() -->");
		result.append(StringUtil.LINE_TERMINATOR);

		result.append(StringUtil.LINE_TERMINATOR);
		if (indent) result.append(anIndent);
		result.append("  <!-- For date and timestamp types, the internal long value obtained from java.util.Date.getTime()");
		result.append(StringUtil.LINE_TERMINATOR);
		if (indent) result.append(anIndent);
		result.append("       will be written as an attribute to the <column-data> tag. That value can be used");
		result.append(StringUtil.LINE_TERMINATOR);
		if (indent) result.append(anIndent);
		result.append("       to create a java.util.Date() object directly, without the need to parse the actual tag content");
		result.append(StringUtil.LINE_TERMINATOR);
		if (indent) result.append(anIndent);
		result.append("  -->");
		result.append(StringUtil.LINE_TERMINATOR);
		result.append(StringUtil.LINE_TERMINATOR);

		/*
		if (indent) result.append(anIndent);
		result.append("  <!-- precision is retrieved from ResultSetMetaData.getPrecision() -->");
		result.append(StringUtil.LINE_TERMINATOR);

		if (indent) result.append(anIndent);
		result.append("  <!-- scale is retrieved from ResultSetMetaData.getScale() -->");
		result.append(StringUtil.LINE_TERMINATOR);

		if (indent) result.append(anIndent);
		result.append("  <!-- display-size is retrieved from ResultSetMetaData.getColumnDisplaySize() -->");
		result.append(StringUtil.LINE_TERMINATOR);
		result.append(StringUtil.LINE_TERMINATOR);
		*/
		String updateTable = this.getUpdateTable();
		boolean hasTable = false;
		if (indent) result.append(anIndent);
		result.append("  <table-name>");
		if (updateTable != null) result.append(updateTable);
		result.append("</table-name>");
		result.append(StringUtil.LINE_TERMINATOR);

		if (indent) result.append(anIndent);
		result.append("  <column-count>");
		result.append(this.colCount);
		result.append("</column-count>");
		result.append(StringUtil.LINE_TERMINATOR);
		result.append(StringUtil.LINE_TERMINATOR);

		for (int i=0; i < this.colCount; i++)
		{
			if (indent) result.append(anIndent);
			result.append("  <column-def index=\"");
			result.append(i);
			result.append("\">");
			result.append(StringUtil.LINE_TERMINATOR);

			if (indent) result.append(anIndent);
			result.append("    <column-name>");
			result.append(this.getColumnName(i));
			result.append("</column-name>");
			result.append(StringUtil.LINE_TERMINATOR);

			if (indent) result.append(anIndent);
			result.append("    <java-class>");
			result.append(this.getColumnClassName(i));
			result.append("</java-class>");
			result.append(StringUtil.LINE_TERMINATOR);

			if (indent) result.append(anIndent);
			result.append("    <java-sql-type-name>");
			result.append(SqlUtil.getTypeName(this.getColumnType(i)));
			result.append("</java-sql-type-name>");
			result.append(StringUtil.LINE_TERMINATOR);

			if (indent) result.append(anIndent);
			result.append("    <java-sql-type>");
			result.append(this.getColumnType(i));
			result.append("</java-sql-type>");
			result.append(StringUtil.LINE_TERMINATOR);

			if (indent) result.append(anIndent);
			result.append("    <dbms-data-type>");
			result.append(this.columnTypeNames[i]);
			result.append("</dbms-data-type>");
			result.append(StringUtil.LINE_TERMINATOR);


			int type = this.getColumnType(i);
			if (SqlUtil.isDateType(type) )
			{
				if (type == Types.TIMESTAMP && this.defaultTimestampFormatter != null)
				{
					if (indent) result.append(anIndent);
					result.append("    <data-format>");
					result.append(this.defaultTimestampFormatter.toPattern());
					result.append("</data-format>");
					result.append(StringUtil.LINE_TERMINATOR);
				}
				else if (this.defaultDateFormatter != null)
				{
					if (indent) result.append(anIndent);
					result.append("    <data-format>");
					result.append(this.defaultDateFormatter.toPattern());
					result.append("</data-format>");
					result.append(StringUtil.LINE_TERMINATOR);
				}
			}
			/*
			else if (SqlUtil.isDecimalType(type, this.columnScale[i], this.columnPrecision[i]))
			{
				if (this.defaultNumberFormatter != null)
				{
					if (indent) result.append(anIndent);
					result.append("    <data-format>");
					result.append(this.defaultNumberFormatter.toPattern());
					result.append("</data-format>");
					result.append(StringUtil.LINE_TERMINATOR);
				}
			}

			if (indent) result.append(anIndent);
			result.append("    <precision>");
			result.append(this.columnPrecision[i]);
			result.append("</precision>");
			result.append(StringUtil.LINE_TERMINATOR);

			if (indent) result.append(anIndent);
			result.append("    <scale>");
			result.append(this.columnScale[i]);
			result.append("</scale>");
			result.append(StringUtil.LINE_TERMINATOR);

			if (indent) result.append(anIndent);
			result.append("    <display-size>");
			result.append(this.columnSizes[i]);
			result.append("</display-size>");
			result.append(StringUtil.LINE_TERMINATOR);
			*/
			if (indent) result.append(anIndent);
			result.append("  </column-def>");
			result.append(StringUtil.LINE_TERMINATOR);
		}
		if (indent) result.append(anIndent);
		result.append("</table-def>");
		result.append(StringUtil.LINE_TERMINATOR);

		return result;
	}

	/**
	 * Return the complete data of this DataStore as an XML document.
	 * This is equivalent to calling:<br>
	 * {@link #getXmlStart()} <br>
	 * {@link #getMetaDataAsXml(String)} <br>
	 * Call {@link #getRowDataAsXml(int)} for each row <br>
	 * {@link #getXmlEnd()} <br>
	 *
	 * @see #getXmlStart()
	 * @see #getMetaDataAsXml(String)
	 * @see #getXmlEnd()
	 * @see #getRowDataAsXml(int)
	 */
	public String getDataAsXml()
	{
		int count = this.getRowCount();
		StringWriter out = new StringWriter(count * 1000 + colCount * 50);
		try
		{
			this.writeXmlData(out);
		}
		catch (IOException io)
		{
			LogMgr.logError("DataStore.getDataAsXml()", "Error writing XML to StringWriter", io);
			return "";
		}
		return out.toString();
	}

	public void writeXmlData(Writer pw)
		throws IOException
	{
		int count = this.getRowCount();
		if (count == 0) return;

		pw.write(this.getXmlStart().toString());

		String indent = "    ";

		for (int row=0; row < count; row++)
		{
			StringBuffer rowData = this.getRowDataAsXml(row, indent);
			pw.write(rowData.toString());
		}

		pw.write(this.getXmlEnd().toString());
	}

	/**
	 * Returns the data contained in the given row as an XML tag.
	 * The display row index will be on greater then the internal one.
	 * The XML will not be indented
	 * @param aRow the internal row number for which the XML data should be returned (starting with 0)
	 *
	 * @return a StringBuffer with a single &lt;row-data&gt; tag
	 * @see #getRowDataAsXml(int, String)
	 * @see #getRowDataAsXml(int, String, int)
	 */
	public StringBuffer getRowDataAsXml(int aRow)
	{
		return this.getRowDataAsXml(aRow, null);
	}

	/**
	 * Returns the data contained in the given row as an XML tag.
	 * The display row index will be on greater then the internal one.
	 * @param aRow the internal row number for which the XML data should be returned (starting with 0)
	 * @param anIndent a String which is used to define the base indention for the XML
	 *
	 * @return a StringBuffer with a single &lt;row-data&gt; tag
	 * @see #getRowDataAsXml(int, String, int)
	 */
	public StringBuffer getRowDataAsXml(int aRow, String anIndent)
	{
		return this.getRowDataAsXml(aRow, anIndent, aRow + 1);
	}

	/**
	 * Returns the data contained in the given row as an XML tag.
	 * @param aRow the internal row number for which the XML data should be returned (starting with 0)
	 * @param anIndent a String which is used to define the base indention for the XML
	 * @param displayRowIndex the actual row index to be written into the attribute row-num
	 * @return a StringBuffer with a single &lt;row-data&gt; tag
	 */
	public StringBuffer getRowDataAsXml(int aRow, String anIndent, int displayRowIndex)
	{
		boolean indent = (anIndent != null && anIndent.length() > 0);
		int colCount = this.getColumnCount();
		StringBuffer xml = new StringBuffer(colCount * 100);
		if (indent) xml.append(anIndent);
		xml.append("<row-data ");
		xml.append("row-num=\"");
		xml.append(displayRowIndex);
		xml.append("\">");
		xml.append(StringUtil.LINE_TERMINATOR);
		for (int c=0; c < colCount; c ++)
		{
			String value = this.getValueAsFormattedString(aRow, c);
			Object data = this.getValue(aRow, c);

			if (indent) xml.append(anIndent);
			xml.append("  <column-data index=\"");
			xml.append(c);
			xml.append('"');
			if (value == null)
			{
				xml.append(" null=\"true\"");
			}
			else if (value.length() == 0)
			{
				xml.append(" null=\"false\"");
			}

			if (SqlUtil.isDateType(this.columnTypes[c]))
			{
				try
				{
					java.util.Date d = (java.util.Date)data;
					xml.append(" longValue=\"");
					xml.append(d.getTime());
					xml.append('"');
				}
				catch (Exception e)
				{
				}
			}
			xml.append('>');

			if (value != null)
			{
				// String data needs to be escaped!
				if (data instanceof String)
				{
					xml.append(StringUtil.escapeXML((String)data));
				}
				else
				{
					xml.append(value);
				}
			}
			xml.append("</column-data>");
			xml.append(StringUtil.LINE_TERMINATOR);
		}
		if (indent) xml.append(anIndent);
		xml.append("</row-data>");
		xml.append(StringUtil.LINE_TERMINATOR);
		return xml;
	}

	public String getDataAsHtml()
	{
		StringWriter html = new StringWriter(this.getRowCount() * 100);
		try
		{
			this.writeHtmlData(html);
		}
		catch (Exception e)
		{
			LogMgr.logError("DataStore.getDataAsHtml()", "Error writing HTML to StringWriter", e);
		}
		return html.toString();
	}

	public StringBuffer getHtmlStart()
	{
		return this.getHtmlStart(true, null);
	}

	public StringBuffer getHtmlStart(boolean createFullPage)
	{
		return this.getHtmlStart(createFullPage, null);
	}

	public StringBuffer getHtmlStart(boolean createFullPage, String title)
	{
		StringBuffer result = new StringBuffer(250);
		if (createFullPage)
		{
			result.append("<html>\n");
			if (title != null && title.length() > 0)
			{
				result.append("<head>\n<title>");
				result.append(title);
				result.append("</title>\n");
			}
			result.append("<style type=\"text/css\">\n");
			result.append("<!--\n");
			result.append("  table { border-spacing:0; border-collapse:collapse}\n");
			result.append("  td { padding:2; border-style:solid;border-width:1px; vertical-align:top;}\n");
			result.append("  .number-cell { text-align:right; white-space:nowrap; } \n");
			result.append("  .text-cell { text-align:left; } \n");
			result.append("  .date-cell { text-align:left; white-space:nowrap;} \n");
			result.append("-->\n</style>\n");

			result.append("</head>\n<body>\n");
			/*
			if (title != null && title.length() > 0)
			{
				result.append("<h3>");
				result.append(title);
				result.append("</h3>\n");
			}*/
		}
		result.append("<table>\n");

		// table header with column names
		result.append("  <tr>\n      ");
		for (int c=0; c < this.getColumnCount(); c ++)
		{
			result.append("<td><b>");
			result.append(this.getColumnName(c));
			result.append("</b></td>");
		}
		result.append("\n  </tr>\n");

		return result;
	}

	public StringBuffer getHtmlEnd()
	{
		return this.getHtmlEnd(true);
	}
	public StringBuffer getHtmlEnd(boolean createFullPage)
	{
		StringBuffer html = new StringBuffer("</table>\n");
		if (createFullPage) html.append("</body>\n</html>\n");
		return html;
	}

	public StringBuffer getRowDataAsHtml(int row)
	{
		int count = this.getColumnCount();
		StringBuffer result = new StringBuffer(count * 30);
		result.append("  <tr>\n      ");
		for (int c=0; c < count; c ++)
		{
			String value = this.getValueAsFormattedString(row, c);
			int type = this.getColumnType(c);
			if (SqlUtil.isDateType(type))
			{
				result.append("<td class=\"date-cell\">");
			}
			else if (SqlUtil.isNumberType(type) || SqlUtil.isDateType(type))
			{
				result.append("<td class=\"number-cell\">");
			}
			else
			{
				result.append("<td class=\"text-cell\">");
			}

			if (value == null)
			{
				result.append("&nbsp;");
			}
			else
			{
				if (this.escapeHtml)
				{
					value = StringUtil.escapeHTML(value);
				}
				result.append(value);
			}
			result.append("</td>");
		}
		result.append("\n  </tr>\n");
		return result;
	}

	public void writeHtmlData(Writer html)
		throws IOException
	{
		int count = this.getRowCount();
		if (count == 0) return;
		html.write(this.getHtmlStart().toString());

		for (int i=0; i < count; i++)
		{
			html.write(this.getRowDataAsHtml(i).toString());
		}
		html.write(this.getHtmlEnd().toString());
	}

	private boolean escapeHtml = true;

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
		if (this.updateTable == null)
			return this.checkUpdateTable();
		else
			return true;
	}

	// =========== SQL Insert generation ================

	/**
	 * Returns the given row as an INSERT statement.
	 * \n will be used as the line terminator
	 * @param aRow the row to be returned
	 * @param aConn the connection object to be used to retrieve MetaData information
	 * @return a StringBuffer with a single INSERT statement.
	 * @see #getRowDataAsSqlInsert(int, String, WbConnection)
	 */
	public StringBuffer getRowDataAsSqlInsert(int aRow, WbConnection aConn)
	{
		return this.getRowDataAsSqlInsert(aRow, "\n", aConn);
	}

	/**
	 * Returns the given row as an INSERT statement.
	 *
	 * @param aRow the row to be returned
	 * @param aLineTerminator the line terminator to be used
	 * @param aConn the connection object to be used to retrieve MetaData information
	 * @return a StringBuffer with a single INSERT statement.
	 * @see #getRowDataAsSqlInsert(int, WbConnection)
	 * @see #getRowDataAsSqlInsert(int, String, WbConnection, String, String)
	 */
	public StringBuffer getRowDataAsSqlInsert(int aRow, String aLineTerminator, WbConnection aConn)
	{
		return this.getRowDataAsSqlInsert(aRow, aLineTerminator, aConn, null, null);
	}

	/**
	 * Returns the given row as an INSERT statement.
	 * If aCharFunc and aConcatString are not null, then all non-printable characters
	 * in character data are replaced with a call to the passed function. <br>
	 * Example:<br>
	 * A character column contains a carriage return character (ASCII 13) and
	 * aCharFunc is passed as "CHR" and aConcatString is passed as "||". The generated
	 * SQL will look like this: <br>
	 * <code>
	 * INSERT INTO table (col1, character_column)
	 * VALUES
	 * (1, "Hello, "||chr(13)||"world");
	 * </code>
	 * @param aRow the row to be returned
	 * @param aLineTerminator the line terminator to be used
	 * @param aConn the connection object to be used to retrieve MetaData information
	 * @param aCharFunc the SQL function to be used for non printable characters
	 * @param aConcatString the operator to be used for string concatenation
	 * @return a StringBuffer with a single INSERT statement.
	 * @see #getRowDataAsSqlInsert(int, WbConnection)
	 * @see #getRowDataAsSqlInsert(int, String, WbConnection)
	 */
	public StringBuffer getRowDataAsSqlInsert(int aRow, String aLineTerminator, WbConnection aConn, String aCharFunc, String aConcatString)
	{
		if (!this.canSaveAsSqlInsert()) return null;
		RowData data = this.getRow(aRow);
		DmlStatement stmt = this.createInsertStatement(data, true, aLineTerminator);
		if (aCharFunc != null)
		{
			stmt.setChrFunction(aCharFunc);
			stmt.setConcatString(aConcatString);
		}
		StringBuffer sql = new StringBuffer(stmt.getExecutableStatement(aConn.getSqlConnection()));
		sql.append(";");
		return sql;
	}

	public String getDataAsSqlInsert()
		throws Exception, SQLException
	{
		return this.getDataAsSqlInsert("\n");
	}

	public String getDataAsSqlInsert(String aLineTerminator)
		throws Exception, SQLException
	{
		if (!this.canSaveAsSqlInsert()) return "";
		StringWriter script = new StringWriter(this.getRowCount() * 150);
		try
		{
			this.writeDataAsSqlInsert(script, aLineTerminator);
		}
		catch (Exception e)
		{
			LogMgr.logError("DataStore.getDataAsSqlInsert()", "Error writing script to StringWriter", e);
			return "";
		}
		return script.toString();
	}

	public void writeDataAsSqlInsert(Writer out, String aLineTerminator)
		throws IOException
	{
		this.writeDataAsSqlInsert(out, aLineTerminator, null, null);
	}

	public void writeDataAsSqlInsert(Writer out, String aLineTerminator, String aCharFunc, String aConcatString)
		throws IOException
	{
		if (!this.canSaveAsSqlInsert()) return;
		int count = this.getRowCount();
		for (int row = 0; row < count; row ++)
		{
			RowData data = this.getRow(row);
			DmlStatement stmt = this.createInsertStatement(data, true, aLineTerminator);
			String sql = stmt.getExecutableStatement(this.originalConnection.getSqlConnection());
			if (aCharFunc != null)
			{
				stmt.setChrFunction(aCharFunc);
				stmt.setConcatString(aConcatString);
			}
			out.write(sql);
			out.write(";");
			out.write(aLineTerminator);
			out.write(aLineTerminator);
		}
	}

	// =========== SQL Update generation ================
	public String getDataAsSqlUpdate()
	{
		return this.getDataAsSqlUpdate("\n");
	}

	public String getDataAsSqlUpdate(String aLineTerminator)
	{
		return this.getDataAsSqlUpdate(aLineTerminator, null, null);
	}

	public String getDataAsSqlUpdate(String aLineTerminator, String aCharFunc, String aConcatString)
	{
		if (!this.canSaveAsSqlInsert()) return "";
		StringWriter script = new StringWriter(this.getRowCount() * 150);
		try
		{
			this.writeDataAsSqlUpdate(script, aLineTerminator, aCharFunc, aConcatString);
		}
		catch (Exception e)
		{
			LogMgr.logError("DataStore.getDataAsSqlInsert()", "Error writing script to StringWriter", e);
			return "";
		}
		return script.toString();
	}

	public void writeDataAsSqlUpdate(Writer out, String aLineTerminator)
		throws IOException
	{
		this.writeDataAsSqlUpdate(out, aLineTerminator, null, null);
	}

	public void writeDataAsSqlUpdate(Writer out, String aLineTerminator, String aCharFunc, String aConcatString)
		throws IOException
	{
		if (!this.canSaveAsSqlInsert()) return;
		int count = this.getRowCount();

		if (this.pkColumns == null)
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
		if (pkColumns == null)
		{
			LogMgr.logWarning("DataStore.writeDataAsSqlUpdate()", "No PK columns found. Cannot write as SQL Update");
			return;
		}

		for (int row = 0; row < count; row ++)
		{
			RowData data = this.getRow(row);
			DmlStatement stmt = this.createUpdateStatement(data, true, aLineTerminator);
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

	public StringBuffer getRowDataAsSqlUpdate(int aRow, WbConnection aConn)
	{
		return this.getRowDataAsSqlUpdate(aRow, "\n", aConn);
	}

	public StringBuffer getRowDataAsSqlUpdate(int aRow, String aLineTerminator, WbConnection aConn)
	{
		return this.getRowDataAsSqlUpdate(aRow, aLineTerminator, aConn, null, null);
	}

	/**
	 * Returns the given row as an UPDATE statement.
	 * If aCharFunc and aConcatString are not null, then all non-printable characters
	 * in character data are replaced with a call to the passed function. <br>
	 * Example:<br>
	 * A character column contains a carriage return character (ASCII 13) and
	 * aCharFunc is passed as "CHR" and aConcatString is passed as "||". The generated
	 * SQL will look like this: <br>
	 * <code>
	 * UPDATE table set character_column = "Hello, "||chr(13)||"world";
	 * </code>
	 * @param aRow the row to be returned
	 * @param aLineTerminator the line terminator to be used
	 * @param aConn to the WbConnection to be used to retrieve the metadata
	 * @param aCharFunc the SQL function to be used for non printable characters
	 * @param aConcatString the operator to be used for string concatenation
	 * @return a StringBuffer with a single INSERT statement.
	 * @see #getRowDataAsSqlUpdate()
	 * @see #getRowDataAsSqlInsert(int, String, WbConnection)
	 */
	public StringBuffer getRowDataAsSqlUpdate(int aRow, String aLineTerminator, WbConnection aConn, String aCharFunc, String aConcatString)
	{
		if (!this.canSaveAsSqlInsert()) return null;
		RowData data = this.getRow(aRow);
		DmlStatement stmt = this.createUpdateStatement(data, true, aLineTerminator);
		if (aCharFunc != null)
		{
			stmt.setChrFunction(aCharFunc);
			stmt.setConcatString(aConcatString);
		}
		StringBuffer sql = new StringBuffer(stmt.getExecutableStatement(aConn.getSqlConnection()));
		sql.append(";");
		return sql;
	}

	/**
	 * Set all values in the given row to NULL
	 */
	public void setRowNull(int aRow)
	{
		for (int i=0; i < this.colCount; i++)
		{
			this.setNull(aRow, i);
		}
	}


	private boolean cancelUpdate = false;
	private boolean cancelImport = false;

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


	/**
	 *	Import a text file (tab separated) with a header row and no column mapping
	 *	into this DataStore
	 * @param aFilename - The text file to import
	 */
	public void importData(String aFilename)
		throws FileNotFoundException
	{
		this.importData(aFilename, null);
	}

	/**
	 *	Import a text file (tab separated) with a header row and no column mapping
	 *	into this DataStore
	 * @param aFilename - The text file to import
	 */
	public void importData(String aFilename, JobErrorHandler errorHandler)
		throws FileNotFoundException
	{
		this.importData(aFilename, true, "\t", "\"", errorHandler);
	}

	/**
	 * Import a text file (tab separated) with no column mapping
	 * into this DataStore.
	 *
	 * @param aFilename - The text file to import
	 * @param hasHeader - wether the text file has a header row
	 */
	public void importData(String aFilename, boolean hasHeader, String quoteChar)
		throws FileNotFoundException
	{
		this.importData(aFilename, hasHeader, "\t", quoteChar, null);
	}

	public void importData(String aFilename, boolean hasHeader, String aColSeparator, String aQuoteChar)
		throws FileNotFoundException
	{
		this.importData(aFilename, hasHeader, aColSeparator, aQuoteChar, null);
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

		// if the data store is empty, we tried to initialize the
		// data array to an approx. size. As we don't know how many lines
		// we really have in the file, we take the length of the first line
		// as the average, and calculate the expected number of lines from
		// this length.
		// Even if we don't get the number of lines correct, this method should be better
		// then not initializing the array at all.
		if (line != null && this.data.size() == 0)
		{
			int initialSize = (int)(fileSize / line.length());
			this.data = new ArrayList(initialSize);
		}

		CsvLineParser tok = new CsvLineParser(aColSeparator.charAt(0), '"');
		int importRow = 0;

		while (line != null)
		{
			tok.setLine(line);

			row = this.addRow();
			importRow ++;

			this.updateProgressMonitor(importRow, -1);

			this.setRowNull(row);

			for (int col=0; col < this.colCount; col++)
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
						if (errorHandler != null)
						{
							int choice = errorHandler.getActionOnError(row, col, (value == null ? null : value.toString()), "");
							if (choice == JobErrorHandler.JOB_ABORT) break;
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
	 * Returns a List of {@link #DmlStatements } which
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

		row = this.getNextDeletedRow();
		while (row != null)
		{
			dml = this.createDeleteStatement(row);
			stmt.add(dml);
			row = this.getNextDeletedRow();
		}

		row = this.getNextChangedRow();
		while (row != null)
		{
			dml = this.createUpdateStatement(row);
			stmt.add(dml);
			row = this.getNextChangedRow();
		}

		row = this.getNextInsertedRow();
		while (row != null)
		{
			dml = this.createInsertStatement(row, false, "\n");
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
					choice = errorHandler.getActionOnError(row, -1, dml.getExecutableStatement(), e.getMessage());
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
			LogMgr.logError("DataStore.executeGuarded()", "Error executing statement " + dml.getExecutableStatement() + " for row = " + row, e);
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
		this.updatePkInformation(aConnection);
		int totalRows = this.getModifiedCount();
		int currentRow = 0;
		if (this.rowActionMonitor != null)
		{
			this.rowActionMonitor.setMonitorType(RowActionMonitor.MONITOR_UPDATE);
		}

		this.ignoreAllUpdateErrors = false;

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
					dml = this.createDeleteStatement(row);
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
					dml = this.createUpdateStatement(row, false, "\r\n");
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
					dml = this.createInsertStatement(row, false);
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
			LogMgr.logError("DataStore.updateDb()", "Error when saving data (row = " + row + ")", e);
			throw e;
		}

		return rows;
	}

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
				RowData row = (RowData)this.deletedRows.get(i);
			row.setDmlSent(false);
			}
		}
	}

	public void resetStatusForSentRow()
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
			ArrayList newDeleted = new ArrayList(this.deletedRows.size());
			rows = this.deletedRows.size();
			for (int i=0; i < rows; i++)
			{
				RowData row = (RowData)this.deletedRows.get(i);
				if (!row.isDmlSent())
				{
					newDeleted.add(row);
				}
			}
			this.deletedRows.clear();
			this.deletedRows = newDeleted;
		}
	}

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


	public int compareRowsByColumn(RowData row1, RowData row2, int column)
	{
		Object o1 = row1.getValue(column);
		Object o2 = row2.getValue(column);

		if ( (o1 == null && o2 == null) ||
		     (o1 instanceof NullValue && o2 instanceof NullValue) )
		{
			return 0;
		}
		else if (o1 == null || o1 instanceof NullValue)
		{
			return 1;
		}
		else if (o2 == null || o2 instanceof NullValue)
		{
			return -1;
		}

		if (o1 instanceof String && o2 instanceof String)
		{
			return defaultCollator.compare(o1, o2);
		}

		try
		{
			int result = ((Comparable)o1).compareTo(o2);
			return result;
		}
		catch (Throwable e)
		{
		}

		// Fallback sorting...
		// if the values didn't implement Comparable
		// the use normale String comparison
		String v1 = o1.toString();
		String v2 = o2.toString();
		return v1.compareTo(v2);
	}

	/**    Compare two rows.  All sorting columns will be sorted.
	 *
	 * @param row1 Row 1
	 * @param row2 Row 2
	 * @return 1, 0, or -1
	 */
	public int compare(RowData row1, RowData row2, int column, boolean ascending)
	{
		int result = compareRowsByColumn(row1, row2, column);
		if (result == 0) return 0;

		//if (result == -2 || result == 2)
		//	return result;
		//else
		return ascending ? result : -result;
	}

	public void sortByColumn(int aColumn, boolean ascending)
	{
		synchronized (this)
		{
			if (this.comparator == null)
			{
				this.comparator = new ColumnComparator();
			}
			this.comparator.setMode(aColumn, ascending);
			Collections.sort(this.data, this.comparator);
		}
	}

	public String getDefaultDateFormat()
	{
		if (this.defaultDateFormatter == null) return null;
		return this.defaultDateFormatter.toPattern();
	}

	public void setDefaultTimestampFormatter(SimpleDateFormat aFormatter)
	{
		this.defaultTimestampFormatter = aFormatter;
	}

	public void setDefaultDateFormatter(SimpleDateFormat aFormatter)
	{
		this.defaultDateFormatter = aFormatter;
	}

	public void setDefaultDateFormat(String aFormat)
	{
		if (aFormat == null) return;
		try
		{
			this.defaultDateFormatter = new SimpleDateFormat(aFormat);
		}
		catch (Exception e)
		{
			this.defaultDateFormatter = null;
			LogMgr.logWarning("DataStore.setDefaultDateFormat()", "Could not create date formatter for format " + aFormat);
		}
	}

	public String getDefaultNumberFormat()
	{
		if (this.defaultNumberFormatter == null) return null;
		return this.defaultNumberFormatter.toPattern();
	}

	public void setDefaultNumberFormatter(DecimalFormat aFormatter)
	{
		this.defaultNumberFormatter = aFormatter;
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

	public Object convertCellValue(Object aValue, int aColumn)
		throws Exception
	{
		int type = this.getColumnType(aColumn);
		if (aValue == null)
		{
			return NullValue.getInstance(type);
		}

		switch (type)
		{
			case Types.DATE:
				return this.parseDate((String)aValue);
			case Types.TIMESTAMP:
				java.sql.Date d = this.parseDate((String)aValue);
				Timestamp t = new Timestamp(d.getTime());
				return t;
			default:
				return converter.convertValue(aValue, type);
		}
	}

  private java.sql.Date parseDate(String aDate)
  {
		java.sql.Date result = null;
		if (this.defaultDateFormatter != null)
		{
			try
			{
				java.util.Date d = defaultDateFormatter.parse(aDate);
				result = new java.sql.Date(d.getTime());
			}
			catch (Exception e)
			{
				LogMgr.logWarning("DataStore.parseDate()", "Could not parse date " + aDate + " with default formatter " + this.defaultDateFormatter.toPattern());
				result = null;
			}

		}

		if (result == null)
		{
			result = converter.parseDate(aDate);
		}

		if (result == null)
		{
			LogMgr.logWarning("DataStore.parseDate()", "Could not parse date " + aDate);
		}
		return result;
  }
	/**
	 * Return the status object for the give row.
	 * The status is one of
	 * <ul>
	 * <li>DataStore.ROW_ORIGINAL</li>
	 * <li>DataStore.ROW_MODIFIED</li>
	 * <li>DataStore.ROW_NEW</li>
	 * <ul>
	 * The status object is used by the renderer in the result
	 * table to display the approriate icon.
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

		if (this.pkColumns == null) return Collections.EMPTY_MAP;

		RowData data = this.getRow(aRow);
		if (data == null) return Collections.EMPTY_MAP;

		int count = this.pkColumns.size();
		HashMap result = new HashMap(count);
		for (int j=0; j < count ; j++)
		{
			int pkcol = ((Integer)this.pkColumns.get(j)).intValue();
			String name = this.getColumnName(pkcol);
			Object value = data.getValue(pkcol);
			if (value instanceof NullValue)
			{
				result.put(name, null);
			}
			else
			{
				result.put(name, value);
			}

		}
		return result;
	}

	public DmlStatement getInsertStatement(int aRow, String aLineTerminator)
	{
		RowData row = this.getRow(aRow);
		DmlStatement result = this.createInsertStatement(row, true, aLineTerminator);
		return result;
	}


	private int currentUpdateRow = 0;
	private int currentInsertRow = 0;
	private int currentDeleteRow = 0;

	private void resetUpdateRowCounters()
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

	private RowData getNextDeletedRow()
	{
		if (this.deletedRows == null || this.deletedRows.size() == 0) return null;
		int count = this.deletedRows.size();

		if (this.currentDeleteRow > count) return null;

		RowData row = null;

		while (this.currentDeleteRow < count)
		{
			row = (RowData)this.deletedRows.get(this.currentDeleteRow);
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

	private DmlStatement createUpdateStatement(RowData aRow)
	{
		return this.createUpdateStatement(aRow, false, System.getProperty("line.separator"));
	}

	private DmlStatement createUpdateStatement(RowData aRow, boolean ignoreStatus, String lineEnd)
	{
		if (aRow == null) return null;
		boolean first = true;
		boolean newLineAfterColumn = (this.colCount > 5);

		DmlStatement dml;

		if (!ignoreStatus && !aRow.isModified()) return null;
		ArrayList values = new ArrayList();
		StringBuffer sql = new StringBuffer("UPDATE ");

		sql.append(SqlUtil.quoteObjectname(this.updateTable));
		sql.append("\n   SET ");
		first = true;
		for (int col=0; col < this.colCount; col ++)
		{
			if (aRow.isColumnModified(col) || (ignoreStatus && !this.isPkColumn(col)))
			{
				if (first)
				{
					first = false;
				}
				else
				{
					sql.append(", ");
					if (newLineAfterColumn) sql.append("\n       ");
				}
				String colName = SqlUtil.quoteObjectname(this.getColumnName(col));
				sql.append(colName);
				Object value = aRow.getValue(col);
				if (value instanceof NullValue)
				{
					sql.append(" = NULL");
				}
				else
				{
					sql.append(" = ?");
					values.add(value);
				}
			}
		}
		sql.append("\n WHERE ");
		first = true;
		for (int j=0; j < this.pkColumns.size(); j++)
		{
			int pkcol = ((Integer)this.pkColumns.get(j)).intValue();
			if (first)
			{
				first = false;
			}
			else
			{
				sql.append(" AND ");
			}
			sql.append(SqlUtil.quoteObjectname(this.getColumnName(pkcol)));
			Object value = aRow.getOriginalValue(pkcol);
			if (value instanceof NullValue)
			{
				sql.append(" IS NULL");
			}
			else
			{
				sql.append(" = ?");
				values.add(value);
			}
		}
		try
		{
			dml = new DmlStatement(sql.toString(), values);
		}
		catch (Exception e)
		{
			dml = null;
			LogMgr.logError(this, "Error creating DmlStatement for " + sql.toString(), e);
		}
		return dml;
	}

	private DmlStatement createInsertStatement(RowData aRow, boolean ignoreStatus)
	{
		return this.createInsertStatement(aRow, ignoreStatus, "\n");
	}

	/**
	 *	Generate an insert statement for the given row
	 *	When creating a script for the DataStore the ignoreStatus
	 *	will be passed as true, thus ignoring the row status and
	 *	some basic formatting will be applied to the SQL Statement
	 */
	private DmlStatement createInsertStatement(RowData aRow, boolean ignoreStatus, String lineEnd)
	{
		boolean first = true;
		DmlStatement dml;

		if (!ignoreStatus && !aRow.isModified()) return null;
		//String lineEnd = System.getProperty("line.separator", "\r\n");
		//String lineEnd = "\n";
		boolean newLineAfterColumn = (this.colCount > 5);

		ArrayList values = new ArrayList();
		StringBuffer sql = new StringBuffer(250);
    sql.append("INSERT INTO ");
		StringBuffer valuePart = new StringBuffer(250);

		sql.append(SqlUtil.quoteObjectname(this.updateTable));
		if (ignoreStatus) sql.append(lineEnd);
		sql.append('(');
		if (newLineAfterColumn)
		{
			sql.append(lineEnd);
			sql.append("  ");
			valuePart.append(lineEnd);
			valuePart.append("  ");
		}

		first = true;
    String colName = null;
		int includedColumns = 0;

		for (int col=0; col < this.colCount; col ++)
		{
			if (ignoreStatus || aRow.isColumnModified(col))
			{
				if (first)
				{
					first = false;
				}
				else
				{
					if (newLineAfterColumn)
					{
						sql.append("  , ");
						valuePart.append("  , ");
					}
					else
					{
						sql.append(',');
						valuePart.append(',');
					}
				}

				colName = SqlUtil.quoteObjectname(this.getColumnName(col));
				sql.append(colName);
				valuePart.append('?');

				if (ignoreStatus && newLineAfterColumn)
				{
					sql.append(lineEnd);
					valuePart.append(lineEnd);
				}
				values.add(aRow.getValue(col));
			}
		}
		sql.append(')');
		if (ignoreStatus)
		{
			sql.append(lineEnd);
			sql.append("VALUES");
			sql.append(lineEnd);
			sql.append('(');
		}
		else
		{
			sql.append(" VALUES (");
		}
		sql.append(valuePart);
		sql.append(')');
		try
		{
			dml = new DmlStatement(sql.toString(), values);
		}
		catch (Exception e)
		{
			dml = null;
			LogMgr.logError(this, "Error creating DmlStatement for " + sql.toString(), e);
		}
		return dml;
	}

	private DmlStatement createDeleteStatement(RowData aRow)
	{
		if (aRow == null) return null;
		if (aRow.isNew()) return null;

		// don't create a statement for a row which was inserted and
		// then deleted
		//if (aRow.isNew()) return null;

		boolean first = true;
		DmlStatement dml;

		ArrayList values = new ArrayList();
		StringBuffer sql = new StringBuffer(250);
    sql.append("DELETE FROM ");
		sql.append(SqlUtil.quoteObjectname(this.updateTable));
		sql.append(" WHERE ");
		first = true;
		for (int j=0; j < this.pkColumns.size(); j++)
		{
			int pkcol = ((Integer)this.pkColumns.get(j)).intValue();
			if (first)
			{
				first = false;
			}
			else
			{
				sql.append(" AND ");
			}
			String colName = SqlUtil.quoteObjectname(this.getColumnName(pkcol));
			sql.append(colName);

			Object value = aRow.getOriginalValue(pkcol);
			if (value instanceof NullValue)
			{
				sql.append(" IS NULL");
			}
			else
			{
				sql.append(" = ?");
				values.add(value);
			}
		}
		try
		{
			dml = new DmlStatement(sql.toString(), values);
		}
		catch (Exception e)
		{
			dml = null;
			LogMgr.logError(this, "Error creating DELETE Statement for " + sql.toString(), e);
		}
		return dml;
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
				schema = aConnection.getMetadata().getSchemaForTable(this.updateTable);
			}
			catch (Exception e)
			{
				schema = null;
			}
		}
		this.updateTableSchema = schema;
		return schema;
	}

	public String getRealUpdateTable()
	{
		if (this.updateTable.indexOf('.') > 0)
		{
			return this.updateTable.substring(this.updateTable.lastIndexOf('.') + 1);
		}
		return this.updateTable;
	}

	private void updatePkInformation(WbConnection aConnection)
		throws SQLException
	{
		if (this.pkColumns != null) return;

		if (aConnection != null)
		{
			Connection sqlConn = aConnection.getSqlConnection();
			DatabaseMetaData meta = sqlConn.getMetaData();
			this.updateTable = aConnection.getMetadata().adjustObjectname(this.updateTable);
			String schema = this.getUpdateTableSchema(aConnection);

			int index;
			String col;
			ResultSet rs = meta.getPrimaryKeys(null, schema, this.getRealUpdateTable());
			ArrayList cols = this.readPkColumns(rs);

			// no primary keys found --> try the bestRowIdentifier...
			if (cols.size() == 0)
			{
				rs = meta.getBestRowIdentifier(null, null, this.updateTable, DatabaseMetaData.bestRowSession, false);
				cols = this.readPkColumns(rs);
			}
			this.pkColumns = cols;
		}
		if (this.pkColumns == null) this.pkColumns = new ArrayList();

		// if we didn't find any columns, use all columns as the identifier
		if (this.pkColumns.size() == 0)
		{
			for (int i=0; i < this.colCount; i++)
			{
				this.pkColumns.add(new Integer(i));
			}
		}
	}

	private boolean isPkColumn(int col)
	{
		if (this.pkColumns == null) return false;
		Integer key = new Integer(col);
		return this.pkColumns.contains(key);
	}

	private ArrayList readPkColumns(ResultSet rs)
	{
		ArrayList result = new ArrayList();
		String col = null;
		int index = 0;
		try
		{
			while (rs.next())
			{
				col = rs.getString("COLUMN_NAME");
				index = this.findColumn(col);
				Integer idx = new Integer(index);
				if (!result.contains(idx))
				{
					result.add(new Integer(index));
				}
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError(this, "Identifier column " + col + " not found in resultset! Using all rows as keys", e);
			result.clear();
		}
		finally
		{
			try { rs.close(); } catch (Exception e) {}
		}
		return result;
	}

	private void checkRowBounds(int aRow)
		throws IndexOutOfBoundsException
	{
		if (aRow < 0 || aRow > this.getRowCount() - 1) throw new IndexOutOfBoundsException("Row index " + aRow + " out of range ([0," + this.getRowCount() + "])");
	}
	private void checkColumnBounds(int aColumn)
		throws IndexOutOfBoundsException
	{
		if (aColumn < 0 || aColumn > this.colCount - 1) throw new IndexOutOfBoundsException("Column index " + aColumn + " out of range ([0," + this.colCount + "])");
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
	 * @param progressMonitor New value of property progressMonitor.
	 *
	 */
	public void setProgressMonitor(RowActionMonitor aMonitor)
	{
		this.rowActionMonitor = aMonitor;
	}

	class ColumnComparator implements Comparator
	{
		int column;
		boolean ascending;

		public ColumnComparator()
		{
		}

		public void setMode(int aCol, boolean aFlag)
		{
			this.column = aCol;
			this.ascending = aFlag;
		}

		public int compare(Object o1, Object o2)
		{
			try
			{
				RowData row1 = (RowData)o1;
				RowData row2 = (RowData)o2;
				return DataStore.this.compare(row1, row2, this.column, this.ascending);
			}
			catch (ClassCastException e)
			{
				// cannot happen
			}
			return 0;
		}
	}


}