/*
 * ZipUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Thomas Kellerer
 */
public class ZipUtil
{

	/**
	 * Test if the given File is a ZIP Archive.
	 * @param f the File to test
	 * @return true if the file is a ZIP Archive, false otherwise
	 */
	public static boolean isZipFile(File f)
	{
		// The JVM crashes (sometimes) if I pass my "fake" ClipboardFile object
		// to the ZipFile constructor, so this is checked beforehand
		if (f instanceof ClipboardFile) return false;

		if (!f.exists()) return false;
		
		boolean isZip = false;
		
		InputStream in = null;
		try
		{
			in = new FileInputStream(f);
			byte[] buffer = new byte[4];
			int bytes = in.read(buffer);
			if (bytes == 4)
			{
				isZip = (buffer[0] == 'P' && buffer[1] == 'K');
				isZip = isZip && (buffer[2] == 3 && buffer[3] == 4);
			}
		}
		catch (Throwable e)
		{
			isZip = false;
		}
		finally
		{
			try { in.close(); } catch (Throwable th) {}
		}
		return isZip;
	}
	
	/**
	 * Get the directory listing of a zip archive. 
	 * Sub-Directories are not scanned.
	 * 
	 * @param archive
	 * @return a list of filenames contained in the archive
	 */
	public static List<String> getFiles(File archive)
		throws IOException
	{
		ZipFile zip = new ZipFile(archive);
		List<String> result = new ArrayList<String>(zip.size());
		
		try
		{
			Enumeration<? extends ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements())
			{
				ZipEntry entry = entries.nextElement();
				result.add(entry.getName());
			}
		}
		finally
		{
			zip.close();
		}
		return result;
	}

	public static void closeQuitely(ZipFile file)
	{
		if (file == null) return;
		try
		{
			file.close();
		}
		catch (Throwable th)
		{
			// ignore
		}
	}
}
