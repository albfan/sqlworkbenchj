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

import java.io.*;
import java.sql.*;

import org.apache.log4j.*;

import workbench.gui.components.*;
import workbench.util.*;

/**
 * The logging class used by SQL Workbench/J
 * 
 * @author Thomas Kellerer
 */
public class LogMgr
{
	private static WbLogger logger = null;
	private static boolean useLog4J;

	public synchronized static void init(boolean useLog4j)
	{
		useLog4J = useLog4j;
		if (useLog4j && Log4JHelper.isLog4JAvailable())
		{
			Log4JLoggerFactory.setLoggerFqcn(LogMgr.class);
		}
		createLogger();
	}

	public static WbFile getLogfile()
	{
		File f = logger.getCurrentFile();
		if (f == null)
		{
			return null;
		}
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
		return logger.getRootLevel().toString();
	}

	public static void setLevel(String aType)
	{
		logger.setRootLevel(LogLevel.getLevel(aType));
		// Demo for log-calls from within this LoggerController
		// logInfo(null, "(LogMgr.setLevel()) Set level to " + logger.getRootLevel());
	}

	public static void shutdown()
	{
		logger.shutdownWbLog();
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
		if (!logger.levelEnabled(LogLevel.error))
		{
			return;
		}

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
		if (th == null)
		{
			return StringUtil.EMPTY_STRING;
		}
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

	private static void createLogger()
	{
		if (useLog4J && Log4JHelper.isLog4JAvailable())
		{
			try
			{
				logger = Log4JLogger.getLogger();
			}
			catch (Throwable e)
			{
				System.err.println("Could not create Log4J logger. Using SimpleLogger!");
				e.printStackTrace(System.err);
				useLog4J = false;
			}
		}
		if (logger == null)
		{
			logger = new SimpleLogger();
		}
	}
}
