/*
 * NumberUtilTest.java
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

import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class NumberUtilTest
{

	@Test
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
