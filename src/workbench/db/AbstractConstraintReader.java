/*
 * AbstractColumnConstraintReader.java
 *
 * Created on February 14, 2004, 1:32 PM
 */

package workbench.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import workbench.log.LogMgr;

/**
 *
 * @author  workbench@kellerer.org
 */
public abstract class AbstractConstraintReader
{
	public AbstractConstraintReader()
	{
	}
	
	public abstract String getColumnConstraintSql();
	public abstract String getTableConstraintSql();
	public String getPrefixTableConstraintKeyword() { return ""; }
	
	/**
	 *	Returns the column constraints for the given table. The key to the Map is 
	 *	the column name, the value is the full expression which can be appended
	 *	to the column definition inside a CREATE TABLE statement.
	 */
	public Map getColumnConstraints(java.sql.Connection dbConnection, TableIdentifier aTable)
	{
		String sql = this.getColumnConstraintSql();
		if (sql == null) return Collections.EMPTY_MAP;
		
		HashMap result = new HashMap();
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try
		{
			stmt = dbConnection.prepareStatement(sql);
			stmt.setString(1, aTable.getTable());
			rs = stmt.executeQuery();
			while (rs.next())
			{
				String column = rs.getString(1);
				String constraint = rs.getString(2);
				if (column != null && constraint != null)
				{
					result.put(column.trim(), constraint.trim());
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("AbstractConstraintReader", "Error when reading column constraints", e);
		}
		finally
		{
			try { rs.close(); } catch (Throwable th) {}
			try { stmt.close(); } catch (Throwable th) {}
		}
		return result;
	}
	
	public String getTableConstraints(Connection dbConnection, TableIdentifier aTable, String indent)
	{
		String sql = this.getTableConstraintSql();
		if (sql == null) return null;
		StringBuffer result = new StringBuffer(100);
		String prefix = this.getPrefixTableConstraintKeyword();
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try
		{
			stmt = dbConnection.prepareStatement(sql);
			stmt.setString(1, aTable.getTable());
			rs = stmt.executeQuery();
			int count = 0;
			while (rs.next())
			{
				String constraint = rs.getString(1);
				if (constraint != null)
				{
					if (count > 0) 
					{
						result.append("\n");
						result.append(indent);
						result.append(',');
					}
					result.append(prefix);
					result.append(constraint);
					count++;
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("AbstractConstraintReader", "Error when reading column constraints", e);
		}
		finally
		{
			try { rs.close(); } catch (Throwable th) {}
			try { stmt.close(); } catch (Throwable th) {}
		}
		return result.toString();	
	}
}
