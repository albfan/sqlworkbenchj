/*
 * StringUtilTest.java
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
import java.io.BufferedReader;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import workbench.log.LogMgr;

/**
 *
 * @author support@sql-workbench.net
 */
public class StringUtilTest extends TestCase
{
	
	public StringUtilTest(String testName)
	{
		super(testName);
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
