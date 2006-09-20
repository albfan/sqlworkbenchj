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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import workbench.util.FixedSizeList;

/**
 * Provide a LRU Lookup for filter definitions saved to a file.
 * 
 * @author support@sql-workbench.net
 */
public class FilterDefinitionManager
{
	
	private LinkedList listeners;
	private FixedSizeList filterFiles;
	private static final FilterDefinitionManager instance = new FilterDefinitionManager();
	
	public FilterDefinitionManager getInstance() 
	{
		return instance;
	}
	
	private FilterDefinitionManager()
	{
		this.filterFiles = new FixedSizeList(15);
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
		PropertyChangeEvent evt = new PropertyChangeEvent(this, "listSize", null, new Integer(filterFiles.size()));
		while (itr.hasNext())
		{
			PropertyChangeListener l = (PropertyChangeListener)itr.next();
			if (l == null) continue;
			l.propertyChange(evt);
		}
	}
	
	public List getFilterFiles()
	{
		return filterFiles.getEntries();
	}
}
