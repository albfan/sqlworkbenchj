/*
 * NumberUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import junit.framework.TestCase;

/**
 *
 * @author Thomas Kellerer
 */
public class NumberUtilTest
	extends TestCase
{

	public NumberUtilTest(String testName)
	{
		super(testName);
	}

	public void testValuesAreEquals()
	{
		assertTrue(NumberUtil.valuesAreEquals(new Integer(42), new Long(42)));
		assertTrue(NumberUtil.valuesAreEquals(new Short((short)5), new BigDecimal(5)));
		assertTrue(NumberUtil.valuesAreEquals(new Long(5), new Double(5)));
		assertTrue(NumberUtil.valuesAreEquals(BigInteger.valueOf(42), new BigDecimal(42)));
		assertTrue(NumberUtil.valuesAreEquals(new Integer(42), BigInteger.valueOf(42)));
		assertFalse(NumberUtil.valuesAreEquals(BigInteger.valueOf(43), new BigDecimal(42)));
		assertFalse(NumberUtil.valuesAreEquals(new Integer(42), BigInteger.valueOf(2)));
	}

}
