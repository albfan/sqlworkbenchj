/*
 * WbNumberFormatterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.util;

import java.math.BigDecimal;
import org.junit.Before;
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
	}

}
