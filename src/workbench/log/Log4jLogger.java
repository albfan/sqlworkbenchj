/*
 * Log4jLogger
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.log;

import java.io.File;
import java.util.Enumeration;
import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import workbench.gui.components.LogFileViewer;

/**
 *
 * @author Thomas Kellerer
 */
public class Log4jLogger
	implements WbLogger
{
	private boolean logToSystemErr = false;
	
	public Log4jLogger()
	{
	}

	private LogLevel toWbLevel(Level level)
	{
		if (level == Level.DEBUG) return LogLevel.debug;
		if (level == Level.ERROR) return LogLevel.error;
		if (level == Level.INFO) return LogLevel.info;
		if (level == Level.WARN) return LogLevel.warning;
		return LogLevel.error;
	}

	private Level toLog4JLevel(LogLevel level)
	{
		switch (level)
		{
			case debug:
				return Level.DEBUG;
			case error:
				return Level.ERROR;
			case info:
				return Level.INFO;
			case warning:
				return Level.WARN;
		}
		return Level.ERROR;
	}
	
	@Override
	public void setLevel(LogLevel level)
	{
		Logger.getRootLogger().setLevel(toLog4JLevel(level));
	}

	@Override
	public LogLevel getLevel()
	{
		return toWbLevel(Logger.getRootLogger().getLevel());
	}

	private String getCallerName(String caller)
	{
		int pos = caller.indexOf('.');
		if (pos > -1)
		{
			return caller.substring(0, pos);
		}
		return caller;
	}

	private Logger getLogger(String caller)
	{
		return Logger.getLogger(getCallerName(caller.toString()));
	}

	@Override
	public void logMessage(LogLevel level, Object caller, String msg, Throwable th)
	{
		Logger log = getLogger(caller.toString());
		
		switch (level)
		{
			case debug:
				log.debug(msg, th);
				break;
			case info:
				log.info(msg, th);
				break;
			case warning:
				log.warn(msg, th);
				break;
			default:
				log.error(msg, th);
		}
		if (logToSystemErr)
		{
			String format = "%1$-5s %2$tF %2$tR %3$s %4$s";
			String out = String.format(format, level, new java.util.Date(), caller == null ? "" : caller, msg == null ? "" : msg);
			System.err.println(out);
			if (th != null)
			{
				th.printStackTrace(System.err);
			}
		}
	}

	@Override
	public void logSqlError(Object caller, String sql, Throwable th)
	{
		logMessage(LogLevel.error, caller, sql, th);
	}

	@Override
	public void setMessageFormat(String newFormat)
	{
		// ignored, should be done by log4j.xml
	}

	@Override
	public void logToSystemError(boolean flag)
	{
		logToSystemErr = flag;
	}

	@Override
	public File getCurrentFile()
	{
		Logger root = Logger.getRootLogger();
		Enumeration appenders = root.getAllAppenders();
		while (appenders.hasMoreElements())
		{
			Appender app = (Appender)appenders.nextElement();
			if (app instanceof FileAppender)
			{
				FileAppender file = (FileAppender)app;
				String fname = file.getFile();
				if (fname != null)
				{
					return new File(fname);
				}
			}
		}
		return null;
	}

	@Override
	public void setOutputFile(File logfile, int maxFilesize)
	{
		Logger.getLogger(getClass()).info("=================== Log started ===================");
	}

	@Override
	public void shutdown()
	{
		Logger.getLogger(getClass()).info("=================== Log stopped ===================");
	}

	@Override
	public void setLogViewer(LogFileViewer logViewer)
	{
		// not supported for Log4j
	}

	public boolean levelEnabled(LogLevel tolog)
	{
		Logger root = Logger.getRootLogger();
		switch (tolog)
		{
			case debug:
				return root.isDebugEnabled();
			case info:
				return root.isInfoEnabled();
			case warning:
				return root.isEnabledFor(Level.WARN);
		}
		return true;
	}
}
