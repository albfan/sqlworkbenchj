/*
 * DirectoryFileFilter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 *
 * @author  support@sql-workbench.net
 */
public class DirectoryFileFilter
	extends FileFilter
{
	private final String desc;
	public DirectoryFileFilter(String aDesc)
	{
		super();
		this.desc = aDesc;
	}

	public boolean accept(File f)
	{
		return f.isDirectory();
	}

	public String getDescription()
	{
		return this.desc;
	}
}
