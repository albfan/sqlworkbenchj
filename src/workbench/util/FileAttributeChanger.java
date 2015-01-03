/*
 * FileAttributeChanger.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import workbench.log.LogMgr;


/**
 *
 * @author Thomas Kellerer
 */
public class FileAttributeChanger
{

	public void removeHidden(File dir)
	{
		if (PlatformHelper.isWindows())
		{
			boolean done = removeAttribute(dir);
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
		try
		{
			Path file = dir.toPath();
			Files.setAttribute(file, "dos:hidden", false, LinkOption.NOFOLLOW_LINKS);
			return true;
		}
		catch (Throwable th)
		{
			LogMgr.logWarning("FileAttributeChanger.removeAttribute()", "Could not remove hidden attribute", th);
			return false;
		}
	}

}
