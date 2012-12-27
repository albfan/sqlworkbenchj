/*
 * CollectionUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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

import java.util.*;

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

	public static boolean isEmpty(Map m)
	{
		return (m == null || m.isEmpty());
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

	public static Set<String> caseInsensitiveSet(Set<String> base, String... a)
	{
		Set<String> result = caseInsensitiveSet();
		result.addAll(base);
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

	/**
	 * Removes all NULL values from the given collection.
	 *
	 * @param elements the collection to cleanup
	 */
	public static void removeNullValues(Collection elements)
	{
		Iterator itr = elements.iterator();
		while (itr.hasNext())
		{
			if (itr.next() == null)
			{
				itr.remove();
			}
		}
	}

}
