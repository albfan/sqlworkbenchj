/*
 * DataStore.java
 *
 * Created on 15. September 2001, 11:29
 */

package workbench.storage;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import workbench.log.LogMgr;

/**
 *
 * @author  thomas
 * @version
 */
public class DataStore
{
  private int rowCount = 0;
	private int colCount = 0;
	private ArrayList data = null;
	private boolean updateable = false;
	private HashMap originalData = null;
  private int[] columnTypes = null;
  private int[] columnSizes = null;
	private String[] columnTypeNames = null;
	private String[] columnNames = null;
	private String[] tableNames = null;
	
  public DataStore (ResultSet aResultSet)
		throws SQLException
  {
		if (aResultSet == null) return;
		this.storeMetaData(aResultSet);
		this.rowCount = 0;
		this.data = new ArrayList();
		while (aResultSet.next())
		{
			this.rowCount ++;
			Object[] row = new Object[this.colCount];
			for (int i=0; i < this.colCount; i++)
			{
				row[i] = aResultSet.getObject(i+1);
			}
			this.data.add(rowCount - 1, row);
		}
  }
  
  public int getRowCount() { return this.rowCount; }
	public int getColumnCount() { return this.colCount; }
	
	private void storeMetaData(ResultSet aResultSet)
	{
		try
		{
			ResultSetMetaData metaData = aResultSet.getMetaData();
			this.colCount = metaData.getColumnCount();
			this.columnTypes = new int[this.colCount];
			this.columnTypeNames = new String[this.colCount];
			this.columnSizes = new int[this.colCount];
			this.columnNames = new String[this.colCount];
			this.tableNames = new String[this.colCount];
			
			for (int i=0; i < this.colCount; i++)
			{
				this.columnTypes[i] = metaData.getColumnType(i + 1);
				this.columnTypeNames[i] = metaData.getColumnTypeName(i + 1);
				this.columnSizes[i] = metaData.getColumnDisplaySize(i + 1);
				this.columnNames[i] = metaData.getColumnName(i + 1);
				this.tableNames[i] = metaData.getTableName(i + 1);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError(this, "Error while retrieving ResultSetMetaData", e);
		}
	}

	public int getColumnType(int aColumn)
		throws IndexOutOfBoundsException
	{
		this.checkColumnBounds(aColumn);
		return this.columnTypes[aColumn];
	}
	
	public String getColumnTypeName(int aColumn)
		throws IndexOutOfBoundsException
	{
		this.checkColumnBounds(aColumn);
		return this.columnTypeNames[aColumn];
	}

	public int getColumnDisplaySize(int aColumn)
		throws IndexOutOfBoundsException
	{
		this.checkColumnBounds(aColumn);
		return this.columnSizes[aColumn];
	}
	
	public Object getValue(int aRow, int aColumn)
		throws IndexOutOfBoundsException
	{
		this.checkRowBounds(aRow);
		this.checkColumnBounds(aColumn);
		Object[] row = (Object[])this.data.get(aRow);
		return row[aColumn];
	}
	
	public void setValue(int aRow, int aColumn, Object aValue)
	{
		this.checkRowBounds(aRow);
		this.checkColumnBounds(aColumn);
		
		Object[] row = (Object[])this.data.get(aRow);
		if (this.originalData == null)
		{
			this.originalData = new HashMap();
		}
		Integer key = new Integer(aRow);
		if (!this.originalData.containsKey(key))
		{
			this.originalData.put(key, row);
		}
		row[aColumn] = aValue;
	}

	private void checkRowBounds(int aRow)
		throws IndexOutOfBoundsException
	{
		if (aRow < 0 || aRow > this.rowCount - 1) throw new IndexOutOfBoundsException("Row index " + aRow + " out of range ([0," + this.rowCount + "])");
	}
	private void checkColumnBounds(int aColumn)
		throws IndexOutOfBoundsException
	{
		if (aColumn < 0 || aColumn > this.colCount - 1) throw new IndexOutOfBoundsException("Column index " + aColumn + " out of range ([0," + this.colCount + "])");
	}
	
	public int getColumnIndex(String aName)
		throws SQLException
	{
		return this.findColumn(aName);
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

}

