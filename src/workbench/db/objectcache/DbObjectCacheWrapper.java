/*
 * DbObjectCacheWrapper
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer No part of this code may be reused without the permission of the author
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
 *
 * @author Thomas Kellerer
 */
public class DbObjectCacheWrapper
	implements DbObjectCache
{

	private ObjectCache objectCache;
	private WbConnection dbConnection;

	DbObjectCacheWrapper(ObjectCache cache, WbConnection connection)
	{
		dbConnection = connection;
		objectCache = cache;
	}

	@Override
	public void addTable(TableDefinition table)
	{
		objectCache.addTable(table);
	}

	@Override
	public void addTableList(DataStore tables, String schema)
	{
		objectCache.addTableList(dbConnection, tables, schema);
	}

	@Override
	public void clear()
	{
		objectCache.clear();
	}

	@Override
	public List<ColumnIdentifier> getColumns(TableIdentifier tbl)
	{
		return objectCache.getColumns(dbConnection, tbl);
	}

	@Override
	public List<ProcedureDefinition> getProcedures(String schema)
	{
		return objectCache.getProcedures(dbConnection, schema);
	}

	@Override
	public Set<TableIdentifier> getTables(String schema)
	{
		return objectCache.getTables(dbConnection, schema);
	}

	@Override
	public Set<TableIdentifier> getTables(String schema, List<String> type)
	{
		return objectCache.getTables(dbConnection, schema, type);
	}

	@Override
	public void removeTable(TableIdentifier tbl)
	{
		objectCache.removeTable(tbl);
	}

}
