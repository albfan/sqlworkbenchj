/*
 * CollectionUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Utility functions for Collection handling.
 * 
 * @author Thomas Kellerer
 */
public class CollectionUtil
{
	public static boolean isNonEmpty(Collection c)
	{
		return (c != null && c.size() > 0);
	}
	
	public static boolean isEmpty(Collection c)
	{
		return (c == null || c.isEmpty());
	}
	
	public static <E> Set<E> hashSet(E... add)
	{
		Set<E> result = new HashSet<E>(add.length);
		for (E e : add)
		{
			result.add(e);
		}
		return result;
	}

	public static <E> Set<E> hashSet(Set<E> base, E... add)
	{
		Set<E> result = new HashSet<E>(base.size() + add.length);
		result.addAll(base);
		for (E e : add)
		{
			result.add(e);
		}
		return result;
	}

	public static Set<String> caseInsensitiveSet()
	{
		return new TreeSet<String>(new CaseInsensitiveComparator());
	}

	public static Set<String> caseInsensitiveSet(String... a)
	{
		Set<String> result = caseInsensitiveSet();

		for (String e : a)
		{
			result.add(e);
		}
		return result;
	}

	public static <E> List<E> arrayList(List<E> source)
	{
		return new ArrayList<E>(source);
	}

	public static <E> List<E> sizedArrayList(int capacity)
	{
		return new ArrayList<E>(capacity);
	}

	public static <E> List<E> arrayList()
	{
		return new ArrayList<E>();
	}

	/**
	 * Create an ArrayList from the given elements. The returned list
	 * can be changed (in constrast to Arrays.asList() where a non-modifieable list
	 * is returned
	 */
	public static <E> List<E> arrayList(E... a)
	{
		ArrayList<E> result = new ArrayList<E>(a.length);
		for (E e : a)
		{
			result.add(e);
		}
		return result;
	}

	public static <E> List<E> readOnlyList(E... a)
	{
		return Collections.unmodifiableList(arrayList(a));
	}

}
