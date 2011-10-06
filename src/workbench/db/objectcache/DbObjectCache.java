/*
 * DbObjectCacheWrapper
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.objectcache;

import java.util.List;
import java.util.Set;
import workbench.db.ColumnIdentifier;
import workbench.db.ProcedureDefinition;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.storage.DataStore;

/**
 * A wrapper around ObjectCache in order to avoid having to supply the Connection for each call.
 *
 * @author Thomas Kellerer
 */
public class DbObjectCache
{
	private final ObjectCache objectCache;
	private final WbConnection dbConnection;

	DbObjectCache(ObjectCache cache, WbConnection connection)
	{
		dbConnection = connection;
		objectCache = cache;
	}

	public void addTable(TableDefinition table)
	{
		objectCache.addTable(table);
	}

	public void addTableList(DataStore tables, String schema)
	{
		objectCache.addTableList(dbConnection, tables, schema);
	}

	public void clear()
	{
		objectCache.clear();
	}

	public List<ColumnIdentifier> getColumns(TableIdentifier tbl)
	{
		return objectCache.getColumns(dbConnection, tbl);
	}

	public List<ProcedureDefinition> getProcedures(String schema)
	{
		return objectCache.getProcedures(dbConnection, schema);
	}

	public Set<TableIdentifier> getTables(String schema)
	{
		return objectCache.getTables(dbConnection, schema, null);
	}

	public Set<TableIdentifier> getTables(String schema, List<String> type)
	{
		return objectCache.getTables(dbConnection, schema, type);
	}

	public void removeTable(TableIdentifier tbl)
	{
		objectCache.removeTable(dbConnection, tbl);
	}

	public boolean supportsSearchPath()
	{
		return (dbConnection != null && dbConnection.getMetadata().isPostgres());
	}

	public List<String> getSearchPath(String defaultSchema)
	{
		return objectCache.getSearchPath(dbConnection, defaultSchema);
	}

	public TableIdentifier getTable(TableIdentifier table)
	{
		return objectCache.findEntry(table);
	}

}
