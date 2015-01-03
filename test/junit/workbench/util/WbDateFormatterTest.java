/*
 * WbDateFormatterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
