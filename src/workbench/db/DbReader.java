/*
 * DBReader.java
 *
 * Created on September 24, 2001, 11:25 PM
 */

package workbench.db;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import sun.jdbc.rowset.CachedRowSet;
import java.lang.ArrayIndexOutOfBoundsException;
import java.lang.StringBuffer;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSetMetaData;

import workbench.exception.WbException;
import workbench.log.LogMgr;

/**
 * Read the result set for a SQL statement from the database.
 *
 * @author  thomas
 * @version 1.0
 */
public class DbReader
{
	private String sqlStatement = null;
	protected CachedRowSet data = null;
	private Connection dbConnection = null;
	private int rowCount = -1;
	private int colCount = -1;
	private ResultSetMetaData metaData = null;
	public static final String NOT_AVAILABLE = "(n/a)";
	
	/**	
	 *	Create a DBReader instance for the given statement.
	 *	The connection must be provided when the load method is called!
	 *	
	 *	@see #load(Connection)
	 */
	public DbReader(String aStatement) 
		throws SQLException
	{
		this(aStatement, null);
	}
	
	/**
	 *	Create an instance of DBReader for the given connection.
	 *	The load() method can be called without specifying the
	 *	connection again.
	 *
	 *	@see #load()
	 */
	public DbReader(String aStatement, WbConnection aConn) 
		throws SQLException
	{
		this.data = new CachedRowSet();
		this.sqlStatement = aStatement;
		this.dbConnection = aConn.getSqlConnection();
		this.data.setCommand(this.sqlStatement);
	}
	
	/**
	 *	Load the data from the database
	 */
	public int load()
		throws SQLException, WbException
	{
		if (this.dbConnection == null) throw new WbException("Missing java.sql.Connection!");
		this.load(this.dbConnection);
		return this.getRowCount();
	}
	
	/**
	 *	Load the date from the database
	 */
	public int load(Connection aConn)
		throws SQLException
	{
		this.rowCount = -1;
		this.colCount = -1;
		this.metaData = null;
		
		this.data.execute(aConn);
		this.metaData = this.data.getMetaData();
		this.colCount = this.metaData.getColumnCount();

		return this.getRowCount();
	}

	/**
	 *	Return the columns maximal widht in characters.
	 */
	public int getColumnWidth(int aColumn)
	{
		try
		{
			return this.metaData.getColumnDisplaySize(aColumn);
		}
		catch (Exception e)
		{
			return 10;
		}
	}
	/**
	 *	Return the tpye for the given column.
	 *	@see java.sql.Types
	 */
	public int getColumnType(int aColumn)
	{
		try
		{
			return this.metaData.getColumnType(aColumn);
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
			return this.metaData.getColumnTypeName(aColumn);
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
		if (this.data == null) return 0;
		if (this.rowCount == -1)
		{
			this.rowCount = this.data.size();
		}
		return this.rowCount;
	}

	/**
	 *	Return the value for the given column in the given row.
	 */
	public Object getValue(int aRow, int aColumn)
		throws ArrayIndexOutOfBoundsException, SQLException
	{
		if (aRow < 0 || aRow > this.data.size()) throw new ArrayIndexOutOfBoundsException("Invalid row (" + aRow + ") specified [" + this.data.size() + "]");
		this.data.absolute(aRow);
		Object result = this.data.getObject(aColumn);
		return result;
	}
	
	/**
	 *	Return the value for the named column in the given row.
	 */
	public Object getValue(int aRow, String aColumn)
		throws ArrayIndexOutOfBoundsException, SQLException
	{
		if (aRow < 0 || aRow > this.data.size()) throw new ArrayIndexOutOfBoundsException("Invalid row (" + aRow + ") specified [" + this.data.size() + "]");
		this.data.absolute(aRow);
		Object result = this.data.getObject(aColumn);
		return result;
	}

	/**
	 *	Return the number of columns for this result set.
	 */
	public int getColumnCount()
		throws SQLException
	{
		try
		{
			if (this.colCount == -1)
			{
				if (this.metaData == null)
				{
					this.metaData = this.data.getMetaData();
				}
				this.colCount = this.metaData.getColumnCount();
			}
			return this.colCount;
		}
		catch (Exception e)
		{
			return 0;
		}
	}
	
	/**
	 *	Return the name for the given column
	 */
	public String getColumnName(int aColumn)
	{
		try
		{
			String name = this.metaData.getColumnLabel(aColumn);
			if (name == null || name.length() == 0)
			{
				name = this.metaData.getColumnName(aColumn);
			}
			if (name == null || name.length() == 0) 
			{
				name = "Col" + aColumn;
			}
			return name;
		}
		catch (Exception e)
		{
			return NOT_AVAILABLE;
		}
	}
	
	public String getRowString(int aRow)
	{
		return this.getRowData(aRow).toString();
	}
	
	public StringBuffer getRowData(int aRow)
	{
		StringBuffer result = new StringBuffer(this.colCount * 20);
		for (int c=1; c <= this.colCount; c++)
		{
			try
			{
				Object value = this.getValue(aRow, c);
				if (value != null) result.append(value.toString());
				if (c < colCount) result.append('\t');
			}
			catch (SQLException e)
			{
				LogMgr.logError(this, "getRowString() - Could not retrieve value for row " + aRow, e);
			}
		}
		return result;
	}

	public String getDataString()
	{
		int rows = this.getRowCount();
		StringBuffer contents = new StringBuffer(this.rowCount * 100); 
		for (int r=1; r <= rows; r ++)
		{
			contents.append(this.getRowData(r));
			contents.append('\r');
		}
		return contents.toString();
	}
	
	public void saveAsAscii(PrintWriter out)
		throws IOException
	{
		int rows = this.getRowCount();
		for (int r=1; r <= rows; r ++)
		{
			out.println(this.getRowString(r));
		}
		out.flush();
	}
	
	public static void main(String args[])
	{
		System.getProperties().list(System.out);
	}
}
