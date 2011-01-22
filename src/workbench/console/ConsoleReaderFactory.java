/*
 * ConsoleReaderFactory
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.console;

import java.io.IOException;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.PlatformHelper;

/**
 *
 * @author Thomas Kellerer
 */
public class ConsoleReaderFactory
{
	private static WbConsoleReader instance;

	public synchronized static WbConsoleReader getConsoleReader()
	{
		if (instance == null)
		{
			if (useJLine())
			{
				try
				{
					instance = new JLineWrapper();
					LogMgr.logDebug("ConsoleReaderFactory", "Using JLine");
					return instance;
				}
				catch (IOException io)
				{
					instance = null;
				}
			}

			if (System.console() != null)
			{
				instance = new SystemConsole();
				LogMgr.logDebug("ConsoleReaderFactory", "Using System.console()");
			}
			
			if (instance == null)
			{
				instance = new SimpleConsole();
			}
		}
		return instance;
	}

	private static boolean useJLine()
	{
		// By default JLine is not used on the Windows platform as System.console() already implements this
		// for Windows (including the standard F7 history window)
		return Settings.getInstance().getBoolProperty("workbench.console.use.jline", !PlatformHelper.isWindows());
	}
	
}
