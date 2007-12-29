/*
 * GenericObjectDropper.java
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
import java.util.List;

import workbench.interfaces.ObjectDropper;
import workbench.log.LogMgr;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A helper class to drop different types of objects
 * 
 * @author  support@sql-workbench.net
 */
public class GenericObjectDropper
	implements ObjectDropper
{
	private List<? extends DbObject> objects;
	private WbConnection connection;
	private Statement currentStatement;
	private boolean cascadeConstraints;
	private TableIdentifier objectTable;
	
	public GenericObjectDropper()
	{
	}

	public List<? extends DbObject> getObjects()
	{
		return objects;
	}
	
	public boolean supportsCascade()
	{
		boolean canCascade = false;

		if (objects != null && this.connection != null)
		{
			int numTypes = this.objects.size();
			for (int i=0; i < numTypes; i++)
			{
				String type = this.objects.get(i).getObjectType();
				String verb = this.connection.getDbSettings().getCascadeConstraintsVerb(type);

				// if at least one type can be dropped with CASCADE, enable the checkbox
				if (!StringUtil.isEmptyString(verb))
				{
					canCascade = true;
					break;
				}
			}
		}
		return canCascade;
	}

	public void setObjects(List<? extends DbObject> toDrop)
	{
		this.objects = toDrop;
	}
	
	public void setObjectTable(TableIdentifier tbl)
	{
		this.objectTable = tbl;
	}
	
	public void setConnection(WbConnection aConn)
	{
		this.connection = aConn;
	}

	public void dropObjects()
		throws SQLException
	{
		boolean needCommit = this.connection.shouldCommitDDL();
		
		try
		{
			if (this.connection == null) throw new NullPointerException("No connection!");
			if (this.connection.isBusy()) return;
			if (this.objects == null || this.objects.size() == 0) return;
			int count = this.objects.size();
			this.connection.setBusy(true);
			
			
    	currentStatement = this.connection.createStatement();
			
			String cascade = null;

			boolean needTableForIndexDrop = this.connection.getDbSettings().needsTableForDropIndex();
			
			for (int i=0; i < count; i++)
			{
				String name = this.objects.get(i).getObjectName();
				String type = this.objects.get(i).getObjectType();
				
				StringBuilder sql = new StringBuilder(120);
				sql.append("DROP ");
				sql.append(type);
				sql.append(' ');
				sql.append(name);
				
				if (needTableForIndexDrop && "INDEX".equals(type) && objectTable != null)
				{
					sql.append(" ON ");
					sql.append(objectTable.getTableExpression(this.connection));
				}

				if (this.cascadeConstraints)
				{
					cascade = this.connection.getDbSettings().getCascadeConstraintsVerb(type);
					if (cascade != null)
					{
						sql.append(' ');
						sql.append(cascade);
					}
				}
				LogMgr.logDebug("ObjectDropper.execute()", "Using SQL: " + sql);
				currentStatement.execute(sql.toString());
			}

			if (needCommit)
			{
				this.connection.commit(); 
			}
		}
		catch (SQLException e)
		{
			if (needCommit)
			{
				try { this.connection.rollback(); } catch (Throwable th) {}
			}
			throw e;
		}
		finally
		{
			SqlUtil.closeStatement(currentStatement);
			this.currentStatement = null;
			this.connection.setBusy(false);
		}
	}

	public void cancel()
		throws SQLException
	{
		if (this.currentStatement == null) return;
		this.currentStatement.cancel();
		if (this.connection.shouldCommitDDL())
		{
			try { this.connection.rollback(); } catch (Throwable th) {}
		}
	}
	
	public void setCascade(boolean flag)
	{
		this.cascadeConstraints = flag;
	}

}
