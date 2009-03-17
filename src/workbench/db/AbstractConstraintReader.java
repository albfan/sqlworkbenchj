/*
 * AbstractConstraintReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
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
import java.sql.Savepoint;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import workbench.util.ExceptionUtil;

import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.CollectionBuilder;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
* A class to read table level constraints from the database.
 * @author  support@sql-workbench.net
 */
public abstract class AbstractConstraintReader
	implements ConstraintReader
{
	public abstract String getColumnConstraintSql();
	public abstract String getTableConstraintSql();

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

	public boolean isSystemConstraintName(String name)
	{
		return false;
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
		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo(getClass().getName() + ".getColumnConstraints()", "Using SQL: " + sql);
		}

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

	public String getConstraintSource(List<TableConstraint> constraints, String indent)
	{
		if (constraints == null) return null;
		StringBuilder result = new StringBuilder();
		
		int count = 0;
		for (int i=0; i < constraints.size(); i++)
		{
			TableConstraint cons = constraints.get(i);
			if (cons == null) continue;
			if (StringUtil.isBlank(cons.getExpression())) continue;
			if (count > 0)
			{
				result.append("\n");
				result.append(indent);
				result.append(',');
			}
			result.append(cons.getSql());
			count++;
		}
		return result.toString();
	}
	
	/**
	 * Returns the SQL Statement that should be appended to a CREATE table
	 * in order to create the constraints defined on the table
	 */
	public List<TableConstraint> getTableConstraints(WbConnection dbConnection, TableIdentifier aTable)
	{
		String sql = this.getTableConstraintSql();
		if (sql == null) return null;

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo(getClass().getName() + ".getTableConstraints()", "Using SQL: " + sql);
		}

		List<TableConstraint> result = CollectionBuilder.arrayList();
		PreparedStatement stmt = null;

		Savepoint sp = null;

		ResultSet rs = null;
		try
		{
			if (dbConnection.getDbSettings().useSavePointForDML())
			{
				sp = dbConnection.setSavepoint();
			}
			
			stmt = dbConnection.getSqlConnection().prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
			
			int index = this.getIndexForSchemaParameter();
			if (index > 0) 
			{
				String schema = aTable.getSchema();
				if (StringUtil.isBlank(schema))
				{
					schema = dbConnection.getCurrentSchema();
				}
				stmt.setString(index, schema);
			}

			index = this.getIndexForCatalogParameter();
			if (index > 0) stmt.setString(index, aTable.getCatalog());

			index = this.getIndexForTableNameParameter();
			if (index > 0) stmt.setString(index, aTable.getTableName());

			Pattern p = Pattern.compile("^check\\s+.*", Pattern.CASE_INSENSITIVE);
			rs = stmt.executeQuery();
			while (rs.next())
			{
				String name = rs.getString(1);
				String constraint = rs.getString(2);
				if (constraint != null)
				{
					constraint = constraint.trim();

					Matcher m = p.matcher(constraint);
					if (constraint.charAt(0) != '(' && !m.matches())
					{
						constraint = "(" + constraint + ")";
					}
					TableConstraint c = new TableConstraint(name, constraint);
					c.setIsSystemName(isSystemConstraintName(name));
					result.add(c);
				}
			}
			dbConnection.releaseSavepoint(sp);
		}
		catch (SQLException e)
		{
			dbConnection.rollback(sp);
			LogMgr.logError("AbstractConstraintReader", "Error when reading table constraints " + ExceptionUtil.getDisplay(e), null);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}

}
