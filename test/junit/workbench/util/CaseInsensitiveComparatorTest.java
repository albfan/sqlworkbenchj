/*
 * CaseInsensitiveComparatorTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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

import java.util.Set;
import java.util.TreeSet;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class CaseInsensitiveComparatorTest
{
	public CaseInsensitiveComparatorTest()
	{
	}

	@Before
	public void setUp()
	{
	}

	@Test
	public void testSetIgnoreQuotes()
	{
		CaseInsensitiveComparator comp = new CaseInsensitiveComparator();
		comp.setIgnoreQuotes(true);
		assertEquals(0, comp.compare("\"foo\"", "FOO"));
		assertEquals(0, comp.compare("'foo'", "\"FOO\""));
		Set<String> values = new TreeSet<String>(comp);
		values.add("foo");
		values.add("FOO");
		assertEquals(1, values.size());
		values.add("foobar");
		assertEquals(2, values.size());
		values.add("\"Foo\"");
		assertEquals(2, values.size());
		assertTrue(values.contains("foo"));
		assertTrue(values.contains("\"foo\""));
		assertTrue(values.contains("FOO"));

		values.add("'BAR'");
		assertTrue(values.contains("bar"));
		assertEquals(3, values.size());
	}

	@Test
	public void testCompare()
	{
		CaseInsensitiveComparator comp = new CaseInsensitiveComparator();
		assertEquals(0, comp.compare("foo", "FOO"));
		assertEquals(0, comp.compare("FOO", "Foo"));
	}

}
