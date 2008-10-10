/*
 * MemoryWatcher.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

/**
 *
 * @author support@sql-workbench.net
 */
public class MemoryWatcher
{
	// 2MB minimum
	private static final long MIN_MEMORY = 2 * 1024 * 1024;

	public synchronized static boolean isMemoryLow()
	{
		long free = Runtime.getRuntime().freeMemory();
		if (free < MIN_MEMORY)
		{
			System.gc();
			free = Runtime.getRuntime().freeMemory();
		}
		return (free < MIN_MEMORY);
	}
}
