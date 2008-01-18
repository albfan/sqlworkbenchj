/*
 * WbThread.java
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

import workbench.log.LogMgr;

/**
 *
 * @author support@sql-workbench.net
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

	public void uncaughtException(Thread thread, Throwable error)
	{
		LogMgr.logError("WbThread.uncaughtException()", "Thread + " + thread.getName() + " caused an exception", error);
	}
}
