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
 * @author  sql.workbench@freenet.de
 * @version
 */
public class DataStore
{
  private int rowCount = 0;
	private int colCount = 0;
	private ArrayList data = null;
	private boolean updateable = false;
	private ArrayList deletedRows = null;
	
	// Cached ResultSetMetaData information
	private int[] columnTypes = null;
  private int[] columnSizes = null;
	private String[] columnTypeNames = null;
	private String[] columnNames = null;
	private String[] tableNames = null;
//	private String[] columnClassNames = null;

	public DataStore(int aColumnCount)
	{
		this.data = new ArrayList();
		this.colCount = aColumnCount;
	}
	
  public DataStore (ResultSet aResultSet)
		throws SQLException
  {
		if (aResultSet == null) return;
		this.storeMetaData(aResultSet);
		this.rowCount = 0;
		this.data = new ArrayList(100);
		while (aResultSet.next())
		{
			this.rowCount ++;
			RowData row = new RowData(this.colCount);
			for (int i=0; i < this.colCount; i++)
			{
				row.setValue(i, aResultSet.getObject(i+1));
			}
			this.data.add(row);
		}
  }
  
  public int getRowCount() { return this.data.size(); }
	public int getColumnCount() { return this.colCount; }
	
	public int getColumnType(int aColumn)
		throws IndexOutOfBoundsException
	{
		return this.columnTypes[aColumn];
	}
	
	public String getTableName(int aColumn)
		throws IndexOutOfBoundsException
	{
		return this.tableNames[aColumn];
	}
		
	public String getColumnName(int aColumn)
		throws IndexOutOfBoundsException
	{
		return this.columnNames[aColumn];
	}
	
	public String getColumnTypeName(int aColumn)
		throws IndexOutOfBoundsException
	{
		return this.columnTypeNames[aColumn];
	}

//	public String getColumnClassName(int aColumn)
//		throws IndexOutOfBoundsException
//	{
//		return this.columnClassNames[aColumn];
//	}

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
	
	public void setValue(int aRow, int aColumn, Object aValue)
		throws IndexOutOfBoundsException
	{
		RowData row = this.getRow(aRow);
		row.setValue(aColumn,aValue);
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
	
	public boolean isRowNew(int aRow)
		throws IndexOutOfBoundsException
	{
		RowData row = this.getRow(aRow);
		return row.isNew();
	}
	
	public int appendRow()
	{
		RowData row = new RowData(this.colCount);
		this.data.add(row);
		return this.getRowCount();
	}

	public boolean isUpdateable()
	{
		return false;
	}
	
	/**
	 *	We define the datastore to be updateable if
	 *	all columns in the resultset are selected from
	 *	the same table
	 */
	private void checkUpdateable()
	{
		try
		{
			// get the tablename for the first column
			// if that is not set, then the DataStore is not updateable anyway
			String tableName = this.getTableName(0);
			if (tableName == null || tableName.length() == 0) 
			{
				this.updateable = false;
				return;
			}
			
			for (int i=1; i <= this.colCount; i++)
			{
				String s = this.getTableName(i);
				if (s == null || !s.equalsIgnoreCase(tableName)) 
				{
					this.updateable = false;
					return;
				}
			}
			this.updateable = true;
		}
		catch (Exception e)
		{
			this.updateable = false;
		}
	}
	
	/* Private methods */
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
//			this.columnClassNames = new String[this.colCount];
			
			for (int i=0; i < this.colCount; i++)
			{
				this.columnTypes[i] = metaData.getColumnType(i + 1);
				this.columnTypeNames[i] = metaData.getColumnTypeName(i + 1);
				this.columnSizes[i] = metaData.getColumnDisplaySize(i + 1);
				this.columnNames[i] = metaData.getColumnName(i + 1);
				this.tableNames[i] = metaData.getTableName(i + 1);
				//this.columnClassNames[i] = metaData.getColumnClassName(i + 1);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError(this, "Error while retrieving ResultSetMetaData", e);
		}
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
	
}

