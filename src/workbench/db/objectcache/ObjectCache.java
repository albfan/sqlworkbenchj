/*
 * DbObjectCache.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.objectcache;

import java.sql.SQLException;
import java.util.*;
import workbench.db.*;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.CollectionUtil;

/**
 * A cache for database objects to support Auto-completion in the editor
 *
 * @author  Thomas Kellerer
 */
class ObjectCache
{
	private static final String NULL_SCHEMA = "-$$wb-null-schema$$-";
	private boolean retrieveOraclePublicSynonyms;

	private Set<String> schemasInCache;
	private Map<TableIdentifier, List<ColumnIdentifier>> objects;
	private Map<String, List<ProcedureDefinition>> procedureCache = new HashMap<String, List<ProcedureDefinition>>();

	ObjectCache(WbConnection conn)
	{
		this.createCache();
		retrieveOraclePublicSynonyms = conn.getMetadata().isOracle() && Settings.getInstance().getBoolProperty("workbench.editor.autocompletion.oracle.public_synonyms", false);
	}

	private void createCache()
	{
		schemasInCache = CollectionUtil.caseInsensitiveSet();
		objects = new HashMap<TableIdentifier, List<ColumnIdentifier>>();
	}

	/**
	 * Add this list of tables to the current cache.
	 */
	private void setTables(List<TableIdentifier> tables)
	{
		for (TableIdentifier tbl : tables)
		{
			if (!this.objects.containsKey(tbl))
			{
				this.objects.put(tbl, null);
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
		return schemas;
	}

	private boolean isSchemaCached(String schema)
	{
		return (schemasInCache.contains(schema == null ? NULL_SCHEMA : schema));
	}
	/**
	 * Get the tables (and views) the are currently in the cache
	 */
	synchronized Set<TableIdentifier> getTables(WbConnection dbConnection, String schema, List<String> type)
	{
		List<String> searchPath = getSearchPath(dbConnection, schema);

		for (String checkSchema  : searchPath)
		{
			if (this.objects.isEmpty() || !isSchemaCached(checkSchema))
			{
				try
				{
					DbMetadata meta = dbConnection.getMetadata();
					List<TableIdentifier> tables = meta.getSelectableObjectsList(null, checkSchema);
					for (TableIdentifier tbl : tables)
					{
						tbl.checkQuotesNeeded(dbConnection);
					}
					this.setTables(tables);
					this.schemasInCache.add(checkSchema == null ? NULL_SCHEMA : checkSchema);
				}
				catch (Exception e)
				{
					LogMgr.logError("DbObjectCache.getTables()", "Could not retrieve table list", e);
				}
			}
		}

		if (type != null)
		{
			return filterTablesByType(dbConnection, searchPath, type);
		}
		else
		{
			return filterTablesBySchema(dbConnection, searchPath);
		}
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
			try
			{
				procs = dbConnection.getMetadata().getProcedureReader().getProcedureList(null, schemaToUse, "%");
				if (dbConnection.getDbSettings().getRetrieveProcParmsForAutoCompletion())
				{
					for (ProcedureDefinition proc : procs)
					{
						proc.getParameterTypes(dbConnection);
					}
				}
				procedureCache.put(schemaToUse, procs);
			}
			catch (SQLException e)
			{
				LogMgr.logError("ExecAnalyzer.checkContext()", "Error retrieving procedures", e);
			}
		}
		return procs;
	}

	private Set<TableIdentifier> filterTablesByType(WbConnection conn, List<String> schemas, List<String> requestedTypes)
	{
		SortedSet<TableIdentifier> result = new TreeSet<TableIdentifier>(new TableNameSorter());
		String currentSchema = null;
		if (schemas.size() == 1)
		{
			currentSchema = schemas.get(0);
		}
		for (TableIdentifier tbl : objects.keySet())
		{
			String ttype = tbl.getType();
			String tSchema = tbl.getSchema();
			if ( (requestedTypes.contains(ttype) && schemas.contains(tSchema)) || tSchema == null || "public".equalsIgnoreCase(tSchema) )
			{
				TableIdentifier copy = tbl.createCopy();
				if (tSchema != null && currentSchema != null && conn.getMetadata().ignoreSchema(tSchema, currentSchema))
				{
					copy.setSchema(null);
				}
				result.add(copy);
			}
		}
		return result;
	}

	private Set<TableIdentifier> filterTablesBySchema(WbConnection dbConnection, List<String> schemas)
	{
		SortedSet<TableIdentifier> result = new TreeSet<TableIdentifier>(new TableNameSorter(true));
		DbMetadata meta = dbConnection.getMetadata();

		boolean alwaysUseSchema = dbConnection.getDbSettings().alwaysUseSchemaForCompletion() || schemas.size() > 1;
		boolean alwaysUseCatalog = dbConnection.getDbSettings().alwaysUseCatalogForCompletion();

		String currentSchema = null;
		if (schemas.size() == 1)
		{
			currentSchema = meta.getCurrentSchema();
		}

		for (TableIdentifier tbl : objects.keySet())
		{
			String tSchema = tbl.getSchema();

			if (schemas.contains(tSchema))
			{
				boolean ignoreSchema = !alwaysUseSchema && currentSchema != null && meta.ignoreSchema(tSchema, currentSchema);

				TableIdentifier copy = tbl.createCopy();
				if (ignoreSchema)
				{
					copy.setSchema(null);
				}

				if (!alwaysUseCatalog && meta.ignoreCatalog(copy.getCatalog()))
				{
					copy.setCatalog(null);
				}
				result.add(copy);
			}
		}

		return result;
	}

	private TableIdentifier findTableInDb(WbConnection dbConnection, List<String> schemas, TableIdentifier toSearch)
	{
		if (toSearch == null) return null;

		for (String schema : schemas)
		{
			TableIdentifier toRead = toSearch.createCopy();
			toRead.setSchema(schema);
			TableIdentifier def = dbConnection.getMetadata().findSelectableObject(toRead);
			if (def != null) return def;
		}
		return null;
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
		String schema = getSchemaToUse(dbConnection, tbl.getSchema());
		List<String> schemas = getSearchPath(dbConnection, schema);

		TableIdentifier toSearch = findEntry(dbConnection, tbl);
		List<ColumnIdentifier> cols = null;

		if (toSearch != null)
		{
			cols = this.objects.get(toSearch);
		}

		if (cols == null)
		{
			toSearch = findTableInDb(dbConnection, schemas, toSearch == null ? tbl : toSearch);
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
				cols = dbConnection.getMetadata().getTableColumns(toSearch);
			}
			catch (Throwable e)
			{
				LogMgr.logError("DbObjectCache.getColumns", "Error retrieving columns for " + toSearch, e);
				cols = null;
			}

			if (toSearch != null && CollectionUtil.isNonEmpty(cols))
			{
				this.objects.put(toSearch, cols);
			}

		}
		return Collections.unmodifiableList(cols);
	}

	synchronized void removeTable(WbConnection dbConn, TableIdentifier tbl)
	{
		if (tbl == null) return;

		TableIdentifier toRemove = findEntry(dbConn, tbl);
		boolean removed = false;
		if (toRemove != null)
		{
			this.objects.remove(toRemove);
			removed = true;
		}

		if (removed)
		{
			LogMgr.logDebug("DbObjectCach.addTableList()", "Removed " + tbl.getTableName() + " from the cache");
		}
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
		LogMgr.logDebug("DbObjectCach.addTableList()", "Added " + count + " objects");
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

	public synchronized void addTable(TableDefinition definition)
	{
		if (definition != null)
		{
			TableIdentifier table = definition.getTable();
			if (table.getSchema() != null)
			{
				List<ColumnIdentifier> old = this.objects.put(definition.getTable(), definition.getColumns());
				if (old == null)
				{
					LogMgr.logDebug("ObjectCache.addTable()", "Added table definition for " + table.getTableExpression());
				}
				else
				{
					LogMgr.logDebug("ObjectCache.addTable()", "Replaced existing table definition for " + table.getTableExpression());
				}
			}
		}
	}

	/**
	 * Return the stored key according to the passed
	 * TableIdentifier.
	 *
	 * The stored key might carry additional properties that the passed key does not have
	 * (even though they are equal)
	 */
	synchronized TableIdentifier findEntry(WbConnection con, TableIdentifier toSearch)
	{
		if (toSearch == null) return null;

		if (toSearch.getSchema() == null)
		{
			TableIdentifier key = toSearch.createCopy();
			key.adjustCase(con);

			List<String> schemas = getSearchPath(con, con.getCurrentSchema());
			for (String schema : schemas)
			{
				TableIdentifier copy = key.createCopy();
				copy.setSchema(schema);
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

	private TableIdentifier findInCache(WbConnection con, TableIdentifier toSearch)
	{
		TableIdentifier key = toSearch.createCopy();
		key.adjustCase(con);
		for (TableIdentifier tbl : objects.keySet())
		{
			if (tbl.equals(key)) return tbl;
		}
		return null;
	}

	/**
	 * Disposes any db objects held in the cache
	 */
	public void clear()
	{
		if (this.objects != null) this.objects.clear();
		this.schemasInCache.clear();
	}

}
