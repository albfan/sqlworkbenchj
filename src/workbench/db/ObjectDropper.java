/*
 * ObjectDropper.java
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
import workbench.util.StringUtil;

/**
 * A help class to drop different types of objects
 * @author  support@sql-workbench.net
 */
public class ObjectDropper
{
	private List objectNames;
	private List objectTypes;
	private WbConnection connection;
	private Statement currentStatement;
	private boolean cascadeConstraints;
	private TableIdentifier indexTable;
	
	public ObjectDropper(List names, List types)
		throws IllegalArgumentException
	{
		if (names == null || types == null) throw new IllegalArgumentException();
		if (names.size() == 0 || types.size() == 0) throw new IllegalArgumentException();
		if (names.size() != types.size()) throw new IllegalArgumentException();
		this.objectNames = names;
		this.objectTypes = types;
	}

	public void setIndexTable(TableIdentifier tbl)
	{
		this.indexTable = tbl;
	}
	
	public void setConnection(WbConnection aConn)
	{
		this.connection = aConn;
	}

	public void execute()
		throws SQLException
	{
		try
		{
			if (this.connection == null) throw new NullPointerException("No connection!");
			if (this.objectNames == null || this.objectNames.size() == 0) return;
			int count = this.objectNames.size();
			currentStatement = this.connection.createStatement();
			String cascade = null;

			boolean needTableForIndexDrop = this.connection.getMetadata().needsTableForDropIndex();
			
			for (int i=0; i < count; i++)
			{
				String name = (String)this.objectNames.get(i);
				String type = (String)this.objectTypes.get(i);
				// we assume that names with special characters are already quoted!

				StringBuffer sql = new StringBuffer(120);
				sql.append("DROP ");
				sql.append(type);
				sql.append(' ');
				sql.append(name);
				
				if (needTableForIndexDrop && "INDEX".equals(type) && indexTable != null)
				{
					sql.append(" ON ");
					sql.append(indexTable.getTableExpression(this.connection));
				}

				if (this.cascadeConstraints)
				{
					cascade = this.connection.getMetadata().getCascadeConstraintsVerb(type);
					if (cascade != null)
					{
						sql.append(' ');
						sql.append(cascade);
					}
				}
				LogMgr.logDebug("ObjectDropper.execute()", "Using SQL: " + sql);
				currentStatement.execute(sql.toString());
			}

			// Check if we need to commit the DDL statements
			if (!this.connection.getAutoCommit() && this.connection.getDdlNeedsCommit())
			{
				try { this.connection.commit(); } catch (Throwable th) {}
			}
		}
		catch (SQLException e)
		{
			if (!this.connection.getAutoCommit() && this.connection.getDdlNeedsCommit())
			{
				try { this.connection.rollback(); } catch (Throwable th) {}
			}
			throw e;
		}
		finally
		{
			try { currentStatement.close(); } catch (Throwable th) {}
			this.currentStatement = null;
		}
	}

	public void cancel()
		throws SQLException
	{
		if (this.currentStatement == null) return;
		this.currentStatement.cancel();
		if (!this.connection.getAutoCommit() && this.connection.getDdlNeedsCommit())
		{
			try { this.connection.rollback(); } catch (Throwable th) {}
		}
	}
	
	public void setCascadeConstraints(boolean flag)
	{
		this.cascadeConstraints = flag;
	}

}
