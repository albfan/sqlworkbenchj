/*
 * SorterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.macros;

import java.util.Set;
import java.util.TreeSet;
import junit.framework.TestCase;

/**
 *
 * @author Thomas Kellerer
 */
public class SorterTest
	extends TestCase
{
	public SorterTest(String testName)
	{
		super(testName);
	}

	public void testCompare()
	{
		Set<SortOrderElement> list = new TreeSet<SortOrderElement>(new Sorter());
		list.add(new SortOrderElement(4));
		list.add(new SortOrderElement(5));
		list.add(new SortOrderElement(1));
		list.add(new SortOrderElement(3));
		list.add(new SortOrderElement(2));

		int index = 1;
		for (SortOrderElement e : list)
		{
			assertEquals(index, e.getSortOrder());
			index ++;
		}
	}

	static class SortOrderElement
		implements Sortable
	{
		private int sortOrder;

		public SortOrderElement(int i)
		{
			sortOrder = i;
		}

		public void setSortOrder(int index)
		{
			sortOrder = index;
		}
		public int getSortOrder()
		{
			return sortOrder;
		}


	}
}
