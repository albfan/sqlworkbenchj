/*
 * DataStore.java
 *
 * Created on 15. September 2001, 11:29
 */

package workbench.storage;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import workbench.db.WbConnection;
import workbench.exception.WbException;
import workbench.log.LogMgr;

/**
 *
 * @author  sql.workbench@freenet.de
 * @version
 */
public class DataStore
{
	private int colCount;
	private ArrayList data;
	private ArrayList pkColumns;
	private ArrayList deletedRows;
	private String updateTable;
	
	// Cached ResultSetMetaData information
	private int[] columnTypes;
  private int[] columnSizes;
	private String[] columnNames;
	private String tableName;
	private String[] columnClassNames = null;

	public static final Object NULL_VALUE = new NullValue();
	
	public DataStore(int aColumnCount)
	{
		this.data = new ArrayList();
		this.colCount = aColumnCount;
	}
	
	public DataStore(ResultSet aResultSet)
		throws SQLException
	{
		this(aResultSet, null);
	}
	
  public DataStore (ResultSet aResultSet, List aColumnList)
		throws SQLException
  {
		if (aResultSet == null) return;
		this.initData(aResultSet, aColumnList);
  }
  
  public int getRowCount() { return this.data.size(); }
	public int getColumnCount() { return this.colCount; }
	
	public int getColumnType(int aColumn)
		throws IndexOutOfBoundsException
	{
		return this.columnTypes[aColumn];
	}
	
	public void setUpdateTable(String aTablename)
	{
		if (aTablename == null)
		{
			this.updateTable = null;
		}
		else if (!aTablename.equalsIgnoreCase(this.updateTable))
		{
			this.updateTable = aTablename;
			this.pkColumns = null;
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
		return (this.updateTable != null);
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
	
	private void initData(ResultSet aResultSet, List aColumnList)
	{
		try
		{
			ResultSetMetaData metaData = aResultSet.getMetaData();
			int realColCount = metaData.getColumnCount();
			if (aColumnList == null)
			{
				this.colCount = realColCount;
			}
			else
			{
				this.colCount = aColumnList.size();
			}
			int col = 0;
			this.columnTypes = new int[this.colCount];
			this.columnSizes = new int[this.colCount];
			this.columnNames = new String[this.colCount];
			int[] colmapping = new int[realColCount];
			
			for (int i=0; i < realColCount; i++)
			{
				colmapping[i] = -1;
				String name = metaData.getColumnName(i + 1);
				if (aColumnList  == null || aColumnList.contains(name))
				{
					colmapping[i] = col;
					this.columnTypes[col] = metaData.getColumnType(i + 1);
					this.columnSizes[col] = metaData.getColumnDisplaySize(i + 1);
					this.columnNames[col] = name;
					col ++;
				}
			}
			
			this.data = new ArrayList(150);
			while (aResultSet.next())
			{
				RowData row = new RowData(this.colCount);
				for (int i=0; i < realColCount; i++)
				{
					if (colmapping[i] > -1)
					{
						Object value = aResultSet.getObject(i + 1);
						if (aResultSet.wasNull())
						{
							row.setNull(colmapping[i]);
						}
						else
						{
							row.setValue(colmapping[i], value);
						}
					}
				}
				row.resetStatus();
				this.data.add(row);
			}
			
		}
		catch (Exception e)
		{
			LogMgr.logError(this, "Error while retrieving ResultSetMetaData", e);
		}
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
		try
		{
			for (int i=0; i < statements.size(); i++)
			{
				DmlStatement stmt = (DmlStatement)statements.get(i);
				rows += stmt.execute(aConnection);
			}
			if (!aConnection.getAutoCommit()) aConnection.commit();
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

	/**
	 * Returns a List of {@link #DmlStatements } which 
	 * would be executed if to store the current content
	 * of the DataStore.
	 */
	public List getUpdateStatements(WbConnection aConnection)
		throws WbException, SQLException
	{
		if (this.updateTable == null) throw new WbException("No update table defined!");
		this.updatePkInformation(aConnection);
		ArrayList stmt = new ArrayList();
		stmt.addAll(this.createDeleteStatements());
		stmt.addAll(this.createUpdateStatements());
		stmt.addAll(this.createInsertStatements());
		return stmt;
	}
	
	private List createUpdateStatements()
	{
		ArrayList result = new ArrayList();
		RowData row;
		boolean first = true;
		for (int i=0; i < this.getRowCount(); i ++)
		{
			row = this.getRow(i);
			if (row.isModified())
			{
				ArrayList values = new ArrayList();
				StringBuffer sql = new StringBuffer("UPDATE ");
				sql.append(this.updateTable);
				sql.append(" SET ");
				first = true;
				for (int col=0; col < this.colCount; col ++)
				{
					if (row.isColumnModified(col))
					{
						if (first)
						{
							first = false;
						}
						else 
						{
							sql.append(", ");
						}
						sql.append(this.getColumnName(col));
						sql.append(" = ?");
						values.add(row.getValue(col));
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
					sql.append(this.getColumnName(pkcol));
					sql.append(" = ?");
					values.add(row.getOriginalValue(pkcol));
				}
				try
				{
					DmlStatement dml = new DmlStatement(sql.toString(), values);
					result.add(dml);
				}
				catch (Exception e)
				{
					LogMgr.logError(this, "Error creating DmlStatement for " + sql.toString(), e);
				}
			}
		}
		return result;
	}
	
	private List createInsertStatements()
	{
		return Collections.EMPTY_LIST;
	}
	
	private List createDeleteStatements()
	{
		return Collections.EMPTY_LIST;
	}
	
	private void updatePkInformation(WbConnection aConnection)
		throws SQLException
	{
		if (this.pkColumns != null) return;
		Connection sqlConn = aConnection.getSqlConnection();
		DatabaseMetaData meta = sqlConn.getMetaData();
		if (meta.storesUpperCaseIdentifiers()) 
		{
			this.updateTable = this.updateTable.toUpperCase();
		}
		else if (meta.storesLowerCaseIdentifiers())
		{
			this.updateTable = this.updateTable.toLowerCase();
		}
		ResultSet rs = meta.getBestRowIdentifier(null, null, this.updateTable, DatabaseMetaData.bestRowSession, false);
		this.pkColumns = new ArrayList();
		int index;
		String col;
		while (rs.next())
		{
			col = rs.getString("COLUMN_NAME");
			try
			{
				index = this.findColumn(col);
				this.pkColumns.add(new Integer(index));
			}
			catch (SQLException e)
			{
				LogMgr.logError(this, "Identifier column " + col + " not found in resultset! Using all rows as keys", e);
				this.pkColumns.clear();
				break;
			}
		}
		rs.close();
		if (this.pkColumns.size() == 0)
		{
			for (int i=0; i < this.colCount; i++)
			{
				this.pkColumns.add(new Integer(i));
			}
		}
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

	private static class NullValue
	{
		public String toString() { return null; }
	}
	
	public static void main(String args[])
	{
		try
		{
			Class.forName("oracle.jdbc.OracleDriver");
			Connection con = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:oradb", "test", "test");
			WbConnection wb = new WbConnection(con);
			try
			{
				Statement stmt = con.createStatement();
				ResultSet rs = stmt.executeQuery("select nr, name from test");
				DataStore ds = new DataStore(rs);
				rs.close();
				ds.setUpdateTable("test");
				ds.setValue(0, 0, new Integer(42));
				ds.setValue(0, 0, new Integer(22));
				ds.setValue(0, 1, "Ein neuer name");
				List l = ds.getUpdateStatements(wb);
				for (int i=0; i < l.size(); i ++)
				{
					System.out.println(l.get(i));
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			con.commit();
			con.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

}

