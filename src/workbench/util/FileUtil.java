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
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.List;
import workbench.log.LogMgr;

/**
 * @author  support@sql-workbench.net
 */
public class FileUtil
{
	private static final int BUFF_SIZE = 8192;
	
	/*
	 * Closes all streams in the list.
	 * @param a list of streams to close
	 */
	public static void closeStreams(List<Closeable> streams)
	{
		if (streams == null) return;

		for (Closeable str : streams)
		{
			try
			{
				if (str != null)
				{
					str.close();
				}
			}
			catch (IOException io)
			{
				LogMgr.logError("FileUtil.closeStreams()", "Error closing stream", io);
			}
		}
	}
	
	/**
	 * Read the contents of the Reader into the provided StringBuilder.
	 * Max. numLines lines are read.
	 * @param in the Reader to be used 
	 * @param buffer the StringBuilder to received the lines
	 * @param numLines the max. number of lines to be read
	 * @param lineEnd the lineEnding to be used
	 * @return the number of lines read
	 */
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

	/**
	 * Tries to estimate the number of records in the given file.
	 * This is done by reading the first <tt>sampleLines</tt> records
	 * of the file and assuming the average size of an row in the first
	 * lines is close to the average row in the complete file.
	 */
	public static final long estimateRecords(File f, long sampleLines)
		throws IOException
	{
		if (sampleLines <= 0) throw new IllegalArgumentException("Sample size must be greater then zero");
		if (!f.exists()) return -1;
		if (!f.isFile()) return -1;
		long size = f.length();
		if (size == 0) return 0;
		
		long lineSize = 0;

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

	/**
	 * Copies the content of the InputStream to the OutputStream.
	 * Both streams are closed automatically.
	 */
	public static long copy(InputStream in, OutputStream out)
		throws IOException
	{
		long filesize = 0;
		try
		{
			byte[] buffer = new byte[BUFF_SIZE];
			int bytesRead = in.read(buffer);
			while (bytesRead != -1)
			{
				filesize += bytesRead;
				out.write(buffer, 0, bytesRead);
				bytesRead = in.read(buffer);
			}
		}
		finally
		{
			try { out.close(); } catch (Throwable th) {}
			try { in.close(); } catch (Throwable th) {}
		}
		return filesize;
	}

	/**
	 * Read the content of the Reader into a String.
	 * The Reader is closed automatically.
	 */
	public static String readCharacters(Reader in)
		throws IOException
	{
		if (in == null) return null;
		StringBuilder result = new StringBuilder(1024);
		char[] buff = new char[BUFF_SIZE];
		int bytesRead = in.read(buff);
		try
		{
			while (bytesRead > -1)
			{
				result.append(buff, 0, bytesRead);
				bytesRead = in.read(buff);
			}
		}
		finally
		{
			try { in.close(); } catch (Throwable th) {}
		}
		return result.toString();
	}
	
	/**
	 * Read the content of the InputStream into a ByteArray.
 * The InputStream is closed automatically.
	 */
	public static byte[] readBytes(InputStream in)
		throws IOException
	{
		if (in == null) return null;
		ByteBuffer result = new ByteBuffer();
		byte[] buff = new byte[BUFF_SIZE];	

		try
		{
			int bytesRead = in.read(buff);

			while (bytesRead > -1)
			{
				result.append(buff, 0, bytesRead);
				bytesRead = in.read(buff);
			}
		}
		finally
		{
			try { in.close(); } catch (Throwable th) {}
		}
		return result.getBuffer();
	}
	
}
