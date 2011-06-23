/*
 * FixedSizeList.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * @author Thomas Kellerer
 */
public class FixedSizeList<T>
	implements List<T>
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

	/**
	 * Add a new entry to "top" of the list.
	 * <br/>
	 * If the entry is already in the list it is "moved" to the top.
	 * If the max size of the list is exceeded the last item is removed.
	 * @param entry
	 * @return
	 */
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

	@Override
	public synchronized int size()
	{
		return entries.size();
	}

	public synchronized T getFirst()
	{
		return entries.getFirst();
	}

	@Override
	public Iterator<T> iterator()
	{
		return entries.iterator();
	}

	@Override
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

	@Override
	public synchronized boolean isEmpty()
	{
		return entries.isEmpty();
	}

	@Override
	public synchronized boolean contains(Object o)
	{
		return entries.contains(o);
	}

	@Override
	public synchronized Object[] toArray()
	{
		return entries.toArray();
	}

	@Override
	public synchronized <T> T[] toArray(T[] a)
	{
		return entries.toArray(a);
	}

	@Override
	public synchronized boolean add(T o)
	{
		addEntry(o);
		return true;
	}

	@Override
	public synchronized boolean remove(Object o)
	{
		return entries.remove(o);
	}

	@Override
	public synchronized boolean containsAll(Collection<?> c)
	{
		return entries.containsAll(c);
	}

	@Override
	public synchronized boolean addAll(Collection<? extends T> c)
	{
		for (T e : c)
		{
			entries.add(e);
		}
		return true;
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> c)
	{
		return addAll(c);
	}

	@Override
	public synchronized boolean removeAll(Collection<?> c)
	{
		return entries.removeAll(c);
	}

	@Override
	public synchronized boolean retainAll(Collection<?> c)
	{
		return entries.retainAll(c);
	}

	@Override
	public synchronized void clear()
	{
		entries.clear();
	}

	@Override
	public synchronized T get(int index)
	{
		return entries.get(index);
	}

	@Override
	public synchronized T set(int index, T element)
	{
		throw new UnsupportedOperationException("Set by index not allowed!");
	}

	@Override
	public synchronized void add(int index, T element)
	{
		addEntry(element);
	}

	@Override
	public synchronized T remove(int index)
	{
		return entries.remove(index);
	}

	@Override
	public synchronized int indexOf(Object o)
	{
		return entries.indexOf(o);
	}

	@Override
	public synchronized int lastIndexOf(Object o)
	{
		return entries.lastIndexOf(o);
	}

	@Override
	public synchronized ListIterator<T> listIterator()
	{
		return entries.listIterator();
	}

	@Override
	public synchronized ListIterator<T> listIterator(int index)
	{
		return entries.listIterator(index);
	}

	@Override
	public synchronized List<T> subList(int fromIndex, int toIndex)
	{
		return entries.subList(fromIndex, toIndex);
	}

}
