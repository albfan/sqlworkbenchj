/*
 * FixedSizeList.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author support@sql-workbench.net
 */
public class FixedSizeList<T>
{
	private LinkedList<T> entries;
	private int maxSize;
	
	public FixedSizeList()
	{
		this(25);
	}
	
	public FixedSizeList(int max)
	{
		this.maxSize = max;
		this.entries = new LinkedList<T>();
	}
	
	/**
	 * Append an entry at the end of the list, without
	 * checking for duplicates or removing entries
	 * that exceed the max size. This should be used
	 * to initially fill the list.
	 */
	public synchronized void append(T entry)
	{
		if (entry == null) return;
		entries.add(entry);
	}
	
	public synchronized int addEntry(T entry)
	{
		if (entry == null) return -1;
		
		// Don't allow duplicates
		if (entries.contains(entry))
		{
			entries.remove(entry);
		}
		entries.addFirst(entry);
		while (entries.size() > maxSize) 
		{
			entries.removeLast();
		}
		return entries.size();
	}
	
	public synchronized int size()
	{
		return entries.size();
	}
	
	public synchronized T getFirst()
	{
		return entries.getFirst();
	}

	public Iterator iterator()
	{
		return entries.iterator();
	}
	
	public synchronized String toString()
	{
		StringBuilder result = new StringBuilder(entries.size() * 80);
		Iterator<T> itr = entries.iterator();
		while (itr.hasNext())
		{
			result.append(itr.next().toString());
			if (itr.hasNext()) result.append(',');
		}
		return result.toString();
	}

	public synchronized List<T> getEntries()
	{
		return Collections.unmodifiableList(this.entries);
	}
	
}
