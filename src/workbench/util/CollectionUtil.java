/*
 * CollectionUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author support@sql-workbench.net
 */
public class CollectionUtil
{

	public static <E> Set<E> createHashSet(E... add)
	{
		Set<E> result = new HashSet<E>(add.length);
		for (E e : add)
		{
			result.add(e);
		}
		return result;
	}

	public static <E> Set<E> createHashSet(Set<E> base, E... add)
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

	/**
	 * Create an ArrayList from the given elements. The returned list
	 * can be changed (in constrast to Arrays.asList() where the returned
	 * List does dot support the add() method)
	 */
	public static <E> List<E> createList(E... a)
	{
		ArrayList<E> result = new ArrayList<E>(a.length);
		for (E e : a)
		{
			result.add(e);
		}
		return result;
	}
}
