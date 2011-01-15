/*
 * CollectionUtilTest.java
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

import java.util.List;
import java.util.Set;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class CollectionUtilTest
{
	
	@Test
	public void testCreateHashSet()
	{
		Set<String> set = CollectionUtil.treeSet("one", "two");
		assertNotNull(set);
		assertEquals(2, set.size());
		set.add("three");
		assertEquals(3, set.size());
		assertTrue(set.contains("one"));
		assertTrue(set.contains("two"));
		assertTrue(set.contains("three"));

		Set<String> second = CollectionUtil.treeSet(set, "four", "five");
		assertEquals(5, second.size());
		assertTrue(second.contains("one"));
		assertTrue(second.contains("two"));
		assertTrue(second.contains("three"));
		assertTrue(second.contains("four"));
		assertTrue(second.contains("five"));

		assertFalse(set.contains("four"));
		assertFalse(set.contains("five"));
	}

	@Test
	public void testCaseInsensitiveSet()
	{
		Set<String> result = CollectionUtil.caseInsensitiveSet("one", "two", "THREE");
		assertTrue(result.contains("ONE"));
		assertTrue(result.contains("Two"));
		assertTrue(result.contains("three"));
	}

	@Test
	public void testCreateList()
	{
		List<Integer> result = CollectionUtil.arrayList(1, 2, 3);
		assertNotNull(result);
		assertEquals(3, result.size());
		result.add(4);
		assertEquals(4, result.size());
	}
}
