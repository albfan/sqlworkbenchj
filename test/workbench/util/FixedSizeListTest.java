/*
 * FixedSizedListTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import junit.framework.*;
import java.util.Iterator;
import java.util.LinkedList;

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
			FixedSizeList list = new FixedSizeList(5);
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
			
			Iterator itr = list.iterator();
			int index = 0;
			while (itr.hasNext())
			{
				if (index == 0)
				{
					assertEquals("Wrong entry", "Three", itr.next());
				}
				else if (index == 1)
				{
					assertEquals("Wrong entry", "Six", itr.next());
				}
				else if (index == 2)
				{
					assertEquals("Wrong entry", "Five", itr.next());
				}
				else if (index == 3)
				{
					assertEquals("Wrong entry", "Four", itr.next());
				}
				else if (index == 4)
				{
					assertEquals("Wrong entry", "Two", itr.next());
				}
				index ++;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	
}
