/*
 * TableSearcher.java
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import workbench.WbManager;
import workbench.interfaces.TableSearchDisplay;
import workbench.log.LogMgr;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbThread;

/**
 *
 * @author  kellererth
 */
public class TableSearcher
{
	private TableIdentifier[] tablesToSearch;
	private String columnFunction;
	private TableSearchDisplay display;
	private String criteria;
	private WbConnection connection;
	private boolean cancelSearch = false;
	private boolean isRunning = false;
	private Statement query = null;
	private Thread searchThread;
	private int maxRows = 0;

	public TableSearcher()
	{
	}

	public void search()
	{
		this.cancelSearch = false;
		this.searchThread = new WbThread("TableSearcher Thread")
		{
			public void run()
			{
				doSearch();
			}
		};
		this.searchThread.start();
	}

	public void cancelSearch()
	{
		this.cancelSearch = true;
		try
		{
			if (this.searchThread != null) this.searchThread.interrupt();
			if (this.query != null)
			{
				this.query.cancel();
			}
		}
		catch (Throwable e)
		{
			LogMgr.logWarning("TableSearcher.cancelSearc()", "Error when cancelling", e);
		}
	}

	private void setRunning(boolean aFlag)
	{
		synchronized (this)
		{
			this.isRunning = aFlag;
		}
		if (this.display != null)
		{
			if (aFlag) this.display.searchStarted();
			else this.display.searchEnded();
		}
		if (!aFlag) this.cancelSearch = false;
	}

	public synchronized boolean isRunning() { return this.isRunning; }


	protected void doSearch()
	{
		if (this.tablesToSearch == null || this.tablesToSearch.length == 0) return;
		this.setRunning(true);
		try
		{
			this.connection.setBusy(true);
			for (int i=0; i < this.tablesToSearch.length; i++)
			{
				TableIdentifier tbl = tablesToSearch[i];
				this.searchTable(tbl);
				if (this.cancelSearch) break;
			}
			if (this.display != null) this.display.setStatusText("");
		}
		catch (Throwable th)
		{
			LogMgr.logError("TableSearcher.doSearch()", "Error searching database", th);
		}
		finally
		{
			this.setRunning(false);
			this.connection.setBusy(false);
		}
	}

	private void searchTable(TableIdentifier table)
	{
		ResultSet rs = null;
		try
		{
			String sql = this.buildSqlForTable(table);
			if (sql == null) return;
			if (this.display != null) this.display.setCurrentTable(table.getTableExpression(), sql);

			this.query = this.connection.createStatementForQuery();
			this.query.setMaxRows(this.maxRows);
			//LogMgr.logInfo("TableSearcher", "Using SQL:\n" + sql);
			rs = this.query.executeQuery(sql);
			while (rs != null && rs.next())
			{
				if (this.cancelSearch)
				{
					break;
				}
				if (this.display != null)this.display.addResultRow(table, rs);
			}
		}
		catch (OutOfMemoryError mem)
		{
			WbManager.getInstance().showOutOfMemoryError();
		}
		catch (Exception e)
		{
			LogMgr.logError("TableSearcher.searchTable()", "Error retrieving data for " + table.getTableExpression(), e);
		}
		finally
		{
			try
			{
				if (rs != null) rs.close();
			}
			catch (Exception ex)
			{
			}
			try
			{
				if (this.query != null) this.query.close();
				this.query = null;
			}
			catch (Exception ex)
			{
			}
		}
	}

	private String buildSqlForTable(TableIdentifier tbl)
		throws SQLException
	{
		DbMetadata meta = this.connection.getMetadata();

		DataStore def = meta.getTableDefinition(tbl);
		int cols = def.getRowCount();
		StringBuilder sql = new StringBuilder(cols * 120);
		sql.append("SELECT * FROM ");
		sql.append(tbl.getTableExpression(this.connection));
		sql.append("\n WHERE ");
		boolean first = true;
		int colcount = 0;
		for (int i=0; i < cols; i++)
		{
			String column = (String)def.getValue(i, DbMetadata.COLUMN_IDX_TABLE_DEFINITION_COL_NAME);
			Integer type = (Integer)def.getValue(i, DbMetadata.COLUMN_IDX_TABLE_DEFINITION_JAVA_SQL_TYPE);
			int sqlType = type.intValue();
			if (SqlUtil.isCharacterType(sqlType))
			{
				colcount ++;
				if (!first)
				{
					sql.append(" OR ");
				}
				if (this.columnFunction != null)
				{
					sql.append(StringUtil.replace(this.columnFunction, "$col$", column));
				}
				else
				{
					sql.append(column);
				}
				sql.append(" LIKE '");
				sql.append(this.criteria);
				sql.append('\'');
				if (i < cols - 1) sql.append('\n');
				first = false;
			}
		}
		if (colcount == 0)
			return null;
		else
			return sql.toString();
	}

	public boolean getCriteriaMightBeCaseInsensitive()
	{
		if (this.columnFunction == null) return false;
		if (this.criteria == null) return false;
		String func = this.columnFunction.toLowerCase();

		// upper() lower() is for Oracle, Postgres, Firebird/Interbase and MS SQL Server
		// lcase, ucase is for Access and HSQLDB
		if (func.indexOf("upper") > -1 || func.indexOf("ucase") > -1)
		{
			return (this.criteria.toUpperCase().equals(this.criteria));
		}
		if (func.indexOf("lower") > -1 || func.indexOf("lcase") > -1)
		{
			return (this.criteria.toLowerCase().equals(this.criteria));
		}
		return false;
	}
	
	public boolean setColumnFunction(String aColFunc)
	{
		this.columnFunction = null;
		boolean result = false;
		if (aColFunc != null && aColFunc.trim().length() > 0)
		{
			if (aColFunc.equalsIgnoreCase("$col$"))
			{
				this.columnFunction = null;
				result = true;
			}
			else if (aColFunc.indexOf("$col$") > -1)
			{
				this.columnFunction = aColFunc;
				result = true;
			}
			else if (aColFunc.indexOf("$COL$") > -1)
			{
				this.columnFunction = StringUtil.replace(aColFunc, "$COL$", "$col$");
				result = true;
			}
		}
		return result;
	}

	public void setTableNames(TableIdentifier[] tables)
	{
		this.tablesToSearch = tables;
	}

	public TableSearchDisplay getDisplay()
	{
		return display;
	}

	public void setDisplay(TableSearchDisplay searchDisplay)
	{
		this.display = searchDisplay;
	}

	public String getCriteria()
	{
		return criteria;
	}

	public void setCriteria(String aText)
	{
    if (aText == null) return;
    this.criteria = StringUtil.trimQuotes(aText);
    return;
	}

	public void setConnection(WbConnection conn)
	{
		this.connection = conn;
	}

	public void setMaxRows(int maxRows)
	{
		this.maxRows = maxRows;
	}

}
