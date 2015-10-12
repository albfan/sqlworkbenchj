/*
 * FixedSizeList.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
	protected final LinkedList<T> entries = new LinkedList<>();
	private int maxSize;
	private boolean appendEntries;
	private boolean allowDuplicates = false;

	public FixedSizeList()
	{
		this(25);
	}

	public FixedSizeList(int max)
	{
		this.maxSize = max;
	}

	public void doAppend(boolean flag)
	{
		this.appendEntries = flag;
	}

	public void setAllowDuplicates(boolean flag)
	{
		this.allowDuplicates = flag;
	}

	/**
	 * Append an entry at the end of the list, without
	 * checking for duplicates or removing entries
	 * that exceed the max size.
	 *
	 * This should be used to initially fill the list.
	 */
	public synchronized void append(T entry)
	{
		if (entry == null) return;
		entries.add(entry);
	}

	/**
	 * Add a new entry to the list.
	 *
	 * <br/>
	 * If duplicates are allowed, no further checks are done. If no duplicates
	 * are allowed and the entry is already in the list it is "moved" to the top.
	 * <br/>
	 * If append mode is enabled, the new entry is add to the end of the list, otherwise to the top.
	 * @param entry
	 * @return the new size of the list
	 *
	 * @see #doAppend(boolean)
	 * @see #setAllowDuplicates(boolean)
	 */
	public synchronized int addEntry(T entry)
	{
		if (entry == null) return -1;

		if (!allowDuplicates && entries.contains(entry))
		{
			entries.remove(entry);
		}

		if (appendEntries)
		{
			entries.addLast(entry);
		}
		else
		{
			entries.addFirst(entry);
		}

		while (entries.size() > maxSize)
		{
			if (appendEntries)
			{
				entries.removeFirst();
			}
			else
			{
				entries.removeLast();
			}
		}
		return entries.size();
	}

  public T removeFirst()
  {
    return entries.removeFirst();
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
