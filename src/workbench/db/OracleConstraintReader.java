/*
 * FirebirdColumnConstraintReader.java
 *
 * Created on February 14, 2004, 1:35 PM
 */

package workbench.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import workbench.log.LogMgr;

/**
 *
 * @author  workbench@kellerer.org
 */
public class OracleConstraintReader
	extends AbstractConstraintReader
{
	private PreparedStatement statement;
	
	private static final String TABLE_SQL =
	         "SELECT search_condition \n" +
           "FROM all_constraints cons   \n" +
           "WHERE constraint_type = 'C' \n" +
           " and owner = ? \n" +
           " and table_name = ?  \n";

	public OracleConstraintReader()
	{
	}

	public int getIndexForSchemaParameter()
	{
		return 1;
	}
	public int getIndexForCatalogParameter()
	{
		return -1;
	}
	public int getIndexForTableNameParameter()
	{
		return 2;
	}
	public String getPrefixTableConstraintKeyword() { return "check ("; }
	public String getSuffixTableConstraintKeyword() { return ")"; }

	public String getColumnConstraintSql() { return null; }
	public String getTableConstraintSql() { return TABLE_SQL; }

	public String getTableConstraints(Connection dbConnection, TableIdentifier aTable, String indent)
	{
		String sql = this.getTableConstraintSql();
		if (sql == null) return null;
		StringBuffer result = new StringBuffer(100);
		ResultSet rs = null;
		try
		{
			if (this.statement == null)
			{
				this.statement = dbConnection.prepareStatement(sql);
			}
			this.statement.setString(1, aTable.getSchema());
			this.statement.setString(2, aTable.getTable());

			rs = this.statement.executeQuery();
			int count = 0;
			while (rs.next())
			{
				String constraint = rs.getString(1);
				if (constraint != null)
				{
          if (constraint.trim().endsWith("NOT NULL")) continue;
					if (count > 0)
					{
						result.append("\n");
						result.append(indent);
						result.append(',');
					}
					result.append("check (");
					result.append(constraint);
					result.append(")");
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
			//try { stmt.close(); } catch (Throwable th) {}
		}
		return result.toString();
	}
  
	public void done()
	{
		if (this.statement != null)
		{
			try { this.statement.close(); } catch (Throwable th) {}
		}
	}
}