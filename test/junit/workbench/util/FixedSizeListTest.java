/*
 * FixedSizeListTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.util.List;
import junit.framework.*;

/**
 *
 * @author support@sql-workbench.net
 */
public class FixedSizeListTest extends TestCase
{

	public FixedSizeListTest(String testName)
	{
		super(testName);
	}

	public void testList()
	{
		try
		{
			FixedSizeList<String> list = new FixedSizeList<String>(5);
			list.addEntry("One");
			list.addEntry("Two");
			list.addEntry("Three");
			list.addEntry("Four");
			list.addEntry("Five");

			assertEquals("Wrong size", 5, list.size());

			list.addEntry("Six");
			assertEquals("Wrong size", 5, list.size());

			String firstEntry = list.getFirst();
			assertEquals("Wrong entry", "Six", firstEntry);

			// Should put "Three" at the "top"
			list.addEntry("Three");
			firstEntry = list.getFirst();
			assertEquals("Wrong entry", "Three", firstEntry);
			assertEquals("Wrong size", 5, list.size());

			int index = 0;
			for (String entry : list.getEntries())
			{
				if (index == 0)
				{
					assertEquals("Wrong entry", "Three", entry);
				}
				else if (index == 1)
				{
					assertEquals("Wrong entry", "Six", entry);
				}
				else if (index == 2)
				{
					assertEquals("Wrong entry", "Five", entry);
				}
				else if (index == 3)
				{
					assertEquals("Wrong entry", "Four", entry);
				}
				else if (index == 4)
				{
					assertEquals("Wrong entry", "Two", entry);
				}
				index ++;
			}

			List<String> t = CollectionUtil.arrayList("one", "two", "three");
			list = new FixedSizeList<String>(5);
			list.addAll(t);
			assertEquals("one", list.get(0));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}


}
