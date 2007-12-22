/*
 * LogMgr.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import workbench.util.ExceptionUtil;
import workbench.util.StrBuffer;
import workbench.util.StringUtil;


/**
 * @author  support@sql-workbench.net
 */
public class LogMgr
{

	public static final String ERROR = "ERROR";
	public static final String WARNING = "WARN";
	public static final String INFO = "INFO";
	public static final String DEBUG = "DEBUG";

	private static final String WARNING_DISPLAY = "WARN ";
	private static final String INFO_DISPLAY = "INFO ";

	public static final List<String> LEVELS;
	static
	{
		LEVELS = new ArrayList<String>(4);
		LEVELS.add(ERROR);
		LEVELS.add(WARNING);
		LEVELS.add(INFO);
		LEVELS.add(DEBUG);
	}

	private static PrintStream logOut = null;
	private static final Date theDate = new Date();
	private static boolean logSystemErr = false;

	private static int typeIndex = -1;
	private static int timeIndex = -1;
	private static int sourceIndex = -1;
	private static int messageIndex = 0;
	private static int exceptionMsgIndex = 1;
	private static boolean showStackTrace = false;
	private static final int NUM_ELEMENTS = 5;
	private static String[] MSG_ELEMENTS = new String[NUM_ELEMENTS];

	private static SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

	private static int loglevel = 3;

	private static int levelDebug;
	private static int levelWarning;
	private static int levelInfo;
	private static int levelError;
	private static boolean debugEnabled;
	private static boolean infoEnabled;

	public static void setMessageFormat(String aFormat)
	{
		if (aFormat == null) return;
		Pattern p = Pattern.compile("\\{[a-zA-Z]+\\}");
		Matcher m = p.matcher(aFormat);

		typeIndex = -1;
		timeIndex = -1;
		sourceIndex = -1;
		messageIndex = -1;
		exceptionMsgIndex = -1;
		showStackTrace = false;

		int currentIndex = 0;
		while (m.find())
		{
			int start = m.start();
			int end = m.end();
			String key = aFormat.substring(start, end).toLowerCase();
			if ("{type}".equals(key))
			{
				typeIndex = currentIndex;
				currentIndex ++;
			}
			else if ("{timestamp}".equals(key))
			{
				timeIndex = currentIndex;
				currentIndex++;
			}
			else if ("{source}".equals(key))
			{
				sourceIndex = currentIndex;
				currentIndex ++;
			}
			else if ("{message}".equals(key))
			{
				messageIndex = currentIndex;
				currentIndex ++;
			}
			else if ("{error}".equals(key))
			{
				exceptionMsgIndex = currentIndex;
				currentIndex ++;
			}
			else if ("{stacktrace}".equals(key))
			{
				showStackTrace = true;
			}
		}
	}

	public static void logErrors() { setLevel(ERROR); }
	public static void logWarnings() { setLevel(WARNING); }
	public static void logInfo() { setLevel(INFO); }
	public static void logDebug() { setLevel(DEBUG); }
	public static void logAll() { logDebug(); }

	public static void logToSystemError(boolean flag)
	{
		logSystemErr = flag;
	}
	
	public static String getLevel()
	{
		if (loglevel == levelDebug) return "DEBUG";
		if (loglevel == levelWarning) return "WARNING";
		if (loglevel == levelError) return "ERROR";
		if (loglevel == levelInfo) return "INFO";
		return "ERROR";
	}
	
	public static void setLevel(String aType)
	{
		if (aType == null) aType = "INFO";
		if ("warning".equalsIgnoreCase(aType)) aType = "WARN";
		else aType = aType.toUpperCase();

		levelDebug = LEVELS.indexOf(DEBUG);
		levelWarning = LEVELS.indexOf(WARNING);
		levelInfo = LEVELS.indexOf(INFO);
		levelError = LEVELS.indexOf(ERROR);

		if (LEVELS.contains(aType))
		{
			loglevel = LEVELS.indexOf(aType);
		}
		else
		{
			logErrors();
			logError("LogMgr.setLevel()", "Requested level " +  aType + " not found! Setting level " + ERROR, null);
		}
		debugEnabled = (loglevel == levelDebug);
		infoEnabled = (loglevel == levelInfo || debugEnabled);
	}


	public static void shutdown()
	{
		if (logOut != null)
		{
			logInfo(null, "=================== Log stopped ===================");
			logOut.close();
		}
	}

	public static void setOutputFile(File logfile, int maxFilesize)
	{
		if (logfile == null) return;
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
			logInfo(null, "=================== Log started ===================");
		}
		catch (Throwable th)
		{
			logOut = null;
			logSystemErr = true;
			logError("LogMgr.checkOutput()", "Error when opening logfile=" + logfile.getAbsolutePath(), th);
		}
	}

	public static boolean isInfoEnabled()
	{
		return infoEnabled;
	}
	public static boolean isDebugEnabled()
	{
		return debugEnabled;
	}

	public static void logDebug(Object aCaller, String aMsg)
	{
		if (levelDebug > loglevel)  return;
		logMessage(DEBUG, aCaller, aMsg, null);
	}

	public static void logSqlError(Object caller, String sql, Throwable th)
	{
		if (th instanceof SQLException)
		{
			logDebug(caller, "Error executing statement: " + sql, th);
		}
		else
		{
			logError(caller, "Error executing statement: " + sql, th);
		}
	}
	
	public static void logDebug(Object aCaller, String aMsg, Throwable th)
	{
		if (levelDebug > loglevel)  return;
		logMessage(DEBUG, aCaller, aMsg, th);
	}

	public static void logInfo(Object aCaller, String aMsg)
	{
		if (levelInfo > loglevel)  return;
		logMessage(INFO_DISPLAY, aCaller, aMsg, null);
	}

	public static void logInfo(Object aCaller, String aMsg, Throwable th)
	{
		if (levelInfo > loglevel)  return;
		logMessage(INFO_DISPLAY, aCaller, aMsg, th);
	}

	public static void logWarning(Object aCaller, String aMsg)
	{
		if (levelWarning > loglevel)  return;
		logMessage(WARNING_DISPLAY, aCaller, aMsg, null);
	}

	public static void logWarning(Object aCaller, String aMsg, Throwable th)
	{
		if (levelWarning > loglevel)  return;
		logMessage(WARNING_DISPLAY, aCaller, aMsg, th);
	}

	public static void logError(Object aCaller, String aMsg, Throwable th)
	{
		if (levelError > loglevel) return;
		logMessage(ERROR, aCaller, aMsg, th);
	}

	public static void logError(Object aCaller, String aMsg, SQLException se)
	{
		if (levelError > loglevel) return;

		logMessage(ERROR, aCaller, aMsg, se);
		if (se != null)
		{
			SQLException next = se.getNextException();
			while (next != null)
			{
				logMessage(ERROR, "Chained exception", ExceptionUtil.getDisplay(next), null);
				next = next.getNextException();
			}
		}
	}

	private synchronized static void logMessage(String aType, Object aCaller, String aMsg, Throwable th)
	{
		StrBuffer s = formatMessage(aType, aCaller, aMsg, th);
		if (logOut != null)
		{
			s.writeTo(logOut);
			logOut.flush();
		}
		if (logSystemErr)
		{
			s.writeTo(System.err);
		}
	}

	private static StrBuffer formatMessage(String aType, Object aCaller, String aMsg, Throwable th)
	{
		StrBuffer buff = new StrBuffer(100);

		for (int i=0; i < NUM_ELEMENTS; i++) MSG_ELEMENTS[i] = null;

		if (timeIndex > -1)
		{
			MSG_ELEMENTS[timeIndex] = getTimeString();
		}

		if (typeIndex > -1)
		{
			MSG_ELEMENTS[typeIndex] = aType;
		}

		if (sourceIndex > -1)
		{
			if (aCaller != null)
			{
				if (aCaller instanceof String)
					MSG_ELEMENTS[sourceIndex] = (String)aCaller;
				else
					MSG_ELEMENTS[sourceIndex] = aCaller.getClass().getName();
			}
		}

		if (messageIndex > -1)
		{
			MSG_ELEMENTS[messageIndex] = aMsg;
		}

		boolean hasException = false;
		if (exceptionMsgIndex > -1 && th != null)
		{
			MSG_ELEMENTS[exceptionMsgIndex] = ExceptionUtil.getDisplay(th);
			hasException = true;
		}

		boolean first = true;

		for (int i=0; i < NUM_ELEMENTS; i++)
		{

			if (MSG_ELEMENTS[i] != null)
			{
				if (!first) buff.append(" ");
				else first = false;
				buff.append(MSG_ELEMENTS[i]);
				if (i == sourceIndex) buff.append(" -");
				if (i == messageIndex && hasException) buff.append(":");
			}
		}
		buff.append(StringUtil.LINE_TERMINATOR);

		// always display the stacktrace in debug level
		if (th != null && (showStackTrace || loglevel == levelDebug || th instanceof NullPointerException))
		{
			buff.append(getStackTrace(th));
			buff.append(StringUtil.LINE_TERMINATOR);
		}

		return buff;
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

	private static String getTimeString()
	{
		theDate.setTime(System.currentTimeMillis());
		return formatter.format(theDate);
	}

}
