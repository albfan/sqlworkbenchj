/*
 * StringIntegerCacheTest.java
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

import junit.framework.TestCase;

/**
 *
 * @author thomas
 */
public class StringIntegerCacheTest
	extends TestCase
{
	public StringIntegerCacheTest(String testName)
	{
		super(testName);
	}

	public void testGetNumberString()
	{
		String s = StringIntegerCache.getNumberString(10);
		assertEquals(s, "10");
		
		char c = 32;
		s = StringIntegerCache.getNumberString(c);
		assertEquals(s, "32");
	}
	
	public void testHexString()
	{
		assertEquals("00", StringIntegerCache.getHexString(0));
		assertEquals("0f", StringIntegerCache.getHexString(15));
		assertEquals("12", StringIntegerCache.getHexString(18));
		assertEquals("ff", StringIntegerCache.getHexString(255));
		assertEquals("100", StringIntegerCache.getHexString(256));
		assertEquals("200", StringIntegerCache.getHexString(512));
	}
}