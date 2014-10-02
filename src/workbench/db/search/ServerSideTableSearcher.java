/*
 * ServerSideTableSearcher.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.search;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import workbench.WbManager;
import workbench.interfaces.TableSearchConsumer;
import workbench.log.LogMgr;

import workbench.db.DbMetadata;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.TableSelectBuilder;
import workbench.db.WbConnection;

import workbench.storage.DataStore;
import workbench.storage.filter.ColumnExpression;
import workbench.storage.filter.ContainsComparator;

import workbench.util.ExceptionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbThread;

/**
 * An implementation of the TableSearcher interface that uses SELECT
 * statements with a LIKE condition to search for data.
 *
 * @author Thomas Kellerer
 */
public class ServerSideTableSearcher
	implements TableDataSearcher
{
	private List<TableIdentifier> tablesToSearch;
	private String columnFunction;
	private TableSearchConsumer display;
	private String criteria;
	private WbConnection connection;
	private boolean cancelSearch = false;
	private boolean isRunning = false;
	private Statement query = null;
	private Thread searchThread;
	private int maxRows = 0;
	private boolean retrieveLobColumns = true;
	private DataStore result = null;

	@Override
	public void startBackgroundSearch()
	{
		this.cancelSearch = false;
		this.searchThread = new WbThread("TableSearcher Thread")
		{
			@Override
			public void run()
			{
				search();
			}
		};
		this.searchThread.start();
	}

	@Override
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
		this.isRunning = aFlag;
		if (this.display != null)
		{
			if (aFlag) this.display.searchStarted();
			else this.display.searchEnded();
		}
		if (!aFlag) this.cancelSearch = false;
	}

	@Override
	public boolean isRunning()
	{
		return this.isRunning;
	}

	@Override
	public void search()
	{
		if (this.tablesToSearch == null || this.tablesToSearch.isEmpty()) return;
		this.setRunning(true);
		try
		{
			this.connection.setBusy(true);
			long total = tablesToSearch.size();
			long current = 1;
			for (TableIdentifier tbl : tablesToSearch)
			{
				this.searchTable(tbl, current, total);
				if (this.cancelSearch) break;
				current ++;
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

	@Override
	public void setRetrieveLobColumns(boolean flag)
	{
		this.retrieveLobColumns = flag;
	}

	private void searchTable(TableIdentifier table, long current, long total)
	{
		ResultSet rs = null;
		Savepoint sp = null;
		boolean useSavepoint = connection.getDbSettings().useSavePointForDML();

		try
		{
			String sql = this.buildSqlForTable(table);
			if (this.display != null) this.display.setCurrentTable(table.getTableExpression(), sql, current, total);
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
			result.setResultName(table.getTableName());
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

	private boolean isSearchable(int sqlType, String dbmsType)
	{
		if (sqlType == Types.VARCHAR || sqlType == Types.CHAR ||
			sqlType == Types.NVARCHAR || sqlType == Types.NCHAR)
		{
			return true;
		}
		return connection.getDbSettings().isSearchable(dbmsType);
	}

	private String buildSqlForTable(TableIdentifier tbl)
		throws SQLException
	{
		DbMetadata meta = this.connection.getMetadata();
		TableDefinition def = meta.getTableDefinition(tbl, false);
		return buildSqlForTable(def, "tablesearch");
	}

	public String buildSqlForTable(TableDefinition def, String sqlTemplateKey)
		throws SQLException
	{
		int colCount = def.getColumnCount();
		if (colCount == 0) return StringUtil.EMPTY_STRING;

		StringBuilder sql = new StringBuilder(colCount * 120);
		TableSelectBuilder builder = new TableSelectBuilder(this.connection, sqlTemplateKey);
		builder.setIncludeBLOBColumns(this.retrieveLobColumns);
		builder.setIncludeCLOBColumns(this.retrieveLobColumns);
		sql.append(builder.getSelectForColumns(def.getTable(), def.getColumns()));
		sql.append("\n WHERE ");
		boolean first = true;
		int colcount = 0;

		Pattern aliasPattern = Pattern.compile("\\s+AS\\s+", Pattern.CASE_INSENSITIVE);

		for (int i=0; i < colCount; i++)
		{
			String colName = def.getColumns().get(i).getColumnName();
			String dbmsType = def.getColumns().get(i).getDbmsType();
			int sqlType = def.getColumns().get(i).getDataType();
			String expr = builder.getColumnExpression(def.getColumns().get(i));
			boolean isExpression = !colName.equalsIgnoreCase(expr);

			if (isExpression || isSearchable(sqlType, dbmsType))
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
			LogMgr.logWarning("TableSearcher.buildSqlForTable()", "Table " + def.getTable().getTableExpression() + " not beeing searched because no character columns were found");
			return null;
		}
		else
		{
			return sql.toString();
		}
	}

	public boolean isCaseSensitive()
	{
		if (this.columnFunction == null) return false;
		if (this.criteria == null) return false;

		boolean sensitive = this.connection.getDbSettings().isStringComparisonCaseSensitive();

		if (!sensitive) return true;

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

	@Override
	public void setTableNames(List<TableIdentifier> tables)
	{
		if (tables == null)
		{
			this.tablesToSearch = new ArrayList<>(0);
		}
		else
		{
			this.tablesToSearch = new ArrayList<>(tables);
		}
	}

	public TableSearchConsumer getDisplay()
	{
		return display;
	}

	@Override
	public void setConsumer(TableSearchConsumer searchDisplay)
	{
		this.display = searchDisplay;
	}

	@Override
	public String getCriteria()
	{
		return criteria;
	}

	@Override
	public void setCriteria(String aText, boolean ignoreCase)
	{
		if (aText == null) return;
		this.criteria = StringUtil.trimQuotes(aText);
	}

	@Override
	public void setConnection(WbConnection conn)
	{
		this.connection = conn;
	}

	@Override
	public void setMaxRows(int max)
	{
		this.maxRows = max;
	}

	@Override
	public ColumnExpression getSearchExpression()
	{
		String expressionPattern = StringUtil.trimQuotes(criteria.replaceAll("[%_]", ""));
		ColumnExpression searchPattern = new ColumnExpression("*", new ContainsComparator(), expressionPattern);
		searchPattern.setIgnoreCase(isCaseSensitive());

		return searchPattern;
	}

}
