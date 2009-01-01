/*
 * DbObjectCache.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import workbench.log.LogMgr;
import workbench.resource.Settings;

/**
 * A cache for database objects to support Auto-completion in the editor
 * @author  support@sql-workbench.net
 */
public class DbObjectCache
	implements PropertyChangeListener
{
	private WbConnection dbConnection;
	private static final String NULL_SCHEMA = "$$wb-null-schema$$";
	private boolean retrieveOraclePublicSynonyms = false;
	
	private Set<String> schemasInCache;
	private SortedMap<TableIdentifier, List<ColumnIdentifier>> objects;
	
	DbObjectCache(WbConnection conn)
	{
		this.dbConnection = conn;
		this.createCache();
		retrieveOraclePublicSynonyms = conn.getMetadata().isOracle() && Settings.getInstance().getBoolProperty("workbench.editor.autocompletion.oracle.public_synonyms", false);
		conn.addChangeListener(this);
	}

	private void createCache()
	{
		schemasInCache = new HashSet<String>();
		objects = new TreeMap<TableIdentifier, List<ColumnIdentifier>>();
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
	
	public Set<TableIdentifier> getTables()
	{
		return getTables(null, null);
	}
	
	public Set<TableIdentifier> getTables(String schema)
	{
		return getTables(schema, null);
	}

	private String getSchemaToUse(String schema)
	{
		DbMetadata meta = this.dbConnection.getMetadata();
		return meta.adjustSchemaNameCase(schema);
	}
	/**
	 * Get the tables (and views) the are currently in the cache
	 */
	public Set<TableIdentifier> getTables(String schema, String type)
	{
		String schemaToUse = getSchemaToUse(schema);
		if (this.objects.size() == 0 || (!schemasInCache.contains(schemaToUse == null ? NULL_SCHEMA : schemaToUse))) 
		{
			try
			{
				DbMetadata meta = this.dbConnection.getMetadata();
				List<TableIdentifier> tables = meta.getSelectableObjectsList(schemaToUse);
				this.setTables(tables);
				this.schemasInCache.add(schema == null ? NULL_SCHEMA : schemaToUse);
			}
			catch (Exception e)
			{
				LogMgr.logError("DbObjectCache.getTables()", "Could not retrieve table list", e);
			}
		}
		if (type != null)
			return filterTablesByType(schemaToUse, type);
		else
			return filterTablesBySchema(schemaToUse);
	}

	private Set<TableIdentifier> filterTablesByType(String schema, String type)
	{
		this.getTables(schema);
		String schemaToUse = getSchemaToUse(schema);
		SortedSet<TableIdentifier> result = new TreeSet<TableIdentifier>(new TableNameComparator());
		for (TableIdentifier tbl : objects.keySet())
		{
			String ttype = tbl.getType();
			String tSchema = tbl.getSchema();
			if ( type.equalsIgnoreCase(ttype) &&
				   ((schemaToUse == null || schemaToUse.equalsIgnoreCase(tSchema) || tSchema == null || "public".equalsIgnoreCase(tSchema)))
				 )
			{
				TableIdentifier copy = tbl.createCopy();
				if (tSchema != null && tSchema.equals(tbl.getSchema())) copy.setSchema(null);
				result.add(copy);
			}
		}
		return result;
	}
	
	private Set<TableIdentifier> filterTablesBySchema(String schema)
	{
		SortedSet<TableIdentifier> result = new TreeSet<TableIdentifier>(new TableNameComparator());
		DbMetadata meta = this.dbConnection.getMetadata();
		String schemaToUse = getSchemaToUse(schema);
		for (TableIdentifier tbl : objects.keySet())
		{
			String tSchema = tbl.getSchema();
			if (schemaToUse == null || meta.ignoreSchema(tSchema) || schemaToUse.equalsIgnoreCase(tSchema))
			{
				TableIdentifier copy = tbl.createCopy();
				copy.setSchema(null);
				String cat = copy.getCatalog();
				if (meta.ignoreCatalog(cat)) copy.setCatalog(null);
				result.add(copy);
			}
		}
		return result;
	}
	
	/**
	 * Return the columns for the given table
	 * @return a List with {@link workbench.db.ColumnIdentifier} objects
	 */
	public List<ColumnIdentifier> getColumns(TableIdentifier tbl)
	{
		String schema = getSchemaToUse(tbl.getSchema());

		if (this.objects.size() == 0 || !schemasInCache.contains(schema == null ? NULL_SCHEMA : schema))
		{
			this.getTables(schema);
		}
		
		TableIdentifier toSearch = tbl.createCopy();
		toSearch.adjustCase(dbConnection);
		
		List<ColumnIdentifier> cols = this.objects.get(toSearch);
		
		// To support Oracle public synonyms, try to find a table with that name 
		// but without a schema
		if (retrieveOraclePublicSynonyms && toSearch.getSchema() != null && cols == null)
		{
			toSearch.setSchema(null);
			toSearch.setType(null);
			cols = this.objects.get(toSearch);
			if (cols == null)
			{
				// retrieve Oracle PUBLIC synonyms
				this.getTables("PUBLIC");
				cols = this.objects.get(toSearch);
			}
		}
		
		if (cols == null || cols == Collections.EMPTY_LIST)
		{
			TableIdentifier tblToUse = null; 
			
			// use the stored key because that might carry the correct type attribute
			// TabelIdentifier.equals() doesn't compare the type, only the expression
			// so we'll get a containsKey() == true even if the type is different
			// (which is necessary because the TableIdentifier passed to this 
			// method will never contain a type!)
			if (objects.containsKey(toSearch))
			{
				// we have already retrieved the list of tables, but not the columns for this table
				// the stored key contains precise type and schema information, so we need
				// to use that
				tblToUse = findKey(toSearch);
			}
			else
			{
				// retrieve the real table identifier based on the table name
				tblToUse = this.dbConnection.getMetadata().findSelectableObject(toSearch);
			}
			
			try
			{
				cols = this.dbConnection.getMetadata().getTableColumns(tblToUse);
				Collections.sort(cols);
			}
			catch (Throwable e)
			{
				LogMgr.logError("DbObjectCache.getColumns", "Error retrieving columns for " + tblToUse, e);
				cols = null;
			}
			if (tblToUse != null && cols != null && cols.size() > 0)
			{
				this.objects.put(tblToUse, cols);
			}
				
		}
		return Collections.unmodifiableList(cols);
	}
	
	/**
	 * Return the stored key according to the passed
	 * TableIdentifier. The stored key might carry additional
	 * properties that the passed key does not have (even 
	 * though they are equal)
	 */
	private TableIdentifier findKey(TableIdentifier key)
	{
		if (key == null) return null;
		for (TableIdentifier tbl : objects.keySet())
		{
			if (key.equals(tbl)) return tbl;
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

	/**
	 * Notification about the state of the connection. If the connection
	 * is closed, we can dispose the object cache
	 */
	public void propertyChange(PropertyChangeEvent evt)
	{
		if (WbConnection.PROP_CONNECTION_STATE.equals(evt.getPropertyName()) &&
			  WbConnection.CONNECTION_CLOSED.equals(evt.getNewValue()))
		{
			this.clear();
		}
	}

}
