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
import java.sql.ParameterMetaData;

/**
 *
 * @author  workbench@kellerer.org
 */
public abstract class AbstractConstraintReader
{
	private PreparedStatement columnConstraintStatement;
	private PreparedStatement tableConstraintStatement;

	public AbstractConstraintReader()
	{
	}

	public abstract String getColumnConstraintSql();
	public abstract String getTableConstraintSql();
	public String getPrefixTableConstraintKeyword() { return ""; }
	public String getSuffixTableConstraintKeyword() { return ""; }

	public int getIndexForSchemaParameter()
	{
		return -1;
	}
	public int getIndexForCatalogParameter()
	{
		return -1;
	}
	public int getIndexForTableNameParameter()
	{
		return 1;
	}

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

		ResultSet rs = null;
		try
		{
			if (this.columnConstraintStatement == null)
			{
				this.columnConstraintStatement = dbConnection.prepareStatement(sql);
			}
			int index = this.getIndexForSchemaParameter();
			if (index > 0) this.columnConstraintStatement.setString(index, aTable.getSchema());

			index = this.getIndexForCatalogParameter();
			if (index > 0) this.columnConstraintStatement.setString(index, aTable.getCatalog());

			index = this.getIndexForTableNameParameter();
			if (index > 0) this.columnConstraintStatement.setString(index, aTable.getTable());

			rs = this.columnConstraintStatement.executeQuery();
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
			//try { stmt.close(); } catch (Throwable th) {}
		}
		return result;
	}

	public String getTableConstraints(Connection dbConnection, TableIdentifier aTable, String indent)
	{
		String sql = this.getTableConstraintSql();
		if (sql == null) return null;
		StringBuffer result = new StringBuffer(100);
		String prefix = this.getPrefixTableConstraintKeyword();
		String suffix = this.getSuffixTableConstraintKeyword();

		ResultSet rs = null;
		try
		{
			if (this.tableConstraintStatement == null)
			{
				this.tableConstraintStatement = dbConnection.prepareStatement(sql);
			}
			int index = this.getIndexForSchemaParameter();
			if (index > 0) this.tableConstraintStatement.setString(index, aTable.getSchema());

			index = this.getIndexForCatalogParameter();
			if (index > 0) this.tableConstraintStatement.setString(index, aTable.getCatalog());

			index = this.getIndexForTableNameParameter();
			if (index > 0) this.tableConstraintStatement.setString(index, aTable.getTable());

			rs = this.tableConstraintStatement.executeQuery();
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
					result.append(suffix);
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
		if (this.tableConstraintStatement != null)
		{
			try { this.tableConstraintStatement.close(); } catch (Throwable th) {}
		}
		if (this.columnConstraintStatement != null)
		{
			try { this.columnConstraintStatement.close(); } catch (Throwable th) {}
		}
	}

}