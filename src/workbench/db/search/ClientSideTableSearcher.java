/*
 * ClientSideTableSearcher.java
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
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;

import workbench.interfaces.TableSearchConsumer;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.db.TableIdentifier;
import workbench.db.TableSelectBuilder;
import workbench.db.WbConnection;

import workbench.storage.DataStore;
import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.storage.RowDataReader;
import workbench.storage.RowDataReaderFactory;
import workbench.storage.filter.ColumnComparator;
import workbench.storage.filter.ColumnExpression;

import workbench.util.CollectionUtil;
import workbench.util.EncodingUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbThread;

/**
 * An implementation fo the TableDataSearch interface to perform a search on the client side.
 *
 * The clas reads each row into memory and compares the data using the functionality of
 * a {@link workbench.storage.filter.ColumnComparator} to retain only those rows that match the criteria.
 *
 * @author Thomas Kellerer
 */
public class ClientSideTableSearcher
	implements TableDataSearcher
{
	private String searchString;
	private boolean isRunning;
	private boolean includeBLOBs;
	private boolean includeCLOBs;

	private int maxRows = Integer.MAX_VALUE;
	private List<TableIdentifier> tablesToSearch;
	private WbConnection connection;
	private boolean cancelSearch;
	private Thread searchThread;
	private Statement searchQuery;
	private TableSearchConsumer consumer;
	private RowDataSearcher searcher;
	private ColumnComparator comparator;
	private boolean treatBlobAsText;
	private String blobEncoding;

	public ClientSideTableSearcher()
	{
	}

	@Override
	public String getCriteria()
	{
		return searchString;
	}

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

	public void setTreatBlobAsText(boolean flag, String encoding)
	{
		this.treatBlobAsText = flag;
		if (treatBlobAsText && EncodingUtil.isEncodingSupported(encoding))
		{
			this.blobEncoding = encoding;
			this.includeBLOBs = true;
		}
		else
		{
			this.blobEncoding = null;
		}
	}

	public void setRetrieveBLOBs(boolean flag)
	{
		this.includeBLOBs = flag;
		this.blobEncoding = null; // only retrieve the BLOBs don't retrieve them
	}

	public void setIncludeCLOBs(boolean flag)
	{
		this.includeCLOBs = flag;
	}

	@Override
	public void search()
	{
		if (isRunning) return;
		cancelSearch = false;
		setRunning(true);
		try
		{
			long total = tablesToSearch.size();
			long current = 1;
			for (TableIdentifier table : tablesToSearch)
			{
				if (cancelSearch) break;
				searchTable(table, current, total);
				current++;
			}
		}
		finally
		{
			setRunning(false);
		}
	}

	protected void searchTable(TableIdentifier table, long current, long total)
	{
		Savepoint sp = null;
		ResultSet rs = null;
		try
		{
			if (connection.getDbSettings().useSavePointForDML())
			{
				sp = connection.setSavepoint();
			}

			TableSelectBuilder builder = new TableSelectBuilder(connection, "tablesearch");
			builder.setIncludeBLOBColumns(includeBLOBs);
			builder.setIncludeCLOBColumns(includeCLOBs);

			String sql = builder.getSelectForTable(table, -1);
			if (StringUtil.isEmptyString(sql))
			{
				LogMgr.logWarning("CleintSideTableSearcher.searchTable()", "No SELECT generated for " + table.getTableExpression() + ". Most probably the table was not fund");
				return;
			}

			if (consumer != null)
			{
				consumer.setCurrentTable(table.getTableName(), sql, current, total);
			}
			searchQuery = connection.createStatementForQuery();

			rs = searchQuery.executeQuery(sql);

			ResultInfo info = new ResultInfo(rs.getMetaData(), connection);
			DataStore result = new DataStore(rs.getMetaData(), connection);
			String explain = "-- " + ResourceMgr.getFormattedString("TxtSearchFilter", this.comparator.getDescription() + " '"  + this.searchString + "'\n\n");
			result.setGeneratingSql(explain + sql);
			result.setResultName(table.getTableName());
			result.setUpdateTableToBeUsed(table);

			boolean trimCharData = this.connection.trimCharData();

			// by specifying a blob encoding the search will automatically convert
			// blob data to a string using the specified encoding
			searcher.setBlobTextEncoding(blobEncoding);

			RowDataReader reader = RowDataReaderFactory.createReader(info, connection);
			while (rs.next())
			{
				if (cancelSearch) break;
				RowData row = reader.read(rs, trimCharData);
				if (searcher.isSearchStringContained(row, info))
				{
					result.addRow(row);
				}
				reader.closeStreams();

				if (result.getRowCount() > maxRows) break;
			}

			if (consumer != null)
			{
				consumer.tableSearched(table, result);
			}
			connection.releaseSavepoint(sp);
		}
		catch (Throwable e)
		{
			LogMgr.logError("ClientSideTableSearcher.searchTable", "Error searching table", e);
			connection.rollback(sp);
		}
		finally
		{
			SqlUtil.closeAll(rs, this.searchQuery);
		}
	}

	@Override
	public boolean isRunning()
	{
		return this.isRunning;
	}

	@Override
	public void setConnection(WbConnection conn)
	{
		connection = conn;
	}

	public void setComparator(ColumnComparator comp)
	{
		this.comparator = comp;
	}

	@Override
	public void setCriteria(String search, boolean ignoreCase)
	{
		setCriteria(search, ignoreCase, null);
	}

	public void setCriteria(String search, boolean ignoreCase, Collection<String> columns)
	{
		if (StringUtil.isNonBlank(search))
		{
			searchString = search.trim();
			if (searchString.charAt(0) == '%')
			{
				searchString = searchString.substring(1);
			}
			if (searchString.endsWith("%"))
			{
				searchString = searchString.substring(0, searchString.length() - 2);
			}
		}
		else
		{
			searchString = null;
		}
		searcher = new RowDataSearcher(searchString, comparator, ignoreCase);
		searcher.setColumnsToSearch(columns);
	}

	@Override
	public void setConsumer(TableSearchConsumer searchConsumer)
	{
		consumer = searchConsumer;
	}

	@Override
	public void setRetrieveLobColumns(boolean flag)
	{
		includeBLOBs = flag;
		includeCLOBs = flag;
		blobEncoding = null; // only retrieve blobs, don't search them
	}

	@Override
	public void setMaxRows(int max)
	{
		maxRows = max <= 0 ? Integer.MAX_VALUE : max;
	}

	@Override
	public void setTableNames(List<TableIdentifier> tables)
	{
		tablesToSearch = CollectionUtil.arrayList(tables);
	}

	@Override
	public ColumnExpression getSearchExpression()
	{
		return searcher.getExpression();
	}
}
