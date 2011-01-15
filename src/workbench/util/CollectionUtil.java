/*
 * CollectionUtil.java
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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

	public static <E> Set<E> treeSet(E... add)
	{
		Set<E> result = new TreeSet<E>();
		result.addAll(Arrays.asList(add));
		return result;
	}
	public static <E> Set<E> treeSet(Set<E> base, E... add)
	{
		Set<E> result = new TreeSet<E>();
		result.addAll(base);
		result.addAll(Arrays.asList(add));
		return result;
	}
	public static Set<String> caseInsensitiveSet()
	{
		return new TreeSet<String>(CaseInsensitiveComparator.INSTANCE);
	}

	public static Set<String> caseInsensitiveSet(String... a)
	{
		Set<String> result = caseInsensitiveSet();
		result.addAll(Arrays.asList(a));
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
	 * is returned)
	 */
	public static <E> List<E> arrayList(E... a)
	{
		ArrayList<E> result = new ArrayList<E>(a.length);
		result.addAll(Arrays.asList(a));
		return result;
	}

	public static <E> List<E> readOnlyList(E... a)
	{
		return Collections.unmodifiableList(arrayList(a));
	}

}
