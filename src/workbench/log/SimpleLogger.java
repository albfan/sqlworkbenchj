/*
 * WbLogger
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
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.MissingFormatArgumentException;
import workbench.gui.components.LogFileViewer;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class SimpleLogger
	implements WbLogger
{
	private LogLevel level = LogLevel.warning;
	
	private PrintStream logOut = null;
	private LogFileViewer viewer;
	private boolean logSystemErr = false;
	private boolean showStackTrace = false;
	private String messageFormat;
	private File currentFile;
	
	public SimpleLogger()
	{
	}

	public void setMessageFormat(String newFormat)
	{
		if (newFormat == null) return;

		messageFormat = newFormat.replace("{type}", "%1$-5s");
		messageFormat = messageFormat.replace("{timestamp}", "%2$tF %2$tR");
		messageFormat = messageFormat.replace("{source}", "%3$s");
		messageFormat = messageFormat.replace("{message}", "%4$s");
		messageFormat = messageFormat.replace("{error}", "%5$s");
		showStackTrace = messageFormat.indexOf("{stacktrace}") > -1;
	}

	public void logToSystemError(boolean flag)
	{
		logSystemErr = flag;
	}

	public void setLevel(LogLevel lvl)
	{
		level = lvl;
	}

	public LogLevel getLevel()
	{
		return level;
	}

	@Override
	public File getCurrentFile()
	{
		return currentFile;
	}

	public void setOutputFile(File logfile, int maxFilesize)
	{
		if (logfile == null) return;
		if (logfile.equals(currentFile)) return;

		try
		{
			if (logOut != null)
			{
				logOut.close();
				logOut = null;
			}

			if (logfile.exists() && logfile.length() > maxFilesize)
			{
				File last = new File(logfile.getAbsolutePath() + ".last");
				if (last.exists()) last.delete();
				logfile.renameTo(last);
			}
			logOut = new PrintStream(new FileOutputStream(logfile,true));
			currentFile = logfile;
			logMessage(LogLevel.info, null, "=================== Log started ===================", null);
		}
		catch (Throwable th)
		{
			logOut = null;
			logSystemErr = true;
			System.err.println("Error when opening logfile=" + logfile.getAbsolutePath());
			th.printStackTrace(System.err);
		}
	}

	public void shutdown()
	{
		if (logOut != null)
		{
			logMessage(LogLevel.info, null, "=================== Log stopped ===================", null);
			logOut.close();
		}
	}
	
	public void logDebug(Object aCaller, String aMsg)
	{
		if (levelEnabled(LogLevel.debug))
		{
			logMessage(LogLevel.debug, aCaller, aMsg, null);
		}
	}

	public void logSqlError(Object caller, String sql, Throwable th)
	{
		if (th instanceof SQLException)
		{
			logMessage(LogLevel.debug, caller, "Error executing statement: " + sql, th);
		}
		else
		{
			logMessage(LogLevel.error, caller, "Error executing statement: " + sql, th);
		}
	}

	public boolean levelEnabled(LogLevel tolog)
	{
		return level.compareTo(tolog) >= 0;
	}

	public synchronized void logMessage(LogLevel level, Object aCaller, String aMsg, Throwable th)
	{
		if (!levelEnabled(LogLevel.warning)) return;
		
		CharSequence s = formatMessage(level, aCaller, aMsg, th);
		if (logOut != null)
		{
			logOut.append(s);
			logOut.append(StringUtil.LINE_TERMINATOR);
			logOut.flush();
		}
		
		if (logSystemErr)
		{
			System.err.println(s);
		}

		if (viewer != null) viewer.append(s.toString());
	}

	private CharSequence formatMessage(LogLevel logLevel, Object caller, String msg, Throwable th)
	{
		try
		{
			if (th != null || logLevel == LogLevel.debug)
			{
				String trace = (th == null ? "" : th.getMessage());
				if (showStackTrace)
				{
					trace = LogMgr.getStackTrace(th);
				}
				return String.format(messageFormat, logLevel, new java.util.Date(), caller == null ? "" : caller, msg, trace);
			}
			else
			{
				return String.format(messageFormat, logLevel, new java.util.Date(), caller == null ? "" : caller, msg, "");
			}
		}
		catch (MissingFormatArgumentException e)
		{
			System.err.println("Error formatting message using: " + messageFormat);
			e.printStackTrace();
		}
		return msg;
	}

	public void setLogViewer(LogFileViewer logViewer)
	{
		this.viewer = logViewer;
	}
}
