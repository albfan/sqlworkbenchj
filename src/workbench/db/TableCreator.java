/*
 * TableCreator.java
 *
 * Created on December 28, 2003, 12:25 PM
 */

package workbench.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import workbench.log.LogMgr;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;

/**
 *
 * @author  workbench@kellerer.org
 */
public class TableCreator
{
	private WbConnection connection;
	private ColumnIdentifier[] columnDefinition;
	private TableIdentifier tablename;
	private HashMap typeInfo;

	public TableCreator(WbConnection target)
		throws SQLException
	{
		this.connection = target;
		ResultSet rs = null;
		this.typeInfo = new HashMap(27);
		try
		{
			rs = this.connection.getSqlConnection().getMetaData().getTypeInfo();
			while (rs.next())
			{
				String name = rs.getString(1);
				int type = rs.getInt(2);
				if (type == java.sql.Types.ARRAY) continue;
				this.typeInfo.put(new Integer(type), name);
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("TableCreator.<init>", "Error reading type info for target connection", e);
			throw e;
		}
		finally
		{
			try { rs.close(); } catch (Throwable th) {}
		}
	}
	
	public void setSourceTable(WbConnection source, TableIdentifier aTable)
		throws SQLException
	{
		List cols = source.getMetadata().getTableColumns(aTable);
		this.tablename = aTable;
		int count = cols.size();
		this.columnDefinition = new ColumnIdentifier[count];
		for (int i=0; i < count; i++)
		{
			this.columnDefinition[i] = (ColumnIdentifier)cols.get(i);
		}
	}
	
	public void createTable()
		throws SQLException
	{
		StringBuffer sql = new StringBuffer(100);
		sql.append("CREATE TABLE ");
		sql.append(this.tablename.getTableExpression());
		sql.append(" (");
		int count = this.columnDefinition.length;
		int numCols = 0;
		for (int i=0; i < count; i++)
		{
			ColumnIdentifier col = this.columnDefinition[i];
			String def = this.getColumnDefintionString(col);
			if (def == null) continue;
			if (numCols > 0) sql.append(", ");
			sql.append(def);
			numCols++;
		}
		sql.append(')');
		LogMgr.logDebug("TableCreator.createTable()", "Using sql=" + sql);
	}
	
	/**
	 *	Return the SQL string for the column definition of the 
	 *	given column index (index into the columnDefinition array)
	 *	The method expects the typeInfo map to be filled!
	 */
	private String getColumnDefintionString(ColumnIdentifier col)
	{
		if (col == null) return null;
		
		int type = col.getDataType();
		Integer typeKey = new Integer(type);
		if (!this.typeInfo.containsKey(typeKey)) return null;
		
		StringBuffer result = new StringBuffer(30);
		result.append(col.getColumnName());
		result.append(' ');
		
		String typeName = (String)this.typeInfo.get(typeKey);
		result.append(typeName);
		
		int size = col.getColumnSize();
		int digits = col.getDecimalDigits();
		
		if (type == Types.VARCHAR || type == Types.CHAR)
		{
			if (size == 0) return null;
			result.append('(');
			result.append(size);
			result.append(')');
		}
		else if (SqlUtil.isNumberType(type))
		{
			if (size == 0) return null;
			if (SqlUtil.isDecimalType(type, size, digits))
			{
				result.append('(');
				result.append(size);
				result.append(',');
				result.append(digits);
				result.append(')');
			}
			else
			{
				result.append('(');
				result.append(size);
				result.append(')');
			}
		}
		
		return result.toString();
	}
}
