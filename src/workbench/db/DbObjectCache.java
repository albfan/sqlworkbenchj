/*
 * DbObjectCache.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
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
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import workbench.log.LogMgr;
import workbench.resource.Settings;

/**
 * @author  support@sql-workbench.net
 */
public class DbObjectCache
	implements PropertyChangeListener
{
	private WbConnection dbConnection;
	private boolean enabled = Settings.getInstance().getUseAutoCompletion();
	private Set schemasInCache = new HashSet();
	
	// This will map a TableIdentifier to a list of ColumnIdentifier's
	private SortedMap objects;
	
	DbObjectCache(WbConnection conn)
	{
		this.dbConnection = conn;
		conn.addChangeListener(this);
	}
	
	/**
	 * Add this list of tables to the current cache. 
	 */
	void setTables(List tables)
	{
		if (!enabled) return;
		if (this.objects == null)
		{
			this.objects = new TreeMap();
		}
		for (int i=0; i < tables.size(); i++)
		{
			TableIdentifier tbl = (TableIdentifier)tables.get(i);
			if (!this.objects.containsKey(tbl));
			{
				this.objects.put(tbl, Collections.EMPTY_LIST);
			}
		}
	}
	
	public Set getTables()
	{
		return getTables(null);
	}
	
	/**
	 * Get the tables (and views) the are currently in the cache
	 */
	public Set getTables(String schema)
	{
		if (!enabled) return Collections.EMPTY_SET;
		if (this.objects == null || (schema != null && !schemasInCache.contains(schema.toUpperCase()))) 
		{
			try
			{
				List tables = this.dbConnection.getMetadata().getTableList(schema, true);
				this.setTables(tables);
				if (schema != null)
				{
					this.schemasInCache.add(schema.toUpperCase());
				}
			}
			catch (Exception e)
			{
				LogMgr.logError("DbObjectCache.getTables()", "Could not retrieve table list", e);
			}
		}
		return filterTablesBySchema(schema);
	}
	
	private Set filterTablesBySchema(String schema)
	{
		if (schema == null)	return Collections.unmodifiableSet(this.objects.keySet());
		Iterator itr = this.objects.keySet().iterator();
		SortedSet result = new TreeSet();
		while (itr.hasNext())
		{
			TableIdentifier tbl = (TableIdentifier)itr.next();
			String tSchema = tbl.getSchema();
			if (schema.equalsIgnoreCase(tSchema) || tSchema == null || "public".equalsIgnoreCase(tSchema))
			{
				TableIdentifier copy = tbl.createCopy();
				copy.setSchema(null);
				result.add(copy);
			}
		}
		return result;
	}
	
	/**
	 * Return the columns for the given table
	 * @return a List with {@link ColumnIdentifier} objects
	 */
	public List getColumns(TableIdentifier tbl)
	{
		if (!enabled) return Collections.EMPTY_LIST;
		String schema = tbl.getSchema();
		if (this.objects == null || (schema != null && schemasInCache.contains(schema.toUpperCase())))
		{
			this.getTables(schema);
		}
		
		List cols = (List)this.objects.get(tbl);
		
		TableIdentifier tblToUse = null;
		TableIdentifier t2 = null;
		
		// if we didn't find one with the schema in the table, try
		// to find a table with that name but without the schema
		// this is to support oracle public synonyms
		if (tbl.getSchema() != null && cols == null || cols == Collections.EMPTY_LIST)
		{
			if (!this.objects.containsKey(tbl))
			{
				t2 = tbl.createCopy();
				t2.setSchema(null);
				cols = (List)this.objects.get(t2);
			}
		}
		
		if (cols == null || cols == Collections.EMPTY_LIST)
		{
			if (objects.containsKey(tbl))
			{
				tblToUse = findKey(tbl);
			}
			else if (t2 != null && objects.containsKey(t2))
			{
				tblToUse = findKey(t2);
			}
			else
			{
				tblToUse = tbl;
			}
			
			try
			{
				cols = this.dbConnection.getMetadata().getTableColumns(tblToUse);
				Collections.sort(cols);
			}
			catch (Throwable e)
			{
				LogMgr.logError("DbObjectCache.getColumns", "Error retrieving columns for " + tbl, e);
				cols = Collections.EMPTY_LIST;
			}
			this.objects.put(tbl, cols);
				
		}
		return Collections.unmodifiableList(cols);
	}
	
	private TableIdentifier findKey(TableIdentifier key)
	{
		if (key == null) return null;
		Iterator itr = this.objects.keySet().iterator();
		while (itr.hasNext())
		{
			TableIdentifier tbl = (TableIdentifier)itr.next();
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
	
	private void dumpTables()
	{
		Iterator keys = this.objects.keySet().iterator();
		System.out.println("Objects in completion cache:");
		while (keys.hasNext())
		{
			TableIdentifier tbl = (TableIdentifier)keys.next();
			System.out.println(tbl.getTableExpression() + "(" + tbl.getType() + ")");
		}
	}
}
