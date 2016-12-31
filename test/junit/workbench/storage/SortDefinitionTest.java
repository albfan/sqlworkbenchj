/*
 * SortDefinitionTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.storage;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class SortDefinitionTest
{

	@Test
	public void testRemoveColumn()
		throws Exception
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

	@Test
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

		sort = new SortDefinition(new int[]
			{
				7, 11
			}, new boolean[]
			{
				false, true
			});
		copy = sort.createCopy();
		assertEquals(sort, copy);
		assertEquals(sort.getColumnCount(), copy.getColumnCount());
		assertTrue(copy.isPrimarySortColumn(7));
		assertTrue(sort.isPrimarySortColumn(7));

		assertTrue(copy.isSortAscending(11));
		assertTrue(sort.isSortAscending(11));

		assertFalse(copy.isSortAscending(7));
		assertFalse(sort.isSortAscending(7));

		for (int i = 0; i < copy.getColumnCount(); i++)
		{
			if (i == 0)
			{
				assertEquals(7, copy.getSortColumnByIndex(i));
			}
			if (i == 1)
			{
				assertEquals(11, copy.getSortColumnByIndex(i));
			}
		}
	}

	@Test
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

	@Test
	public void testGetDefinitionString()
	{
		int[] columns = new int[] { 1,3 };
		boolean[] asc = new boolean[] { true, false };

		SortDefinition def = new SortDefinition(columns, asc);

		String result = def.getDefinitionString();
		String expected = "1,a;3,d";
		assertEquals(expected, result);

		SortDefinition newDef = SortDefinition.parseDefinitionString(result);
		assertEquals(expected, newDef.getDefinitionString());
	}

}
