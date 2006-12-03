/*
 * TableCreator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.StringUtil;

/**
 *
 * @author  support@sql-workbench.net
 */
public class TableCreator
{
	private WbConnection connection;
	private ColumnIdentifier[] columnDefinition;
	private TableIdentifier tablename;
	private TypeMapper mapper;
	private boolean useDbmsDataType = false;

	public TableCreator(WbConnection target, TableIdentifier newTable, ColumnIdentifier[] columns)
		throws SQLException
	{
		this.connection = target;
		this.tablename = newTable.createCopy();
//		this.tablename.setNeverAdjustCase(false);
//		this.tablename.adjustCase(this.connection);
		
		this.columnDefinition = new ColumnIdentifier[columns.length];

		// As we are sorting the columns we have to create a copy of the array
		// to ensure that the caller does not see a different ordering
		for (int i = 0; i < columns.length; i++)
		{
			this.columnDefinition [i] = columns[i];
		}
		
		// Now sort the columns according to their DBMS position
		sortColumns();

		//Retrieve the list of datatypes that should be ignored for the current 
		//connection. The names in that list must match the names returned 
		//by DatabaseMetaData.getTypeInfo()
		DbMetadata meta = this.connection.getMetadata();
		String types = Settings.getInstance().getProperty("workbench.ignoretypes." + meta.getDbId(), null);;
		List ignored = StringUtil.stringToList(types, ",", true, true);
		
		this.mapper = new TypeMapper(this.connection, ignored);
	}

	public void useDbmsDataType(boolean flag) { this.useDbmsDataType = flag; }
	public TableIdentifier getTable() { return this.tablename; }
	
	private void sortColumns()
	{
		Comparator c = new Comparator()
		{
			public int compare(Object o1, Object o2)
			{
				ColumnIdentifier c1 = (ColumnIdentifier)o1;
				ColumnIdentifier c2 = (ColumnIdentifier)o2;
				int pos1 = c1.getPosition();
				int pos2 = c2.getPosition();
				
				if (pos1 < pos2) return -1;
				else if (pos1 > pos2) return 1;
				return 0;
			}
		};
		Arrays.sort(columnDefinition, c);
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
		int count = this.columnDefinition.length;
		int numCols = 0;
		List pkCols = new ArrayList();
		
		for (int i=0; i < count; i++)
		{
			ColumnIdentifier col = this.columnDefinition[i];
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
