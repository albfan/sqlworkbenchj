/*
 * FixedSizedList.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
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
public class FixedSizeList
{
	
	private LinkedList entries;
	private int maxSize;
	
	public FixedSizeList()
	{
		this(25);
	}
	
	public FixedSizeList(int max)
	{
		this.maxSize = max;
		this.entries = new LinkedList();
	}
	
	/**
	 * Append an entry at the end of the list, without
	 * checking for duplicates or removing entries
	 * that exceed the max size. This should be used
	 * to initially fill the list.
	 */
	public synchronized void append(String entry)
	{
		entries.add(entry);
	}
	
	public synchronized int addEntry(String entry)
	{
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
	
	public synchronized String getFirst()
	{
		return (String)entries.getFirst();
	}

	public Iterator iterator()
	{
		return entries.iterator();
	}
	
	public synchronized String toString()
	{
		StringBuffer result = new StringBuffer(entries.size() * 80);
		Iterator itr = entries.iterator();
		while (itr.hasNext())
		{
			result.append((String)itr.next());
			if (itr.hasNext()) result.append(',');
		}
		return result.toString();
	}

	public synchronized List getEntries()
	{
		return Collections.unmodifiableList(this.entries);
	}
	
}
