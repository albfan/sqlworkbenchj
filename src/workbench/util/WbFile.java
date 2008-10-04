/*
 * WbFile.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;

/**
 * @author support@sql-workbench.net
 */
public class WbFile
	extends File
{
	
	public WbFile(String parent, String filename)
	{
		super(parent, filename);
	}
	
	public WbFile(File parent, String filename)
	{
		super(parent, filename);
	}
	
	public WbFile(File f)
	{
		super(f.getAbsolutePath());
	}
	
	public WbFile(String filename)
	{
		super(filename);
	}
	
	/**
	 * Renames this file by adding the current timestamp to the filename.
	 */
	public void makeBackup()
	{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
		String newname = this.getName() + "." + sdf.format(new java.util.Date());
		WbFile newfile = new WbFile(this.getParent(), newname);
		this.renameTo(newfile);
	}
	
	/**
	 * Returns the filename without an extension
	 */
	public String getFileName()
	{
		String name = getName();
		int pos = name.lastIndexOf('.');
		if (pos == -1) return name;
		return name.substring(0, pos);
	}
	
	public String getExtension()
	{
		String name = getName();
		int pos = name.lastIndexOf('.');
		if (pos == -1) return null;
		return name.substring(pos + 1);
	}
	
	public boolean isWriteable()
	{
		if (exists()) return canWrite();
		return canCreate();
	}
	
	public boolean canCreate()
	{
		try
		{
			tryCreate();
			return true;
		}
		catch (IOException e)
		{
			return false;
		}
	}
	
	public void tryCreate()
		throws IOException
	{
		FileOutputStream out = null;
		try
		{
			out = new FileOutputStream(this);
		}
		catch (IOException e)
		{
			throw e;
		}
		finally
		{
			FileUtil.closeQuitely(out);
			this.delete();
		}
	}
	
	/**
	 * Returns the canoncial name for this file
	 * @return the canonical filename or the absolute filename if getCanonicalPath threw an Exception
	 */
	public String getFullPath()
	{
		try
		{
			return this.getCanonicalPath();
		}
		catch (Throwable th)
		{
			return this.getAbsolutePath();
		}
	}

	public String toString()
	{
		return getFullPath();
	}
}
