/*
 * FilterDefinitionManager.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.filter;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.filter.FilterExpression;
import workbench.util.FixedSizeList;
import workbench.util.WbFile;
import workbench.util.WbPersistence;

/**
 * Provide a MRU lookup for filter definitions saved to a file.
 * 
 * @author support@sql-workbench.net
 */
public class FilterDefinitionManager
{
	private LinkedList<PropertyChangeListener> listeners;
	private FixedSizeList<WbFile> filterFiles;
	private static FilterDefinitionManager instance;
	private static final int DEFAULT_MAX_SIZE = 15;
	
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
		int size = Settings.getInstance().getIntProperty("workbench.gui.filter.mru.maxsize", DEFAULT_MAX_SIZE);
		this.filterFiles = new FixedSizeList<WbFile>(size);
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
				WbFile f = new WbFile(filename);
				if (f.exists())
				{
					filterFiles.append(f);
				}
				else
				{
					LogMgr.logInfo("FilterDefinitionManager.loadMRUList()", "Removed filter file '" + f.getFullPath() + "' from list because it does not longer exist");
				}
			}
		}
	}
	
	private void removeOldSettings()
	{
		Settings s = Settings.getInstance();
		int size = s.getIntProperty("workbench.gui.filter.mru.maxsize", DEFAULT_MAX_SIZE);
		for (int i=0; i < size; i++)
		{
			s.removeProperty("workbench.gui.filter.mru.entry." + i);
		}
	}
	
	public void saveMRUList()
	{
		removeOldSettings();
		
		Settings s = Settings.getInstance();
		int index = 0;
		for (WbFile f : filterFiles.getEntries())
		{
			s.setProperty("workbench.gui.filter.mru.entry." + index, f.getFullPath());
			index ++;
		}
		s.setProperty("workbench.gui.filter.mru.size", index);
	}
	
	public synchronized void addPropertyChangeListener(PropertyChangeListener l)
	{
		if (this.listeners == null) this.listeners = new LinkedList<PropertyChangeListener>();
		this.listeners.add(l);
	}
	
	public synchronized void filterSaved(WbFile filename)
	{
		filterFiles.addEntry(filename);
		firePropertyChanged();
	}
	
	private synchronized void firePropertyChanged()
	{
		if (this.listeners == null) return;
		PropertyChangeEvent evt = new PropertyChangeEvent(this, "mruList", null, new Integer(filterFiles.size()));
		for(PropertyChangeListener l : listeners)
		{
			if (l == null) continue;
			l.propertyChange(evt);
		}
	}
	
	public List<WbFile> getEntries()
	{
		return filterFiles.getEntries();
	}
	
	public void saveFilter(FilterExpression filter, WbFile file)
		throws IOException
	{
		WbPersistence p = new WbPersistence(file.getFullPath());
		p.writeObject(filter);
		filterSaved(file);
	}
	
	public FilterExpression loadFilter(String filename)
		throws Exception
	{
		WbPersistence p = new WbPersistence(filename);
		FilterExpression result = null;
		Object o = p.readObject();
		if (o instanceof FilterExpression)
		{
			result = (FilterExpression)o;
		} 
		return result;
	}
}
