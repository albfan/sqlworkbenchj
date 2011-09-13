/*
 * GenericObjectDropper.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Set;

import workbench.interfaces.ObjectDropper;
import workbench.log.LogMgr;
import workbench.storage.RowActionMonitor;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A helper class to drop different types of objects.
 * To drop table columns, {@link ColumnDropper} should be used.
 *
 * @author  Thomas Kellerer
 */
public class GenericObjectDropper
	implements ObjectDropper
{
	private List<? extends DbObject> objects;
	private WbConnection connection;
	private Statement currentStatement;
	private boolean cascadeConstraints;
	private TableIdentifier objectTable;
	private RowActionMonitor monitor;
	private boolean cancel;
	private boolean transactional = true;

	public GenericObjectDropper()
	{
	}

	public void setUseTransaction(boolean flag)
	{
		transactional = flag;
	}

	@Override
	public List<? extends DbObject> getObjects()
	{
		return objects;
	}

	@Override
	public void setRowActionMonitor(RowActionMonitor mon)
	{
		this.monitor = mon;
	}

	@Override
	public boolean supportsFKSorting()
	{
		if (objects == null) return false;

		int numTypes = this.objects.size();
		for (int i=0; i < numTypes; i++)
		{
			DbObject obj = this.objects.get(i);
			if (!(obj instanceof TableIdentifier))
			{
				return false;
			}
		}
		return true;
	}

	@Override
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
				if (StringUtil.isNonBlank(verb))
				{
					canCascade = true;
					break;
				}
			}
		}
		return canCascade;
	}

	@Override
	public void setObjects(List<? extends DbObject> toDrop)
	{
		this.objects = toDrop;
	}

	@Override
	public void setObjectTable(TableIdentifier tbl)
	{
		this.objectTable = tbl;
	}

	@Override
	public WbConnection getConnection()
	{
		return this.connection;
	}

	@Override
	public void setConnection(WbConnection aConn)
	{
		this.connection = aConn;
	}

	@Override
	public CharSequence getScript()
	{
		if (this.connection == null) throw new NullPointerException("No connection!");
		if (this.objects == null || this.objects.isEmpty()) return null;

		boolean needCommit = transactional && this.connection.shouldCommitDDL();
		int count = this.objects.size();
		StringBuffer result = new StringBuffer(count * 40);
		for (int i=0; i < count; i++)
		{
			CharSequence sql = getDropStatement(i);
			result.append(sql);
			result.append(";\n");
			result.append('\n');
		}
		if (needCommit) result.append("COMMIT;\n");
		return result;
	}

	private CharSequence getDropStatement(int index)
	{
		String drop = this.objects.get(index).getDropStatement(connection, cascadeConstraints);
		if (drop != null) return drop;

		String name = this.objects.get(index).getObjectNameForDrop(this.connection);
		String type = this.objects.get(index).getObjectType();

		StringBuilder sql = new StringBuilder(120);
		String ddl = this.connection.getDbSettings().getDropDDL(type, cascadeConstraints);

		if (objectTable != null)
		{
			ddl = ddl.replace(MetaDataSqlManager.TABLE_NAME_PLACEHOLDER, objectTable.getTableExpression(this.connection));
		}
		ddl = ddl.replace("%name%", name);
		sql.append(ddl);

		return sql;
	}

	@Override
	public void dropObjects()
		throws SQLException
	{
		cancel = false;

		try
		{
			if (this.connection == null) throw new NullPointerException("No connection!");
			if (this.connection.isBusy()) return;
			if (this.objects == null || this.objects.isEmpty()) return;
			int count = this.objects.size();
			this.connection.setBusy(true);

    	currentStatement = this.connection.createStatement();
			Set<String> types = connection.getMetadata().getObjectsWithData();

			for (int i=0; i < count; i++)
			{
				DbObject object = objects.get(i);

				String sql = getDropStatement(i).toString();
				LogMgr.logDebug("GenericObjectDropper.execute()", "Using SQL: " + sql);
				if (monitor != null)
				{
					String name = object.getObjectName();
					monitor.setCurrentObject(name, i + 1, count);
				}
				currentStatement.execute(sql);

				if (types.contains(object.getObjectType()))
				{
					try
					{
						TableIdentifier tbl = (TableIdentifier)object;
						connection.getObjectCache().removeTable(tbl);
					}
					catch (ClassCastException cce)
					{
						LogMgr.logWarning("GenericObjectDropper.dropObjects()", "Could not cast a table type to a TableIdentifier!", cce);
					}
				}
				if (this.cancel) break;
			}

			if (connection.shouldCommitDDL())
			{
				this.connection.commit();
			}
		}
		catch (SQLException e)
		{
			if (connection.shouldCommitDDL())
			{
				this.connection.rollbackSilently();
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

	@Override
	public void cancel()
		throws SQLException
	{
		if (this.currentStatement == null) return;
		cancel = true;
		try
		{
			this.currentStatement.cancel();
		}
		finally
		{
			if (this.connection.shouldCommitDDL())
			{
				this.connection.rollbackSilently();
			}
		}
	}

	@Override
	public void setCascade(boolean flag)
	{
		if (this.supportsCascade())
		{
			this.cascadeConstraints = flag;
		}
	}

}
