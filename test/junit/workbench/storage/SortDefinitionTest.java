/*
 * SortDefinitionTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage;

import junit.framework.TestCase;

/**
 *
 * @author Thomas Kellerer
 */
public class SortDefinitionTest
	extends TestCase
{
	public SortDefinitionTest(String testName)
	{
		super(testName);
	}

	public void testRemoveColumn()
	{
		try
		{
			SortDefinition sort = new SortDefinition();
			sort.addSortColumn(5, true);
			sort.addSortColumn(7, false);
			sort.addSortColumn(8, true);
			assertEquals(3, sort.getColumnCount());

			sort.removeSortColumn(7);
			assertEquals(2, sort.getColumnCount());

			// Check to remove non-existing column
			sort.removeSortColumn(2558);
			assertEquals(2, sort.getColumnCount());
			
			int col = sort.getSortColumnByIndex(0);
			assertEquals(5, col);
			
			col = sort.getSortColumnByIndex(1);
			assertEquals(8, col);
			assertTrue(sort.isSortAscending(5));
			assertTrue(sort.isSortAscending(8));
			
			sort.setSortColumn(13, true);
			assertEquals(1, sort.getColumnCount());
			sort.addSortColumn(7, true);
			sort.addSortColumn(8, true);
			
			sort.removeSortColumn(13);
			assertEquals(2, sort.getColumnCount());
			assertTrue(sort.isPrimarySortColumn(7));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	public void testCreateCopy()
	{
		SortDefinition sort = new SortDefinition();
		SortDefinition copy = sort.createCopy();
		assertEquals(sort, copy);
		assertEquals(sort.getColumnCount(), copy.getColumnCount());
		
		sort = new SortDefinition(2, true);
		copy = sort.createCopy();
		assertEquals(sort, copy);
		assertEquals(sort.getColumnCount(), copy.getColumnCount());
		assertTrue(copy.isPrimarySortColumn(2));
		assertTrue(sort.isPrimarySortColumn(2));

		assertTrue(copy.isSortAscending(2));
		assertTrue(sort.isSortAscending(2));
		
		sort = new SortDefinition(new int[] {7,11}, new boolean[] { false, true});
		copy = sort.createCopy();
		assertEquals(sort, copy);
		assertEquals(sort.getColumnCount(), copy.getColumnCount());
		assertTrue(copy.isPrimarySortColumn(7));
		assertTrue(sort.isPrimarySortColumn(7));

		assertTrue(copy.isSortAscending(11));
		assertTrue(sort.isSortAscending(11));
		
		assertFalse(copy.isSortAscending(7));
		assertFalse(sort.isSortAscending(7));
		
		for (int i=0; i < copy.getColumnCount(); i++)
		{
			if (i == 0) assertEquals(7, copy.getSortColumnByIndex(i));
			if (i == 1) assertEquals(11, copy.getSortColumnByIndex(i));
		}
	}

	public void testSort()
	{
		SortDefinition sort = new SortDefinition();
		sort.setSortColumn(3, true);
		assertTrue(sort.isSortAscending(3));
		
		sort.addSortColumn(4, false);
		assertEquals(2, sort.getColumnCount());
		
		assertFalse(sort.isSortAscending(4));
		assertTrue(sort.isSortAscending(3));
		assertTrue(sort.isPrimarySortColumn(3));
		assertFalse(sort.isPrimarySortColumn(4));
	}

}
