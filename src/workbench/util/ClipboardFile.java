/*
 * ClipboardFile.java
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

import java.io.File;
import java.io.IOException;

/**
 * @author support@sql-workbench.net
 */
public class ClipboardFile
	extends File
{
	
	private String buffer;
	
	public ClipboardFile(String contents)
	{
		super("Clipboard");
		buffer = contents;
	}

	public String getContents() 
	{
		return this.buffer;
	}
	
	public boolean canRead() { return true; }
	public boolean canWrite() { return false; }
	public boolean delete() { return false; }
	public String getAbsolutePath() { return "Clipboard"; }
	public File getAbsoluteFile() {  return this; }
	public String getName() { return "Clipboard"; }
	public boolean isDirectory() { return false; }
	public boolean isHidden() { return false; }
	public boolean exists() { return true; }
	public String getParent() { return null; }
	public File getParentFile() { return null; }
	public String getPath() { return getAbsolutePath(); }

	public String getCanonicalPath() 
		throws IOException
	{ 
		return getAbsolutePath(); 
	}
	
	public File getCanonicalFile() 
		throws IOException
	{
		return this;
	}

	public boolean createNewFile() 
		throws IOException
	{
		return false;
	}

	public int compareTo(File pathname) { return -1; }
	public boolean equals(Object obj) { return false; }

	public long length() 
	{
		if (this.buffer == null) return 0;
		return this.buffer.length();
	}
	
	public int hashCode()
	{
		if (buffer == null) return 0;
		return buffer.hashCode();
	}

	public static void main(String[] args)
	{
		try
		{
			ClipboardFile f = new ClipboardFile("Bla");
			ZipUtil.isZipFile(f);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
