/*
 * KeepAliveDaemon.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
import workbench.util.WbThread;

/**
 *
 * @author support@sql-workbench.net
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
		this.sqlScript = sql;
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
				LogMgr.logWarning("KeepAliveThread.shutdown()", "Error when stopping thread", e);
			}
		}
	}

	public synchronized void setLastDbAction(long millis)
	{
		this.lastAction = millis;
	}

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
					LogMgr.logError("KeepAliveThread.run()", Thread.currentThread().getName() + ": Thread was interrupted!", e);
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
				stmt = this.dbConnection.createStatement();
				LogMgr.logInfo("KeepAliveThread.runSqlScript()", Thread.currentThread().getName() + " - executing SQL: " + this.sqlScript);
				stmt.execute(sqlScript);
			}
			catch (SQLException sql)
			{
				LogMgr.logError("KeepAliveThread.runSqlScript()", Thread.currentThread().getName() + ": SQL Error when running keep alive script: " + ExceptionUtil.getDisplay(sql), null);
			}
			catch (Throwable e)
			{
				LogMgr.logError("KeepAliveThread.runSqlScript()", "Error when running keep alive script", e);
			}
			finally
			{
				SqlUtil.closeStatement(stmt);
			}
		}
	}
}
