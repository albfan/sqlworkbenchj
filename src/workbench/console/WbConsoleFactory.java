/*
 * ConsoleReaderFactory.java
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
package workbench.console;

import java.io.IOException;

import workbench.log.LogMgr;
import workbench.resource.Settings;

/**
 *
 * @author Thomas Kellerer
 */
public class WbConsoleFactory
{
	private static WbConsole instance;

	public synchronized static WbConsole getConsole()
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
		return Settings.getInstance().getBoolProperty("workbench.console.use.jline", true);
	}

}
