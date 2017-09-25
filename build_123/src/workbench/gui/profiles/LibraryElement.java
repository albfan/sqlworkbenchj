/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */

package workbench.gui.profiles;


import workbench.gui.components.LibListUtil;

import workbench.util.WbFile;

/**
 *
 * @author Thomas Kellerer
 */
public class LibraryElement
{
	private final String fullPath;
	private String displayString;

	public LibraryElement(String filename)
	{
		this(new WbFile(filename));
	}

	public LibraryElement(WbFile file)
	{
		String fname = file.getName();
		if (fname.equalsIgnoreCase("rt.jar"))
		{
			// this is for the Look & Feel dialog and the JDBC/ODBC bridge driver
			// otherwise the "rt.jar" would be shown with a wrong file path
			displayString = fname;
			fullPath = fname;
		}
		else
		{
			LibListUtil util = new LibListUtil();
			WbFile realFile = util.replacePlaceHolder(file);

			// if replacePlaceHolder() returned the same file, no placeholder is present
			if (realFile == file)
			{
				if (file.isAbsolute())
				{
					fullPath = file.getFullPath();
				}
				else
				{
					// don't use getFullPath() on files that are not absolute filenames
					// otherwise driver templates that don't contain a path to the driver jar
					// would show up as defined in the current directory which is quite confusing.
					fullPath = file.getName();
				}

				if (file.exists())
				{
					displayString = fullPath;
				}
				else
				{
					displayString = "<html><span style='color:red'><i>" + fullPath + "</i></span></html>";
				}
			}
			else
			{
				// we can't use WbFile.getFullPath() or File.getAbsolutePath() due to the placeholder
				fullPath = file.getParent() + System.getProperty("file.separator") + file.getName();
				displayString = fullPath;
				if (!realFile.exists())
				{
					displayString = "<html><span style='color:red'><i>" + displayString + "</i></span></html>";
				}
			}
		}
	}

	public String getRealPath()
	{
		LibListUtil util = new LibListUtil();
		WbFile file = util.replacePlaceHolder(new WbFile(fullPath));
		return file.getFullPath();
	}

	public String getPath()
	{
		return fullPath;
	}

	@Override
	public String toString()
	{
		return displayString;
	}


}
