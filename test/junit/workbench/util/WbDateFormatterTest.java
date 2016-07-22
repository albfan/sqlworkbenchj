/*
 * WbDateFormatterTest.java
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

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

import workbench.db.exporter.InfinityLiterals;

import org.junit.Test;

import static org.junit.Assert.*;

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
		String result = formatter.formatUtilDate(cal.getTime());
		assertEquals("2012-01-01", result);

		result = formatter.formatUtilDate(new Date(WbDateFormatter.DATE_POSITIVE_INFINITY));
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
		Date result = formatter.parseDate(source);
		assertEquals(expected, result);
		expected = new Date(WbDateFormatter.DATE_POSITIVE_INFINITY);
		assertEquals(expected, formatter.parseDate(InfinityLiterals.PG_POSITIVE_LITERAL));

    result = formatter.parseTimestamp(source);
    assertEquals(cal.getTime(), result);
	}

  @Test
  public void testTimestamp()
  {
    WbDateFormatter format = new WbDateFormatter("dd.MM.yyyy HH:mm:ss");
    Timestamp ts = Timestamp.valueOf("2015-03-27 20:21:22.123456");
    assertEquals("27.03.2015 20:21:22", format.formatTimestamp(ts));

    format.applyPattern("dd.MM.yyyy HH:mm:ss.SSS");
    assertEquals("27.03.2015 20:21:22.123", format.formatTimestamp(ts));

    format.applyPattern("dd.MM.yyyy HH:mm:ss.SSSSSS");
    assertEquals("27.03.2015 20:21:22.123456", format.formatTimestamp(ts));

    format.applyPattern("SSSSSS dd.MM.yyyy HH:mm:ss");
    assertEquals("123456 27.03.2015 20:21:22", format.formatTimestamp(ts));

    format.applyPattern("SSS dd.MM.yyyy HH:mm:ss");
    assertEquals("123 27.03.2015 20:21:22", format.formatTimestamp(ts));

    format.applyPattern("dd.MM.yyyy HH:mm:ss.SSS");
    ts = Timestamp.valueOf("2015-03-27 20:21:22.123789");
    assertEquals("27.03.2015 20:21:22.123", format.formatTimestamp(ts));

    format.applyPattern("dd.MM.yyyy HH:mm:ss.SSSSSSSSS");
    ts = Timestamp.valueOf("2015-03-27 20:21:22.123456789");
    assertEquals("27.03.2015 20:21:22.123456789", format.formatTimestamp(ts));

    format.applyPattern("dd.MM.yyyy HH:mm:ss.SSSSSS");

    ts = Timestamp.valueOf("2015-03-27 20:21:22.123");
    assertEquals("27.03.2015 20:21:22.123000", format.formatTimestamp(ts));

    ts = Timestamp.valueOf("2015-03-27 20:21:22.0001");
    assertEquals("27.03.2015 20:21:22.000100", format.formatTimestamp(ts));

    format.applyPattern("dd.MM.yyyy HH:mm:ss.SSSSSS");
    ts = Timestamp.valueOf("2015-03-27 20:21:22");
    assertEquals("27.03.2015 20:21:22.000000", format.formatTimestamp(ts));

    format.applyPattern("'TIMESTAMP '''yyyy-MM-dd HH:mm:ss.SSSSSS''");
    ts = Timestamp.valueOf("2015-03-27 20:21:22.123456");
    assertEquals("TIMESTAMP '2015-03-27 20:21:22.123456'", format.formatTimestamp(ts));

    format.applyPattern("dd.MM.yyyy HH:mm:ss.SSSSSSSSS");
    ts = Timestamp.valueOf("2015-03-27 20:21:22.789");
    assertEquals("27.03.2015 20:21:22.789000000", format.formatTimestamp(ts));
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
