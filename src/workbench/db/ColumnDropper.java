/*
 * ColumnDropper.java
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
import java.util.ArrayList;
import java.util.List;
import workbench.interfaces.ObjectDropper;
import workbench.log.LogMgr;
import workbench.storage.RowActionMonitor;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * An implementation of the ObjectDropper interface to drop columns from a table.
 *
 * The necessary SQL statement is retrieved from the <tt>workbench.settings</tt>
 *
 * @author Thomas Kellerer
 * @see workbench.db.DbSettings#getDropMultipleColumnSql()
 * @see workbench.db.DbSettings#getDropSingleColumnSql()
 *
 */
public class ColumnDropper
	implements ObjectDropper
{
	private WbConnection conn;
	private List<ColumnIdentifier> columns;
	private TableIdentifier table;
	private boolean cancelDrop = false;
	private Statement currentStatement;

	public ColumnDropper()
	{
	}

	public ColumnDropper(WbConnection db, TableIdentifier tbl, List<ColumnIdentifier> toDrop)
	{
		this.conn = db;
		this.columns = toDrop;
		this.table = tbl;
	}

	public void setRowActionMonitor(RowActionMonitor mon)
	{
	}

	public boolean supportsCascade()
	{
		return false;
	}

	public void setCascade(boolean flag)
	{
	}

	public boolean supportsFKSorting()
	{
		return false;
	}

	public void cancel()
		throws SQLException
	{
		cancelDrop = true;
		if (this.currentStatement != null)
		{
			this.currentStatement.cancel();
		}
	}

	public WbConnection getConnection()
	{
		return this.conn;
	}

	public void setConnection(WbConnection con)
	{
		this.conn = con;
	}

	public void setObjectTable(TableIdentifier tbl)
	{
		this.table = tbl;
	}

	public List<? extends DbObject> getObjects()
	{
		return columns;
	}

	public void setObjects(List<? extends DbObject> toDrop)
	{
		this.columns = new ArrayList<ColumnIdentifier>();
		if (toDrop == null) return;
		for (DbObject dbo : toDrop)
		{
			if (dbo instanceof ColumnIdentifier)
			{
				columns.add((ColumnIdentifier)dbo);
			}
		}
	}

	public CharSequence getScript()
	{
		List<String> statements = getSql(table, columns, conn);
		StringBuffer result = new StringBuffer(statements.size() * 40);
		boolean needCommit = (conn != null ? conn.shouldCommitDDL() : false);

		for (String sql : statements)
		{
			result.append(sql);
			result.append(";\n");
			if (needCommit) result.append("COMMIT;\n");
			result.append('\n');
		}
		return result;
	}

	public void dropObjects()
		throws SQLException
	{
		if (this.conn == null) return;
		if (this.table == null) return;
		if (this.columns == null || this.columns.size() == 0) return;

		List<String> statements = getSql(table, columns, conn);

		try
		{
			this.currentStatement = this.conn.createStatement();

			for (String sql : statements)
			{
				if (cancelDrop) break;
				LogMgr.logDebug("ColumnDropper.dropObjects()", "Using sql: " + sql);
				this.currentStatement.executeUpdate(sql);
			}

			if (conn.shouldCommitDDL())
			{
				if (cancelDrop)
				{
					conn.rollback();
				}
				else
				{
					conn.commit();
				}
			}
		}
		catch (SQLException e)
		{
			if (conn.shouldCommitDDL())
			{
				conn.rollback();
			}
			throw e;
		}
		finally
		{
			SqlUtil.closeStatement(currentStatement);
			currentStatement = null;
		}
	}

	public static List<String> getSql(TableIdentifier table, List<ColumnIdentifier> columns, WbConnection conn)
	{
		String multiSql = conn.getDbSettings().getDropMultipleColumnSql();
		String singleSql = conn.getDbSettings().getDropSingleColumnSql();
		List<String> result = new ArrayList<String>(columns.size());

		if (columns.size() == 1 || StringUtil.isEmptyString(multiSql))
		{
			singleSql = StringUtil.replace(singleSql, MetaDataSqlManager.TABLE_NAME_PLACEHOLDER, table.getTableExpression(conn));

			for (ColumnIdentifier col : columns)
			{
				result.add(StringUtil.replace(singleSql, MetaDataSqlManager.COLUMN_NAME_PLACEHOLDER, col.getColumnName(conn)));
			}
		}
		else
		{
			multiSql = StringUtil.replace(multiSql, MetaDataSqlManager.TABLE_NAME_PLACEHOLDER, table.getTableExpression(conn));

			StringBuilder cols = new StringBuilder(columns.size());
			int nr = 0;
			for (ColumnIdentifier col : columns)
			{
				if (nr > 0) cols.append(", ");
				cols.append(col.getColumnName(conn));
				nr ++;
			}
			result.add(StringUtil.replace(multiSql, MetaDataSqlManager.COLUMN_LIST_PLACEHOLDER, cols.toString()));
		}

		return result;
	}
}
