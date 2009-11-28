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

/**
 *
 * @author Thomas Kellerer
 */
public class NumberUtil
{

	/**
	 * Compare two Numbers regardless of their actual class because e.g. BigInteger
	 * cannot be compared to a BigDecimal. The equals method would return false.
	 * <br/>
	 * When comparing data across different DBMS (and thus JDBC) drivers, values
	 * that are "equal" might otherwise not be considered equal e.g. an ID=42 in Oracle
	 * stored as NUMBER(38) would not be equal to an ID=42 stored in an integer column
	 * in Postgres as both drivers use a different representation class.
	 *
	 * @param one
	 * @param other
	 * 
	 * @return true, if both numbers are equals
	 */
	public static boolean valuesAreEquals(Number one, Number other)
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
