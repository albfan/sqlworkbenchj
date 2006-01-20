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
		this.tablename = newTable;
		this.columnDefinition = columns;

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
	
	public void createTable()
		throws SQLException
	{
		StringBuffer sql = new StringBuffer(100);
		sql.append("CREATE TABLE ");
		//String name = this.tablename.isNewTable() ? this.tablename.getTableName() : this.tablename.getTableExpression();
		String name = this.tablename.getTableExpression(this.connection);
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
		//Integer typeKey = new Integer(type);
		int size = col.getColumnSize();
		int digits = col.getDecimalDigits();

		StringBuffer result = new StringBuffer(30);
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
