package workbench.gui.sql;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import sun.jdbc.rowset.CachedRowSet;

import workbench.db.WbDataStore;
import workbench.db.WbConnection;
import workbench.exception.WbException;
import workbench.log.LogMgr;


/**
 *	Table model for displaying the contents of a SQL statement
 */
public class DwTableModel 
	extends AbstractTableModel
{
	//private ResultSet dbData;
	private CachedRowSet dataCache;
	private int rowCount = -1;
	private int colCount = -1;
	private ResultSetMetaData metaData = null;
	public static final String NOT_AVAILABLE = "(n/a)";
	private WbConnection dbConnection;
	private boolean isUpdateable = false;
	
	public DwTableModel(ResultSet aResultSet, WbConnection aConnection) 
		throws SQLException, WbException
	{
		this.rowCount = -1;
		this.colCount = -1;
		this.metaData = null;
		
		this.dataCache = new CachedRowSet();
		this.dataCache.populate(aResultSet);
		this.metaData = this.dataCache.getMetaData();
		this.rowCount = this.dataCache.size();
		this.dbConnection = aConnection;
		this.colCount = this.metaData.getColumnCount();
		this.checkUpdateable();
	}
		
	/**
	 *	Return the contents of the field at the given position
	 *	in the result set.
	 *	@parm row - The row to get. Counting starts at zero.
	 *	@parm col - The column to get. Counting starts at zero.
	 */
	public Object getValueAt(int row, int col)
	{
		try
		{
			Object result;
			this.dataCache.absolute(row + 1);
			result = this.dataCache.getObject(col + 1);
			return result;
		}
		catch (Exception e)
		{
			StringBuffer msg = new StringBuffer("Error @");
			msg.append(row);
			msg.append('/');
			msg.append(col);
			msg.append(" - ");
			if (e.getMessage() == null)
			{
				msg.append(e.getClass().getName());
			}
			else
			{
				msg.append(e.getMessage());
			}
			return msg.toString();
		}
	}	

	private void checkUpdateable()
	{
		try
		{
			String tableName = this.metaData.getTableName(1);
			if (tableName == null || tableName.length() == 0) 
			{
				this.isUpdateable = false;
				return;
			}
			for (int i=2; i <= this.colCount; i++)
			{
				String s = this.metaData.getTableName(i);
				if (s == null || !s.equalsIgnoreCase(tableName)) 
				{
					this.isUpdateable = false;
					return;
				}
			}
			this.isUpdateable = true;
			this.dataCache.setTableName(tableName);
		}
		catch (SQLException e)
		{
			this.isUpdateable = false;
		}
	}

	public boolean isUpdateable() { return this.isUpdateable; }
	
	public void setValueAt(Object aValue, int row, int column)
	{
		try
		{
			if (this.isUpdateable)
			{
				this.dataCache.absolute(row + 1);
				this.dataCache.updateObject(column + 1, aValue);
				this.dataCache.updateRow();
				fireTableDataChanged();
			}
		}
		catch (Exception e)
		{
			System.err.println("Error in setValueAt");
			e.printStackTrace(System.err);
		}
	}
	
	/**
	 *	Return the number of columns in the model.
	 */
	public int getColumnCount()
	{
		return this.colCount;
	}

	public int getColumnWidth(int aColumn)
	{
		try
		{
			return this.metaData.getColumnDisplaySize(aColumn + 1);
		}
		catch (Exception e)
		{
			return 10;
		}
	}
	
	public int getColumnType(int aColumn)
	{
		try
		{
			return this.metaData.getColumnType(aColumn + 1);
		}
		catch (Exception e)
		{
			return -1;
		}
	}		

	/**
	 *	Return type of the column as a string.
	 */
	public String getColumnTypeName(int aColumn)
	{
		try
		{
			return this.metaData.getColumnTypeName(aColumn + 1);
		}
		catch (Exception e)
		{
			return NOT_AVAILABLE;
		}
	}
	
	/**
	 *	Number of rows in the result set
	 */
	public int getRowCount() 
	{ 
		return this.rowCount;
	}

	public String getColumnClassName(int aColumn)
	{
		try
		{
			return this.metaData.getColumnClassName(aColumn + 1);
		}
		catch (Exception e)
		{
			return NOT_AVAILABLE;
		}
	}
	
	public Class getColumnClass(int aColumn)
	{
		int type = this.getColumnType(aColumn);
		switch (type)
		{
			case Types.BIGINT:
			case Types.DECIMAL:
			case Types.DOUBLE:
			case Types.FLOAT:
			case Types.INTEGER:
			case Types.NUMERIC:
			case Types.REAL:
			case Types.SMALLINT:
				return Number.class;
			case Types.CHAR:
			case Types.VARCHAR:
				return String.class;
			case Types.DATE:
				return java.util.Date.class;
			case Types.TIMESTAMP:
				return java.sql.Timestamp.class;
			default:
				return Object.class;
		}
	}
	
	
	/*
	public Object getTypedValue(int aColumn)
		throws SQLException
	{
		int type = this.getColumnType(aColumn);
		switch (type)
		{
			case Types.SMALLINT:
			case Types.INTEGER:
			case Types.BIGINT:
				return BigInteger.valueOf(this.dbData.getInt(aColumn + 1));
			case Types.DECIMAL:
			case Types.DOUBLE:
			case Types.FLOAT:
			case Types.NUMERIC:
			case Types.REAL:
				return this.dbData.getBigDecimal(aColumn + 1);
			case Types.CHAR:
			case Types.VARCHAR:
				return this.dbData.getString(aColumn + 1);
			case Types.DATE:
				return this.dbData.getDate(aColumn + 1);
			case Types.TIMESTAMP:
				return this.dbData.getTimestamp(aColumn + 1);
			default:
				return "";
		}
	}
	*/
	
	public void dispose()
	{
		try
		{
			if (this.dataCache != null)
			{
				this.dataCache.close();
				this.dataCache = null;
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError(this, "Error cleaning up dataCache", e);
		}
	}
	
	public void finalize()
	{
		this.dispose();
	}
	
	/** Return the name of the column as defined by the ResultSetData.
	 */	
	public String getColumnName(int aColumn)
	{
		try
		{
			return this.metaData.getColumnName(aColumn + 1);
		}
		catch (Exception e)
		{
			return NOT_AVAILABLE;
		}
	}
	
	public StringBuffer getRowData(int aRow)
	{
		StringBuffer result = new StringBuffer(this.colCount * 20);
		for (int c=1; c <= this.colCount; c++)
		{
			Object value = this.getValueAt(aRow, c);
			if (value != null) result.append(value.toString());
			if (c < colCount) result.append('\t');
		}
		return result;
	}
	
	public boolean isCellEditable(int row, int column)
	{
		return true;
	}
	
	public void saveChangesToDatabase()
		throws SQLException
	{
		this.dataCache.acceptChanges(this.dbConnection.getSqlConnection());
	}
	
}
