/*
 * NumberUtilTest.java
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
