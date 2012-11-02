/*
 * LookupDataLoader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.storage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
 * A class to retrieve the data from the table a specific column references.
 * <br/>
 * This server a different purpose than {@link ReferenceTableNavigation} which is
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
	private String referencedColumn;

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

	public DataStore getLookupData(WbConnection conn, int maxRows)
		throws SQLException
	{
		return getLookupData(conn, maxRows, null);
	}

	public DataStore getLookupData(WbConnection conn, int maxRows, String searchValue)
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
			builder.setExcludeLobColumns(true);
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

		sql += getOrderBy(conn, lookupTable.getTable().getPrimaryKey());

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
		}
		return order.toString();
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
						referencedColumn = entry.getKey();
					}
				}
			}
			lookupTable = conn.getMetadata().getTableDefinition(table);
		}
		finally
		{
			retrieved = true;
		}
	}

	public String getReferencedColumn()
	{
		return referencedColumn;
	}
	/**
	 * Return the cached lookup table.
	 *
	 * {@link #retrieveReferencedTable(workbench.db.WbConnection)} must be called before
	 * calling this method.
	 *
	 * @return  the lookup table.
	 */
	public TableIdentifier getReferencedTable()
	{
		if (lookupTable == null) return null;
		return lookupTable.getTable();
	}

	public PkDefinition getPK()
	{
		if (lookupTable == null) return null;
		return lookupTable.getTable().getPrimaryKey();
	}

}
