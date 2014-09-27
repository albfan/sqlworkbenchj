/*
 * LookupDataLoader.java
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
package workbench.storage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import workbench.log.LogMgr;

import workbench.db.DependencyNode;
import workbench.db.PkDefinition;
import workbench.db.QuoteHandler;
import workbench.db.ReferenceTableNavigation;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.TableSelectBuilder;
import workbench.db.WbConnection;
import workbench.db.search.ServerSideTableSearcher;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;

/**
 * A class to retrieve the data from the table that is referenced through a FK constraint from another table.
 * <br/>
 * This serves a different purpose than {@link ReferenceTableNavigation} which is
 * used to retrieve the data that is referenced by (or referencing) specific <b>values</b>
 * in a table.
 * <br/>
 * The DataStore returned by getLookupData() can e.g. be used to fill a DropDown or a popup window to
 * select one of the possible values to make editing FK columns easier.
 *
 * @author Thomas Kellerer
 */
public class LookupDataLoader
{
	private TableDefinition lookupTable;
	private boolean retrieved;
	private TableIdentifier baseTable;
	private String referencingColumn;
	private Map<String, String> columnMap;

	/**
	 * Create a new LookupDataLoader
	 *
	 * @param table   the table that contains the foreign key column
	 * @param column  the column for which to load the lookup data
	 */
	public LookupDataLoader(TableIdentifier table, String column)
	{
		referencingColumn = column;
		baseTable = table;
	}

	public DataStore getLookupData(WbConnection conn, int maxRows, String searchValue, boolean useOrderBy)
		throws SQLException
	{
		if (lookupTable == null && !retrieved)
		{
			retrieveReferencedTable(conn);
		}
		if (lookupTable == null)
		{
			return null;
		}
		String sql = null;
		if (searchValue == null)
		{
			TableSelectBuilder builder = new TableSelectBuilder (conn, "lookupretrieval");
			builder.setSortPksFirst(true);
			builder.setIncludeBLOBColumns(false);
			builder.setIncludeCLOBColumns(false);
			sql = builder.getSelectForColumns(lookupTable.getTable(), lookupTable.getColumns());
		}
		else
		{
			ServerSideTableSearcher searcher = new ServerSideTableSearcher();
			searcher.setColumnFunction(conn.getDbSettings().getLowerFunctionTemplate());
			searcher.setCriteria(searchValue, true);
			searcher.setConnection(conn);
			sql = searcher.buildSqlForTable(lookupTable, "lookupretrieval");
		}

		if (useOrderBy)
		{
			sql += getOrderBy(conn, lookupTable.getTable().getPrimaryKey());
		}

		Statement stmt = null;
		ResultSet rs = null;
		DataStore result = null;

		LogMgr.logDebug("LookupDataLoader.getLookupData()", "Using sql: " + sql);

		try
		{
			stmt = conn.createStatementForQuery();
			rs = stmt.executeQuery(sql);
			result = new DataStore(rs, true, maxRows);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}

	private String getOrderBy(WbConnection conn, PkDefinition pk)
	{
		if (pk == null || CollectionUtil.isEmpty(pk.getColumns())) return "";

		QuoteHandler handler = (conn == null ? QuoteHandler.STANDARD_HANDLER : conn.getMetadata());

		StringBuilder order = new StringBuilder(50);
		order.append(" ORDER BY ");
		int cols = 0;
		for (String colName : pk.getColumns())
		{
			if (cols > 0)
			{
				order.append(',');
			}
			order.append(handler.quoteObjectname(colName));
			cols++;
		}
		return order.toString();
	}

	/**
	 * Get the mapping between the foreign keys and the primary keys for this loader.
	 *
	 * The key to the map is the PK column from the table being referenced.
	 * The value is the FK column of the referencing table.
	 *
	 * @return the FK to PK mapping
	 */
	public Map<String, String> getForeignkeyMap()
	{
		return Collections.unmodifiableMap(columnMap);
	}

	public List<String> getReferencingColumns()
	{
		return new ArrayList<>(columnMap.values());
	}

	public void retrieveReferencedTable(WbConnection conn)
		throws SQLException
	{
		try
		{
			lookupTable = null;
			TableIdentifier table = null;
			List<DependencyNode> nodes = conn.getObjectCache().getReferencedTables(baseTable);
			for (DependencyNode node : nodes)
			{
				Map<String, String> columns = node.getColumns();

				for (Map.Entry<String, String> entry : columns.entrySet())
				{
					if (entry.getValue().equalsIgnoreCase(referencingColumn))
					{
						table = node.getTable();
						columnMap = node.getColumns();
					}
				}
			}
			// can't use the objectCache here because I also need the PK of the table
			conn.setBusy(true);
			lookupTable = conn.getMetadata().getTableDefinition(table);
		}
		finally
		{
			conn.setBusy(false);
			retrieved = true;
		}
	}

	/**
	 * Return the cached lookup table.
	 *
	 * {@link #retrieveReferencedTable(workbench.db.WbConnection)} must be called before
	 * calling this method.
	 *
	 * @return the referenced table or null if no FK was found or retrieveReferencedTable() was not yet called
	 */
	public TableIdentifier getLookupTable()
	{
		if (lookupTable == null) return null;
		return lookupTable.getTable();
	}

	/**
	 * Return the PK of the lookup table.
	 *
	 * {@link #retrieveReferencedTable(workbench.db.WbConnection)} must be called before
	 * calling this method, otherwise null will be returned.
	 *
	 * @return the PK of the referenced table or null if no FK was found or retrieveReferencedTable() was not yet called
	 * @see #getLookupTable()
	 * @see #getReferencingColumns()
	 */
	public PkDefinition getPK()
	{
		if (lookupTable == null) return null;
		return lookupTable.getTable().getPrimaryKey();
	}

}
