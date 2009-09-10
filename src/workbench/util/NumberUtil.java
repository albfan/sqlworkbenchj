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
import java.util.List;

/**
 *
 * @author Thomas Kellerer
 */
public class NumberUtil
{
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
	
	public static Number makeBigDecimal(Number value)
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

	private static List<? extends Class> numberClasses = CollectionUtil.arrayList(
			BigDecimal.class,
			Double.class,
			Float.class,
			BigInteger.class,
			Long.class,
			Integer.class,
			Short.class
		);

	/**
	 * Returns the "super" type of the given two numbers, i.e. the class
	 * with the higher value range (e.g. If the input is BigInteger and Integer,
	 * BigInteger will be returned)
	 * @param one
	 * @param other
	 * @return
	 */
	protected static Class getUpperType(Number one, Number other)
	{
		int indexOne = numberClasses.indexOf(one.getClass());
		int indexTwo = numberClasses.indexOf(other.getClass());
		if (indexOne < indexTwo) return numberClasses.get(indexOne);
		return numberClasses.get(indexTwo);
	}

}
