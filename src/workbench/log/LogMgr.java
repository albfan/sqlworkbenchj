package workbench.log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import workbench.WbManager;
import workbench.util.StringUtil;

/**
 *
 * @author  workbench@kellerer.org
 * @version
 */
public class LogMgr
{

	public static final String ERROR = "ERROR";
	public static final String WARNING = "WARN";
	public static final String INFO = "INFO";
	public static final String DEBUG = "DEBUG";

	public static final List LEVELS;
	static
	{
		LEVELS = new ArrayList(4);
		LEVELS.add(ERROR);
		LEVELS.add(WARNING);
		LEVELS.add(INFO);
		LEVELS.add(DEBUG);
	}

	private static PrintStream logOut = null;
	private static final Date theDate = new Date();
	private static final int EXC_TYPE_MSG = 1;
	private static final int EXC_TYPE_BRIEF = 2;
	private static final int EXC_TYPE_COMPLETE = 3;
	private static int exceptionType = EXC_TYPE_COMPLETE;

	private static SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

	public static void setExceptionTypeMessageOnly() { exceptionType = EXC_TYPE_MSG; }
	public static void setExceptionTypeBrief() { exceptionType = EXC_TYPE_BRIEF; }
	public static void setExceptionTypeComplete() { exceptionType = EXC_TYPE_COMPLETE; }

	public static void logErrors() { setLevel(ERROR); }
	public static void logWarnings() { setLevel(WARNING); }
	public static void logInfo() { setLevel(INFO); }
	public static void logDebug() { setLevel(DEBUG); }
	public static void logAll() { logDebug(); }

	public static void setLevel(String aType)
	{
		if (aType == null) return;
		if ("warning".equalsIgnoreCase(aType)) aType = "WARN";
		aType = aType.toUpperCase();
		if (LEVELS.contains(aType))
		{
			loglevel = LEVELS.indexOf(aType);
		}
		else
		{
			logErrors();
		}
	}

	private static int loglevel = 4;


	public static boolean isDebug()
	{
		return (loglevel == 3);
	}
	
	public static void shutdown()
	{
		if (logOut != null)
		{
			logOut.close();
		}
	}

	public static void setOutputFile(String aFilename)
	{
	  if (WbManager.trace) System.out.println("LogMgr.setOutputFile() - " + aFilename);
		if (aFilename == null || aFilename.length() == 0) return;
		if (aFilename.startsWith("System")) return;
		try
		{
			if (logOut != null)
			{
				logOut.close();
				logOut = null;
			}
      if (WbManager.trace) System.out.println("LogMgr.checkOutput() - Opening logfile " + aFilename);
			File f = new File(aFilename);
			if (f.exists())
			{
				File last = new File(aFilename + ".last");
				if (last.exists()) last.delete();
				f.renameTo(last);
			}
			logOut = new PrintStream(new BufferedOutputStream(new FileOutputStream(aFilename)));
      logInfo("LogMgr", "Log started");
		}
		catch (Throwable th)
		{
			logOut = null;
			logError("LogMgr.checkOutput()", "Error when opening logfile=" + aFilename, th);
		}
	  if (WbManager.trace) System.out.println("LogMgr.setOutputFile() - done");
	}

	public static void logDebug(Object aCaller, String aMsg)
	{
		logDebug(aCaller, aMsg, null);
	}
	public static void logDebug(Object aCaller, String aMsg, Throwable th)
	{
		logMessage(DEBUG, aCaller, aMsg, th);
	}

	public static void logInfo(Object aCaller, String aMsg)
	{
		logInfo(aCaller, aMsg, null);
	}

	public static void logInfo(Object aCaller, String aMsg, Throwable th)
	{
		logMessage(INFO, aCaller, aMsg, th);
	}

	public static void logWarning(Object aCaller, String aMsg)
	{
		logWarning(aCaller, aMsg, null);
	}

	public static void logWarning(Object aCaller, String aMsg, Throwable th)
	{
		logMessage(WARNING, aCaller, aMsg, th);
	}

	public static void logError(Object aCaller, String aMsg, Throwable th)
	{
		logMessage(ERROR, aCaller, aMsg, th);
	}

	private static void logMessage(String aType, Object aCaller, String aMsg, Throwable th)
	{
		int level = LEVELS.indexOf(aType);
		if (level > loglevel) return;

		String s = formatMessage(aType, aCaller, aMsg, th);
		if (logOut != null) 
		{
			logOut.print(s);
			logOut.flush();
		}
		System.out.print(s);
	}
	
	private static String formatMessage(String aType, Object aCaller, String aMsg, Throwable th)
	{
		StringBuffer buff;
		if (th == null) buff = new StringBuffer(200);
		else buff = new StringBuffer(500);
		
		buff.append(aType);
		buff.append(" ");
		buff.append(getTimeString());
		buff.append(" - ");
		if (aCaller instanceof String)
			buff.append((String)aCaller);
		else
			buff.append(aCaller.getClass().getName());
		buff.append(" - ");
		buff.append(aMsg);
		if (th == null)
		{
			buff.append(StringUtil.LINE_TERMINATOR);
		}
		else
		{
			if (exceptionType == EXC_TYPE_MSG)
			{
				buff.append(" (");
				buff.append(th.getMessage());
				buff.append(')');
				buff.append(StringUtil.LINE_TERMINATOR);
			}
			else if (exceptionType == EXC_TYPE_BRIEF)
			{
				buff.append(StringUtil.LINE_TERMINATOR);
				buff.append("     ");
				buff.append(th.getClass());
				buff.append(": ");
				buff.append(th.getMessage());
				buff.append(StringUtil.LINE_TERMINATOR);
			}
			else if (exceptionType == EXC_TYPE_COMPLETE)
			{
				String msg = th.getMessage();
				if (msg != null) 
				{
					buff.append(msg);
					buff.append(StringUtil.LINE_TERMINATOR);
				}
				
				buff.append(getStackTrace(th));
				buff.append(StringUtil.LINE_TERMINATOR);
			}
		}
		return buff.toString();
	}

	public static String getStackTrace(Throwable th)
	{
		if (th == null) return "";
		try
		{
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			th.printStackTrace(pw);
			pw.close();
			return sw.toString();
		} 
		catch (Exception ex)
		{
		}
		return "";
	}

	private static String getTimeString()
	{
		theDate.setTime(System.currentTimeMillis());
		return formatter.format(theDate);
	}
}