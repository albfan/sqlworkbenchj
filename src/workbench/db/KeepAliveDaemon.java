/*
 * KeepAliveDaemon.java
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
package workbench.db;

import java.sql.SQLException;
import java.sql.Statement;

import workbench.log.LogMgr;

import workbench.util.ExceptionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbThread;

/**
 *
 * @author Thomas Kellerer
 */
public class KeepAliveDaemon
	implements Runnable
{
	private long idleTime;
	private WbThread idleThread;
	private boolean stopThread;
	final private WbConnection dbConnection;
	private String sqlScript;
	private long lastAction;

	public KeepAliveDaemon(long idle, WbConnection con, String sql)
	{
		this.idleTime = idle;
		this.dbConnection = con;
		this.sqlScript = SqlUtil.trimSemicolon(sql);
	}

	public void startThread()
	{
		this.shutdown();
		LogMgr.logInfo("KeepAliveDaemon.startThread()", "Initializing keep alive every " + getTimeDisplay(idleTime) + " with sql: " + this.sqlScript);
		this.idleThread = new WbThread(this, "KeepAlive/" + this.dbConnection.getId());
		this.idleThread.setPriority(Thread.MIN_PRIORITY);
		this.lastAction = 0;
		this.stopThread = false;
		this.idleThread.start();
	}

	public void shutdown()
	{
		if (this.idleThread != null)
		{
			try
			{
				this.stopThread = true;
				this.idleThread.interrupt();
			}
			catch (Exception e)
			{
				LogMgr.logWarning("KeepAliveDaemon.shutdown()", "Error when stopping thread", e);
			}
		}
	}

	public synchronized void setLastDbAction(long millis)
	{
		this.lastAction = millis;
	}

	@Override
	public void run()
	{
		while (!stopThread)
		{
			if (this.dbConnection == null)
			{
				stopThread = true;
				break;
			}

			long now = System.currentTimeMillis();

			try
			{
				long newSleep = idleTime - (now - lastAction);
				if (newSleep <= 0)
				{
					newSleep = idleTime;
				}
				LogMgr.logDebug("KeepAliveDaemon.run()", Thread.currentThread().getName() + ": sleeping for " + newSleep + "ms");
				Thread.sleep(idleTime);
			}
			catch (InterruptedException e)
			{
				if (!this.stopThread)
				{
					LogMgr.logError("KeepAliveDaemon.run()", Thread.currentThread().getName() + ": Thread was interrupted!", e);
				}
			}

			now = System.currentTimeMillis();

			synchronized (this)
			{
				if (((now - lastAction) > idleTime))
				{
					runSqlScript();
					this.lastAction = now;
				}
			}
		}
  }

	public static long parseTimeInterval(String interval)
	{
		if (StringUtil.isBlank(interval)) return 0;
		long result = 0;

		if (interval.endsWith("s"))
		{
			interval = interval.substring(0, interval.length() - 1);
			result = StringUtil.getLongValue(interval, 0) * 1000;
		}
		else if (interval.endsWith("m"))
		{
			interval = interval.substring(0, interval.length() - 1);
			result = StringUtil.getLongValue(interval, 0) * 1000 * 60;
		}
		else if (interval.endsWith("h"))
		{
			interval = interval.substring(0, interval.length() - 1);
			result = StringUtil.getLongValue(interval, 0) * 1000 * 60 * 60;
		}
		else
		{
			result = StringUtil.getLongValue(interval, 0);
		}
		return result;
	}

	public static String getTimeDisplay(long millis)
	{
		if (millis == 0) return "";

		if (millis < 60 * 1000)
		{
			return Long.toString((millis / 1000)) + "s";
		}
		return Long.toString((millis / (60 * 1000))) + "m";
	}


	private void runSqlScript()
	{
		if (this.dbConnection == null) return;
		if (this.dbConnection.isBusy()) return;

		Statement stmt = null;
		synchronized (this.dbConnection)
		{
			try
			{
				if (this.dbConnection == null) return;
				stmt = this.dbConnection.createStatement();
				LogMgr.logInfo("KeepAliveDaemon.runSqlScript()", Thread.currentThread().getName() + " - executing SQL: " + this.sqlScript);
				stmt.execute(sqlScript);
			}
			catch (SQLException sql)
			{
				LogMgr.logError("KeepAliveDaemon.runSqlScript()", Thread.currentThread().getName() + ": SQL Error when running keep alive script: " + ExceptionUtil.getDisplay(sql), null);
			}
			catch (Throwable e)
			{
				LogMgr.logError("KeepAliveDaemon.runSqlScript()", "Error when running keep alive script", e);
			}
			finally
			{
				SqlUtil.closeStatement(stmt);
			}
		}
	}
}
