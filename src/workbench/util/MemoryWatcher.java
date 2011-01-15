/*
 * MemoryWatcher.java
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

import workbench.resource.Settings;

/**
 *
 * @author Thomas Kellerer
 */
public class MemoryWatcher
{
	private static final long MIN_FREE_MEMORY =
		Settings.getInstance().getIntProperty("workbench.memorywatcher.minmemory", 5) * 1024 * 1024;

	// the maxMemory() will not change during the lifetime of the JVM
	// so I can spare some CPU cycles by not calling maxMemory() constantly
	public static final long MAX_MEMORY = Runtime.getRuntime().maxMemory();

	public synchronized static boolean isMemoryLow()
	{
		long free = getFreeMemory();
		if (free < MIN_FREE_MEMORY)
		{
			// As we have not yet hit an OutOfMemoryException, running gc()
			// can actually free some memory. This will slow down e.g.
			// data retrieval when the memory is filled up, but at least
			// we can prevent the OOME to a certain extent
			System.gc();
			free = Runtime.getRuntime().freeMemory();
		}
		return (free < MIN_FREE_MEMORY);
	}

	public static final long getFreeMemory()
	{
		long free = Runtime.getRuntime().freeMemory();
		long total = Runtime.getRuntime().totalMemory();

		// freeMemory reports the amount of memory that is free
		// in the totalMemory. But the total memory can actually
		// expand to maxMemory. So we need to add the difference
		// between max and total to the currently free memory
		free = free + (MAX_MEMORY - total);
		return free;
	}
}
