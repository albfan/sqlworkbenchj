/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer.
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

package workbench.gui.profiles;

import workbench.resource.Settings;
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
			// this is for the Look & Feel dialog
			// otherwise the "rt.jar" would be shown with a wrong file path
			displayString = fname;
			fullPath = fname;
		}
		else
		{
			String libdir = Settings.getInstance().getProperty(Settings.PROP_LIBDIR, null);
			String dir = file.getParent();
			if (libdir != null && dir != null && dir.toLowerCase().contains(Settings.LIB_DIR_KEY.toLowerCase()))
			{
				displayString = Settings.LIB_DIR_KEY + "/" + fname;
				fullPath = Settings.LIB_DIR_KEY + "/" + fname;
				WbFile f = new WbFile(Settings.getInstance().getLibDir(), fname);
				if (!f.exists())
				{
					displayString = "<html><span style='color:red'><i>" + displayString + "</i></span></html>";
				}
			}
			else
			{
				fullPath = file.getFullPath();
				if (file.exists())
				{
					displayString = fullPath;
				}
				else
				{
					displayString = "<html><span style='color:red'><i>" + fullPath + "</i></span></html>";
				}
			}
		}
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
