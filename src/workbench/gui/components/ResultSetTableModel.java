package workbench.gui.components;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import workbench.db.WbConnection;
import workbench.exception.WbException;
import workbench.log.LogMgr;
import workbench.storage.DataStore;


/** 	TableModel for displaying the contents of a ResultSet.
 * 	The data is cached in a DataStore.
 */
public class ResultSetTableModel 
	extends AbstractTableModel
{
	private DataStore dataCache;
	public static final String NOT_AVAILABLE = "(n/a)";
	
	public ResultSetTableModel(ResultSet aResultSet) 
		throws SQLException, WbException
	{
		this(aResultSet, null);
	}
	
	public ResultSetTableModel(ResultSet aResultSet, List aColumnList) 
		throws SQLException, WbException
	{
		this.dataCache = new DataStore(aResultSet, aColumnList);
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
			if (this.isUpdateable())
			{
				Object realValue = this.convertCellValue(aValue, row, column);
				this.dataCache.setValue(row, column, realValue);
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
	 *	Convert a String to the internal storage class for this cell.
	 */
	private Object convertCellValue(Object aValue, int aRow, int aColumn)
		throws Exception
	{
		Object lastValue = this.getValueAt(aRow, aColumn);
		int type = this.getColumnType(aColumn);
		switch (type)
		{
			case Types.BIGINT:
				return new BigInteger((String)aValue);
			case Types.INTEGER:
			case Types.SMALLINT:
				return Integer.valueOf((String)aValue);
			case Types.NUMERIC:
			case Types.DECIMAL:
				return new BigDecimal((String)aValue);
			case Types.DOUBLE:
				return new Double((String)aValue);
			case Types.REAL:
			case Types.FLOAT:
				return new Float((String)aValue);
			case Types.CHAR:
			case Types.VARCHAR:
				return (String)aValue;
			case Types.DATE:
				DateFormat df = new SimpleDateFormat();
				return df.parse((String)aValue);
			case Types.TIMESTAMP:
				return java.sql.Timestamp.valueOf((String)aValue);
			default:
				return aValue;
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
			return 100;
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
			return Types.VARCHAR;
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
	
	public void saveChangesToDatabase(WbConnection aConnection)
		throws SQLException
	{
		//this.dataCache.acceptChanges(this.dbConnection.getSqlConnection());
	}
	
}
