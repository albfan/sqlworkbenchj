/*
 * SimpleLogger.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingFormatArgumentException;

import workbench.util.ExceptionUtil;
import workbench.util.FileVersioner;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class SimpleLogger
	implements WbLogger
{
	private LogLevel level = LogLevel.info;
	private PrintStream logOut = null;
	private boolean logSystemErr = false;
	private String messageFormat;
	private File currentFile;
  private List<LogListener> listenerList = new ArrayList<>(1);

	public SimpleLogger()
	{
	}

	@Override
	public void setMessageFormat(String newFormat)
	{
		if (newFormat == null) return;

		messageFormat = newFormat.replace("{type}", "%1$-5s");
		messageFormat = messageFormat.replace("{timestamp}", "%2$tF %2$tT");
		messageFormat = messageFormat.replace("{source}", "%3$s");
		messageFormat = messageFormat.replace("{message}", "%4$s");
		messageFormat = messageFormat.replace("{error}", "%5$s");
	}

	@Override
	public void logToSystemError(boolean flag)
	{
		logSystemErr = flag;
	}

	@Override
	public void setRootLevel(LogLevel lvl)
	{
		level = lvl;
	}

	@Override
	public LogLevel getRootLevel()
	{
		return level;
	}

	@Override
	public File getCurrentFile()
	{
		return currentFile;
	}

	@Override
	public void setOutputFile(File logfile, int maxFilesize, int maxBackups)
	{
		if (logfile == null)
		{
			System.err.println("setOutputFile() called with a NULL file!");
			return;
		}

		if (currentFile != null && logfile.equals(currentFile))
		{
			return;
		}

		try
		{
			if (logOut != null)
			{
				logOut.close();
				logOut = null;
			}

			if (logfile.exists() && logfile.length() > maxFilesize)
			{
        FileVersioner fv = new FileVersioner(maxBackups);
        fv.createBackup(logfile);
        logfile.delete();
			}
			logOut = new PrintStream(new FileOutputStream(logfile, true), true, LogMgr.DEFAULT_ENCODING);
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

	@Override
	public void shutdownWbLog()
	{
		if (logOut != null)
		{
			logMessage(LogLevel.info, null, "=================== Log stopped ===================", null);
			logOut.close();
			logOut = null;
			logSystemErr = false;
		}
	}

	@Override
	public boolean levelEnabled(LogLevel tolog)
	{
		return level.compareTo(tolog) >= 0;
	}

	/**
	 * Log a message at a specific loglevel
	 *
	 * @param logLevel  the loglevel
	 * @param aCaller   the caller (only logged at debug or higher)
	 * @param message   the message to log
	 * @param th        the exception (may be null)
	 */
	@Override
	public synchronized void logMessage(LogLevel logLevel, Object aCaller, CharSequence message, Throwable th)
	{
		if (!levelEnabled(logLevel))
		{
			return;
		}

		CharSequence s = formatMessage(logLevel, aCaller, message, th);
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
    notifyListener(s);
	}

	private CharSequence formatMessage(LogLevel logLevel, Object caller, CharSequence msg, Throwable th)
	{
    if (messageFormat == null) return msg;
		try
		{
			if (th != null)
			{
				// if the requested level is error or finer, include the stacktrace
				boolean includeStacktrace = logLevel.compareTo(LogLevel.error) >= 0;
				String error = ExceptionUtil.getDisplay(th, includeStacktrace);
				return String.format(messageFormat, logLevel, new java.util.Date(), caller == null ? "" : caller, msg, error);
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

  private void notifyListener(CharSequence msg)
  {
    for (LogListener listener : listenerList)
    {
      if (listener != null)
      {
        listener.messageLogged(msg);
      }
    }
  }

  @Override
  public void addLogListener(LogListener listener)
  {
    if (listener != null)
    {
      listenerList.add(listener);
    }
  }

  @Override
  public void removeLogListener(LogListener listener)
  {
    listenerList.remove(listener);
  }

}
