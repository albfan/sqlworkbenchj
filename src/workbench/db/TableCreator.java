/*
 * TableCreator.java
 *
 * Created on December 28, 2003, 12:25 PM
 */

package workbench.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
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
	private StringBuffer messages;
	
	public TableCreator(WbConnection target, TableIdentifier newTable, ColumnIdentifier[] columns)
		throws SQLException
	{
		this.connection = target;
		this.tablename = newTable;
		this.columnDefinition = columns;

		ResultSet rs = null;
		this.typeInfo = new HashMap(27);
		try
		{
			List ignored = target.getMetadata().getIgnoredDataTypes();
			rs = this.connection.getSqlConnection().getMetaData().getTypeInfo();
			while (rs.next())
			{
				String name = rs.getString(1);
				int type = rs.getInt(2);
				
				// we can't handle arrays anyway
				if (type == java.sql.Types.ARRAY || type == java.sql.Types.OTHER) continue;
				if (ignored.contains(name)) continue;
				
				TypeInfo info = new TypeInfo();
				info.name = name;
				info.type = type;
				info.precision = rs.getInt(3);
				info.min_scale = rs.getInt(14);
				info.max_scale = rs.getInt(15);
				Integer key = new Integer(type);
				if (this.typeInfo.containsKey(key))
				{
					LogMgr.logWarning("TableCreator.<init>", "The mapping from JDBC type "  + SqlUtil.getTypeName(type) + " to  DB type " + name + " will be ignored. A mapping is already present.");
				}
				else
				{
					LogMgr.logInfo("TableCreator.<init>", "Mapping JDBC type "  + SqlUtil.getTypeName(type) + " to DB type " + name);
					this.typeInfo.put(key, info);
				}
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

	public void createTable()
		throws SQLException
	{
		StringBuffer sql = new StringBuffer(100);
		sql.append("CREATE TABLE ");
		String name = this.tablename.isNewTable() ? this.tablename.getTable() : this.tablename.getTableExpression();
		sql.append(name);
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
		LogMgr.logInfo("TableCreator.createTable()", "Using sql=" + sql);
		Statement stmt = this.connection.createStatement();
		try
		{
			stmt.executeUpdate(sql.toString());
			if (this.connection.getMetadata().getDDLNeedsCommit() && !this.connection.getAutoCommit())
			{
				LogMgr.logDebug("TableCreator.createTable()", "Commiting the changes");
				this.connection.commit();
			}
		}
		finally
		{
			try { stmt.close(); } catch (Throwable th) {}
		}
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

		TypeInfo info = (TypeInfo)this.typeInfo.get(typeKey);
		if (info == null) return null;

		String typeName = info.name;
		result.append(typeName);

		int size = col.getColumnSize();
		int digits = col.getDecimalDigits();

		// Now we need to check if the data type needs an argument.
		// I could use the "parameter" column from the driver's type info
		// result set, but it seems that not every DBMS returns the correct
		// information there, so I'm using my "own" logic to determine
		// if the data type needs an argument.
		if (type == Types.VARCHAR || type == Types.CHAR)
		{
			if (size == 0) return null;
			result.append('(');
			result.append(size);
			result.append(')');
		}
		// INTEGER's normally don't need a size argument
		else if (SqlUtil.isNumberType(type) && !SqlUtil.isIntegerType(type))
		{
			//if (size == 0) return null;
			if (size > info.max_scale)
			{
				size = info.max_scale;
				String msg = ResourceMgr.getString("MsgTableCreatorSizeReduced");
				msg = msg.replaceAll("%column%", col.getColumnName());
				msg = msg.replaceAll("%current%", Integer.toString(size));
				msg = msg.replaceAll("%max%", Integer.toString(info.max_scale));
				LogMgr.logWarning("TableCreator.getColumnDefinition()", "Reducing size for column " + col.getColumnName() + " from " + size + " to " + info.max_scale);
				this.addMessage(msg);
			}
			if (SqlUtil.isDecimalType(type, size, digits))
			{
				result.append('(');
				result.append(size);
				result.append(',');
				result.append(digits);
				result.append(')');
			}
			else if (type != Types.INTEGER)
			{
				result.append('(');
				result.append(size);
				result.append(')');
			}
		}

		return result.toString();
	}
	
	public String getMessages()
	{
		if (this.messages == null) return null;
		return this.messages.toString();
	}
	
	private void addMessage(String aMsg)
	{
		if (this.messages == null) 
		{
			this.messages = new StringBuffer(100);
		}
		else
		{
			this.messages.append("\n");
		}
		
		this.messages.append(aMsg);
	}
}

class TypeInfo
{
	String name;
	int type;
	int precision;
	int min_scale;
	int max_scale;
}