/*
 * NumberStringCacheTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author thomas
 */
public class NumberStringCacheTest
{
	@Test
	public void testGetNumberString()
	{
		String s = NumberStringCache.getNumberString(10);
		assertEquals(s, "10");

		char c = 32;
		s = NumberStringCache.getNumberString(c);
		assertEquals(s, "32");

		s = NumberStringCache.getNumberString(NumberStringCache.CACHE_SIZE);
		assertEquals(Integer.toString(NumberStringCache.CACHE_SIZE), s);
	}

	@Test
	public void testHexString()
	{
		assertEquals("00", NumberStringCache.getHexString(0));
		assertEquals("0f", NumberStringCache.getHexString(15));
		assertEquals("12", NumberStringCache.getHexString(18));
		assertEquals("ff", NumberStringCache.getHexString(255));
		assertEquals("100", NumberStringCache.getHexString(256));
		assertEquals("200", NumberStringCache.getHexString(512));
	}
}
