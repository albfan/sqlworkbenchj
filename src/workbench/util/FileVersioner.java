/*
 * FileVersioner.java
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
package workbench.util;

import java.io.File;
import java.io.IOException;

import workbench.log.LogMgr;
import workbench.resource.Settings;

/**
 * A class to create versioned backups of files up to a specified limit.
 *
 * @author Thomas Kellerer
 */
public class FileVersioner
{
	private String versionSeparator = "."; // should be compatible with all file systems
	private final int maxVersions;
	private File backupDir;

	/**
	 * Create a FileVersioner that saves the backup in the same
	 * directory as the target file.
	 *
	 * @param maxCount max. number of backups to maintain
	 */
	public FileVersioner(int maxCount)
	{
		this(maxCount, null, ".");
	}

	/**
	 * Create a new FileVersioner that saves the backup files in a specific directory
	 * <br/>
	 * If the backup directory is not an absolute pathname, then it's
	 * considered relative to the config directory.
	 *
	 * @param maxCount max. number of backups to maintain
	 * @param dirName  the directory where to save the backup files
	 * @param separator the character to put before the version number. Only the first character is used
	 * @see Settings#getConfigDir()
	 */
	public FileVersioner(int maxCount, String dirName, String separator)
	{
		this.maxVersions = (maxCount > 0 ? maxCount : 5);
		if (StringUtil.isNonBlank(dirName))
		{
			backupDir = new File(dirName);
			if (!backupDir.isAbsolute())
			{
				backupDir = new File(Settings.getInstance().getConfigDir(), dirName);
			}
		}
		if (StringUtil.isNonBlank(separator))
		{
			versionSeparator = separator;
		}
	}

	/**
	 * Create a versioned backup of the specified file.
	 * <br/>
	 * If the max. number of versions has not yet been reached for the given
	 * file, this method will simply create a new version (highest number is
	 * the newest version).
	 * <br/>
	 * File versions will be appended to the input filename (myfile.txt -> myfile.txt.1).
	 * <br/>
	 * If the max. number of versions is reached, the oldest version (version #1) will
	 * be deleted, and the other versions will be renamed (2 -> 1, 3 -> 2, and so on).
	 * <br/>
	 * Then the new version will be created.
	 * The backup file will be stored in the directory specified in the constructor,
	 * or the directory of the file that is backed up (if no backup directory was
	 * specified)
	 *
	 * @param toBackup the file to backup
	 * @return the complete filename of the backup
	 * @throws java.io.IOException
	 */
	public File createBackup(File toBackup)
		throws IOException
	{
		if (toBackup == null) return null;
		if (!toBackup.exists()) return null;

		int nextVersion = findNextIndex(toBackup);
		File dir = getTargetDir(toBackup);
		if (dir == null)
		{
			LogMgr.logWarning("FileVersioner.createBackup()", "Could not determine target directory. Using current directory");
			dir = new File(".");
		}

		if (!dir.exists())
		{
			if (!dir.mkdirs())
			{
				LogMgr.logError("FileVersioner.createBackup()", "Could not create backup dir: " + dir.getAbsolutePath() + ", using workspace directory: " + toBackup.getParentFile().getAbsolutePath(), null);
				dir = toBackup.getParentFile();
			}
		}
		File backup = new File(dir, toBackup.getName() + versionSeparator + nextVersion);
		FileUtil.copy(toBackup, backup);
		LogMgr.logDebug("FileVersioner.createBackup()", "Created backup file: " + backup.getAbsolutePath());
		return backup;
	}

	private File getTargetDir(File target)
	{
		if (backupDir != null) return backupDir;
		return target.getAbsoluteFile().getParentFile();
	}

	private int findNextIndex(File target)
	{
		File dir = getTargetDir(target);
		String name = target.getName();
		if (!target.exists())	return 1;

		for (int index = 1; index <= maxVersions; index++)
		{
			File bck = new File(dir, name + versionSeparator + index);
			if (!bck.exists())
			{
				return index;
			}
		}
		slideVersions(target);
		return maxVersions;
	}

	private void slideVersions(File target)
	{
		File dir = getTargetDir(target);
		String name = target.getName();

		File max = new File(dir, name + versionSeparator + '1');
		max.delete();

		for (int i = 2; i <= maxVersions; i++)
		{
			File old = new File(dir, name + versionSeparator + i);
			if (old.exists())
			{
				File newIndex = new File(dir, name + versionSeparator + (i - 1));
				old.renameTo(newIndex);
			}
		}
	}
}
