/*
 * NumberStringCache.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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

/**
 *
 * @author Thomas Kellerer
 */
public class NumberStringCache
{
	private final int CACHE_SIZE = 5000;
	private final String[] cache = new String[CACHE_SIZE];
	private final String[] hexCache = new String[256];

	public static NumberStringCache getInstance()
	{
		return InstanceHolder.LAZY_INSTANCE;
	}

	private static class InstanceHolder
	{
		static final NumberStringCache LAZY_INSTANCE = new NumberStringCache();
	}

	private NumberStringCache()
	{
	}

	public static String getHexString(int value)
	{
		return getInstance()._getHexString(value);
	}

	private String _getHexString(int value)
	{
		if (value > 255 || value < 0) return Integer.toHexString(value);
		if (hexCache[value] == null)
		{
			if (value < 16)
			{
				hexCache[value] = "0" + Integer.toHexString(value);
			}
			else
			{
				hexCache[value] = Integer.toHexString(value);
			}

		}
		return hexCache[value];
	}

	public static String getNumberString(long lvalue)
	{
		return getInstance()._getNumberString(lvalue);
	}

	private String _getNumberString(long lvalue)
	{
		if (lvalue < 0 || lvalue >= CACHE_SIZE) return Long.toString(lvalue);

		int value = (int)lvalue;

		// I'm not synchronizing this, because the worst that can
		// happen is, that the same number is created two or three times
		// instead of exactly one time.
		// And because this is most of the time called from Swing Event Thread
		// it is more or less a single-threaded access anyway.
		if (cache[value] == null)
		{
			cache[value] = Integer.toString(value);
		}
		return cache[value];
	}

}
