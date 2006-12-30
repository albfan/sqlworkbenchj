/*
 * RowDataListSorterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage;

import junit.framework.*;
import workbench.TestUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class RowDataListSorterTest extends TestCase
{
	
	public RowDataListSorterTest(String testName)
	{
		super(testName);
	}

	public void testSort()
	{
		try
		{
			TestUtil util = new TestUtil(getClass().getName() + "_testSort");
			util.prepareEnvironment();
			
			int cols = 2;
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
			
			Integer i1 = (Integer)data.get(0).getValue(0);
			assertEquals(i1.intValue(), 2);
			
			RowDataListSorter sorter = new RowDataListSorter(0, true);
			sorter.sort(data);
			
			i1 = (Integer)data.get(0).getValue(0);
			assertEquals(i1.intValue(), 1);
			
			i1 = (Integer)data.get(3).getValue(0);
			assertEquals(i1.intValue(), 2);
			
			sorter = new RowDataListSorter(0, false);
			sorter.sort(data);			
			
			i1 = (Integer)data.get(0).getValue(0);
			assertEquals(i1.intValue(), 2);
			
			i1 = (Integer)data.get(3).getValue(0);
			assertEquals(i1.intValue(), 1);
			
			sorter = new RowDataListSorter(new int[] {0,1}, new boolean[] { true, true });
			sorter.sort(data);			

			i1 = (Integer)data.get(0).getValue(0);
			assertEquals(i1.intValue(), 1);
			
			i1 = (Integer)data.get(0).getValue(1);
			assertEquals(i1.intValue(), 1);

			i1 = (Integer)data.get(1).getValue(0);
			assertEquals(i1.intValue(), 1);
			
			i1 = (Integer)data.get(1).getValue(1);
			assertEquals(i1.intValue(), 2);

			sorter = new RowDataListSorter(new int[] {0,1}, new boolean[] { true, false });
			sorter.sort(data);			

			i1 = (Integer)data.get(0).getValue(0);
			assertEquals(i1.intValue(), 1);
			
			i1 = (Integer)data.get(0).getValue(1);
			assertEquals(i1.intValue(), 3);

			i1 = (Integer)data.get(1).getValue(0);
			assertEquals(i1.intValue(), 1);
			
			i1 = (Integer)data.get(1).getValue(1);
			assertEquals(i1.intValue(), 2);
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
