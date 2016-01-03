/*
 * WbNumberFormatterTest.java
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

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class WbNumberFormatterTest
{
	public WbNumberFormatterTest()
	{
	}

	@Test
	public void testFormatter()
	{
		WbNumberFormatter f = new WbNumberFormatter(3, '.');
		assertEquals("1.124", f.format(new BigDecimal("1.1237")));
		assertEquals("1.123", f.format(new BigDecimal("1.1231")));
		assertEquals("0", f.format(new BigDecimal("0")));
		assertEquals("42", f.format(new Integer(42)));
		assertEquals("1.5", f.format(new Double(1.5)));
		assertEquals("1.5", f.format(new BigDecimal("1.5")));
		assertEquals("42", f.format(new BigDecimal("42")));
		assertEquals("1", f.format(new Double(1)));
		assertEquals("1.123", f.format(new Double(1.123)));
		assertEquals("1.123", f.format(new Double(1.1234)));
		assertEquals("1.124", f.format(new Double(1.1236)));
		assertEquals("", f.format(null));

		f = new WbNumberFormatter('.');
		assertEquals("1.1", f.format(new BigDecimal("1.1")));
		assertEquals("1.1234", f.format(new BigDecimal("1.1234")));
		assertEquals("1.1234567890", f.format(new BigDecimal("1.1234567890")));
		assertEquals("1", f.format(new Double(1)));
		assertEquals("1.123456", f.format(new Double(1.123456)));

		f = new WbNumberFormatter(',');
		assertEquals("3,14", f.format(new BigDecimal("3.14")));
		assertEquals("42", f.format(new BigDecimal("42")));

		f = new WbNumberFormatter(2, ',');
		assertEquals("3,14", f.format(new BigDecimal("3.14152")));
		assertEquals("3,14", f.format(new Double(3.14)));

		f = new WbNumberFormatter(4, ',');
		assertEquals("3,1415", f.format(new BigDecimal("3.14152")));
		assertEquals("3,1415", f.format(new Double(3.14152)));
		assertEquals("0,1", f.format(new Double(0.1)));
	}

	@Test
	public void testFixedDigits()
	{
		WbNumberFormatter f = new WbNumberFormatter(6, '.', true);
		assertEquals("1.123700", f.format(new BigDecimal("1.1237")));
		assertEquals("0.000000", f.format(new BigDecimal("0")));
		assertEquals("1.500000", f.format(new Double(1.5)));
		assertEquals("0.000000", f.format(new Double(0)));
		assertEquals("42", f.format(new Integer(42)));

		f = new WbNumberFormatter(4, ',', true);
		assertEquals("1,5000", f.format(new Double(1.5)));
	}
}
