/*
 * ObjectNameFilterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.util.Collection;
import java.util.Set;
import workbench.util.CollectionUtil;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class ObjectNameFilterTest
{

	@Test
	public void testInclusion()
	{
		ObjectNameFilter filter = new ObjectNameFilter();
		filter.setIsInclusionFilter(true);
		filter.resetModified();

		Set<String> names = CollectionUtil.treeSet("ONE ", "^DEV[0-9]+");
		filter.setFilterExpressions(names);
		assertFalse(filter.isExcluded("one"));
		assertFalse(filter.isExcluded("dev1"));
		assertTrue(filter.isExcluded("public"));
	}

	@Test
	public void testIsExcluded()
	{
		ObjectNameFilter filter = new ObjectNameFilter();
		filter.setIsInclusionFilter(false);
		Set<String> names = CollectionUtil.treeSet("ONE ", "^DEV[0-9]+");
		filter.setFilterExpressions(names);
		assertTrue(filter.isExcluded("one"));
		assertTrue(filter.isExcluded("dev1"));
		assertFalse(filter.isExcluded("public"));
		assertFalse(filter.isModified());
		filter.addExpression("something");
		assertTrue(filter.isModified());
		assertTrue(filter.isExcluded("something"));
		filter.resetModified();
		assertFalse(filter.isModified());

		filter = new ObjectNameFilter();
		filter.setExpressionList("ONE;^DEV[0-9]+");
		assertTrue(filter.isExcluded("one"));
		assertTrue(filter.isExcluded("dev1"));
		assertFalse(filter.isExcluded("public"));
		assertFalse(filter.isModified());
		Collection<String> expr = filter.getFilterExpressions();
		assertNotNull(expr);
		assertEquals(2, expr.size());

		filter = new ObjectNameFilter();
		expr = filter.getFilterExpressions();
		assertNull(expr);
	}

	public void testCopy()
	{
		ObjectNameFilter filter = new ObjectNameFilter();
		Set<String> names = CollectionUtil.treeSet("ONE", "two");
		filter.setFilterExpressions(names);
		ObjectNameFilter copy = filter.createCopy();
		assertTrue(filter.equals(copy));
		filter.addExpression("three");
		assertEquals(2, copy.getSize());
		assertTrue(copy.isExcluded("one"));
		assertFalse(copy.isExcluded("three"));

		filter.setIsInclusionFilter(true);
		filter.resetModified();
		copy = filter.createCopy();
		assertTrue(filter.getIsInclusionFilter());
	}

}
