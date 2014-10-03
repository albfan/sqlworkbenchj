/*
 * ObjectCache.java
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
package workbench.db.objectcache;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.DbSearchPath;
import workbench.db.DependencyNode;
import workbench.db.IndexDefinition;
import workbench.db.IndexReader;
import workbench.db.ObjectNameFilter;
import workbench.db.PkDefinition;
import workbench.db.ProcedureDefinition;
import workbench.db.ReaderFactory;
import workbench.db.TableDefinition;
import workbench.db.TableDependency;
import workbench.db.TableIdentifier;
import workbench.db.TableNameSorter;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A cache for database objects to support auto-completion in the editor
 *
 * @author  Thomas Kellerer
 */
class ObjectCache
{
	private static final String NULL_SCHEMA = "-$$wb-null-schema$$-";
	private boolean retrieveOraclePublicSynonyms;

	private final Set<String> schemasInCache = CollectionUtil.caseInsensitiveSet();
	private final Map<TableIdentifier, List<DependencyNode>> referencedTables = new HashMap<>();
	private final Map<TableIdentifier, List<DependencyNode>> referencingTables = new HashMap<>();
	private final Map<TableIdentifier, List<ColumnIdentifier>> objects = new HashMap<>();
	private final Map<TableIdentifier, TableIdentifier> synonymMap = new HashMap<>();
	private final Map<TableIdentifier, List<IndexDefinition>> indexMap= new HashMap<>();
	private final Map<TableIdentifier, PkDefinition> pkMap = new HashMap<>();
	private final Map<String, List<ProcedureDefinition>> procedureCache = new HashMap<>();
	private ObjectNameFilter schemaFilter;
	private ObjectNameFilter catalogFilter;
	private boolean supportsSchemas;

	private final TableIdentifier dummyTable = new TableIdentifier("-$WB DUMMY$-", "-$WB DUMMY$-");

	ObjectCache(WbConnection conn)
	{
		retrieveOraclePublicSynonyms = conn.getMetadata().isOracle() && Settings.getInstance().getBoolProperty("workbench.editor.autocompletion.oracle.public_synonyms", false);
		schemaFilter = conn.getProfile().getSchemaFilter();
		catalogFilter = conn.getProfile().getCatalogFilter();
		supportsSchemas = conn.getDbSettings().supportsSchemas();
	}

	private String[] getCompletionTypes(WbConnection conn)
	{
		String dbId = conn.getDbId();
		List<String> typeList = Settings.getInstance().getListProperty("workbench.db." + dbId + ".completion.types", true, null);

		if (CollectionUtil.isEmpty(typeList)) return conn.getMetadata().getTableTypesArray();
		return StringUtil.toArray(typeList, true);
	}

	private boolean isFiltered(TableIdentifier table)
	{
		boolean filtered = false;
		if (schemaFilter != null)
		{
			filtered = schemaFilter.isExcluded(table.getSchema());
		}

		if (filtered) return true;

		if (catalogFilter != null)
		{
			filtered = catalogFilter.isExcluded(table.getCatalog());
		}
		return filtered;
	}

	/**
	 * Add this list of tables to the current cache.
	 */
	private void setTables(List<TableIdentifier> tables)
	{
		for (TableIdentifier tbl : tables)
		{
			if (!isFiltered(tbl) && !this.objects.containsKey(tbl))
			{
				this.objects.put(tbl, null);

				String schema = supportsSchemas ? tbl.getSchema() : tbl.getCatalog();
				if (schema != null)
				{
					this.schemasInCache.add(schema);
				}
				else
				{
					this.schemasInCache.add(NULL_SCHEMA);
				}
			}
		}
	}

	private String getSchemaToUse(WbConnection dbConnection, String schema)
	{
		DbMetadata meta = dbConnection.getMetadata();
		return meta.adjustSchemaNameCase(schema);
	}

	List<String> getSearchPath(WbConnection dbConn, String defaultSchema)
	{
		List<String> schemas = DbSearchPath.Factory.getSearchPathHandler(dbConn).getSearchPath(dbConn, defaultSchema);
		if (schemas.isEmpty())
		{
			return CollectionUtil.arrayList((String)null);
		}

		Collection<String> ignore = dbConn.getDbSettings().getIgnoreCompletionSchemas();
		schemas.removeAll(ignore);
		return schemas;
	}

	private boolean isSchemaCached(String schema)
	{
		return (schemasInCache.contains(schema == null ? NULL_SCHEMA : schema));
	}

	/**
	 * Get the tables (and views) the are currently in the cache
	 */
	synchronized Set<TableIdentifier> getTables(WbConnection dbConnection, String schema, List<String> types)
	{
		List<String> searchPath = getSearchPath(dbConnection, schema);
		LogMgr.logDebug("ObjectCache.getTables()", "Getting tables using schema: " + schema + ", filter: " + types + ", search path: " + searchPath);

		for (String checkSchema  : searchPath)
		{
			if (this.objects.isEmpty() || !isSchemaCached(checkSchema))
			{
				try
				{
					if (!dbConnection.isBusy())
					{
						DbMetadata meta = dbConnection.getMetadata();
						List<TableIdentifier> tables = meta.getSelectableObjectsList(null, checkSchema, getCompletionTypes(dbConnection));
						for (TableIdentifier tbl : tables)
						{
							tbl.checkQuotesNeeded(dbConnection);
						}
						this.setTables(tables);
						LogMgr.logDebug("ObjectCache.getTables()", "Schema: " + checkSchema + " not found in cache. Retrieved " + tables.size() + " objects");
					}
				}
				catch (Exception e)
				{
					LogMgr.logError("ObjectCache.getTables()", "Could not retrieve table list", e);
				}
			}
		}

		if (types != null)
		{
			return filterTablesByType(dbConnection, searchPath, types);
		}
		else
		{
			return filterTablesBySchema(dbConnection, searchPath);
		}
	}

	public List<DependencyNode> getReferencingTables(WbConnection dbConn, TableIdentifier table)
	{
		if (table == null || dbConn.isBusy()) return Collections.emptyList();

		TableIdentifier tbl = dbConn.getMetadata().findTable(table, false);
		List<DependencyNode> referencing = referencingTables.get(tbl);
		if (referencing == null)
		{
			TableDependency deps = new TableDependency(dbConn, tbl);
			deps.setRetrieveDirectChildrenOnly(true);
			deps.readTreeForChildren();
			referencing = deps.getLeafs();
			referencingTables.put(table, referencing);
		}
		return referencing;
	}

	public List<DependencyNode> getReferencedTables(WbConnection dbConn, TableIdentifier table)
	{
		if (table == null || dbConn.isBusy()) return Collections.emptyList();

		TableIdentifier tbl = dbConn.getMetadata().findTable(table, false);
		List<DependencyNode> referenced = referencedTables.get(tbl);
		if (referenced == null)
		{
			TableDependency deps = new TableDependency(dbConn, tbl);
			deps.setRetrieveDirectChildrenOnly(true);
			deps.readTreeForParents();
			referenced = deps.getLeafs();
			referencedTables.put(table, referenced);
		}
		return referenced;
	}

	public void addReferencedTables(TableIdentifier table, List<DependencyNode> referenced)
	{
		if (table == null) return;
		List<DependencyNode> old = referencedTables.put(table, referenced);
		if (old == null)
		{
			LogMgr.logDebug("ObjectCache.addReferencedTables()", "Added referenced tables for " + table + "(" + referenced + ")");
		}
		else
		{
			LogMgr.logDebug("ObjectCache.addReferencedTables()", "Replaced existing referenced tables for " + table);
		}
	}

	public void addReferencingTables(TableIdentifier table, List<DependencyNode> referencing)
	{
		if (table == null) return;
		List<DependencyNode> old = referencingTables.put(table, referencing);
		if (old == null)
		{
			LogMgr.logDebug("ObjectCache.addReferencingTables()", "Added referencing tables for " + table + "(" + referencing + ")");
		}
		else
		{
			LogMgr.logDebug("ObjectCache.addReferencingTables()", "Replaced existing referencing tables for " + table);
		}
	}

	Map<String, List<ProcedureDefinition>> getProcedures()
	{
		if (procedureCache == null) return new HashMap<>(0);
		return procedureCache;
	}

	/**
	 * Get the procedures the are currently in the cache
	 */
	public List<ProcedureDefinition> getProcedures(WbConnection dbConnection, String schema)
	{
		String schemaToUse = getSchemaToUse(dbConnection, schema);
		List<ProcedureDefinition> procs = procedureCache.get(schemaToUse);
		if (procs == null)
		{
			// nothing in the cache. We can only retrieve this from the database if the connection isn't busy
			if (dbConnection.isBusy())
			{
				return Collections.emptyList();
			}

			try
			{
				procs = dbConnection.getMetadata().getProcedureReader().getProcedureList(null, schemaToUse, "%");
				if (dbConnection.getDbSettings().getRetrieveProcParmsForAutoCompletion())
				{
					for (ProcedureDefinition proc : procs)
					{
						proc.readParameters(dbConnection);
					}
				}
				procedureCache.put(schemaToUse, procs);
			}
			catch (SQLException e)
			{
				LogMgr.logError("ObjectCache.getProcedures()", "Error retrieving procedures", e);
			}
		}
		return procs;
	}

	private Set<TableIdentifier> filterTablesByType(WbConnection conn, List<String> schemas, List<String> requestedTypes)
	{
		SortedSet<TableIdentifier> result = new TreeSet<>(new TableNameSorter());
		String currentSchema = null;
		if (schemas.size() == 1)
		{
			currentSchema = schemas.get(0);
		}

		boolean alwaysUseSchema = conn.getDbSettings().alwaysUseSchemaForCompletion() || schemas.size() > 1;

		for (TableIdentifier tbl : objects.keySet())
		{
			String ttype = tbl.getType();
			String tSchema = tbl.getSchema();
			if ( (requestedTypes.contains(ttype) && schemas.contains(tSchema)) || tSchema == null || "public".equalsIgnoreCase(tSchema) )
			{
				TableIdentifier copy = tbl.createCopy();
				adjustSchemaAndCatalog(conn, copy, currentSchema, alwaysUseSchema);
				result.add(copy);
			}
		}
		return result;
	}

	private void adjustSchemaAndCatalog(WbConnection conn, TableIdentifier table, String currentSchema, boolean alwaysUseSchema)
	{
		DbMetadata meta = conn.getMetadata();
		boolean alwaysUseCatalog = conn.getDbSettings().alwaysUseCatalogForCompletion();
		String tSchema = table.getSchema();
		boolean ignoreSchema = (alwaysUseSchema ? false : meta.ignoreSchema(tSchema, currentSchema));
		if (ignoreSchema)
		{
			table.setSchema(null);
		}

		boolean ignoreCatalog = (alwaysUseCatalog ? false : meta.ignoreCatalog(table.getCatalog()));
		if (ignoreCatalog)
		{
			table.setCatalog(null);
		}
	}

	private Set<TableIdentifier> filterTablesBySchema(WbConnection dbConnection, List<String> schemas)
	{
		SortedSet<TableIdentifier> result = new TreeSet<>(new TableNameSorter(true));
		DbMetadata meta = dbConnection.getMetadata();

		List<String> schemasToCheck = new ArrayList<>(schemas.size());
		for (String s : schemas)
		{
			if (s != null) schemasToCheck.add(s);
		}

		boolean alwaysUseSchema = dbConnection.getDbSettings().alwaysUseSchemaForCompletion() || schemasToCheck.size() > 1;

		String currentSchema = null;
		if (schemasToCheck.size() == 1)
		{
			currentSchema = supportsSchemas ? meta.getCurrentSchema() : meta.getCurrentCatalog();
		}

		for (TableIdentifier tbl : objects.keySet())
		{
			String tSchema = supportsSchemas ? tbl.getSchema() : tbl.getCatalog();

			if (schemasToCheck.contains(tSchema) || schemasToCheck.isEmpty())
			{
				TableIdentifier copy = tbl.createCopy();
				adjustSchemaAndCatalog(dbConnection, copy, currentSchema, alwaysUseSchema);
				result.add(copy);
			}
		}

		return result;
	}

	public synchronized void addSynonym(TableIdentifier synonym, TableIdentifier baseTable)
	{
		this.synonymMap.put(synonym, baseTable);
	}

	public synchronized TableIdentifier getSynonymTable(WbConnection dbConn, TableIdentifier synonym)
	{
		TableIdentifier baseTable = this.synonymMap.get(synonym);
		if (baseTable == dummyTable)
		{
			// we already tested for a synonym but did not found any
			return null;
		}
		if (baseTable != null)
		{
			return baseTable;
		}

		if (baseTable == null)
		{
			baseTable = dbConn.getMetadata().resolveSynonym(synonym);
		}
		if (baseTable == null)
		{
			// "negative caching. Avoid repeated lookup for non-synonyms
			synonymMap.put(synonym, dummyTable);
		}
		else
		{
			synonymMap.put(synonym, baseTable);
		}
		return baseTable;
	}

	/**
	 * Return the columns for the given table.
	 *
	 * If the table columns are not in the cache they are retrieved from the database.
	 *
	 * @return the columns of the table.
	 * @see DbMetadata#getTableDefinition(workbench.db.TableIdentifier)
	 */
	public synchronized List<ColumnIdentifier> getColumns(WbConnection dbConnection, TableIdentifier tbl)
	{
		LogMgr.logDebug("ObjectCache.getColumns()", "Checking columns for: " + tbl.getTableExpression(dbConnection));

		TableIdentifier toSearch = findEntry(dbConnection, tbl);
		List<ColumnIdentifier> cols = null;

		if (toSearch != null)
		{
			cols = this.objects.get(toSearch);
		}

		// nothing in the cache. We can only retrieve this from the database if the connection isn't busy
		if (dbConnection.isBusy())
		{
			return Collections.emptyList();
		}

		if (cols == null)
		{
			toSearch = dbConnection.getMetadata().searchSelectableObjectOnPath(toSearch == null ? tbl : toSearch);
			if (toSearch == null) return null;
		}

		// To support Oracle public synonyms, try to find a table with that name but without a schema
		if (retrieveOraclePublicSynonyms && toSearch.getSchema() != null && cols == null)
		{
			toSearch.setSchema(null);
			toSearch.setType(null);
			cols = this.objects.get(toSearch);
			if (cols == null)
			{
				// retrieve Oracle PUBLIC synonyms
				this.getTables(dbConnection, "PUBLIC", null);
				cols = this.objects.get(toSearch);
			}
		}

		if (CollectionUtil.isEmpty(cols))
		{
			try
			{
				LogMgr.logDebug("ObjectCache.getColumns()", "Table not in cache, retrieving columns for " + toSearch.getTableExpression());
				cols = dbConnection.getMetadata().getTableColumns(toSearch);
			}
			catch (Throwable e)
			{
				LogMgr.logError("ObjectCache.getColumns()", "Error retrieving columns for " + toSearch, e);
				cols = null;
			}

			if (toSearch != null && CollectionUtil.isNonEmpty(cols))
			{
				this.objects.put(toSearch, cols);
			}

		}
		return Collections.unmodifiableList(cols);
	}

	synchronized void removeProcedure(WbConnection dbConn, ProcedureDefinition toRemove)
	{
		if (toRemove == null) return;
		String schema = toRemove.getSchema();
		String fullName = toRemove.getObjectNameForDrop(dbConn);

		List<ProcedureDefinition> procedures = getProcedures(dbConn, schema);
		Iterator<ProcedureDefinition> itr = procedures.iterator();
		while (itr.hasNext())
		{
			ProcedureDefinition proc = itr.next();
			String procName = proc.getObjectNameForDrop(dbConn);
			if (procName.equals(fullName))
			{
				LogMgr.logDebug("ObjectCache.removeProcedure()", "Procedure " + fullName + " removed from the cache");
				itr.remove();
				break;
			}
		}
	}

	synchronized void removeTable(WbConnection dbConn, TableIdentifier tbl)
	{
		if (tbl == null) return;

		TableIdentifier toRemove = findEntry(dbConn, tbl);
		if (toRemove == null) return;

		this.objects.remove(toRemove);
		LogMgr.logDebug("ObjectCache.removeTable()", "Removed " + tbl.getTableName() + " from the cache");
	}

	synchronized void addTableList(WbConnection dbConnection, DataStore tables, String schema)
	{
		Set<String> selectable = dbConnection.getMetadata().getObjectsWithData();

		int count = 0;

		for (int row = 0; row < tables.getRowCount(); row++)
		{
			String type = tables.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE);
			if (selectable.contains(type))
			{
				TableIdentifier tbl = createIdentifier(tables, row);
				if (objects.get(tbl) == null)
				{
					// The table is either not there, or no columns have been retrieved so it's safe to add
					objects.put(tbl, null);
					count ++;
				}
			}
		}

		this.schemasInCache.add(schema);
		LogMgr.logDebug("ObjectCache.addTableList()", "Added " + count + " objects");
	}

	synchronized void addProcedureList(DataStore procs, String schema)
	{
		if (schema == null) return;
		int count = procs.getRowCount();
		List<ProcedureDefinition> procList = new ArrayList<>();
		for (int row=0; row < count; row++)
		{
			Object uo = procs.getRow(row).getUserObject();
			if (uo instanceof ProcedureDefinition)
			{
				ProcedureDefinition proc = (ProcedureDefinition)uo;
				procList.add(proc);
			}
		}
		procedureCache.put(schema, procList);
		LogMgr.logDebug("ObjectCache.addTableList()", "Added " + procList.size() + " procedures");
	}


	private TableIdentifier createIdentifier(DataStore tableList, int row)
	{
		String name = tableList.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
		String schema = tableList.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA);
		String catalog = tableList.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_CATALOG);
		String type = tableList.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE);
		String comment = tableList.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_REMARKS);
		TableIdentifier tbl = new TableIdentifier(catalog, schema, name);
		tbl.setType(type);
		tbl.setNeverAdjustCase(true);
		tbl.setComment(comment);
		return tbl;
	}

	public synchronized void addTable(TableIdentifier table, WbConnection con)
	{
		if (table == null) return;
		if (findInCache(con, table) == null)
		{
			this.objects.put(table, null);
		}
	}

	public synchronized void addTable(TableDefinition definition, WbConnection conn)
	{
		if (definition == null) return;
		TableIdentifier table = definition.getTable();
		String tbName = table.getTableExpression(SqlUtil.getCatalogSeparator(conn), SqlUtil.getSchemaSeparator(conn));
		List<ColumnIdentifier> old = this.objects.put(definition.getTable(), definition.getColumns());
		if (old == null)
		{
			LogMgr.logDebug("ObjectCache.addTable()", "Added table definition for " + tbName);
		}
		else
		{
			LogMgr.logDebug("ObjectCache.addTable()", "Replaced existing table definition for " + tbName);
		}
	}

	/**
	 * Return the stored key according to the passed TableIdentifier.
	 *
	 * The stored key might carry additional properties that the passed key does not have
	 * (even though they are equal)
	 */
	synchronized TableIdentifier findEntry(WbConnection con, TableIdentifier toSearch)
	{
		if (toSearch == null) return null;

		String schemaCat = supportsSchemas ? toSearch.getSchema() : toSearch.getCatalog();
		if (schemaCat == null)
		{
			TableIdentifier key = toSearch.createCopy();
			key.adjustCase(con);

			List<String> schemas = getSearchPath(con, con.getCurrentSchema());
			for (String schema : schemas)
			{
				TableIdentifier copy = key.createCopy();
				if (supportsSchemas)
				{
					copy.setSchema(schema);
				}
				else
				{
					copy.setCatalog(schema);
				}
				TableIdentifier tbl = findInCache(con, copy);
				if (tbl != null) return tbl;
			}
		}
		else
		{
			return findInCache(con, toSearch);
		}

		return null;
	}

	PkDefinition getPrimaryKey(WbConnection con, TableIdentifier table)
	{
		synchronized (pkMap)
		{
			PkDefinition pk = getPkFromTableCache(con, table);

			if (pk == null)
			{
				TableIdentifier tbl = findInCache(con, table, pkMap.keySet());
				if (tbl != null)
				{
					pk = pkMap.get(table);
				}
			}
			if (pk == null)
			{
				pk = con.getMetadata().getIndexReader().getPrimaryKey(table);
				pkMap.put(table, pk);
			}
			return pk;
		}
	}

	private PkDefinition getPkFromTableCache(WbConnection con, TableIdentifier table)
	{
		List<ColumnIdentifier> columns = getColumns(con, table);
		if (CollectionUtil.isEmpty(columns)) return null;

		List<String> pkCols = new ArrayList<>(1);
		for (ColumnIdentifier col : columns)
		{
			if (col.isPkColumn())
			{
				pkCols.add(col.getColumnName());
			}
		}
		return new PkDefinition(pkCols);
	}

	List<IndexDefinition> getUniqueIndexes(WbConnection con, TableIdentifier table)
	{
		synchronized (indexMap)
		{
			TableIdentifier tbl = findInCache(con, table, indexMap.keySet());
			List<IndexDefinition> indexes = null;
			if (tbl != null)
			{
				indexes = indexMap.get(tbl);
			}

			if (indexes  == null)
			{
				IndexReader reader = ReaderFactory.getIndexReader(con.getMetadata());
				indexes = reader.getUniqueIndexes(table);
				if (indexes == null) indexes = new ArrayList<>(0);
				indexMap.put(table, indexes);
			}
			return indexes;
		}
	}


	private TableIdentifier findInCache(WbConnection con, TableIdentifier toSearch)
	{
		return findInCache(con, toSearch, objects.keySet());
	}

	private TableIdentifier findInCache(WbConnection con, TableIdentifier toSearch, Set<TableIdentifier> keys)
	{
		TableIdentifier tbl = toSearch.createCopy();
		tbl.adjustCase(con);
		for (TableIdentifier key : keys)
		{
			if (tbl.compareNames(key)) return key;
		}
		return null;
	}

	/**
	 * Disposes any db objects held in the cache.
	 */
	public void clear()
	{
		objects.clear();
		schemasInCache.clear();
		referencedTables.clear();
		referencingTables.clear();
		procedureCache.clear();
		synonymMap.clear();
		LogMgr.logDebug("ObjectCache.clear()", "Removed all entries from the cache");
	}

	Set<String> getSchemasInCache()
	{
		if (schemasInCache == null) return Collections.emptySet();
		return Collections.unmodifiableSet(schemasInCache);
	}

	Map<TableIdentifier, List<DependencyNode>> getReferencedTables()
	{
		return Collections.unmodifiableMap(referencedTables);
	}

	Map<TableIdentifier, List<DependencyNode>> getReferencingTables()
	{
		return Collections.unmodifiableMap(referencingTables);
	}

	Map<TableIdentifier, List<ColumnIdentifier>> getObjects()
	{
		return Collections.unmodifiableMap(objects);
	}

	public Map<TableIdentifier, TableIdentifier> getSynonyms()
	{
		return Collections.unmodifiableMap(synonymMap);
	}

	public Map<TableIdentifier, PkDefinition> getPKMap()
	{
		return Collections.unmodifiableMap(pkMap);
	}

	public Map<TableIdentifier, List<IndexDefinition>> getIndexes()
	{
		return Collections.unmodifiableMap(indexMap);
	}

	void initExternally(
		Map<TableIdentifier, List<ColumnIdentifier>> newObjects, Set<String> schemas,
		Map<TableIdentifier, List<DependencyNode>> referencedTables,
		Map<TableIdentifier, List<DependencyNode>> referencingTables,
		Map<String, List<ProcedureDefinition>> procs,
		Map<TableIdentifier, TableIdentifier> synonyms,
		Map<TableIdentifier, List<IndexDefinition>> indexes,
		Map<TableIdentifier, PkDefinition> pk)
	{
		if (newObjects == null || schemas == null) return;

		clear();

		objects.putAll(newObjects);
		schemasInCache.addAll(schemas);

		int refCount = 0;
		if (referencedTables != null)
		{
			this.referencedTables.putAll(referencedTables);
			refCount += referencedTables.size();
		}

		if (referencingTables != null)
		{
			this.referencingTables.putAll(referencingTables);
			refCount += referencingTables.size();
		}
		if (procs != null)
		{
			this.procedureCache.putAll(procs);
		}
		if (synonyms != null)
		{
			synonymMap.putAll(synonyms);
		}
		if (indexes != null)
		{
			indexMap.putAll(indexes);
		}
		if (pk != null)
		{
			pkMap.putAll(pk);
		}

		LogMgr.logDebug("ObjectCache.initExternally",
			"Added " + objects.size() + " objects, " +
			procedureCache.values().size() + " procedures, " +
			synonymMap.size() + " synonyms and "
			+ refCount + " foreign key definitions from local storage");
	}
}
