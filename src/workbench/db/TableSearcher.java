/*
 * TableSearcher.java
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

import workbench.gui.WbSwingUtilities;
import workbench.interfaces.TableSearchDisplay;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.storage.DataStore;
import workbench.util.StringUtil;

/**
 *
 * @author  kellererth
 */
public class TableSearcher
{
	private List tableNames;
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
		this.createThread();
	}

	private Thread createThread()
	{
		Thread t = new Thread()
		{
			public void run()
			{
				doSearch();
			}
		};
		t.setName("TableSearcher Thread");
		t.setDaemon(true);
		return t;
	}

	public void search()
	{
		this.cancelSearch = false;
		this.searchThread = this.createThread();
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
				if (this.getConnection().cancelNeedsReconnect())
				{
					this.getConnection().reconnect();
				}
			}
			//this.setRunning(false);
		}
		catch (Exception e)
		{
		}
	}

	private synchronized void setRunning(boolean aFlag)
	{
		this.isRunning = aFlag;
		if (this.display != null)
		{
			if (aFlag) this.display.searchStarted();
			else this.display.searchEnded();
		}
		if (!aFlag) this.cancelSearch = false;
	}

	public synchronized boolean isRunning() { return this.isRunning; }


	private void doSearch()
	{
		if (this.tableNames == null || this.tableNames.size() == 0) return;
		this.setRunning(true);
		try
		{
			for (int i=0; i < this.tableNames.size(); i++)
			{
				this.searchTable((String)this.tableNames.get(i));
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
		}
	}

	private void searchTable(String aTable)
	{
		ResultSet rs = null;
		try
		{
			String sql = this.buildSqlForTable(aTable);
			if (sql == null) return;
			if (this.display != null) this.display.setCurrentTable(aTable, sql);

			this.query = this.connection.getSqlConnection().createStatement();
			this.query.setMaxRows(this.maxRows);
			//LogMgr.logInfo("TableSearcher", "Using SQL:\n" + sql);
			rs = this.query.executeQuery(sql);
			while (rs != null && rs.next())
			{
				if (this.cancelSearch)
				{
					break;
				}
				if (this.display != null)this.display.addResultRow(aTable, rs);
			}
		}
		catch (OutOfMemoryError mem)
		{
			WbSwingUtilities.showErrorMessage(null, ResourceMgr.getString("MsgOutOfMemoryError"));
		}
		catch (Exception e)
		{
			LogMgr.logError("TableSearcher.searchTable()", "Error retrieving data for " + aTable, e);
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

	private String buildSqlForTable(String aTable)
		throws SQLException
	{
		DbMetadata meta = this.connection.getMetadata();
		String tablename = aTable;
		String schema = null;
		int pos = aTable.indexOf('.');
		if (pos > -1)
		{
			tablename = aTable.substring(pos + 1);
			schema = aTable.substring(0, pos);
		}

		DataStore def = meta.getTableDefinition(null, schema, tablename);
		int cols = def.getRowCount();
		StringBuffer sql = new StringBuffer(cols * 120);
		sql.append("SELECT * FROM ");
		sql.append(aTable);
		sql.append("\n WHERE ");
		boolean first = true;
		int colcount = 0;
		for (int i=0; i < cols; i++)
		{
			String column = (String)def.getValue(i, DbMetadata.COLUMN_IDX_TABLE_DEFINITION_COL_NAME);
			Integer type = (Integer)def.getValue(i, DbMetadata.COLUMN_IDX_TABLE_DEFINITION_JAVA_SQL_TYPE);
			int sqlType = type.intValue();
			boolean isChar = (sqlType == Types.VARCHAR || sqlType == Types.CHAR);
			if (isChar)
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

	/** Getter for property tableNames.
	 * @return Value of property tableNames.
	 *
	 */
	public java.util.List getTableNames()
	{
		return tableNames;
	}

	/** Setter for property tableNames.
	 * @param tableNames New value of property tableNames.
	 *
	 */
	public void setTableNames(java.util.List tableNames)
	{
		this.tableNames = tableNames;
	}

	/** Getter for property display.
	 * @return Value of property display.
	 *
	 */
	public workbench.interfaces.TableSearchDisplay getDisplay()
	{
		return display;
	}

	/** Setter for property display.
	 * @param display New value of property display.
	 *
	 */
	public void setDisplay(workbench.interfaces.TableSearchDisplay display)
	{
		this.display = display;
	}

	/** Getter for property criteria.
	 * @return Value of property criteria.
	 *
	 */
	public java.lang.String getCriteria()
	{
		return criteria;
	}

	/** Setter for property criteria.
	 * @param criteria New value of property criteria.
	 *
	 */
	public void setCriteria(java.lang.String aText)
	{
    if (aText == null) return;
    if (aText.startsWith("'"))
    {
      int pos = 1;
      int len = aText.length();
      if (aText.endsWith("'")) len--;
      this.criteria = aText.substring(pos, len);
    }
    else
    {
  		this.criteria = aText;
    }
    return;
	}

	/** Getter for property connection.
	 * @return Value of property connection.
	 *
	 */
	public workbench.db.WbConnection getConnection()
	{
		return connection;
	}

	/** Setter for property connection.
	 * @param connection New value of property connection.
	 *
	 */
	public void setConnection(workbench.db.WbConnection connection)
	{
		this.connection = connection;
	}


	/** Getter for property maxRows.
	 * @return Value of property maxRows.
	 *
	 */
	public int getMaxRows()
	{
		return maxRows;
	}

	/** Setter for property maxRows.
	 * @param maxRows New value of property maxRows.
	 *
	 */
	public void setMaxRows(int maxRows)
	{
		this.maxRows = maxRows;
	}

}
