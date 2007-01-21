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

	public void testGetEmptyValues()
	{
		// Check for empty elements at the end
		CsvLineParser parser = new CsvLineParser('\t');
		parser.setLine("one\t");
		parser.setReturnEmptyStrings(true);
		List<String> result = getParserElements(parser);
		assertEquals("Not enough values", 2, result.size());
		
		assertNotNull("Null string returned", result.get(1));
		
		parser.setLine("one\t");
		parser.setReturnEmptyStrings(false);
		result = getParserElements(parser);
		assertEquals("Not enough values", 2, result.size());
		assertNull("Empty string returned", result.get(1));

		// Check for empty element at the beginning
		parser.setReturnEmptyStrings(true);
		parser.setLine("\ttwo\tthree");
		if (parser.hasNext())
		{
			String value = parser.getNext();
			assertEquals("First value not an empty string", "", value);
		}
		else
		{
			fail("No value returned");
		}
		
		parser.setReturnEmptyStrings(false);
		parser.setLine("\ttwo\tthree");
		if (parser.hasNext())
		{
			String value = parser.getNext();
			assertNull("First value not null", value);
		}
		else
		{
			fail("No value returned");
		}
	}
	
	public void testParser()
	{
		String line = "one\ttwo\tthree\tfour\tfive";
		CsvLineParser parser = new CsvLineParser('\t');
		parser.setLine(line);
		List<String> elements = getParserElements(parser);
		assertEquals("Wrong number of elements", 5, elements.size());
		assertEquals("Wrong first value", "one", elements.get(0));
		assertEquals("Wrong second value", "two", elements.get(1));
		
		// check for embedded quotes without a quote defined!
		parser.setLine("one\ttwo\"values\tthree");
		parser.getNext(); // skip the first
		String value = parser.getNext();
		assertEquals("Invalid second element", "two\"values", value);
		
		parser = new CsvLineParser('\t', '"');
		parser.setTrimValues(false);
		parser.setReturnEmptyStrings(false);
		parser.setLine("one\t\"quoted\tdelimiter\"\t  three  ");
		List<String> l = getParserElements(parser);
		
		assertEquals("Not enough values", 3, l.size());
		assertEquals("Wrong quoted value", "quoted\tdelimiter", l.get(1));
		assertEquals("Value was trimmed", "  three  ", l.get(2));

		parser.setTrimValues(true);
		parser.setLine("one\t   two   ");
		l = getParserElements(parser);
		
		assertEquals("Not enough values", 2, l.size());
		assertEquals("Value was not trimmed", "two", l.get(1));

		// Test a different delimiter
		parser = new CsvLineParser(';', '"');
		parser.setLine("one;two;\"one;element\"");
		elements = getParserElements(parser);
		assertEquals("Wrong number of elements", 3, elements.size());
		assertEquals("Wrong first value", "one", elements.get(0));
		assertEquals("Wrong second value", "two", elements.get(1));
		assertEquals("Wrong third value", "one;element", elements.get(2));
		
	}

	private List<String> getParserElements(CsvLineParser parser)
	{
		List<String> result = new ArrayList<String>();
		while (parser.hasNext())
		{
			result.add(parser.getNext());
		}
		return result;
	}
}
