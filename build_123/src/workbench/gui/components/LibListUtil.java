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

package workbench.gui.components;


import java.io.File;

import workbench.resource.Settings;

import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 *
 * @author Thomas Kellerer
 */
public class LibListUtil
{
	private final String libDir;

	public LibListUtil()
	{
		this(Settings.getInstance().getProperty(Settings.PROP_LIBDIR, null));
	}

	public LibListUtil(String dir)
	{
		if (StringUtil.isBlank(dir))
		{
			this.libDir = null;
		}
		else
		{
			this.libDir = dir;
		}
	}

	public WbFile replacePlaceHolder(WbFile file)
	{
		if (libDir == null) return file;

		String dir = file.getParent();
		if (dir != null && dir.toLowerCase().contains(Settings.LIB_DIR_KEY.toLowerCase()))
		{
			String fullpath = dir.replace(Settings.LIB_DIR_KEY, libDir);
			return new WbFile(fullpath, file.getName());
		}
		else
		{
			return file;
		}
	}

	public WbFile replaceLibDir(WbFile file)
	{
		if (libDir == null) return file;

		WbFile lib = new WbFile(libDir);

		File fileDir = file.getParentFile();
		while (!fileDir.equals(lib))
		{
			fileDir = fileDir.getParentFile();
			if (fileDir == null) break;
		}
		if (fileDir == null) return file;
		String fpath = file.getAbsolutePath().replace(fileDir.getAbsolutePath(), Settings.LIB_DIR_KEY);
		return new WbFile(fpath);
	}

}
