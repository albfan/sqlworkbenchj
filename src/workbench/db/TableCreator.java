/*
 * TableCreator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import workbench.log.LogMgr;

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
		this.columnDefinition = new ArrayList<ColumnIdentifier>(columns.size());
		this.columnDefinition.addAll(columns);
		
		// Now sort the columns according to their DBMS position
		sortColumns();

		this.mapper = new TypeMapper(this.connection);
	}

	public void useDbmsDataType(boolean flag) { this.useDbmsDataType = flag; }
	public TableIdentifier getTable() { return this.tablename; }
	
	private void sortColumns()
	{
		Comparator<ColumnIdentifier> c = new Comparator<ColumnIdentifier>()
		{
			public int compare(ColumnIdentifier o1, ColumnIdentifier o2)
			{
				int pos1 = o1.getPosition();
				int pos2 = o2.getPosition();
				
				if (pos1 < pos2) return -1;
				else if (pos1 > pos2) return 1;
				return 0;
			}
		};
		Collections.sort(columnDefinition, c);
	}
	
	public void createTable()
		throws SQLException
	{
		StringBuilder sql = new StringBuilder(100);
		sql.append("CREATE TABLE ");
		//String name = this.tablename.isNewTable() ? this.tablename.getTableName() : this.tablename.getTableExpression();
		String name = this.tablename.getTableExpression(this.connection);
		sql.append(name);
		sql.append(" (");
		int count = this.columnDefinition.size();
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
				StringBuilder pkSql = this.connection.getMetadata().getPkSource(this.tablename.getTableName(), pkCols, null);
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
		//Integer typeKey = new Integer(type);
		int size = col.getColumnSize();
		int digits = col.getDecimalDigits();

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

		return result.toString();
	}

}
