/*
 * AbstractConstraintReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import workbench.util.ExceptionUtil;

import workbench.log.LogMgr;
import workbench.util.SqlUtil;

/**
* A class to read table level constraints from the database.
 * @author  support@sql-workbench.net
 */
public abstract class AbstractConstraintReader
	implements ConstraintReader
{
	public abstract String getColumnConstraintSql();
	public abstract String getTableConstraintSql();
	public String getPrefixTableConstraintKeyword()
	{
		return "";
	}
	
	public String getSuffixTableConstraintKeyword()
	{
		return "";
	}

	public boolean isColumnConstraintNameIncluded() { return false; }
	public boolean isTableConstraintNameIncluded() { return false; }
	
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
	public Map<String, String> getColumnConstraints(Connection dbConnection, TableIdentifier aTable)
	{
		String sql = this.getColumnConstraintSql();
		if (sql == null) return Collections.emptyMap();

		HashMap<String, String> result = new HashMap<String, String>();

		ResultSet rs = null;
		PreparedStatement stmt = null;
		try
		{
			stmt = dbConnection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
			int index = this.getIndexForSchemaParameter();
			if (index > 0) stmt.setString(index, aTable.getSchema());

			index = this.getIndexForCatalogParameter();
			if (index > 0) stmt.setString(index, aTable.getCatalog());

			index = this.getIndexForTableNameParameter();
			if (index > 0) stmt.setString(index, aTable.getTableName());

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
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}

	/**
	 * Returns the SQL Statement that should be appended to a CREATE table
	 * in order to create the constraints defined on the table
	 */
	public String getTableConstraints(Connection dbConnection, TableIdentifier aTable, String indent)
		throws SQLException
	{
		String sql = this.getTableConstraintSql();
		if (sql == null) return null;
		StringBuilder result = new StringBuilder(100);
		String prefix = this.getPrefixTableConstraintKeyword();
		String suffix = this.getSuffixTableConstraintKeyword();
		PreparedStatement stmt = null;
		
		ResultSet rs = null;
		try
		{
			stmt = dbConnection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
			
			int index = this.getIndexForSchemaParameter();
			if (index > 0) stmt.setString(index, aTable.getSchema());

			index = this.getIndexForCatalogParameter();
			if (index > 0) stmt.setString(index, aTable.getCatalog());

			index = this.getIndexForTableNameParameter();
			if (index > 0) stmt.setString(index, aTable.getTableName());

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
					result.append(suffix);
					count++;
				}
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("AbstractConstraintReader", "Error when reading column constraints " + ExceptionUtil.getDisplay(e), null);
			throw e;
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result.toString();
	}

}
