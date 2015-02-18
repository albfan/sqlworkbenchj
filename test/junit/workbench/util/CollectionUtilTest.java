/*
 * CollectionUtilTest.java
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

import java.util.List;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.*;

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

	@Test
	public void testRemoveElement()
	{
		String[] list = new String[] {"ONE", "TWO", "THREE"};
		String[] clean = CollectionUtil.removeElement(list, "TWO");
		assertNotNull(clean);
		assertEquals(2, clean.length);
		assertEquals("ONE", clean[0]);
		assertEquals("THREE", clean[1]);

		clean = CollectionUtil.removeElement(list, "foo");
		assertNotNull(clean);
		assertEquals(3, clean.length);
		assertEquals("ONE", clean[0]);
		assertEquals("TWO", clean[1]);
		assertEquals("THREE", clean[2]);
	}
}
