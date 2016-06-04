/*
 * ValueConverterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Types;
import java.time.LocalDate;
import java.util.Calendar;

import workbench.WbTestCase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class ValueConverterTest
	extends WbTestCase
{

	public ValueConverterTest()
	{
		super("ValueConverterTest");
	}

	@Test
	public void testDefaultBools()
		throws Exception
	{
		ValueConverter converter = new ValueConverter();
		Object flag = converter.convertValue("", Types.BOOLEAN);
		assertNull(flag);

		flag = converter.convertValue("False", Types.BOOLEAN);
		assertEquals(Boolean.FALSE, flag);
		flag = converter.convertValue("FALSE", Types.BOOLEAN);
		assertEquals(Boolean.FALSE, flag);
		flag = converter.convertValue("false", Types.BOOLEAN);
		assertEquals(Boolean.FALSE, flag);

		flag = converter.convertValue("TruE", Types.BOOLEAN);
		assertEquals(Boolean.TRUE, flag);
		flag = converter.convertValue("true", Types.BOOLEAN);
		assertEquals(Boolean.TRUE, flag);
		flag = converter.convertValue("1", Types.BOOLEAN);
		assertEquals(Boolean.TRUE, flag);
	}

	@Test
	public void testConvertBoolLiterals()
		throws Exception
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

	@Test
	public void testIntegerConvert()
		throws Exception
	{
		ValueConverter converter = new ValueConverter();
		converter.setAutoConvertBooleanNumbers(false);
		converter.setDecimalCharacter('.');
		try
		{
			Object i = converter.convertValue("42", Types.INTEGER);
			assertEquals("Wrong value returned", Integer.valueOf(42), i);

			i = converter.convertValue("32168", Types.BIGINT);
			assertEquals("Wrong value returned", Long.valueOf(32168), i);

			i = converter.convertValue("4.2E+1", Types.INTEGER);
			assertEquals("Wrong value returned", Integer.valueOf(42), i);

			converter.setDecimalCharacter(',');
			i = converter.convertValue("4,2E+1", Types.INTEGER);
			assertEquals("Wrong value returned", Integer.valueOf(42), i);

			converter.setDecimalCharacter('.');
			i = converter.convertValue("3.2168E+4", Types.BIGINT);
			assertEquals("Wrong value returned", Long.valueOf(32168), i);

			boolean exceptionThrown = false;
			try
			{
				i = converter.convertValue("3.2168E+2", Types.BIGINT);
			}
			catch (ConverterException e)
			{
				exceptionThrown = true;
			}
			assertTrue(exceptionThrown);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testNumericListerals()
		throws Exception
	{
		ValueConverter converter = new ValueConverter();
		converter.setNumericBooleanValues(-24, 42);
		converter.setBooleanLiterals(CollectionUtil.arrayList("true", "yes", "J"), CollectionUtil.arrayList("false", "no", "N"));
		assertEquals("Wrong value returned", Integer.valueOf(-24), converter.convertValue("no", Types.INTEGER));
		assertEquals("Wrong value returned", Integer.valueOf(-24), converter.convertValue("N", Types.INTEGER));
		assertEquals("Wrong value returned", Integer.valueOf(-24), converter.convertValue("false", Types.INTEGER));
		assertEquals("Wrong value returned", Integer.valueOf(42), converter.convertValue("yes", Types.INTEGER));
		assertEquals("Wrong value returned", Integer.valueOf(42), converter.convertValue("true", Types.INTEGER));
		assertEquals("Wrong value returned", Integer.valueOf(42), converter.convertValue("J", Types.INTEGER));
		boolean exception = false;
		try
		{
			converter.convertValue("foobar", Types.INTEGER);
		}
		catch (ConverterException e)
		{
			exception = true;
		}
		assertTrue("No converter exception thrown", exception);

		exception = false;
		try
		{
			converter.convertValue("Nein", Types.INTEGER);
		}
		catch (ConverterException e)
		{
			exception = true;
		}
		assertTrue("No converter exception thrown", exception);

	}

	@Test
	public void testBooleanConvert()
		throws Exception
	{
		ValueConverter converter = new ValueConverter();
		converter.setAutoConvertBooleanNumbers(true);
		try
		{
			Object i = converter.convertValue("true", Types.INTEGER);
			assertEquals("Wrong value returned", Integer.valueOf(1), i);
			i = converter.convertValue("false", Types.INTEGER);
			assertEquals("Wrong value returned", Integer.valueOf(0), i);
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

	@Test
	public void testConvertNumbers()
		throws Exception
	{
		ValueConverter converter = new ValueConverter();
		converter.setDecimalCharacter('.');
		Object data = converter.convertValue("42", Types.INTEGER);
		assertEquals("Number not converted", Integer.valueOf(42), data);

		data = converter.convertValue("42", Types.BIGINT);
		assertEquals("Number not converted", Long.valueOf(42), data);

		data = converter.convertValue("3.14152", Types.DECIMAL);
		BigDecimal d = (BigDecimal)data;
		assertEquals("Wrong value", 3.14152, d.doubleValue(), 0.001);
	}

	@Test
	public void testConvertStrings()
		throws Exception
	{
		ValueConverter converter = new ValueConverter();
		Object data = converter.convertValue("Test", Types.VARCHAR);
		assertEquals("Test", data);

		data = converter.convertValue(new StringBuilder("Test"), Types.VARCHAR);
		assertEquals("Test", data);
	}

	@Test
	public void testInfinity()
		throws Exception
	{
		ValueConverter converter = new ValueConverter("yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss");
		Object o = converter.convertValue("infinity", Types.DATE);
		assertTrue(o instanceof Date);
		java.sql.Date d = new java.sql.Date(WbDateFormatter.DATE_POSITIVE_INFINITY);
		assertEquals(d, o);

		o = converter.convertValue("-infinity", Types.DATE);
		assertTrue(o instanceof Date);
		d = new java.sql.Date(WbDateFormatter.DATE_NEGATIVE_INFINITY);
		assertEquals(d, o);
	}

	@Test
	public void testConvertDateLiterals()
		throws Exception
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
		java.sql.Date now = java.sql.Date.valueOf(LocalDate.now());
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


		try
		{
			converter.setIllegalDateIsNull(true);
			data = converter.convertValue("2011-02-31", Types.DATE);
			assertNull(data);
		}
		catch (ConverterException ex)
		{
			fail("No exception should have been thrown");
		}
	}

}
