/*
 * FixedLengthLineParserTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;

/**
 *
 * @author thomas
 */
public class FixedLengthLineParserTest
	extends TestCase
{

	public FixedLengthLineParserTest(String testName)
	{
		super(testName);
	}

	public void testParser()
	{
		try
		{
			List<Integer> cols = new ArrayList<Integer>();
			cols.add(new Integer(5));
			cols.add(new Integer(1));
			cols.add(new Integer(10));
			FixedLengthLineParser parser = new FixedLengthLineParser(cols);
			String line = "12345H1234567890";
			parser.setLine(line);
			String first = parser.getNext();
			assertEquals("12345", first);
			String second = parser.getNext();
			assertEquals("H", second);
			String third = parser.getNext();
			assertEquals("1234567890", third);

			line = "    1H        10";
			parser.setLine(line);
			parser.setTrimValues(true);
			first = parser.getNext();
			assertEquals("1", first);
			second = parser.getNext();
			assertEquals("H", second);
			third = parser.getNext();
			assertEquals("10", third);

			parser.setLine(line);
			parser.setTrimValues(false);
			first = parser.getNext();
			assertEquals("    1", first);
			second = parser.getNext();
			assertEquals("H", second);
			third = parser.getNext();
			assertEquals("        10", third);

		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
