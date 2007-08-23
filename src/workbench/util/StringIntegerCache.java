/*
 * StringIntegerCache.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author support@sql-workbench.net
 */
public class StringIntegerCache
{
	private static Map<Integer, String> overflowMap = new HashMap<Integer, String>(500, 0.5f);
	
	// As this is class is used to cache the String representation for 
	// Line numbers in the editor, caching 1000 numbers should suffice
	// for most cases. Any editor text larger than that will fall back
	// to the objects cached in overflowMap
	private static final int CACHE_SIZE = 1500;
	private static String[] cache = new String[CACHE_SIZE];

	private static final int MAX_NUMBER_TO_CACHE = 10000;
	
	private StringIntegerCache()
	{
	}

	public static synchronized String getNumberString(int value)
	{
		if (value > MAX_NUMBER_TO_CACHE) return Integer.toString(value);
		
		String result = null;
		if (value >= 0 && value < CACHE_SIZE)
		{
			if (cache[value] == null)
			{
				cache[value] = Integer.toString(value);
			}
			result = cache[value];
		}
		else
		{
			Integer key = new Integer(value);
			result = overflowMap.get(key);
			if (result == null)
			{
				result = Integer.toString(value);
				overflowMap.put(key, result);
			}
		}
		return result;
	}
}