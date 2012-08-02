/*
 * DeadlockMonitor.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.util;

import workbench.log.LogMgr;
import workbench.resource.Settings;

/**
 *
 * @author Thomas Kellerer
 */
public class DeadlockMonitor
	implements Runnable
{
	private ThreadDumper monitor;
	private int sleepTime;

	public DeadlockMonitor()
	{
		monitor = new ThreadDumper();
		sleepTime = Settings.getInstance().getIntProperty("workbench.gui.debug.deadlockmonitor.sleeptime", 10000);
	}

	@Override
	public void run()
	{
		try
		{
			while (true)
			{
				long start = System.currentTimeMillis();
				String dump = monitor.getDeadlockDump();
				long duration = System.currentTimeMillis() - start;
				LogMgr.logDebug("DeadlockMonitor.run()", "Checking for deadlocks took: " + duration + "ms");
				if (dump != null)
				{
					LogMgr.logError("DeadlockMonitor.run()", "Deadlock detected:\n" + dump, null);
				}
				Thread.sleep(sleepTime);
			}
		}
		catch (InterruptedException e)
		{
		}
	}

}
