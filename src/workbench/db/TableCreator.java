/*
 * TableCreator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import workbench.log.LogMgr;

/**
 *
 * @author  info@sql-workbench.net
 */
public class TableCreator
{
	private WbConnection connection;
	private ColumnIdentifier[] columnDefinition;
	private TableIdentifier tablename;
	private StringBuffer messages;
	private TypeMapper mapper;

	public TableCreator(WbConnection target, TableIdentifier newTable, ColumnIdentifier[] columns)
		throws SQLException
	{
		this.connection = target;
		this.tablename = newTable;
		this.columnDefinition = columns;

		List ignored = target.getMetadata().getIgnoredDataTypes();
		this.mapper = new TypeMapper(this.connection, ignored);
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
		int size = col.getColumnSize();
		int digits = col.getDecimalDigits();

		StringBuffer result = new StringBuffer(30);
		result.append(col.getColumnName());
		result.append(' ');

		String typeName = this.mapper.getTypeName(type, size, digits);
		result.append(typeName);

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
