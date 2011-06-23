/*
 * ClipboardFile.java
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
import java.io.IOException;

/**
 * @author Thomas Kellerer
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

	@Override
	public boolean canRead()
	{
		return true;
	}

	@Override
	public boolean canWrite()
	{
		return false;
	}

	@Override
	public boolean delete()
	{
		return false;
	}

	@Override
	public String getAbsolutePath()
	{
		return "Clipboard";
	}

	@Override
	public File getAbsoluteFile()
	{
		return this;
	}

	@Override
	public String getName()
	{
		return "Clipboard";
	}

	@Override
	public boolean isDirectory()
	{
		return false;
	}

	@Override
	public boolean isHidden()
	{
		return false;
	}

	@Override
	public boolean exists()
	{
		return true;
	}

	@Override
	public String getParent()
	{
		return null;
	}

	@Override
	public File getParentFile()
	{
		return null;
	}

	@Override
	public String getPath()
	{
		return getAbsolutePath();
	}

	@Override
	public String getCanonicalPath()
		throws IOException
	{
		return getAbsolutePath();
	}

	@Override
	public File getCanonicalFile()
		throws IOException
	{
		return this;
	}

	@Override
	public boolean createNewFile()
		throws IOException
	{
		return false;
	}

	@Override
	public int compareTo(File pathname)
	{
		return -1;
	}

	@Override
	public boolean equals(Object obj)
	{
		return false;
	}

	@Override
	public long length()
	{
		if (this.buffer == null)
		{
			return 0;
		}
		return this.buffer.length();
	}

	@Override
	public int hashCode()
	{
		if (buffer == null)
		{
			return 0;
		}
		return buffer.hashCode();
	}
}
