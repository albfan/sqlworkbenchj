/*
 * DataStore.java
 *
 * Created on 15. September 2001, 11:29
 */

package workbench.storage;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import workbench.WbManager;
import workbench.db.DbMetadata;
import workbench.db.WbConnection;
import workbench.exception.WbException;
import workbench.log.LogMgr;
import workbench.util.LineTokenizer;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;


/**
 * @author  workbench@kellerer.org
 */
public class DataStore
{
	// Needed for the status display in the table model
	public static final Integer ROW_MODIFIED = new Integer(RowData.MODIFIED);
	public static final Integer ROW_NEW = new Integer(RowData.NEW);
	public static final Integer ROW_ORIGINAL = new Integer(RowData.NOT_MODIFIED);

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
	private String[] columnNames;
	private String[] columnClassNames;
	
	private String updateTable;
	private String updateTableSchema;
	private ArrayList updateTableColumns;
	
	private WbConnection originalConnection;

	// used during sorting to speed up comparison
	private Class currentSortColumnClass;
	
	
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
  public DataStore (final ResultSet aResultSet, WbConnection aConn)
		throws SQLException
  {
		if (aResultSet == null) return;
		this.originalConnection = aConn;
		this.initData(aResultSet);
  }

	public DataStore(ResultSet aResult)
		throws SQLException
	{
		this(aResult, false);
	}
	/**
	 *	Create a DataStore based on the given ResultSet but do not 
	 *	add the data yet
	 */
	public DataStore(ResultSet aResult, boolean readData)
		throws SQLException
	{
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
	 * object. The datastore can be populated with the {@link #addRow(ResultSet)} method.
	 */
	public DataStore(ResultSetMetaData metaData, WbConnection aConn)
		throws SQLException
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
	
	public int getColumnType(int aColumn)
		throws IndexOutOfBoundsException
	{
		return this.columnTypes[aColumn];
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
				DbMetadata meta = this.originalConnection.getMetadata();
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
					String table = columns.getValue(i, 0).toString();
					this.updateTableColumns.add(table.toLowerCase());
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
		/*
		if (this.updateTableColumns != null)
		{
			String col = this.columnNames[aColumn].toLowerCase();
			if (!this.updateTableColumns.contains(col)) return;
		}
		*/
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
		return row.isNew();
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
		for (int c=0; c < count; c++)
		{
			RowData row = this.getRow(aRow);
			Object value = row.getValue(c);
			if (value != null) result.append(value.toString());
			if (c < count - 1) result.append(aDelimiter);
		}
		return result;
	}	
	
	public StringBuffer getHeaderString()
	{
		String delimit = WbManager.getSettings().getDefaultTextDelimiter();
		return this.getHeaderString(delimit);
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
		return this.getDataString("\t", aLineTerminator, includeHeaders);
	}
	
	public String getDataString(String aFieldDelimiter, String aLineTerminator, boolean includeHeaders)
	{
		int colCount = this.getColumnCount();
		int count = this.getRowCount();
		StringBuffer result = new StringBuffer(count * 250);
		if (includeHeaders)
		{
			result.append(this.getHeaderString(aFieldDelimiter));
			result.append(aLineTerminator);
		}
		for (int i=0; i < count; i++)
		{
			result.append(this.getRowDataAsString(i, aFieldDelimiter));
			result.append(aLineTerminator);
		}
		return result.toString();
	}
	
	public void reset()
	{
		this.reset(50);
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
		this.columnSizes = new int[this.colCount];
		this.columnNames = new String[this.colCount];

		for (int i=0; i < this.colCount; i++)
		{
			String name = metaData.getColumnName(i + 1);
			this.columnTypes[i] = metaData.getColumnType(i + 1);
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
	}
	
	private void initData(ResultSet aResultSet)
	{
		try
		{
			ResultSetMetaData metaData = aResultSet.getMetaData();
			this.initMetaData(metaData);
			
			this.data = new ArrayList(500);
			while (aResultSet.next())
			{
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
		catch (Exception e)
		{
			LogMgr.logError(this, "Error while retrieving ResultSetMetaData", e);
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
		List tables = SqlUtil.getTables(aSql);
		if (tables.size() != 1) return false;
		String table = (String)tables.get(0);
		this.setUpdateTable(table, aConn);
		return true;
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
		if (!this.canSaveAsSqlInsert()) return null;
		RowData data = this.getRow(aRow);
		DmlStatement stmt = this.createInsertStatement(data, true, aLineTerminator); 
		StringBuffer sql = new StringBuffer(stmt.getExecutableStatement(aConn));
		sql.append(";");
		return sql;
	}
	
	public String getDataAsSqlInsert()
		throws WbException, SQLException
	{
		return this.getDataAsSqlInsert("\n");
	}
	
	public String getDataAsSqlInsert(String aLineTerminator)
		throws WbException, SQLException
	{
		if (!this.canSaveAsSqlInsert()) return "";
		StringBuffer script = new StringBuffer(this.getRowCount() * 100);
		int count = this.getRowCount();
		for (int row = 0; row < count; row ++)
		{
			RowData data = this.getRow(row);
			DmlStatement stmt = this.createInsertStatement(data, true, aLineTerminator); 
			String sql = stmt.getExecutableStatement(this.originalConnection);
			script.append(sql);
			script.append(";");
			script.append(aLineTerminator);
			script.append(aLineTerminator);
		}
		return script.toString();
	}
	
	/**
	 *	Import a text file (tab separated) with a header row and no column mapping
	 *	into this datastore
	 * @param aFilename - The text file to import
	 */
	public void importData(String aFilename)
		throws FileNotFoundException
	{
		this.importData(aFilename, true, "\t", Collections.EMPTY_MAP);
	}
	
	/** 
	 * Import a text file (tab separated) with no column mapping
	 * into this datastore.
	 *
	 * @param aFilename - The text file to import
	 * @param hasHeader - wether the text file has a header row
	 */
	public void importData(String aFilename, boolean hasHeader)
		throws FileNotFoundException
	{
		this.importData(aFilename, hasHeader, "\t", Collections.EMPTY_MAP);
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
	public void importData(String aFilename, boolean hasHeader, String aColSeparator)
		throws FileNotFoundException
	{
		this.importData(aFilename, hasHeader, aColSeparator, Collections.EMPTY_MAP);
	}
	
	/** 	
	 *	Import a text file into this datastore.
	 * @param aFilename - The text file to import
	 * @param hasHeader - wether the text file has a header row
	 * @param aColSeparator - the separator for column data
	 * @param aColumnMapping - a mapping between columns in the text file and the datastore
	 */
	public void importData(String aFilename
	                     , boolean hasHeader
											 , String aColSeparator
											 , Map aColumnMapping)
		throws FileNotFoundException
	{
		BufferedReader in = new BufferedReader(new FileReader(aFilename));
		String line;
		List data;
		Object colData;
		boolean doMapping = (aColumnMapping != null && aColumnMapping.size() > 0);
		int col;
		int row;
		
		try
		{
			line = in.readLine();
			if (hasHeader) line = in.readLine();
		}
		catch (IOException e)
		{
			line = null;
		}
		while (line != null)
		{
			data = StringUtil.stringToList(line, aColSeparator);
			row = this.addRow();
			this.setRowNull(row);
			
			int count = data.size();
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
					try
					{
						colData = this.convertCellValue(data.get(i), col);
						this.setValue(row, col, colData);
					}
					catch (Exception e)
					{
						LogMgr.logWarning("DataStore.importData()","Error reading line #" + row + ",contents=" + line, e);
					}
				}
				
			}
			
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
	
	/**
	 * Save the changes to this datastore to the database.
	 * The changes are applied in the following order
	 * <ul>
	 * <li>Delete statements</li>
	 * <li>Insert statements</li>
	 * <li>Update statements</li>
	 * </ul>
	 */
	public int updateDb(WbConnection aConnection)
		throws WbException, SQLException
	{
		int rows = 0;
		List statements = this.getUpdateStatements(aConnection);
		if (statements.size() == 0) return 0;
		
		try
		{
			for (int i=0; i < statements.size(); i++)
			{
				DmlStatement stmt = (DmlStatement)statements.get(i);
				rows += stmt.execute(aConnection);
			}
			if (!aConnection.getAutoCommit()) aConnection.commit();
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

	public void resetStatus()
	{
		this.deletedRows = null;
		this.modified = false;
		for (int i=0; i < this.data.size(); i++)
		{
			RowData row = this.getRow(i);
			row.resetStatus();
		}
	}
	
	public int compareRowsByColumn(RowData row1, RowData row2, int column)
	{
		Object o1 = row1.getValue(column);
		Object o2 = row2.getValue(column);
		
		// If both values are null, return 0.
		if (o1 == null && o2 == null)
		{
			return 0;
		}
		else if (o1 == null)
		{// Define null less than everything.
			return -1;
		}
		else if (o2 == null)
		{
			return 1;
		}

		// first we try if the two objects implement
		// the comparable interface.
		try
		{
			int result = ((Comparable)o1).compareTo(o2);
			return result;
		}
		catch (Exception e)
		{
		}

		// if we wind up here, at least one of them did not implement
		// the comparable interface. This shouldn't actually happen but if it does,
		// we want to make sure they are compared in a decent manner.

		if (this.currentSortColumnClass.getSuperclass() == java.lang.Number.class)
		{
			Number n1 = (Number)o1;
			double d1 = n1.doubleValue();
			Number n2 = (Number)o2;
			double d2 = n2.doubleValue();
			if (d1 < d2)
			{
				return -1;
			}
			else if (d1 > d2)
			{
				return 1;
			}
			else
			{
				return 0;
			}
		}
		else if (this.currentSortColumnClass.isAssignableFrom(java.util.Date.class))
		{
			java.util.Date d1 = (java.util.Date)o1;;
			long n1 = d1.getTime();
			java.util.Date d2 = (java.util.Date)o2;
			long n2 = d2.getTime();
			if (n1 < n2)
			{
				return -1;
			}
			else if (n1 > n2)
			{
				return 1;
			}
			else
			{
				return 0;
			}
		}
		else if (this.currentSortColumnClass == String.class)
		{
			String s1 = (String)o1;
			String s2 = (String)o2;
			return s1.compareTo(s2);
		}
		else if (this.currentSortColumnClass == Boolean.class)
		{
			Boolean bool1 = (Boolean)o1;
			boolean b1 = bool1.booleanValue();
			Boolean bool2 = (Boolean)o2;
			boolean b2 = bool2.booleanValue();
			if (b1 == b2)
			{
				return 0;
			}
			else if (b1)
			{// Define false < true
				return 1;
			}
			else
			{
				return -1;
			}
		}
		else
		{
			String v1 = o1.toString();
			String v2 = o2.toString();
			return v1.compareTo(v2);
		}
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
		if (result != 0)
		{
			return ascending ? result : -result;
		}
		return 0;
	}
	
	
	public void sortByColumn(int aColumn, boolean ascending)
	{
		this.currentSortColumnClass = this.getColumnClass(aColumn);
		Collections.sort(this.data, new ColumnComparator(aColumn, ascending));
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
			case Types.BIGINT:
				return new BigInteger(aValue.toString());
			case Types.INTEGER:
			case Types.SMALLINT:
					return new Integer(aValue.toString());
			case Types.NUMERIC:
			case Types.DECIMAL:
				return new BigDecimal(aValue.toString());
			case Types.DOUBLE:
				return new Double((String)aValue);
			case Types.REAL:
			case Types.FLOAT:
				return new Float(aValue.toString());
			case Types.CHAR:
			case Types.VARCHAR:
				if (aValue instanceof String)
					return aValue;
				else
					return aValue.toString();
			case Types.DATE:
				DateFormat df = new SimpleDateFormat();
				return df.parse(((String)aValue).trim());
			case Types.TIMESTAMP:
				return Timestamp.valueOf(((String)aValue).trim());
			default:
				return aValue;
		}
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

	/**
	 * Returns a List of {@link #DmlStatements } which 
	 * would be executed in order to store the current content
	 * of the DataStore.
	 */
	public List getUpdateStatements(WbConnection aConnection)
		throws WbException, SQLException
	{
		if (this.updateTable == null) throw new WbException("No update table defined!");
		this.updatePkInformation(aConnection);
		ArrayList deletes  = new ArrayList();
		ArrayList updates = new ArrayList();
		ArrayList inserts = new ArrayList();
		RowData row;
		DmlStatement dml;
		for (int i=0; i < this.getRowCount(); i ++)
		{
			row = this.getRow(i);
			if (row.isModified() && !row.isNew())
			{
				dml = this.createUpdateStatement(row);
				if (dml != null) updates.add(dml);
			}
			else if (row.isNew() && row.isModified())
			{
				dml = this.createInsertStatement(row, false);
				if (dml != null) inserts.add(dml);
			}
		}

		if (this.deletedRows != null && this.deletedRows.size() > 0)
		{
			for (int i=0; i < this.deletedRows.size(); i++)
			{
				row = (RowData)this.deletedRows.get(i);
				if (!row.isNew())
				{
					dml = this.createDeleteStatement(row);
					if (dml != null) deletes.add(dml);
				}
			}
		}
		ArrayList stmt = new ArrayList();
		stmt.addAll(deletes);
		stmt.addAll(updates);
		stmt.addAll(inserts);
		return stmt;
	}
	
	private DmlStatement createUpdateStatement(RowData aRow)
	{
		return this.createUpdateStatement(aRow, false, "");
	}
	
	private DmlStatement createUpdateStatement(RowData aRow, boolean ignoreStatus, String lineEnd)
	{
		boolean first = true;
		DmlStatement dml;
		
		if (!aRow.isModified()) return null;
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
		boolean newLineAfterColumn = false; //this.colCount > 5;
		
		ArrayList values = new ArrayList();
		StringBuffer sql = new StringBuffer("INSERT INTO ");
		StringBuffer valuePart = new StringBuffer();
		sql.append(SqlUtil.quoteObjectname(this.updateTable));
		if (ignoreStatus) sql.append(lineEnd);
		sql.append('(');
		if (newLineAfterColumn) sql.append(lineEnd);
		
		first = true;
		for (int col=0; col < this.colCount; col ++)
		{
			if (ignoreStatus || aRow.isColumnModified(col))
			{
				if (col > 0)
				{
					sql.append(',');
					valuePart.append(',');
				}
				if (ignoreStatus && newLineAfterColumn)
				{
					sql.append("  ");
					valuePart.append("  ");
				}
				
				String colName = SqlUtil.quoteObjectname(this.getColumnName(col));
				sql.append(colName);
				
				valuePart.append('?');
				if (ignoreStatus && newLineAfterColumn) 
				{
					valuePart.append(lineEnd);
					sql.append(lineEnd);
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
			if (newLineAfterColumn) sql.append(lineEnd);
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
		
		// don't create a statement for a row which was inserted and 
		// then deleted
		//if (aRow.isNew()) return null;
		
		boolean first = true;
		DmlStatement dml;
		
		ArrayList values = new ArrayList();
		StringBuffer sql = new StringBuffer("DELETE FROM ");
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
		
		// if we didn't find any columns, so use all columns as the identifier
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
				result.add(new Integer(index));
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

	class ColumnComparator implements Comparator
	{
		int column;
		boolean ascending;
		public ColumnComparator(int col, boolean asc)
		{
			this.column = col;
			this.ascending = asc;
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

