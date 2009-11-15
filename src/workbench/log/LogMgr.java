/*
 * LogMgr.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.log;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;

import workbench.gui.components.LogFileViewer;
import workbench.util.ExceptionUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;


/**
 * The logging class used by SQL Workbench/J
 *
 * @author Thomas Kellerer
 */
public class LogMgr
{
	private static WbLogger logger;
	
	public synchronized static void init(boolean useLog4j)
	{
		if (logger == null)
		{
			if (useLog4j && Log4JHelper.isLog4JAvailable())
			{
				try
				{
					Class log4j = Class.forName("workbench.log.Log4jLogger");
					Object log = log4j.newInstance();
					if (log != null && log instanceof WbLogger)
					{
						logger = (WbLogger)log;
					}
				}
				catch (Throwable e)
				{
					System.err.println("Could not create Log4J logger. Using SimpleLogger!");
					e.printStackTrace(System.err);
					logger = new SimpleLogger();
				}
			}
			else
			{
				logger = new SimpleLogger();
			}
		}
	}

	public static WbFile getLogfile()
	{
		File f = logger.getCurrentFile();
		if (f == null) return null;
		return new WbFile(f);
	}

	public synchronized static void removeViewer()
	{
		logger.setLogViewer(null);
	}

	public synchronized static void registerViewer(LogFileViewer v)
	{
		logger.setLogViewer(v);
	}

	public static void setMessageFormat(String aFormat)
	{
		logger.setMessageFormat(aFormat);
	}

	public static void logToSystemError(boolean flag)
	{
		logger.logToSystemError(flag);
	}

	public static String getLevel()
	{
		return logger.getLevel().toString();
	}

	public static void setLevel(String aType)
	{
		logger.setLevel(LogLevel.getLevel(aType));
	}


	public static void shutdown()
	{
		logger.shutdown();
	}

	public static void setOutputFile(File logfile, int maxFilesize)
	{
		logger.setOutputFile(logfile, maxFilesize);
	}

	public static boolean isInfoEnabled()
	{
		return logger.levelEnabled(LogLevel.info);
	}

	public static boolean isDebugEnabled()
	{
		return logger.levelEnabled(LogLevel.debug);
	}

	public static void logDebug(Object aCaller, String aMsg)
	{
		logger.logMessage(LogLevel.debug, aCaller, aMsg, null);
	}

	public static void logDebug(Object aCaller, String aMsg, Throwable th)
	{
		logger.logMessage(LogLevel.debug, aCaller, aMsg, th);
	}

	public static void logInfo(Object aCaller, String aMsg)
	{
		logger.logMessage(LogLevel.info, aCaller, aMsg, null);
	}

	public static void logInfo(Object aCaller, String aMsg, Throwable th)
	{
		logger.logMessage(LogLevel.info, aCaller, aMsg, th);
	}

	public static void logWarning(Object aCaller, String aMsg)
	{
		logger.logMessage(LogLevel.warning, aCaller, aMsg, null);
	}

	public static void logWarning(Object aCaller, String aMsg, Throwable th)
	{
		logger.logMessage(LogLevel.warning, aCaller, aMsg, th);
	}

	public static void logError(Object aCaller, String aMsg, Throwable th)
	{
		logger.logMessage(LogLevel.error, aCaller, aMsg, th);
	}

	public static void logError(Object aCaller, String aMsg, SQLException se)
	{
		if (!logger.levelEnabled(LogLevel.error)) return;

		logger.logMessage(LogLevel.error, aCaller, aMsg, se);
		if (se != null)
		{
			SQLException next = se.getNextException();
			while (next != null)
			{
				logger.logMessage(LogLevel.error, "Chained exception", ExceptionUtil.getDisplay(next), null);
				next = next.getNextException();
			}
		}
	}

	public static String getStackTrace(Throwable th)
	{
		if (th == null) return StringUtil.EMPTY_STRING;
		try
		{
			StringWriter sw = new StringWriter(2000);
			PrintWriter pw = new PrintWriter(sw);
			pw.println();
			th.printStackTrace(pw);
			pw.close();
			return sw.toString();
		}
		catch (Exception ex)
		{
		}
		return StringUtil.EMPTY_STRING;
	}

}
