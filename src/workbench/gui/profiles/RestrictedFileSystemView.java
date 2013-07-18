/*
 * RestrictedFileSystemView.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013 Thomas Kellerer.
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

import java.io.File;
import java.io.IOException;

import javax.swing.filechooser.FileSystemView;

/**
 * Based on: http://stackoverflow.com/questions/32529
 * @author Thomas Kellerer
 */
class RestrictedFileSystemView
	extends FileSystemView
{
	private final File rootDir;

	RestrictedFileSystemView(File rootDirectory)
	{
		this.rootDir = rootDirectory;
	}

	@Override
	public File createNewFolder(File containingDir)
		throws IOException
	{
		throw new UnsupportedOperationException("Unable to create directory");
	}

	@Override
	public File[] getRoots()
	{
		return new File[] {	rootDir	};
	}

	@Override
	public boolean isRoot(File file)
	{
		return rootDir.equals(file);
	}

	@Override
	public File getHomeDirectory()
	{
		return rootDir;
	}

	@Override
	public File getDefaultDirectory()
	{
		return rootDir;
	}

	@Override
	public Boolean isTraversable(File f)
	{
		if (f.isDirectory())
		{
			return Boolean.valueOf(f.equals(rootDir));
		}
		return Boolean.FALSE;
	}

}
