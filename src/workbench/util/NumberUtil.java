/*
 * NumberUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 *
 * @author Thomas Kellerer
 */
public class NumberUtil
{

	/**
	 * Compare two Numbers regardless of their actual class.
	 * This works around the problem that e.g. BigInteger cannot be compared to a BigDecimal.
	 * The equals method would return false.
	 * <br/>
	 * When comparing data across different DBMS (and thus JDBC drivers), values
	 * that are "equal" might otherwise not be considered equal e.g. an ID=42 in Oracle
	 * stored as NUMBER(38) would not be equal to an ID=42 stored in an integer column
	 * in Postgres as both drivers use a different representation class.
	 * <br/>
	 * The comparison is done by converting both values to BigDecimal and then using equals()
	 * on those instances.
	 *
	 * @param one   the first value
	 * @param other the second value
	 *
	 * @return true, if both numbers are equals
	 */
	public static boolean valuesAreEqual(Number one, Number other)
	{
		if (one.getClass() == other.getClass())
		{
			return one.equals(other);
		}

		Number first = makeBigDecimal(one);
		Number second = makeBigDecimal(other);
		return first.equals(second);
	}

	protected static Number makeBigDecimal(Number value)
	{
		if (value instanceof BigDecimal) return value;

		if (value instanceof BigInteger)
		{
			return new BigDecimal((BigInteger)value);
		}
		if (value instanceof Integer)
		{
			return new BigDecimal(((Integer)value).intValue());
		}
		if (value instanceof Long)
		{
			return new BigDecimal(((Long)value).longValue());
		}
		if (value instanceof Double)
		{
			return new BigDecimal(((Double)value).doubleValue());
		}
		if (value instanceof Byte)
		{
			return new BigDecimal(((Byte)value).intValue());
		}
		if (value instanceof Short)
		{
			return new BigDecimal(((Short)value).intValue());
		}
		if (value instanceof Float)
		{
			return new BigDecimal(((Float)value).doubleValue());
		}
		return value;
	}

}
