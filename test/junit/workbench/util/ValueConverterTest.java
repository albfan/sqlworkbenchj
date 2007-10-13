/*
 * ValueConverterTest.java
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

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import junit.framework.TestCase;

/**
 *
 * @author support@sql-workbench.net
 */
public class ValueConverterTest 
	extends TestCase
{
	
	public ValueConverterTest(String testName)
	{
		super(testName);
	}
	
	protected void setUp() throws Exception
	{
		super.setUp();
	}
	
	protected void tearDown() throws Exception
	{
		super.tearDown();
	}
	
	public void testConvertBoolLiterals()
	{
		ValueConverter converter = new ValueConverter();
		try
		{
			converter.setBooleanLiterals(StringUtil.stringToList("true,1,-1", ","), StringUtil.stringToList("false,0", ","));
			Object o = converter.convertValue("true", Types.BOOLEAN);
			assertEquals(Boolean.TRUE, o);
			
			o = converter.convertValue("1", Types.BOOLEAN);
			assertEquals(Boolean.TRUE, o);
			
			o = converter.convertValue("-1", Types.BOOLEAN);
			assertEquals(Boolean.TRUE, o);
			
			o = converter.convertValue("false", Types.BOOLEAN);
			assertEquals(Boolean.FALSE, o);
			
			o = converter.convertValue("0", Types.BOOLEAN);
			assertEquals(Boolean.FALSE, o);
			
			boolean hadError = false;
			try
			{
				o = converter.convertValue("FALSE", Types.BOOLEAN);
			}
			catch (ConverterException e)
			{
				hadError = true;
			}
			assertEquals("No error thrown", true, hadError);

			hadError = false;
			try
			{
				o = converter.convertValue("YES", Types.BOOLEAN);
			}
			catch (ConverterException e)
			{
				hadError = true;
			}
			assertEquals("No error thrown", true, hadError);
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void testBooleanConvert()
		throws Exception
	{
		ValueConverter converter = new ValueConverter();
		converter.setAutoConvertBooleanNumbers(true);
		try
		{
			Object i = converter.convertValue("true", Types.INTEGER);
			assertEquals("Wrong value returned", new Integer(1), i);
			i = converter.convertValue("false", Types.INTEGER);
			assertEquals("Wrong value returned", new Integer(0), i);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		boolean exception = false;
		try
		{
			Object i = converter.convertValue("gaga", Types.INTEGER);
		}
		catch (Exception e)
		{
			exception = true;
		}
		assertEquals("Not exception thrown", true, exception);

		converter.setAutoConvertBooleanNumbers(false);
		exception = false;
		try
		{
			Object i = converter.convertValue("true", Types.INTEGER);
		}
		catch (Exception e)
		{
			exception = true;
		}
		assertEquals("Not exception thrown", true, exception);
		
	}

	public void testConvertNumbers()
		throws Exception
	{
		ValueConverter converter = new ValueConverter();
		converter.setDecimalCharacter('.');
		Object data = converter.convertValue("42", Types.INTEGER);
		assertEquals("Number not converted", new Integer(42), data);
		
		data = converter.convertValue("42", Types.BIGINT);
		assertEquals("Number not converted", new Long(42), data);
		
		data = converter.convertValue("3.14152", Types.DECIMAL);
		BigDecimal d = (BigDecimal)data;
		assertEquals("Wrong value", 3.14152, d.doubleValue(), 0.001);
	}
	
	public void testConvertStrings() throws Exception
	{
		ValueConverter converter = new ValueConverter();
		Object data = converter.convertValue("Test", Types.VARCHAR);
		assertEquals("Test", data);
		
		data = converter.convertValue(new StringBuilder("Test"), Types.VARCHAR);
		assertEquals("Test", data);
	}
	
	public void testConvertDateLiterals() throws Exception
	{
		String aDate = "2007-04-01";
		ValueConverter converter = new ValueConverter("yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss");
		Calendar c = Calendar.getInstance();
		c.clear();
		c.set(2007, 3, 1);
		Date expResult = new java.sql.Date(c.getTime().getTime());
		Date result = converter.parseDate(aDate);
		
		assertEquals(expResult, result);
		
		Object data = converter.convertValue(aDate, Types.DATE);
		assertEquals(expResult, data);
		
		c = Calendar.getInstance();
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.HOUR, 0);
		c.clear(Calendar.SECOND);
		c.clear(Calendar.MINUTE);
		c.clear(Calendar.MILLISECOND);
		java.sql.Date now = new java.sql.Date(c.getTime().getTime());
		data = converter.convertValue("today", Types.DATE);
		assertEquals(0, now.compareTo((java.sql.Date)data));
		
		data = converter.convertValue("now", Types.TIME);
		assertTrue(data instanceof java.sql.Time);
		
		long justnow = System.currentTimeMillis();
		data = converter.convertValue("current_timestamp", Types.TIMESTAMP);
		assertTrue(data instanceof java.sql.Timestamp);
		java.sql.Timestamp sql = (java.sql.Timestamp)data;
		
		// I cannot compare for equality, but the whole call should not take longer
		// than 250ms so the difference should not be greater than that.
		assertEquals("Not really 'now': ", true, (sql.getTime() - justnow) < 250);
	}
	
}
