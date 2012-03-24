/*
 * WbDateFormatterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.util;

import java.util.Calendar;
import java.util.Date;
import org.junit.*;
import static org.junit.Assert.*;
import workbench.db.exporter.InfinityLiterals;

/**
 *
 * @author Thomas Kellerer
 */
public class WbDateFormatterTest
{

	public WbDateFormatterTest()
	{
	}

	@Test
	public void testFormat()
	{
		Calendar cal = Calendar.getInstance();
		cal.set(2012, 0, 1);

		WbDateFormatter formatter = new WbDateFormatter("yyyy-MM-dd");
		String result = formatter.format(cal.getTime());
		assertEquals("2012-01-01", result);

		result = formatter.format(new Date(WbDateFormatter.DATE_POSITIVE_INFINITY));
		assertEquals(InfinityLiterals.PG_POSITIVE_LITERAL, result);
	}

	@Test
	public void testParse()
		throws Exception
	{
		String source = "2012-01-01";
		WbDateFormatter formatter = new WbDateFormatter("yyyy-MM-dd");
		Calendar cal = Calendar.getInstance();
		cal.set(2012, 0, 1, 0, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		Date expected = cal.getTime();
		Date result = formatter.parse(source);
		assertEquals(expected, result);
		expected = new Date(WbDateFormatter.DATE_POSITIVE_INFINITY);
		assertEquals(expected, formatter.parse(InfinityLiterals.PG_POSITIVE_LITERAL));
	}

	/**
	 * Test of getDisplayValue method, of class WbDateFormatter.
	 */
	@Test
	public void testGetDisplayValue()
	{
		Calendar cal = Calendar.getInstance();
		cal.set(2012, 0, 1);
		String result = WbDateFormatter.getDisplayValue("hello");
		assertEquals("hello", result);

		result = WbDateFormatter.getDisplayValue(new Date(WbDateFormatter.DATE_POSITIVE_INFINITY));
		assertEquals(InfinityLiterals.PG_POSITIVE_LITERAL, result);

		result = WbDateFormatter.getDisplayValue(Integer.valueOf(42));
		assertEquals("42", result);
	}
}
