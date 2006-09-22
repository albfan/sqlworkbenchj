/*
 * FilterLRUManager.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.filter;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import workbench.resource.Settings;
import workbench.storage.filter.FilterExpression;
import workbench.util.FixedSizeList;
import workbench.util.WbPersistence;

/**
 * Provide a MRU lookup for filter definitions saved to a file.
 * 
 * @author support@sql-workbench.net
 */
public class FilterDefinitionManager
{
	private LinkedList listeners;
	private FixedSizeList filterFiles;
	private static FilterDefinitionManager instance;
	
	public static synchronized FilterDefinitionManager getInstance() 
	{
		if (instance == null)
		{
			instance = new FilterDefinitionManager();
		}
		return instance;
	}
	
	private FilterDefinitionManager()
	{
		int size = Settings.getInstance().getIntProperty("workbench.gui.filter.mru.maxsize", 15);
		this.filterFiles = new FixedSizeList(size);
		loadMRUList();
	}

	private void loadMRUList()
	{
		Settings s = Settings.getInstance();
		int size = s.getIntProperty("workbench.gui.filter.mru.size", 0);
		for (int i=0; i < size; i++)
		{
			String filename = s.getProperty("workbench.gui.filter.mru.entry." + i, null);
			if (filename != null)
			{
				filterFiles.append(filename);
			}
		}
	}
	
	private void removeOldSettings()
	{
		Settings s = Settings.getInstance();
		int size = s.getIntProperty("workbench.gui.filter.mru.maxsize", 15);
		for (int i=0; i < size; i++)
		{
			s.removeProperty("workbench.gui.filter.mru.entry." + i);
		}
	}
	
	public void saveMRUList()
	{
		removeOldSettings();
		
		Settings s = Settings.getInstance();
		Iterator itr = filterFiles.iterator();
		int index = 0;
		while (itr.hasNext())
		{
			String file = (String)itr.next();
			s.setProperty("workbench.gui.filter.mru.entry." + index, file);
			index ++;
		}
		s.setProperty("workbench.gui.filter.mru.size", index);
	}
	
	public synchronized void addPropertyChangeListener(PropertyChangeListener l)
	{
		if (this.listeners == null) this.listeners = new LinkedList();
		this.listeners.add(l);
	}
	
	public void filterSaved(String filename)
	{
		filterFiles.addEntry(filename);
		firePropertyChanged();
	}
	
	private synchronized void firePropertyChanged()
	{
		if (this.listeners == null) return;
		Iterator itr = this.listeners.iterator();
		PropertyChangeEvent evt = new PropertyChangeEvent(this, "mruList", null, new Integer(filterFiles.size()));
		while (itr.hasNext())
		{
			PropertyChangeListener l = (PropertyChangeListener)itr.next();
			if (l == null) continue;
			l.propertyChange(evt);
		}
	}
	
	public List getEntries()
	{
		return Collections.unmodifiableList(filterFiles.getEntries());
	}
	
	public void saveFilter(FilterExpression filter, String file)
		throws IOException
	{
		WbPersistence p = new WbPersistence(file);
		p.writeObject(filter);
		filterSaved(file);
	}
	
	public FilterExpression loadFilter(String filename)
		throws Exception
	{
		WbPersistence p = new WbPersistence(filename);
		FilterExpression result = null;
		Object o = p.readObject();
		if (o != null && o instanceof FilterExpression)
		{
			result = (FilterExpression)o;
		} 
		return result;
	}
}
