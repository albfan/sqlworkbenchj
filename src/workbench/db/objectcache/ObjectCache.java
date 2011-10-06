/*
 * DbObjectCache.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.objectcache;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.ProcedureDefinition;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.TableNameSorter;
import workbench.db.WbConnection;
import workbench.db.postgres.PostgresUtil;
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
	private SortedMap<TableIdentifier, List<ColumnIdentifier>> objects;
	private Map<String, List<ProcedureDefinition>> procedureCache = new HashMap<String, List<ProcedureDefinition>>();

	ObjectCache(WbConnection conn)
	{
		this.createCache();
		retrieveOraclePublicSynonyms = conn.getMetadata().isOracle() && Settings.getInstance().getBoolProperty("workbench.editor.autocompletion.oracle.public_synonyms", false);
	}

	private void createCache()
	{
		schemasInCache = CollectionUtil.caseInsensitiveSet();
		objects = new TreeMap<TableIdentifier, List<ColumnIdentifier>>(new TableNameSorter(true));
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
		if (dbConn == null || !dbConn.getMetadata().isPostgres())
		{
			return Collections.singletonList(getSchemaToUse(dbConn, defaultSchema));
		}
		return PostgresUtil.getSearchPath(dbConn);
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
		String[] selectableTypes = dbConnection.getMetadata().getSelectableTypes();

		for (String checkSchema  : searchPath)
		{
			if (this.objects.size() == 0 || !isSchemaCached(checkSchema))
			{
				try
				{
					DbMetadata meta = dbConnection.getMetadata();
					List<TableIdentifier> tables = meta.getObjectList(null, checkSchema, selectableTypes, false);
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
				// meta.ignoreSchema() needs to be tested, because if that is true
				// the returned Tables will not contain the schema...
				boolean ignoreSchema = !alwaysUseSchema && currentSchema != null && meta.ignoreSchema(tSchema, currentSchema);

				TableIdentifier copy = tbl.createCopy();
				if (ignoreSchema)
				{
					if (ignoreSchema)
					{
						copy.setSchema(null);
					}
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

	private TableIdentifier searchTableDefinition(WbConnection dbConnection, List<String> schemas, TableIdentifier toSearch)
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

		TableIdentifier toSearch = null;
		List<ColumnIdentifier> cols = null;

		if (tbl.getSchema() == null)
		{
			for (String searchSchema : schemas)
			{
				TableIdentifier table = tbl.createCopy();
				table.adjustCase(dbConnection);
				table.setSchema(searchSchema);
				cols = this.objects.get(table);
				if (cols != null)
				{
					toSearch = table;
					break;
				}
			}
		}
		else
		{
			toSearch = tbl.createCopy();
			toSearch.adjustCase(dbConnection);
			cols = this.objects.get(toSearch);
		}

		if (cols == null)
		{
			toSearch = searchTableDefinition(dbConnection, schemas, toSearch == null ? tbl : toSearch);
			if (toSearch == null) return null;
			cols = this.objects.get(toSearch);
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
			TableIdentifier tblToUse = null;

			// use the stored key because that might carry the correct type attribute
			// TabelIdentifier.equals() doesn't compare the type, only the expression
			// so we'll get a containsKey() == true even if the type is different
			// (which is necessary because the TableIdentifier passed to this
			// method will never contain a type!)
			// only using objects.get() would not return anything!
			if (objects.containsKey(toSearch))
			{
				// we have already retrieved the list of tables, but not the columns for this table
				// the table identifier in the object map contains correct type and schema information, so we need
				// to use that
				tblToUse = findEntry(toSearch);
			}
			else
			{
				// retrieve the real table identifier based on the table name
				tblToUse = dbConnection.getMetadata().findObject(toSearch);
			}

			try
			{
				cols = dbConnection.getMetadata().getTableColumns(tblToUse);
			}
			catch (Throwable e)
			{
				LogMgr.logError("DbObjectCache.getColumns", "Error retrieving columns for " + tblToUse, e);
				cols = null;
			}

			if (tblToUse != null && CollectionUtil.isNonEmpty(cols))
			{
				this.objects.put(tblToUse, cols);
			}

		}
		return Collections.unmodifiableList(cols);
	}

	synchronized void removeTable(WbConnection dbConn, TableIdentifier tbl)
	{
		if (tbl == null) return;

		boolean removed = false;
		if (tbl.getSchema() == null)
		{
			List<String> schemas = getSearchPath(dbConn, dbConn.getCurrentSchema());
			for (String schema : schemas)
			{
				TableIdentifier copy = tbl.createCopy();
				copy.setSchema(schema);
				TableIdentifier toRemove = findEntry(copy);
				if (toRemove != null)
				{
					removed = this.objects.remove(toRemove) != null;
					break;
				}
			}
		}
		else
		{
			removed = this.objects.remove(tbl) != null;
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
	TableIdentifier findEntry(TableIdentifier key)
	{
		if (key == null) return null;

		// as contains() is using the comparator as well, we have to use it here also!
		Comparator<? super TableIdentifier> comparator = objects.comparator();

		for (TableIdentifier tbl : objects.keySet())
		{
			if (comparator.compare(key, tbl) == 0) return tbl;
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
