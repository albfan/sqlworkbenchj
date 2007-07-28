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

import java.util.Comparator;
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

	public void testGetObjectNames()
	{
		try
		{
			String s = "\"MIND\",\"test\"";
			List<String> tables = StringUtil.getObjectNames(s);
			assertEquals(2, tables.size());
			assertEquals("\"MIND\"", tables.get(0));
			assertEquals("\"test\"", tables.get(1));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	public void testReplace()
	{
		try
		{
			String s = StringUtil.replace(null, "gaga", "gogo");
			assertNull(s);
			
			s = StringUtil.replace("gaga", null, "gogo");
			assertEquals("gaga", s);
			
			s = StringUtil.replace("gaga", "gogo", null);
			assertEquals("gaga", s);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	public void testLastIndexOf()
	{
		try
		{
			String s = "this is a test.";
			int pos = StringUtil.lastIndexOf(s, '.');
			assertEquals(s.length() - 1, pos);
			
			s = "this. is. a. test.";
			pos = StringUtil.lastIndexOf(s, '.');
			assertEquals(s.length() - 1, pos);
			
			s = "this is a test";
			pos = StringUtil.lastIndexOf(s, '.');
			assertEquals(-1, pos);
			
			StringBuilder b = new StringBuilder("this is a test.");
			pos = StringUtil.lastIndexOf(b, '.');
			assertEquals(b.length() - 1, pos);
			
			b = new StringBuilder("this. is a test");
			pos = StringUtil.lastIndexOf(b, '.');
			assertEquals(4, pos);
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	public void testDecodeUnicode()
	{
		try
		{
			String value = "Incorrect \\ string";
			String decoded = StringUtil.decodeUnicode(value);
			assertEquals(value, decoded);
			
			value = "Test \\u00E4\\u00E5";
			decoded = StringUtil.decodeUnicode(value);
			assertEquals("Test \u00E4\u00E5", decoded);
			
			value = "Wrong \\uxyz encoded";
			decoded = StringUtil.decodeUnicode(value);
			assertEquals(value, decoded);
			
			value = "Wrong \\u04";
			decoded = StringUtil.decodeUnicode(value);
			assertEquals("Wrong string not decoded", value, decoded);

			value = "test \\u";
			decoded = StringUtil.decodeUnicode(value);
			assertEquals("Wrong string not decoded", value, decoded);

			value = "test \\u wrong";
			decoded = StringUtil.decodeUnicode(value);
			assertEquals("Wrong string not decoded", value, decoded);
			
			decoded = StringUtil.decodeUnicode("\\r\\ntest");
			assertEquals("Single char not replaced correctly", "\r\ntest", decoded);

			decoded = StringUtil.decodeUnicode("Hello \\t World");
			assertEquals("Single char not replaced correctly", "Hello \t World", decoded);
			
			decoded = StringUtil.decodeUnicode("test\\t");
			assertEquals("Single char not replaced correctly", "test\t", decoded);
			
			decoded = StringUtil.decodeUnicode("test\\x");
			assertEquals("Single char not replaced correctly", "test\\x", decoded);

			decoded = StringUtil.decodeUnicode("test\\");
			assertEquals("Single char not replaced correctly", "test\\", decoded);

			decoded = StringUtil.decodeUnicode("test\\\\");
			assertEquals("test\\", decoded);
			
			value = "abc\\\\def";
			decoded = StringUtil.decodeUnicode(value);
			assertEquals("abc\\def", decoded);
				
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}

	}
	
	public void testEncodeUnicode()
	{
		String value = "\u00E4";
		String enc = StringUtil.escapeUnicode(value, null, CharacterRange.RANGE_7BIT, false);
		assertEquals("Umlaut not replaced", "\\u00E4", enc);
		
		value = "\n";
		enc = StringUtil.escapeUnicode(value, null, CharacterRange.RANGE_7BIT, true);
		assertEquals("NL not replaced" , "\\u000A", enc);
		
		enc = StringUtil.escapeUnicode(value, null, CharacterRange.RANGE_7BIT, false);
		assertEquals("NL not replaced" , "\\n", enc);
		
		value = "abcdefghijk";
		enc = StringUtil.escapeUnicode(value, null, CharacterRange.RANGE_7BIT, true);
		assertEquals("NL not replaced" , value, enc);

		value = "abc;def;ghi";
		enc = StringUtil.escapeUnicode(value, ";", CharacterRange.RANGE_7BIT, true);
		assertEquals("Additional characters not replaced", "abc\\u003Bdef\\u003Bghi", enc);
		//System.out.println("enc=" + enc);
		
	}
	
	public void testComparator()
	{
		Comparator<String> c = StringUtil.getCaseInsensitiveComparator();
		int i = c.compare("Test", "TEST");
		assertEquals(0, i);
		
		i = c.compare("TEST", "test");
		assertEquals(0, i);
		
		i = c.compare("test", "test");
		assertEquals(0, i);
		
		i = c.compare("test", "tesd");
		assertEquals(false, (i == 0));
	}
	
	public void testMakePlainLF()
	{
		String line = "line1\r\nline2";
		String newline = StringUtil.makePlainLinefeed(line);
		assertEquals("No LF", "line1\nline2", newline);
	}
	
	public void testRtrim()
	{
		String s = "bla";
		assertEquals(s, StringUtil.rtrim(s));
		
		s = " \tbla";
		assertEquals(s, StringUtil.rtrim(s));
		
		s = "bla \t\n";
		assertEquals("bla", StringUtil.rtrim(s));
		
		s = "bla \t\nbla";
		assertEquals(s, StringUtil.rtrim(s));
		
		s = " \n\r\t";
		assertEquals("", StringUtil.rtrim(s));
		
		s = "";
		assertEquals(s, StringUtil.rtrim(s));
	}
	
	public void testEqualString()
	{
		String one = "bla";
		String two = null;
		
		assertEquals(false, StringUtil.equalString(one, two));
		assertEquals(false, StringUtil.equalString(two, one));
		
		assertEquals(false, StringUtil.equalStringIgnoreCase(one, two));
		assertEquals(false, StringUtil.equalStringIgnoreCase(two, one));
		
		one = "bla";
		two = "bla";
		
		assertEquals(true, StringUtil.equalString(one, two));
		assertEquals(true, StringUtil.equalStringIgnoreCase(two, one));

		one = "bla";
		two = "BLA";
		
		assertEquals(false, StringUtil.equalString(one, two));
		assertEquals(true, StringUtil.equalStringIgnoreCase(two, one));
		
		one = "bla";
		two = "blub";
		
		assertEquals(false, StringUtil.equalString(one, two));
		assertEquals(false, StringUtil.equalStringIgnoreCase(two, one));
		
		
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
			List<String> elements = new LinkedList<String>();
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
	
	public void testMaxString()
	{
		String s = StringUtil.getMaxSubstring("Dent", 4, null);
		assertEquals("Truncated", "Dent", s);
		
		s = StringUtil.getMaxSubstring("Dent1", 4, null);
		assertEquals("Truncated", "Dent", s);
		
		s = StringUtil.getMaxSubstring("Den", 4, null);
		assertEquals("Truncated", "Den", s);
		
		s = StringUtil.getMaxSubstring("Beeblebrox", 5, null);
		assertEquals("Truncated", "Beebl", s);
		
		s = StringUtil.getMaxSubstring("Beeblebrox", 5, "...");
		assertEquals("Truncated", "Beebl...", s);
		
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
