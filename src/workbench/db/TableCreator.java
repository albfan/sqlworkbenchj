/*
 * TableCreator.java
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

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import workbench.log.LogMgr;
import workbench.util.SqlUtil;

/**
 *
 * @author  support@sql-workbench.net
 */
public class TableCreator
{
	private WbConnection connection;
	private List<ColumnIdentifier> columnDefinition;
	private TableIdentifier tablename;
	private TypeMapper mapper;
	private boolean useDbmsDataType = false;

	public TableCreator(WbConnection target, TableIdentifier newTable, Collection<ColumnIdentifier> columns)
		throws SQLException
	{
		this.connection = target;
		this.tablename = newTable.createCopy();
		
		// As we are sorting the columns we have to create a copy of the array
		// to ensure that the caller does not see a different ordering
		this.columnDefinition = new ArrayList<ColumnIdentifier>(columns);
		
		// Now sort the columns according to their DBMS position
		ColumnIdentifier.sortByPosition(columnDefinition);

		this.mapper = new TypeMapper(this.connection);
	}

	public void useDbmsDataType(boolean flag) { this.useDbmsDataType = flag; }
	public TableIdentifier getTable() { return this.tablename; }
	
	public void createTable()
		throws SQLException
	{
		StringBuilder sql = new StringBuilder(100);
		sql.append("CREATE TABLE ");
		String name = this.tablename.getTableExpression(this.connection);
		sql.append(name);
		sql.append(" (");
		int numCols = 0;
		List<String> pkCols = new ArrayList<String>();
		
		for (ColumnIdentifier col : columnDefinition)
		{
			if (col.isPkColumn()) pkCols.add(col.getColumnName());
			String def = this.getColumnDefintionString(col);
			if (def == null) continue;
			
			if (numCols > 0) sql.append(", ");
			sql.append(def);
			numCols++;
		}
		sql.append(')');
		LogMgr.logInfo("TableCreator.createTable()", "Creating table using sql: " + sql);
		Statement stmt = this.connection.createStatement();
		try
		{
			stmt.executeUpdate(sql.toString());
			
			if (pkCols.size() > 0)
			{
				CharSequence pkSql = this.connection.getMetadata().getPkSource(this.tablename, pkCols, null);
				if (pkSql.length() > 0)
				{
					LogMgr.logInfo("TableCreator.createTable()", "Adding primary key using: " + pkSql.toString());
					stmt.executeUpdate(pkSql.toString());
				}
			}
			
			if (this.connection.getDbSettings().ddlNeedsCommit() && !this.connection.getAutoCommit())
			{
				LogMgr.logDebug("TableCreator.createTable()", "Commiting the changes");
				this.connection.commit();
			}
		}
		finally
		{
			SqlUtil.closeStatement(stmt);
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
		int size = col.getColumnSize();
		int digits = col.getDecimalDigits();
		String def = col.getDefaultValue();

		StringBuilder result = new StringBuilder(30);
		result.append(col.getColumnName());
		result.append(' ');

		String typeName = null;
		if (this.useDbmsDataType)
		{
			typeName = col.getDbmsType();
		}
		else
		{
			typeName = this.mapper.getTypeName(type, size, digits);
		}
		result.append(typeName);

		if (!col.isNullable())
		{
			result.append(" NOT NULL");
		}

		return result.toString();
	}

}
