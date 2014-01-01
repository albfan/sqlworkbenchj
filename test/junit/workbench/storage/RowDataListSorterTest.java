/*
 * RowDataListSorterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
package workbench.storage;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class RowDataListSorterTest
{

	@Test
	public void testSort()
		throws Exception
	{
		RowDataList data = new RowDataList(20);
		RowData row = null;

		row = new RowData(2);
		row.setValue(0, new Integer(2));
		row.setValue(1, new Integer(2));
		data.add(row);

		row = new RowData(2);
		row.setValue(0, new Integer(2));
		row.setValue(1, new Integer(3));
		data.add(row);

		row = new RowData(2);
		row.setValue(0, new Integer(2));
		row.setValue(1, new Integer(1));
		data.add(row);

		row = new RowData(2);
		row.setValue(0, new Integer(1));
		row.setValue(1, new Integer(3));
		data.add(row);

		row = new RowData(2);
		row.setValue(0, new Integer(1));
		row.setValue(1, new Integer(1));
		data.add(row);

		row = new RowData(2);
		row.setValue(0, new Integer(1));
		row.setValue(1, new Integer(2));
		data.add(row);

		assertEquals(data.size(), 6);

		Integer i1 = (Integer) data.get(0).getValue(0);
		assertEquals(i1.intValue(), 2);

		RowDataListSorter sorter = new RowDataListSorter(0, true);
		sorter.sort(data);

		i1 = (Integer) data.get(0).getValue(0);
		assertEquals(i1.intValue(), 1);

		i1 = (Integer) data.get(3).getValue(0);
		assertEquals(i1.intValue(), 2);

		sorter = new RowDataListSorter(0, false);
		sorter.sort(data);

		i1 = (Integer) data.get(0).getValue(0);
		assertEquals(i1.intValue(), 2);

		i1 = (Integer) data.get(3).getValue(0);
		assertEquals(i1.intValue(), 1);

		sorter = new RowDataListSorter(new int[]{0, 1}, new boolean[]{true, true});
		sorter.sort(data);

		i1 = (Integer) data.get(0).getValue(0);
		assertEquals(i1.intValue(), 1);

		i1 = (Integer) data.get(0).getValue(1);
		assertEquals(i1.intValue(), 1);

		i1 = (Integer) data.get(1).getValue(0);
		assertEquals(i1.intValue(), 1);

		i1 = (Integer) data.get(1).getValue(1);
		assertEquals(i1.intValue(), 2);

		sorter = new RowDataListSorter(new int[]{0, 1}, new boolean[]{true, false});
		sorter.sort(data);

		i1 = (Integer) data.get(0).getValue(0);
		assertEquals(i1.intValue(), 1);

		i1 = (Integer) data.get(0).getValue(1);
		assertEquals(i1.intValue(), 3);

		i1 = (Integer) data.get(1).getValue(0);
		assertEquals(i1.intValue(), 1);

		i1 = (Integer) data.get(1).getValue(1);
		assertEquals(i1.intValue(), 2);
	}
}
