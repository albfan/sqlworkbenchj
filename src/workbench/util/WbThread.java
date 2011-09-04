/*
 * WbThread.java
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

import workbench.log.LogMgr;

/**
 *
 * @author Thomas Kellerer
 */
public class WbThread
	extends Thread
	implements Thread.UncaughtExceptionHandler
{

	public WbThread(String name)
	{
		super(name);
		this.setDaemon(true);
		this.setUncaughtExceptionHandler(this);
	}

	public WbThread(Runnable run, String name)
	{
		super(run, name);
		this.setDaemon(true);
		this.setUncaughtExceptionHandler(this);
	}

	@Override
	public void uncaughtException(Thread thread, Throwable error)
	{
		LogMgr.logError("WbThread.uncaughtException()", "Thread + " + thread.getName() + " caused an exception", error);
	}

	/**
	 * Implementation of sleep() without throwing an exception.
	 *
	 * @param time
	 * @see Thread#sleep(long) 
	 */
	public static void sleepSilently(long time)
	{
		try { Thread.sleep(time); } catch (Throwable th) {}
	}

	public static void runWithTimeout(Thread toRun, long timeout)
	{
		toRun.start();
		try
		{
			toRun.join(timeout);
			toRun.interrupt();
		}
		catch (InterruptedException ie)
		{
			LogMgr.logWarning("WbThread.runWithTimeout()", "Waiting was interrupted", ie);
		}
	}
}
