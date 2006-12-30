/*
 * CsvLineParserTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.util.ArrayList;
import java.util.List;
import junit.framework.*;

/**
 *
 * @author support@sql-workbench.net
 */
public class CsvLineParserTest 
	extends TestCase
{
	
	public CsvLineParserTest(String testName)
	{
		super(testName);
	}

	public void testParser()
	{
		String line = "one\ttwo\tthree\tfour\tfive";
		CsvLineParser parser = new CsvLineParser('\t');
		parser.setLine(line);
		int count = 0;
		while (parser.hasNext())
		{
			count ++;
			String value = parser.getNext();
			if (count == 1)
			{
				assertEquals("Wrong first value", "one", value);
			}
			else if (count == 2)
			{
				assertEquals("Wrong second value", "two", value);
			}
			else if (count == 3)
			{
				assertEquals("Wrong third value", "three", value);
			}
			else if (count == 4)
			{
				assertEquals("Wrong forth value", "four", value);
			}
			else if (count == 5)
			{
				assertEquals("Wrong fifth value", "five", value);
			}
		}
		assertEquals("Not enough values", 5, count);
		
		parser.setReturnEmptyStrings(true);
		parser.setLine("\ttwo\tthree");
		if (parser.hasNext())
		{
			String value = parser.getNext();
			assertEquals("First value not an empty string", "", value);
		}
		
		// check for embedded quotes without a quote defined!
		parser.setReturnEmptyStrings(true);
		parser.setLine("one\ttwo\"values\tthree");
		parser.getNext(); // skip the first
		String value = parser.getNext();
		assertEquals("Invalid element with \" character", "two\"values", value);
		
		parser = new CsvLineParser('\t', '"');
		parser.setTrimValues(false);
		parser.setReturnEmptyStrings(false);
		parser.setLine("one\t\"quoted\tdelimiter\"\t  three  ");
		List l = getParserElements(parser);
		
		assertEquals("Not enough values", 3, l.size());
		assertEquals("Wrong quoted value", "quoted\tdelimiter", l.get(1));
		assertEquals("Value was trimmed", "  three  ", l.get(2));

		parser.setTrimValues(true);
		parser.setLine("one\t   two   ");
		l = getParserElements(parser);
		
		assertEquals("Not enough values", 2, l.size());
		assertEquals("Value was not trimmed", "two", l.get(1));
		
	}

	private List getParserElements(CsvLineParser parser)
	{
		List result = new ArrayList();
		while (parser.hasNext())
		{
			result.add(parser.getNext());
		}
		return result;
	}
}
