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

import workbench.db.WbConnection;
import workbench.exception.WbException;
import workbench.log.LogMgr;
import workbench.storage.DataStore;


/**
 *	Table model for displaying the contents of a SQL statement
 */
public class DwTableModel 
	extends AbstractTableModel
{
	//private ResultSet dbData;
	private DataStore dataCache;
	public static final String NOT_AVAILABLE = "(n/a)";
	private WbConnection dbConnection;
	private boolean isUpdateable = false;
	
	public DwTableModel(ResultSet aResultSet)
		throws SQLException, WbException
	{
		this(aResultSet, null);
	}
	
	public DwTableModel(ResultSet aResultSet, WbConnection aConnection) 
		throws SQLException, WbException
	{
		this.dataCache = new DataStore(aResultSet);
		this.dbConnection = aConnection;
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
			//this.dataCache.absolute(row + 1);
			result = this.dataCache.getValue(row, col);
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

	public int findColumn(String aColname)
	{
		int index = -1;
		try
		{
			index = this.dataCache.getColumnIndex(aColname);
		}
		catch (SQLException e)
		{
			index = -1;
		}
		return index;
	}
	public boolean isUpdateable() { return this.dataCache.isUpdateable(); }
	
	public void setValueAt(Object aValue, int row, int column)
	{
		try
		{
			if (this.isUpdateable)
			{
				this.dataCache.setValue(row, column, aValue);
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
		return this.dataCache.getColumnCount();
	}

	public int getColumnWidth(int aColumn)
	{
		try
		{
			return this.dataCache.getColumnDisplaySize(aColumn);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return 10;
		}
	}
	
	public int getColumnType(int aColumn)
	{
		try
		{
			return this.dataCache.getColumnType(aColumn);
		}
		catch (Exception e)
		{
			e.printStackTrace();
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
			return this.dataCache.getColumnTypeName(aColumn);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return NOT_AVAILABLE;
		}
	}
	
	/**
	 *	Number of rows in the result set
	 */
	public int getRowCount() 
	{ 
		return this.dataCache.getRowCount();
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
	
	
	public void dispose()
	{
		this.dataCache = null;
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
			String name = this.dataCache.getColumnName(aColumn);
			if (name == null || name.length() == 0)
			{
				name = "Col" + (aColumn + 1);
			}
			return name;
		}
		catch (Exception e)
		{
			return NOT_AVAILABLE;
		}
	}
	
	public StringBuffer getRowData(int aRow)
	{
		int count = this.getColumnCount();
		StringBuffer result = new StringBuffer(count * 20);
		for (int c=0; c < count; c++)
		{
			Object value = this.getValueAt(aRow, c);
			if (value != null) result.append(value.toString());
			if (c < count) result.append('\t');
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
		//this.dataCache.acceptChanges(this.dbConnection.getSqlConnection());
	}
	
}
