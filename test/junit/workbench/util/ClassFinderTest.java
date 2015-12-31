/*
 * ClassFinderTest.java
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

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class ClassFinderTest
{

	public ClassFinderTest()
	{
	}

	@Test
	public void testFindClass()
		throws Exception
	{
		String path = System.getProperty("java.class.path");
		List<String> elements = StringUtil.stringToList(path, System.getProperty("path.separator"));
		List<String> toSearch = new ArrayList<>();
		for (String entry : elements)
		{
			if (entry.endsWith(".jar") &&
				!entry.contains("poi") &&
				!entry.contains("jemmy") &&
				!entry.contains("log4j") &&
				!entry.contains("ant") &&
				!entry.contains("junit-4.8"))
			{
				toSearch.add(entry);
			}
		}
		ClassFinder finder = new ClassFinder(java.sql.Driver.class);
		List<String> drivers = finder.findImplementations(toSearch);
		assertTrue(drivers.size() >= 5);
	}

}
