/*
 * DataStore.java
 *
 * Created on 15. September 2001, 11:29
 */

package workbench.storage;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import workbench.WbManager;
import workbench.db.WbConnection;
import workbench.exception.WbException;
import workbench.log.LogMgr;
import workbench.util.SqlUtil;

/**
 *
 * @author  workbench@kellerer.org
 * @version
 */
public class DataStore
{
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
	private ArrayList updateTableColumns;

	
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
	/**
	 *	Create a DataStore based on the contents of the given
	 *	ResultSet. All columns of that ResultSet will be cached/used.
	 */
	public DataStore(ResultSet aResultSet)
		throws SQLException
	{
		this(aResultSet, null);
	}
	
	/**
	 *	Create a DataStore where only those columns are used
	 *	which are listed in the given List
	 */
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

	public void deleteRow(int aRow)
		throws IndexOutOfBoundsException
	{
		Object row = this.data.get(aRow);
		if (this.deletedRows == null) this.deletedRows = new ArrayList();
		this.deletedRows.add(row);
		this.data.remove(aRow);
		this.modified = true;
	}
	
	public int addRow()
	{
		RowData row = new RowData(this.colCount);
		this.data.add(row);
		this.modified = true;
		return this.getRowCount() - 1;
	}
	
	public int insertRowAfter(int anIndex)
	{
		RowData row = new RowData(this.colCount);
		anIndex ++;
		int newIndex = -1;
		
		if (anIndex > this.data.size())
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
		if (aTablename == null)
		{
			this.updateTable = null;
			this.updateTableColumns = null;
		}
		else if (!aTablename.equalsIgnoreCase(this.updateTable))
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
			DataStore columns = WbManager.getInstance().getConnectionMgr().getTableDefinition(aTablename);
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
		// do not allow setting the value for columns
		// which do not have a name. Those columns cannot 
		// be saved to the database (because most likely they 
		// are computed columns like count(*) etc)
		if (this.columnNames[aColumn] == null) return;
		
		// If an updatetable is defined, we only accept
		// values for columns in that table 
		if (this.updateTableColumns != null)
		{
			String col = this.columnNames[aColumn].toLowerCase();
			if (!this.updateTableColumns.contains(col)) return;
		}
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
	
	public void reset()
	{
		this.data = new ArrayList(50);
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
					if (name != null && name.trim().length() > 0) this.realColumns ++;
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
						int realCol = colmapping[i];
						if (aResultSet.wasNull() || value == null)
						{
							row.setNull(realCol, this.columnTypes[realCol]);
						}
						else
						{
							row.setValue(realCol, value);
						}
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
	
	public boolean checkUpdateTable()
	{
		if (this.sql == null) return false;
		return this.checkUpdateTable(this.sql);
	}
			
	public boolean checkUpdateTable(String aSql)
	{
		List tables = SqlUtil.getTables(aSql);
		if (tables.size() != 1) return false;
		String table = (String)tables.get(0);
		this.setUpdateTable(table);
		return true;
	}
	
	public boolean canSaveAsSqlInsert()
	{
		if (this.updateTable == null)
			return this.checkUpdateTable();
		else
			return true;
	}
	
	public String getDataAsSqlInsert()
		throws WbException, SQLException
	{
		if (!this.canSaveAsSqlInsert()) return "";
		StringBuffer script = new StringBuffer(this.getRowCount() * 100);
		int count = this.getRowCount();
		for (int row = 0; row < count; row ++)
		{
			RowData data = this.getRow(row);
			DmlStatement stmt = this.createInsertStatement(data, true); 
			String sql = stmt.getExecutableStatement();
			script.append(sql);
			script.append(";");
			script.append("\n\n");
		}
		return script.toString();
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
	
	public Object convertCellValue(Object aValue, int aColumn)
		throws Exception
	{
		int type = this.getColumnType(aColumn);
		if (aValue == null)
		{
			return NullValue.getInstance(type);
		}
		System.out.println("converting " + aValue.getClass().getName());
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
		boolean first = true;
		DmlStatement dml;
		
		if (!aRow.isModified()) return null;
		ArrayList values = new ArrayList();
		StringBuffer sql = new StringBuffer("UPDATE ");
		
		sql.append(this.updateTable);
		sql.append(" SET ");
		first = true;
		for (int col=0; col < this.colCount; col ++)
		{
			if (aRow.isColumnModified(col))
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
			sql.append(this.getColumnName(pkcol));
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
	
	/**
	 *	Generate an insert statement for the given row
	 *	When creating a script for the DataStore the ignoreStatus
	 *	will be passed as true, thus ignoring the row status and
	 *	some basic formatting will be applied to the SQL Statement
	 */
	private DmlStatement createInsertStatement(RowData aRow, boolean ignoreStatus)
	{
		boolean first = true;
		DmlStatement dml;
		
		if (!ignoreStatus && !aRow.isModified()) return null;
		//String lineEnd = System.getProperty("line.separator", "\r\n");
		String lineEnd = "\n";
		boolean newLineAfterColumn = false; //this.colCount > 5;
		
		ArrayList values = new ArrayList();
		StringBuffer sql = new StringBuffer("INSERT INTO ");
		StringBuffer valuePart = new StringBuffer();
		sql.append(this.updateTable);
		if (ignoreStatus) sql.append(lineEnd);
		sql.append('(');
		if (newLineAfterColumn) sql.append(lineEnd);
		
		first = true;
		for (int col=0; col < this.colCount; col ++)
		{
			if (ignoreStatus || aRow.isColumnModified(col))
			{
				if (ignoreStatus && newLineAfterColumn)
				{
					sql.append("  ");
					valuePart.append("  ");
				}
				sql.append(this.getColumnName(col));
				valuePart.append('?');
				if (col < this.colCount - 1)
				{
					sql.append(',');
					valuePart.append(',');
				}
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
		sql.append(this.updateTable);
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
	
	private void updatePkInformation(WbConnection aConnection)
		throws SQLException
	{
		if (this.pkColumns != null) return;
		this.pkColumns = new ArrayList();
		if (aConnection != null)
		{
			Connection sqlConn = aConnection.getSqlConnection();
			DatabaseMetaData meta = sqlConn.getMetaData();
			this.updateTable = aConnection.getMetadata().adjustObjectname(this.updateTable);
			ResultSet rs = meta.getBestRowIdentifier(null, null, this.updateTable, DatabaseMetaData.bestRowSession, false);
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
		}
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

	public static void main(String args[])
	{
		try
		{
			Class.forName("oracle.jdbc.OracleDriver");
			Connection con = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:oradb", "test", "test");
			WbConnection wb = new WbConnection(con);
			try
			{
				//Statement stmt = con.createStatement();
				//ResultSet rs = stmt.executeQuery("select nr, name from test");
				PreparedStatement stmt = con.prepareStatement("insert into test (nr, name) values (?,?)");
				stmt.setObject(1, new Integer(10));
				stmt.setNull(2, java.sql.Types.VARCHAR);
				stmt.executeUpdate();
				con.commit();
				stmt.close();
				/*
				//DataStore ds = new DataStore(rs);
				//rs.close();
				DataStore ds = new DataStore(new String[] { "Column1", "Column2"} );
				ds.setUpdateTable("mytable");
				int row = ds.addRow();
				ds.setValue(row, 0, "Testing");
				ds.setValue(row, 1, new Integer(42));
				ds.resetStatus();
				ds.deleteRow(row);
				row = ds.addRow();
				ds.setValue(row, 0, "Second row");
				ds.setValue(row, 1, new Integer(21));
				
				row = ds.addRow();
				ds.setValue(row, 0, "delete test");
				ds.deleteRow(row);
				
				ds.addRow();
				
				List l = ds.getUpdateStatements(null);
				for (int i=0; i < l.size(); i ++)
				{
					System.out.println(l.get(i));
				}
				 */
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

