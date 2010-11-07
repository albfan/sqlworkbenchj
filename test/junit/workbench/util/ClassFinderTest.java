/*
 * ClassFinderTest
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
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
		List<String> toSearch = new ArrayList<String>();
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
