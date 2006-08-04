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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import workbench.resource.Settings;

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

	private static int getBuffSize()
	{
		return Settings.getInstance().getIntProperty("workbench.lob.buffsize",32*1024);
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

	public static long copy(InputStream in, OutputStream out)
	{
		long filesize = 0;
		try
		{
			byte[] buffer = new byte[getBuffSize()];
			int bytesRead = in.read(buffer);
			while (bytesRead != -1)
			{
				filesize += bytesRead;
				out.write(buffer, 0, bytesRead);
				bytesRead = in.read(buffer);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try { out.close(); } catch (Throwable th) {}
			try { in.close(); } catch (Throwable th) {}
		}
		return filesize;
	}

	public static String readCharacters(Reader in)
		throws IOException
	{
		if (in == null) return null;
		StringBuffer result = new StringBuffer(1024);
		char[] buff = new char[getBuffSize()];
		int bytesRead = in.read(buff);
		while (bytesRead > -1)
		{
			result.append(buff, 0, bytesRead);
			bytesRead = in.read(buff);
		}
		return result.toString();
	}
	
	public static byte[] readBytes(InputStream in)
		throws IOException
	{
		if (in == null) return null;
		ByteBuffer result = new ByteBuffer();
		byte[] buff = new byte[getBuffSize()];
		int bytesRead = in.read(buff);
		while (bytesRead > -1)
		{
			result.append(buff, 0, bytesRead);
			bytesRead = in.read(buff);
		}
		return result.getBuffer();
	}
	
	public static int countLines(File f)
		throws IOException
	{
		BufferedReader in = null;
		int lines = 0;
		try
		{
			in = new BufferedReader(new FileReader(f));
			String s = in.readLine();
			while (s != null)
			{
				lines ++;
				s = in.readLine();
			}
		}
		finally
		{
			try { in.close(); } catch (Throwable th) {}
		}
		return lines;
	}
	
}
