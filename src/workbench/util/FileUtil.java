/*
 * FileUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author  support@sql-workbench.net
 */
public class FileUtil
{

	public static final long estimateRecords(String filename)
		throws IOException
	{
		return estimateRecords(filename, 5);
	}

	public static final long estimateRecords(String filename, long sampleLines)
		throws IOException
	{
		File f = new File(filename);
		return estimateRecords(f,sampleLines);
	}

	public static final long estimateRecords(File f)
		throws IOException
	{
		return estimateRecords(f, 5);
	}

	public static final long estimateRecords(File f, long sampleLines)
		throws IOException
	{
		if (!f.exists()) return -1;
		if (!f.isFile()) return -1;
		long size = f.length();
		long lineSize = 0;
		if (sampleLines <= 0) throw new IllegalArgumentException("Sample size must be greater then zero");

		BufferedReader in = null;
		try
		{
			in = new BufferedReader(new FileReader(f), 8192);
			in.readLine(); // skip the first line
			int lfSize = StringUtil.LINE_TERMINATOR.length();
			for (int i=0; i < sampleLines; i++)
			{
				String line = in.readLine();
				if (line == null) break;
				lineSize += (line.length() + lfSize);
			}
		}
		finally
		{
			try { in.close(); } catch (Throwable th) { }
		}
		return (size / (lineSize / sampleLines));

	}

}
