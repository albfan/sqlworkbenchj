/*
 * TableSearcher.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Types;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import workbench.WbManager;
import workbench.interfaces.TableSearchDisplay;
import workbench.log.LogMgr;
import workbench.storage.DataStore;
import workbench.util.ExceptionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbThread;

/**
 * @author  support@sql-workbench.net
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
	private boolean excludeLobColumns = true;
	private DataStore result = null;

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
			if (this.result != null)
			{
				result.cancelRetrieve();
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

	public void setExcludeLobColumns(boolean flag)
	{
		this.excludeLobColumns = flag;
	}

	private void searchTable(TableIdentifier table)
	{
		ResultSet rs = null;
		Savepoint sp = null;
		boolean useSavepoint = connection.getDbSettings().useSavePointForDML();

		try
		{
			String sql = this.buildSqlForTable(table);
			if (this.display != null) this.display.setCurrentTable(table.getTableExpression(), sql);
			if (sql == null) return;

			if (!connection.getAutoCommit() && useSavepoint)
			{
				try
				{
					sp = connection.setSavepoint();
				}
				catch (SQLException e)
				{
					LogMgr.logWarning("TableSearcher.searchTable()", "Could not create savepoint", e);
					sp = null;
					useSavepoint = false;
				}
			}
			this.query = this.connection.createStatementForQuery();
			this.query.setMaxRows(this.maxRows);

			rs = this.query.executeQuery(sql);
			result = new DataStore(rs, this.connection, true);
			result.setGeneratingSql(sql);
			result.setUpdateTableToBeUsed(table);

			if (this.display != null) this.display.tableSearched(table, result);
			result = null;

			if (sp != null)
			{
				connection.releaseSavepoint(sp);
				sp = null;
			}
		}
		catch (OutOfMemoryError mem)
		{
			WbManager.getInstance().showOutOfMemoryError();
		}
		catch (Exception e)
		{
			LogMgr.logError("TableSearcher.searchTable()", "Error retrieving data for " + table.getTableExpression(), e);
			if (this.display != null) this.display.error(ExceptionUtil.getDisplay(e));
			if (sp != null)
			{
				connection.rollback(sp);
			}
		}
		finally
		{
			SqlUtil.closeAll(rs, query);
			this.query = null;
			if (sp != null)
			{
				connection.releaseSavepoint(sp);
			}
		}
	}

	private String buildSqlForTable(TableIdentifier tbl)
		throws SQLException
	{
		DbMetadata meta = this.connection.getMetadata();

		TableDefinition def = meta.getTableDefinition(tbl);
		int colCount = def.getColumnCount();
		if (colCount == 0) return StringUtil.EMPTY_STRING;

		StringBuilder sql = new StringBuilder(colCount * 120);
		TableSelectBuilder builder = new TableSelectBuilder(this.connection);
		builder.setExcludeLobColumns(this.excludeLobColumns);
		sql.append(builder.getSelectForColumns(tbl, def.getColumns()));
		sql.append("\n WHERE ");
		boolean first = true;
		int colcount = 0;

		Pattern aliasPattern = Pattern.compile("\\s+AS\\s+", Pattern.CASE_INSENSITIVE);

		for (int i=0; i < colCount; i++)
		{
			String colName = def.getColumns().get(i).getColumnName();
			int sqlType = def.getColumns().get(i).getDataType();
			String expr = builder.getColumnExpression(def.getColumns().get(i));
			boolean isExpression = !colName.equalsIgnoreCase(expr);
			
			if (isExpression || sqlType == Types.VARCHAR || sqlType == Types.CHAR)
			{
				if (!isExpression)
				{
					expr = this.connection.getMetadata().quoteObjectname(colName);
				}
				else
				{
					// Check if the column expression was defined with a column alias
					// in that case we have to remove the alias otherwise it cannot be
					// used in a WHERE condition
					Matcher m = aliasPattern.matcher(expr);
					if (m.find())
					{
						int pos = m.start();
						expr = expr.substring(0, pos);
					}
				}

				colcount ++;
				if (!first)
				{
					sql.append(" OR ");
				}

				if (this.columnFunction != null)
				{
					sql.append(StringUtil.replace(this.columnFunction, "$col$", expr));
				}
				else
				{
					sql.append(expr);
				}
				sql.append(" LIKE '");
				sql.append(this.criteria);
				sql.append('\'');
				if (i < colCount - 1) sql.append('\n');

				first = false;
			}
		}
		if (colcount == 0)
		{
			LogMgr.logWarning("TableSearcher.buildSqlForTable()", "Table " + tbl.getTableExpression() + " not beeing searched because no character columns were found");
			return null;
		}
		else
		{
			return sql.toString();
		}
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
			return this.criteria.toUpperCase().equals(this.criteria);
		}
		if (func.indexOf("lower") > -1 || func.indexOf("lcase") > -1)
		{
			return this.criteria.toLowerCase().equals(this.criteria);
		}
		return false;
	}

	public boolean setColumnFunction(String aColFunc)
	{
		this.columnFunction = null;
		boolean setResult = false;
		if (StringUtil.isNonBlank(aColFunc))
		{
			if (aColFunc.equalsIgnoreCase("$col$"))
			{
				this.columnFunction = null;
				setResult = true;
			}
			else if (aColFunc.indexOf("$col$") > -1)
			{
				this.columnFunction = aColFunc;
				setResult = true;
			}
			else if (aColFunc.indexOf("$COL$") > -1)
			{
				this.columnFunction = StringUtil.replace(aColFunc, "$COL$", "$col$");
				setResult = true;
			}
		}
		return setResult;
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

	public void setMaxRows(int max)
	{
		this.maxRows = max;
	}

}
