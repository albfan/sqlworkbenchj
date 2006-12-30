/*
 * FileUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
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
import java.util.Iterator;
import java.util.List;
import workbench.resource.Settings;

/**
 * @author  support@sql-workbench.net
 */
public class FileUtil
{
	
	private static int getBuffSize()
	{
		return Settings.getInstance().getIntProperty("workbench.lob.buffsize",32*1024);
	}
	
	/*
	 * Expects instances of {@link workbench.util.CloseableDataStream} in the list
	 * and closes all of them
	 */
	public static void closeStreams(List streams)
	{
		if (streams == null) return;
		Iterator itr = streams.iterator();
		while (itr.hasNext())
		{
			CloseableDataStream str = (CloseableDataStream)itr.next();
			if (str != null) str.close();
		}
	}
	
	public static final int readLines(BufferedReader in, StringBuilder buffer, int numLines, String lineEnd)
		throws IOException
	{
		int lines = 0;
		String line = in.readLine();
		while (line != null && lines < numLines) 
		{
			buffer.append(line);
			buffer.append(lineEnd);
			lines ++;
			line = in.readLine();
		}
		if (line != null) 
		{
			// loop was ended due to numLines reached, so append the 
			// last line retrieved
			buffer.append(line);
			buffer.append(lineEnd);
		}
		return lines;
	}
	
	public static final String getLineEnding(Reader in)
		throws IOException
	{
		String ending = null;
		char c = (char)in.read();
		while (c != -1)
		{
			if (c == '\r')
			{ 
				char n = (char)in.read();
				ending = "\r\n";
				break;
			}
			else if (c == '\n')
			{
				ending = "\n";
				break;
			}
			c = (char)in.read();
		}
		return ending;
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
		if (size == 0) return 0;
		
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
				if (line == null) return i + 1;
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
		StringBuilder result = new StringBuilder(1024);
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
	
}
