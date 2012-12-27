/*
 * FileAttributeChanger.java
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
package workbench.util;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Method;

import workbench.log.LogMgr;


/**
 *
 * @author Thomas Kellerer
 */
public class FileAttributeChanger
{

	public void removeHidden(File dir)
	{
		if (isWindows())
		{
			boolean done = false;
			if (isJava7())
			{
				done = removeAttribute(dir);
			}
			if (!done)
			{
				runAttribCommand(dir);
			}
		}
	}

	private void runAttribCommand(File dir)
	{
		try
		{
			LogMgr.logDebug("FileAttributeChanger.runAttribCommand()", "Using Runtime to remove hidden attribute");
			Runtime.getRuntime().exec("attrib -H \""  + dir.getAbsolutePath() + "\"");
		}
		catch (Throwable th)
		{
			LogMgr.logWarning("FileAttributeChanger.runAttribCommand()", "Could not run attrib command", th);
		}
	}

	private boolean removeAttribute(File dir)
	{
		// this code does essentially the following:
		// Path file = dir.toPath();
		// Files.setAttribute(file, "dos:hidden", false);

		// In order to be able to compile on Java6, I'm using reflection.
		try
		{
			LogMgr.logDebug("FileAttributeChanger.removeAttribute()", "Using Files.setAttribute() to remove hidden attribute");
			Method m = dir.getClass().getMethod("toPath", (Class<?>[])null);
			Object path = m.invoke(dir, (Object[]) null);
			Class pathClass = Class.forName("java.nio.file.Path");
			Class filesClass = Class.forName("java.nio.file.Files");
			Class option = Class.forName("java.nio.file.LinkOption");
			Class optionArray = Class.forName("[Ljava.nio.file.LinkOption;");
			Method setAttr = filesClass.getMethod("setAttribute", pathClass, String.class, Object.class, optionArray);

			Enum noFollow = Enum.valueOf(option, "NOFOLLOW_LINKS");
			Object options = Array.newInstance(option, 1);
			Array.set(options, 0, noFollow);
			setAttr.invoke(null, path, "dos:hidden", Boolean.FALSE, options);
			return true;
		}
		catch (Throwable th)
		{
			LogMgr.logWarning("FileAttributeChanger.removeAttribute()", "Could not remove hidden attribute", th);
			return false;
		}
	}

	private boolean isWindows()
	{
		return System.getProperty("os.name").contains("Windows");
	}

	public static boolean isJava7()
	{
		VersionNumber java = VersionNumber.getJavaVersion();
		VersionNumber java7 = new VersionNumber(1, 7);
		return java.isNewerOrEqual(java7);
	}
}
