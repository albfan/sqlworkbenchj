/*
 * ClientSideTableSearcher
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.search;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.List;
import workbench.db.TableIdentifier;
import workbench.db.TableSelectBuilder;
import workbench.db.WbConnection;
import workbench.interfaces.TableSearchConsumer;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.storage.DataStore;
import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.storage.filter.ColumnComparator;
import workbench.storage.filter.ColumnExpression;
import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbThread;

/**
 *
 * @author Thomas Kellerer
 */
public class ClientSideTableSearcher
	implements TableDataSearcher
{
	private String searchString;
	private boolean isRunning;
	private boolean excludeLobs;
	private int maxRows = Integer.MAX_VALUE;
	private List<TableIdentifier> tablesToSearch;
	private WbConnection connection;
	private boolean cancelSearch;
	private Thread searchThread;
	private Statement searchQuery;
	private TableSearchConsumer consumer;
	private RowDataSearcher searcher;
	private ColumnComparator comparator;

	public ClientSideTableSearcher()
	{
	}

	public String getCriteria()
	{
		return searchString;
	}

	public void startBackgroundSearch()
	{
		this.cancelSearch = false;
		this.searchThread = new WbThread("TableSearcher Thread")
		{
			public void run()
			{
				search();
			}
		};
		this.searchThread.start();
	}

	public void cancelSearch()
	{
		this.cancelSearch = true;
		try
		{
			if (this.searchThread != null)
			{
				this.searchThread.interrupt();
			}

			if (this.searchQuery != null)
			{
				this.searchQuery.cancel();
			}
		}
		catch (Throwable e)
		{
			LogMgr.logWarning("TableSearcher.cancelSearc()", "Error when cancelling", e);
		}
	}

	private void setRunning(boolean runningFlag)
	{
		this.isRunning = runningFlag;
		if (this.consumer != null)
		{
			if (runningFlag)
			{
				this.consumer.searchStarted();
			}
			else
			{
				this.consumer.searchEnded();
			}
		}
	}

	public void search()
	{
		if (isRunning) return;
		cancelSearch = false;
		setRunning(true);
		try
		{
			for (TableIdentifier table : tablesToSearch)
			{
				if (cancelSearch) break;
				searchTable(table);
			}
		}
		finally
		{
			setRunning(false);
		}
	}

	protected void searchTable(TableIdentifier table)
	{
		Savepoint sp = null;
		ResultSet rs = null;
		try
		{
			TableSelectBuilder builder = new TableSelectBuilder(connection);
			builder.setExcludeLobColumns(excludeLobs);
			String sql = builder.getSelectForTable(table);
			if (consumer != null)
			{
				consumer.setCurrentTable(table.getTableName(), sql);
			}
			searchQuery = connection.createStatementForQuery();

			if (cancelSearch) return;


			if (connection.getDbSettings().useSavePointForDML())
			{
				sp = connection.setSavepoint();
			}

			rs = searchQuery.executeQuery(sql);

			ResultInfo info = new ResultInfo(rs.getMetaData(), connection);
			DataStore result = new DataStore(rs.getMetaData(), connection);
			String explain = "-- " + ResourceMgr.getFormattedString("TxtSearchFilter", this.comparator.getDescription() + " '"  + this.searchString + "'\n\n");
			result.setGeneratingSql(explain + sql);
			result.setResultName(table.getTableName());
			result.setUpdateTableToBeUsed(table);
			
			while (rs.next())
			{
				if (cancelSearch) break;
				RowData row = new RowData(info.getColumnCount());
				row.read(rs, info);
				if (searcher.isSearchStringContained(row, info))
				{
					result.addRow(row);
				}
				if (cancelSearch) break;
				if (result.getRowCount() > maxRows) break;
			}

			if (consumer != null)
			{
				consumer.tableSearched(table, result);
			}
			connection.releaseSavepoint(sp);
		}
		catch (SQLException sql)
		{
			LogMgr.logError("ClientSideTableSearcher.searchTable", "Error searching table", sql);
			connection.rollback(sp);
		}
		finally
		{
			SqlUtil.closeAll(rs, this.searchQuery);
		}
	}

	public boolean isRunning()
	{
		return this.isRunning;
	}

	public void setConnection(WbConnection conn)
	{
		connection = conn;
	}

	public void setComparator(ColumnComparator comp)
	{
		this.comparator = comp;
	}

	public void setCriteria(String search, boolean ignoreCase)
	{
		if (StringUtil.isBlank(search)) return;

		searchString = search.trim();
		if (searchString.charAt(0) == '%')
		{
			searchString = searchString.substring(1);
		}
		if (searchString.endsWith("%"))
		{
			searchString = searchString.substring(0, searchString.length() - 2);
		}
		searcher = new RowDataSearcher(searchString, comparator, ignoreCase);
	}

	public void setConsumer(TableSearchConsumer searchConsumer)
	{
		consumer = searchConsumer;
	}

	public void setExcludeLobColumns(boolean flag)
	{
		excludeLobs = flag;
	}

	public void setMaxRows(int max)
	{
		maxRows = max <= 0 ? Integer.MAX_VALUE : max;
	}

	public void setTableNames(List<TableIdentifier> tables)
	{
		tablesToSearch = CollectionUtil.arrayList(tables);
	}

	public ColumnExpression getSearchExpression()
	{
		return searcher.getExpression();
	}
}
