/*
 * WbFile.java
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
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author support@sql-workbench.net
 */
public class WbFile
	extends File
{
	
	public WbFile(File f)
	{
		super(f.getAbsolutePath());
	}
	
	public WbFile(String filename)
	{
		super(filename);
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
			try	{ out.close(); } catch (Throwable th) {}
			this.delete();
		}
	}
}
