/*
 * WbThread.java
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
		try
		{
			Thread.sleep(time);
		}
		catch (Throwable th)
		{
		}
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
