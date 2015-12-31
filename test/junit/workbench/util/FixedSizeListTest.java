/*
 * FixedSizeListTest.java
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
package workbench.util;

import java.util.List;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class FixedSizeListTest
{

	@Test
	public void testList()
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
			index++;
		}

		List<String> t = CollectionUtil.arrayList("one", "two", "three");
		list = new FixedSizeList<String>(5);
		list.addAll(t);
		assertEquals("one", list.get(0));
	}
}
