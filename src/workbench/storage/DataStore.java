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
import workbench.db.DbMetadata;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.util.LineTokenizer;
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
	/**
	 *	Create a DataStore based on the given ResultSet but do not
	 *	add the data yet
	 */
	public DataStore(ResultSet aResult, boolean readData) throws SQLException
	{
		this(aResult, readData, null);

	}
	public DataStore(ResultSet aResult, boolean readData, RowActionMonitor aMonitor) throws SQLException
	{
		this.rowActionMonitor = aMonitor;
		if (readData)
		{
			this.originalConnection = null;
			this.initData(aResult);
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
	 *	it into the delete buffer. So now DELETE statement
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
		this.updateTable = null;
		this.updateTableColumns = null;

		if (aSql == null) return false;
		List tables = SqlUtil.getTables(aSql);
		if (tables.size() != 1) return false;

		String table = (String)tables.get(0);
		this.useUpdateTable(table);
		return true;
	}

	public void useUpdateTable(String aTablename)
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
	}

	public void setUpdateTable(String aTablename, WbConnection aConn)
	{
		if (aTablename == null)
		{
			this.updateTable = null;
			this.updateTableColumns = null;
		}
		else if (!aTablename.equalsIgnoreCase(this.updateTable) && aConn != null)
		{
			this.pkColumns = null;
			this.updateTable = null;
			this.updateTableColumns = null;
			// now check the columns which are in that table
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
					String column = columns.getValue(i, 0).toString();
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

	public String getUpdateTable()
	{
		return this.updateTable;
	}

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
		//return row.colData[aColumn]; //getValue(aColumn);
		//return ((RowData)this.data.elementData[aRow]).colData[aColumn];
	}

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
			if (value instanceof java.util.Date && this.defaultDateFormatter != null)
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


	public int getValueAsInt(int aRow, int aColumn, int aDefault)
	{
		RowData row = this.getRow(aRow);
		Object value = row.getValue(aColumn);
    if (value == null || value instanceof NullValue)
      return aDefault;
    else if (value instanceof Number)
		{
      //return value.toString();
			return ((Number)value).intValue();
		}
		else
		{
			int result = aDefault;
			try { result = Integer.parseInt(value.toString()); } catch (Exception e) {}
			return result;
		}
	}

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

	public void setNull(int aRow, int aColumn)
	{
		NullValue nul = NullValue.getInstance(this.columnTypes[aColumn]);
		this.setValue(aRow, aColumn, nul);
	}

	public int getColumnIndex(String aName)
		throws SQLException
	{
		return this.findColumn(aName);
	}

	public boolean isRowModified(int aRow)
	{
		RowData row = this.getRow(aRow);
		return (row.isNew() && row.isModified() || row.isModified());
	}

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
		try
		{
			ResultSetMetaData metaData = aResultSet.getMetaData();
			this.initMetaData(metaData);
		}
		catch (SQLException e)
		{
			LogMgr.logError(this, "Error while retrieving ResultSetMetaData", e);
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
				if (this.rowActionMonitor != null)
				{
					rowCount ++;
					this.rowActionMonitor.setCurrentRow(rowCount, -1);
				}

				RowData row = new RowData(this.colCount);
				for (int i=0; i < this.colCount; i++)
				{
					Object value = aResultSet.getObject(i + 1);
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

	public StringBuffer getMetaDataAsXml()
	{
		return this.getMetaDataAsXml(null);
	}

	public StringBuffer getXmlStart()
	{
		return this.getXmlStart("UTF-8");
	}

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

	public StringBuffer getXmlEnd()
	{
		StringBuffer xml = new StringBuffer(100);
		xml.append("  </data>");
		xml.append(StringUtil.LINE_TERMINATOR);
		xml.append("</wb-export>");
		xml.append(StringUtil.LINE_TERMINATOR);
		return xml;
	}

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

	public StringBuffer getRowDataAsXml(int aRow)
	{
		return this.getRowDataAsXml(aRow, null);
	}

	public StringBuffer getRowDataAsXml(int aRow, String anIndent)
	{
		return this.getRowDataAsXml(aRow, anIndent, aRow + 1);
	}

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

	public void writeHtmlData(Writer html)
		throws IOException
	{
		int count = this.getRowCount();
		if (count == 0) return;
		html.write("<style type=\"text/css\">\n");
		html.write("<!--\n");
		html.write("  table { border-spacing:0; border-left-style:solid; border-left-width:1px; border-bottom-style:solid; border-bottom-width:1px;}\n");
		html.write("  td { padding:2; border-top-style:solid;border-top-width:1px;border-right-style:solid;border-right-width:1px;}\n");
		html.write("  .number-cell { text-align:right; } \n");
		html.write("  .text-cell { text-align:left; } \n");
		html.write("-->\n</style>\n");
		html.write("<table>\n");

		// table header with column names
		html.write("  <tr>\n      ");
		for (int c=0; c < this.getColumnCount(); c ++)
		{
			html.write("<td><b>");
			html.write(this.getColumnName(c));
			html.write("</b></td>");
		}
		html.write("\n  </tr>\n");
		for (int i=0; i < count; i++)
		{
			html.write("  <tr>\n      ");
			for (int c=0; c < this.getColumnCount(); c ++)
			{
				String value = this.getValueAsString(i, c);
				int type = this.getColumnType(c);
				if (SqlUtil.isNumberType(type) || SqlUtil.isDateType(type))
					html.write("<td class=\"number-cell\">");
				else
					html.write("<td class=\"text-cell\">");
				if (value == null)
				{
					html.write("&nbsp;");
				}
				else
				{
					html.write(StringUtil.escapeHTML(value));
				}
				html.write("</td>");
			}
			html.write("\n  </tr>\n");
		}
		html.write("</table>\n");
	}

	public boolean canSaveAsSqlInsert()
	{
		if (this.updateTable == null)
			return this.checkUpdateTable();
		else
			return true;
	}

	public StringBuffer getRowDataAsSqlInsert(int aRow, WbConnection aConn)
	{
		return this.getRowDataAsSqlInsert(aRow, "\n", aConn);
	}

	public StringBuffer getRowDataAsSqlInsert(int aRow, String aLineTerminator, WbConnection aConn)
	{
		return this.getRowDataAsSqlInsert(aRow, aLineTerminator, aConn, null, null);
	}
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

	public void writeDataAsSqlInsert(Writer out, String aLineTerminator)
		throws IOException
	{
		if (!this.canSaveAsSqlInsert()) return;
		int count = this.getRowCount();
		for (int row = 0; row < count; row ++)
		{
			RowData data = this.getRow(row);
			DmlStatement stmt = this.createInsertStatement(data, true, aLineTerminator);
			String sql = stmt.getExecutableStatement(this.originalConnection.getSqlConnection());
			out.write(sql);
			out.write(";");
			out.write(aLineTerminator);
			out.write(aLineTerminator);
		}
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

	/**
	 *	Import a text file (tab separated) with a header row and no column mapping
	 *	into this DataStore
	 * @param aFilename - The text file to import
	 */
	public void importData(String aFilename)
		throws FileNotFoundException
	{
		this.importData(aFilename, true, "\t", "\"", Collections.EMPTY_MAP);
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
		this.importData(aFilename, hasHeader, "\t", quoteChar, Collections.EMPTY_MAP);
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

	public void importData(String aFilename, boolean hasHeader, String aColSeparator, String aQuoteChar)
		throws FileNotFoundException
	{
		this.importData(aFilename, hasHeader, aColSeparator, aQuoteChar, Collections.EMPTY_MAP);
	}

	private boolean cancelUpdate = false;
	private boolean cancelImport = false;

	public void cancelUpdate()
	{
		this.cancelUpdate = true;
	}
	public void cancelImport()
	{
		this.cancelImport = true;
	}

	/**
	 *	Import a text file into this DataStore.
	 * @param aFilename - The text file to import
	 * @param hasHeader - wether the text file has a header row
	 * @param aColSeparator - the separator for column data
	 * @param aColumnMapping - a mapping between columns in the text file and the DataStore
	 */
	public void importData(String aFilename
	                     , boolean hasHeader
											 , String aColSeparator
											 , String aQuoteChar
											 , Map aColumnMapping)
		throws FileNotFoundException
	{
		File f = new File(aFilename);
		long fileSize = f.length();
		BufferedReader in = new BufferedReader(new FileReader(aFilename),1024*512);
		String line;
		List lineData;
		Object colData;
		boolean doMapping = (aColumnMapping != null && aColumnMapping.size() > 0);
		int col;
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
		// Event if we don't get the number of lines correct, this method should be better
		// then not initializing the array at all.
		if (line != null && this.data.size() == 0)
		{
			int initialSize = (int)(fileSize / line.length());
			this.data = new ArrayList(initialSize);
		}
		lineData = new ArrayList(this.colCount);
		WbStringTokenizer tok = new WbStringTokenizer(aColSeparator.charAt(0), "", false);
		int importRow = 0;
		while (line != null)
		{
			lineData.clear();
			tok.setSourceString(line);
			while (tok.hasMoreTokens())
			{
				lineData.add(tok.nextToken());
			}

			row = this.addRow();
			importRow ++;
			this.updateProgressMonitor(importRow, -1);

			this.setRowNull(row);

			int count = lineData.size();
			for (int i=0; i < count; i++)
			{
				if (doMapping)
				{
					Integer key = new Integer(i);
					Integer target = (Integer)aColumnMapping.get(key);
					if (target != null)
					{
						col = target.intValue();
					}
					else
					{
						col = -1;
					}
				}
				else
				{
					col = i;
				}
				if (col > -1)
				{
					Object value = null;
					try
					{
						value = lineData.get(i);
						if (value == null)
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

	/**
	 * Save the changes to this DataStore to the database.
	 * The changes are applied in the following order
	 * <ul>
	 * <li>Delete statements</li>
	 * <li>Insert statements</li>
	 * <li>Update statements</li>
	 * </ul>
	 */
	public synchronized int updateDb(WbConnection aConnection)
		throws SQLException
	{
		int rows = 0;
		this.updatePkInformation(aConnection);
		int totalRows = this.getModifiedCount();
		int currentRow = 0;
		if (this.rowActionMonitor != null)
		{
			this.rowActionMonitor.setMonitorType(RowActionMonitor.MONITOR_UPDATE);
		}

		try
		{
			this.resetUpdateRowCounters();
			RowData row = this.getNextDeletedRow();
			DmlStatement dml = null;
			while (row != null)
			{
				currentRow ++;
				this.updateProgressMonitor(currentRow, totalRows);
				if (!row.isDmlSent())
				{
					dml = this.createDeleteStatement(row);
					rows += dml.execute(aConnection);
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
					rows += dml.execute(aConnection);
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
					rows += dml.execute(aConnection);
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
			result.put(name, value);
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
		DmlStatement dml;

		if (!ignoreStatus && !aRow.isModified()) return null;
		ArrayList values = new ArrayList();
		StringBuffer sql = new StringBuffer("UPDATE ");

		sql.append(SqlUtil.quoteObjectname(this.updateTable));
		sql.append(" SET ");
		first = true;
		for (int col=0; col < this.colCount; col ++)
		{
			if (aRow.isColumnModified(col) || ignoreStatus)
			{
				if (first)
				{
					first = false;
				}
				else
				{
					sql.append(", ");
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
		LineTokenizer tok = new LineTokenizer(this.updateTable, ".");
		String schema = null;

		if (tok.countTokens() > 1)
		{
			schema = tok.nextToken();
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
