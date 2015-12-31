/*
 * ObjectNameFilterTest.java
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
package workbench.db;

import java.util.Collection;
import java.util.Set;

import workbench.util.CollectionUtil;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class ObjectNameFilterTest
{

	@Test
	public void testEmpty()
	{
		ObjectNameFilter filter = new ObjectNameFilter();
		assertFalse(filter.isExcluded(null));
		assertFalse(filter.isExcluded(""));
		assertFalse(filter.isExcluded("foo"));
	}

	@Test
	public void testInclusion()
	{
		ObjectNameFilter filter = new ObjectNameFilter();
		filter.setInclusionFilter(true);
		filter.resetModified();

		Set<String> names = CollectionUtil.treeSet("ONE ", "^DEV[0-9]+");
		filter.setFilterExpressions(names);
		assertFalse(filter.isExcluded("one"));
		assertFalse(filter.isExcluded("dev1"));
		assertTrue(filter.isExcluded("public"));
		assertTrue(filter.isExcluded(null));
	}

	@Test
	public void testIsExcluded()
	{
		ObjectNameFilter filter = new ObjectNameFilter();
		filter.setInclusionFilter(false);
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
		assertTrue(expr.isEmpty());
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

		filter.setInclusionFilter(true);
		filter.resetModified();
		copy = filter.createCopy();
		assertTrue(filter.isInclusionFilter());
	}

}
