/*
 * LogMgr.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.log;

import java.io.File;
import java.sql.SQLException;
import workbench.util.*;

/**
 * A facade to the actual logging implementation used.
 * <br>
 * Depending on the flag passed to {@link #init(boolean)}
 * either a {@link SimpleLogger} or a {@link Log4JLogger} will be created.
 * <br>
 * If Log4J is used as the logging sub-system, none of the SQL Workbench/J
 * log settings will be applied. Everything needs to be configured through Log4J
 * <br>
 * {@link Log4JHelper} is used to check if the Log4J classes are available at
 * runtime (using reflection).
 * The Log4J classes are expected to be in a file called <tt>log4j.jar</tt>
 * that resides in the same directory as <tt>sqlworkbench.jar</tt> (for details
 * see the manifest that is created in build.xml)
 *
 * @author Thomas Kellerer
 */
public class LogMgr
{
	private static WbLogger logger = null;
	private static boolean useLog4J;

	public synchronized static void init(boolean useLog4j)
	{
		useLog4J = useLog4j && Log4JHelper.isLog4JAvailable();
		if (!useLog4j)
		{
			// Initialize the Workbench logging right away
			getLogger();
		}
	}

	public static WbFile getLogfile()
	{
		File f = getLogger().getCurrentFile();
		if (f == null)
		{
			return null;
		}
		return new WbFile(f);
	}

	public static void setMessageFormat(String aFormat)
	{
		getLogger().setMessageFormat(aFormat);
	}

	public static void logToSystemError(boolean flag)
	{
		getLogger().logToSystemError(flag);
	}

	public static String getLevel()
	{
		return getLogger().getRootLevel().toString();
	}

	public static void setLevel(String aType)
	{
		getLogger().setRootLevel(LogLevel.getLevel(aType));
	}

	public static void shutdown()
	{
		getLogger().shutdownWbLog();
	}

	public static void setOutputFile(File logfile, int maxFilesize)
	{
		getLogger().setOutputFile(logfile, maxFilesize);
	}

	public static boolean isInfoEnabled()
	{
		return getLogger().levelEnabled(LogLevel.info);
	}

	public static boolean isDebugEnabled()
	{
		return getLogger().levelEnabled(LogLevel.debug);
	}

	public static void logDebug(Object aCaller, String aMsg)
	{
		getLogger().logMessage(LogLevel.debug, aCaller, aMsg, null);
	}

	public static void logDebug(Object aCaller, String aMsg, Throwable th)
	{
		getLogger().logMessage(LogLevel.debug, aCaller, aMsg, th);
	}

	public static void logInfo(Object aCaller, String aMsg)
	{
		getLogger().logMessage(LogLevel.info, aCaller, aMsg, null);
	}

	public static void logInfo(Object aCaller, String aMsg, Throwable th)
	{
		getLogger().logMessage(LogLevel.info, aCaller, aMsg, th);
	}

	public static void logWarning(Object aCaller, String aMsg)
	{
		getLogger().logMessage(LogLevel.warning, aCaller, aMsg, null);
	}

	public static void logWarning(Object aCaller, String aMsg, Throwable th)
	{
		getLogger().logMessage(LogLevel.warning, aCaller, aMsg, th);
	}

	public static void logError(Object aCaller, String aMsg, Throwable th)
	{
		getLogger().logMessage(LogLevel.error, aCaller, aMsg, th);
	}

	public static void logError(Object aCaller, String aMsg, SQLException se)
	{
		if (!getLogger().levelEnabled(LogLevel.error))
		{
			return;
		}

		getLogger().logMessage(LogLevel.error, aCaller, aMsg, se);
		if (se != null)
		{
			SQLException next = se.getNextException();
			while (next != null)
			{
				getLogger().logMessage(LogLevel.error, "Chained exception", ExceptionUtil.getDisplay(next), null);
				next = next.getNextException();
			}
		}
	}

	private synchronized static WbLogger getLogger()
	{
		if (useLog4J)
		{
			try
			{
				return Log4JLogger.getLogger();
			}
			catch (Throwable e)
			{
				System.err.println("Could not create Log4J getLogger(). Using SimpleLogger!");
				e.printStackTrace(System.err);
				useLog4J = false;
			}
		}
		if (logger == null)
		{
			logger = new SimpleLogger();
		}
		return logger;
	}
}
