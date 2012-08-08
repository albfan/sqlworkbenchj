/*
 * CaseInsensitiveComparatorTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
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
