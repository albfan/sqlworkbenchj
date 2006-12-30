/*
 * StringUtilTest.java
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

import java.util.LinkedList;
import junit.framework.*;
import java.util.List;

/**
 *
 * @author support@sql-workbench.net
 */
public class StringUtilTest 
	extends TestCase
{
	
	public StringUtilTest(String testName)
	{
		super(testName);
	}

	public void testCaseCheck()
	{
		assertEquals(false, StringUtil.isUpperCase("This is a test"));
		assertEquals(true, StringUtil.isMixedCase("This is a test"));
		assertEquals(false, StringUtil.isLowerCase("This is a test"));

		assertEquals(true, StringUtil.isLowerCase("this is a test 12345 #+*-.,;:!\"$%&/()=?"));
		assertEquals(true, StringUtil.isUpperCase("THIS IS A TEST 12345 #+*-.,;:!\"$%&/()=?"));
		assertEquals(true, StringUtil.isUpperCase("1234567890"));
		assertEquals(true, StringUtil.isLowerCase("1234567890"));
	}
	
	public void testGetRealLineLenght()
	{
		int len = StringUtil.getRealLineLength("bla\r");
		assertEquals(3,len);
		
		len = StringUtil.getRealLineLength("bla\r\n");
		assertEquals(3,len);
		
		len = StringUtil.getRealLineLength("bla\r\n\n");
		assertEquals(3,len);
		
		len = StringUtil.getRealLineLength("bla \r\n\n\r");
		assertEquals(4,len);

		len = StringUtil.getRealLineLength("bla");
		assertEquals(3,len);
		
		len = StringUtil.getRealLineLength("\r\n");
		assertEquals(0,len);
		
		len = StringUtil.getRealLineLength("\n");
		assertEquals(0,len);
	}
	
	public void testIsWhitespace()
	{
		try
		{
			String s = "bla";
			assertEquals(false, StringUtil.isWhitespace(s));
			
			s = "   bla   ";
			assertEquals(false, StringUtil.isWhitespace(s));
			
			s = " \n \n";
			assertEquals(true, StringUtil.isWhitespace(s));
			
			s = " \t\r\n   ;";
			assertEquals(false, StringUtil.isWhitespace(s));
			
			s = "";
			assertEquals(false, StringUtil.isWhitespace(s));
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	public void testTrimBuffer()
	{
		try
		{
			StringBuilder b = new StringBuilder("bla");
			StringUtil.trimTrailingWhitespace(b);
			assertEquals("Buffer was changed", "bla", b.toString());
			
			b = new StringBuilder("bla bla ");
			StringUtil.trimTrailingWhitespace(b);
			assertEquals("Whitespace not removed", "bla bla", b.toString());

			b = new StringBuilder("bla bla \t");
			StringUtil.trimTrailingWhitespace(b);
			assertEquals("Whitespace not removed", "bla bla", b.toString());
			
			b = new StringBuilder("bla bla \t\n\r  \t");
			StringUtil.trimTrailingWhitespace(b);
			assertEquals("Whitespace not removed", "bla bla", b.toString());
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	public void testToArray()
	{
		try
		{
			List elements = new LinkedList();
			elements.add("one");
			elements.add("two");
			elements.add("three");
			
			String[] result = StringUtil.toArray(elements);
			assertEquals(result.length, 3);
			assertEquals(result[1], "two");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	public void testGetDoubleValue()
	{
		try
		{
			double value = StringUtil.getDoubleValue("123.45", -1);
			assertEquals(123.45, value, 0.01);
			
			value = StringUtil.getDoubleValue(" 123.45 ", -1);
			assertEquals(123.45, value, 0.01);
			
			value = StringUtil.getDoubleValue("bla", -66);
			assertEquals(-66, value, 0.01);
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	public void testGetIntValue()
	{
		try
		{
			int iValue = StringUtil.getIntValue(" 123 ", -1);
			assertEquals(123, iValue);
			
			iValue = StringUtil.getIntValue("42", -1);
			assertEquals(42, iValue);
			
			iValue = StringUtil.getIntValue("bla", -24);
			assertEquals(-24, iValue);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	public void testStringToList()
	{
		String list = "1,2,3";
		List l = StringUtil.stringToList(list, ",", true, true, true);
		assertEquals("Wrong number of elements returned", 3, l.size());
		
		list = "1,2,,3";
		l = StringUtil.stringToList(list, ",", true, true, true);
		assertEquals("Empty element not removed", 3, l.size());
		
		list = "1,2, ,3";
		l = StringUtil.stringToList(list, ",", false);
		assertEquals("Empty element removed", 4, l.size());
		
		list = "1,2,,3";
		l = StringUtil.stringToList(list, ",", false);
		assertEquals("Null element not removed", 3, l.size());
		
		list = " 1 ,2,3";
		l = StringUtil.stringToList(list, ",", true);
		assertEquals("Null element not removed", 3, l.size());
		assertEquals(" 1 ", l.get(0));
		
		l = StringUtil.stringToList(list, ",", true, true);
		assertEquals("Element not trimmed", "1", l.get(0));
		
		list = "1,\"2,5\",3";
		l = StringUtil.stringToList(list, ",", true, true, true);
		assertEquals("Quoted string not recognized","2,5", l.get(1));
	}
	
	public void testHasOpenQuotes()
	{
		String value = "this line does not have quotes";
		assertEquals("Wrong check for non-quotes", false, StringUtil.hasOpenQuotes(value, '\''));
		
		value = "this line 'does' have quotes";
		assertEquals("Wrong check for quotes", false, StringUtil.hasOpenQuotes(value, '\''));
		
		value = "this line leaves a 'quote open";
		assertEquals("Wrong check for open quotes", true, StringUtil.hasOpenQuotes(value, '\''));
	}
	
	public void testIsNumber()
	{
		boolean isNumber = StringUtil.isNumber("1");
		assertEquals(true, isNumber);
		
		isNumber = StringUtil.isNumber("1.234");
		assertEquals(true, isNumber);
		
		isNumber = StringUtil.isNumber("1.xxx");
		assertEquals(false, isNumber);
		
		isNumber = StringUtil.isNumber("bla");
		assertEquals(false, isNumber);
	}
	
	public void testTrimQuotes()
	{
		String s = StringUtil.trimQuotes(" \"bla\" ");
		assertEquals("bla", s);
		s = StringUtil.trimQuotes(" \"bla ");
		assertEquals(" \"bla ", s);
		s = StringUtil.trimQuotes(" 'bla' ");
		assertEquals("bla", s);
	}
	
	
}
