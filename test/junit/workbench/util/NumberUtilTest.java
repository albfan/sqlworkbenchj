/*
 * NumberUtilTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
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
		assertTrue(NumberUtil.valuesAreEqual(new Integer(42), new Long(42)));
		assertTrue(NumberUtil.valuesAreEqual(new Short((short)5), new BigDecimal(5)));
		assertTrue(NumberUtil.valuesAreEqual(new Long(5), new Double(5)));
		assertTrue(NumberUtil.valuesAreEqual(BigInteger.valueOf(42), new BigDecimal(42)));
		assertTrue(NumberUtil.valuesAreEqual(new Integer(42), BigInteger.valueOf(42)));
		assertFalse(NumberUtil.valuesAreEqual(BigInteger.valueOf(43), new BigDecimal(42)));
		assertFalse(NumberUtil.valuesAreEqual(new Integer(42), BigInteger.valueOf(2)));
	}

}
