/*
 * SimpleLogger.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
import java.util.MissingFormatArgumentException;

import workbench.util.*;

/**
 *
 * @author Thomas Kellerer
 */
public class SimpleLogger
	implements WbLogger
{

	private LogLevel level = LogLevel.warning;
	private PrintStream logOut = null;
	private boolean logSystemErr = false;
	private String messageFormat;
	private File currentFile;

	public SimpleLogger()
	{
	}

	@Override
	public void setMessageFormat(String newFormat)
	{
		if (newFormat == null) return;

		messageFormat = newFormat.replace("{type}", "%1$-5s");
		messageFormat = messageFormat.replace("{timestamp}", "%2$tF %2$tR");
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
	public void setOutputFile(File logfile, int maxFilesize)
	{
		if (logfile == null)
		{
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
				File last = new File(logfile.getAbsolutePath() + ".last");
				if (last.exists())
				{
					last.delete();
				}
				logfile.renameTo(last);
			}
			logOut = new PrintStream(new FileOutputStream(logfile, true));
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

	@Override
	public synchronized void logMessage(LogLevel level, Object aCaller, String aMsg, Throwable th)
	{
		if (!levelEnabled(level))
		{
			return;
		}

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
	}

	private CharSequence formatMessage(LogLevel logLevel, Object caller, String msg, Throwable th)
	{
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

}
