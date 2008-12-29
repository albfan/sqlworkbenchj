/*
 * 
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * Copyright 2002-2008, Thomas Kellerer
 * 
 * No part of this code maybe reused without the permission of the author
 * 
 * To contact the author please send an email to: support@sql-workbench.net
 * 
 */
package workbench.util;

import java.io.File;
import java.io.IOException;

/**
 * A class to create versioned backups of files up to a specified limit.
 *
 * @author Thomas Kellerer
 */
public class FileVersioner
{
	private static final char VERSION_SEPARATOR = '.';
	private final int maxVersions;


	/**
	 * Create a new FileVersioner with the max. number of backup files.
	 *
	 * @param maxCount
	 */
	public FileVersioner(int maxCount)
	{
		this.maxVersions = (maxCount > 0 ? maxCount : 5);
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
	 * 
	 * @param target
	 * @throws java.io.IOException
	 */
	public void createBackup(File target)
		throws IOException
	{
		if (target == null) return;
		if (!target.exists()) return;
		int nextVersion = findNextIndex(target);
		File backup = new File(target.getParentFile(), target.getName() + VERSION_SEPARATOR + nextVersion);
		FileUtil.copy(target, backup);
	}

	private int findNextIndex(File target)
	{
		File dir = target.getParentFile();
		String name = target.getName();
		if (!target.exists())	return 1;

		for (int index = 1; index <= maxVersions; index++)
		{
			File bck = new File(dir, name + "." + index);
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
		if (!target.exists()) return;

		File dir = target.getParentFile();
		String name = target.getName();

		File max = new File(dir, name + VERSION_SEPARATOR + '1');
		max.delete();
		
		for (int i = 2; i <= maxVersions; i++)
		{
			File old = new File(dir, name + VERSION_SEPARATOR + i);
			if (old.exists())
			{
				File newIndex = new File(dir, name + VERSION_SEPARATOR + (i - 1));
				old.renameTo(newIndex);
			}
		}
	}
}
