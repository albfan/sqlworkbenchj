/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015 Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql;

import java.util.ArrayList;
import java.util.List;

import workbench.log.LogMgr;

import workbench.util.ClassFinder;



/**
 *
 * @author Thomas Kellerer
 */
public class CommandRegistry
{
	private static final String PACKAGE_NAME = "workbench.extensions";
	private final List<Class> commands = new ArrayList<>();

	/**
	 * Thread safe singleton-instance
	 */
	protected static class LazyInstanceHolder
	{
		protected static final CommandRegistry instance = new CommandRegistry();
	}

	public static CommandRegistry getInstance()
	{
		return LazyInstanceHolder.instance;
	}

	private CommandRegistry()
	{
	}

	public List<SqlCommand> getCommands()
	{
		List<SqlCommand> result = new ArrayList<>(commands.size());
		for (Class clz : commands)
		{
			try
			{
				SqlCommand cmd = (SqlCommand)clz.newInstance();
				result.add(cmd);
			}
			catch (Throwable th)
			{
				LogMgr.logError("CommandRegistry.getCommands()", "Could not create instance of: " + clz.getCanonicalName(), th);
			}
		}
		return result;
	}

	public synchronized void scanForExtensions()
	{
		long start = System.currentTimeMillis();
		commands.clear();
		try
		{
			List<Class> classes = ClassFinder.getClasses(PACKAGE_NAME);
			for (Class cls : classes)
			{
				LogMgr.logDebug("CommandRegistry.scanForExtensions()", "Found class " + cls.getName());
				if (SqlCommand.class.isAssignableFrom(cls))
				{
					commands.add(cls);
				}
			}
			long duration = System.currentTimeMillis() - start;
			LogMgr.logDebug("CommandRegistry.scanForExtensions()", "Found " + commands.size() + " commands in " + duration + "ms");
		}
		catch (Exception ex)
		{
			LogMgr.logWarning("CommandRegistry.scanForExtensions", "Error when scanning for exentensions", ex);
		}
	}


}
