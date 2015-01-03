/*
 * LogMgr.java
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
package workbench.log;

import java.io.File;
import java.sql.SQLException;

import workbench.util.ExceptionUtil;
import workbench.util.WbFile;

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
	public static final String DEFAULT_ENCODING = "UTF-8";

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

	public static void setMessageFormat(String msgFormat)
	{
		getLogger().setMessageFormat(msgFormat);
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

	public static boolean isTraceEnabled()
	{
		return getLogger().levelEnabled(LogLevel.trace);
	}

	public static void logDebug(Object caller, CharSequence message)
	{
		getLogger().logMessage(LogLevel.debug, caller, message, null);
	}

	public static void logTrace(Object caller, CharSequence message)
	{
		getLogger().logMessage(LogLevel.trace, caller, message, null);
	}

	public static void logDebug(Object caller, CharSequence message, Throwable th)
	{
		getLogger().logMessage(LogLevel.debug, caller, message, th);
		logChainedException(LogLevel.debug, th);
	}

	public static void logInfo(Object caller, CharSequence message)
	{
		getLogger().logMessage(LogLevel.info, caller, message, null);
	}

	public static void logInfo(Object caller, CharSequence message, Throwable th)
	{
		getLogger().logMessage(LogLevel.info, caller, message, th);
	}

	public static void logWarning(Object caller, CharSequence message)
	{
		getLogger().logMessage(LogLevel.warning, caller, message, null);
	}

	public static void logWarning(Object caller, CharSequence message, Throwable th)
	{
		getLogger().logMessage(LogLevel.warning, caller, message, th);
		logChainedException(LogLevel.warning, th);
	}

	public static void logError(Object caller, CharSequence message, Throwable th)
	{
		getLogger().logMessage(LogLevel.error, caller, message, th);
		logChainedException(LogLevel.error, th);
	}

	public static void logUserSqlError(Object caller, String sql, Throwable th)
	{
		String logMsg = "Error executing:\n" + sql + "\n  ";
		if (th instanceof SQLException && !getLogger().levelEnabled(LogLevel.debug))
		{
			logMsg += ExceptionUtil.getDisplay(th);
			logError(caller, logMsg, null);
		}
		else
		{
			logError(caller, logMsg, th);
		}
	}

	public static void logChainedException(LogLevel level, Throwable se)
	{
		if (getLogger().levelEnabled(level) && se instanceof SQLException)
		{
			SQLException next = ((SQLException)se).getNextException();
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
