package workbench.log;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.PrintStream;
import java.io.PrintWriter;

/**
 *
 * @author  workbench@kellerer.org
 * @version
 */
public class LogMgr
{

	public static final String DEBUG = "DEBUG";
	public static final String INFO = "INFO";
	public static final String WARNING = "WARN";
	public static final String ERROR = "ERROR";

	private static PrintStream logOut = System.err;
	private static String outputfile;
	private static boolean outputOpened;
	
	private static final int EXC_TYPE_MSG = 1;
	private static final int EXC_TYPE_BRIEF = 2;
	private static final int EXC_TYPE_COMPLETE = 3;
	private static int exceptionType = EXC_TYPE_COMPLETE;
	
	private static SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
	
	public static void setExceptionTypeMessageOnly() { exceptionType = EXC_TYPE_MSG; }
	public static void setExceptionTypeBrief() { exceptionType = EXC_TYPE_BRIEF; }
	public static void setExceptionTypeComplete() { exceptionType = EXC_TYPE_COMPLETE; }


	public static void shutdown()
	{
		if (outputOpened && outputfile != null)
		{
			logOut.close();
		}
	}
	
	public static void setOutputFile(String aFilename)
	{
		if (aFilename == null || aFilename.length() == 0) return;
		if ("System.out".equals(aFilename)) 
		{
			if (logOut != null && logOut != System.out && logOut != System.err)
			{
				logOut.close();
			}
			outputOpened = true;
			logOut = System.out;
			return;
		}
		if ("System.err".equals(aFilename)) 
		{
			if (logOut != null && logOut != System.out && logOut != System.err)
			{
				logOut.close();
			}
			outputOpened = true;
			logOut = System.err;
			return;
		}
		outputfile = aFilename;
		outputOpened = false;
	}
	
	private static void checkOutput()
	{
		if (outputOpened) return;
    if (outputfile == null) 
    {
      logOut = System.err;
      outputOpened = true;
      return;
    }
		try
		{
			if (logOut != null)
			{
				logOut.close();
			}
      //System.out.println("Opening logfile " + outputfile);
			logOut = new PrintStream(new BufferedOutputStream(new FileOutputStream(outputfile)));
      outputOpened = true;
      logInfo("LogMgr", "Log started");
		}
		catch (Throwable th)
		{
			logOut = System.err;
      outputOpened = true;
			logError("LogMgr.checkOutput()", "Error when opening logfile=" + outputfile, th);
		}
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
		checkOutput();
		logOut.print(aType);
		logOut.print(" ");
		logOut.print(getTimeString());
		logOut.print(" - ");
		if (aCaller instanceof String)
			logOut.print(aCaller);
		else
			logOut.print(aCaller.getClass().getName());
		logOut.print(" - ");
		logOut.print(aMsg);
		if (th == null)
		{
			logOut.println();
		}
		else
		{
			if (exceptionType == EXC_TYPE_MSG)
			{
				logOut.print(" (");
				logOut.print(th.getMessage());
				logOut.println(")");
			}
			else if (exceptionType == EXC_TYPE_BRIEF)
			{
				logOut.println();
				logOut.print("     ");
				logOut.print(th.getClass());
				logOut.print(": ");
				logOut.println(th.getMessage());
			}
			else if (exceptionType == EXC_TYPE_COMPLETE)
			{
				logOut.println();
				logStackTrace(th);
			}
		}
	}

	public static void logStackTrace(Throwable th)
	{
		if (th != null) th.printStackTrace(logOut);
	}
	
	private static String getTimeString()
	{
		return formatter.format(new Date());
	}		
}
