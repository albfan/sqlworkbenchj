/*
 * FileNameSorter.java
 * 
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author.
 * 
 * To contact the author please send an email to: support@sql-workbench.net
 */

package workbench.db.importer;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.util.WbFile;

/**
 *
 * @author support@sql-workbench.net
 */
public class FileNameSorter 
{
	private List<WbFile> toProcess;
	private WbConnection dbConn;
	
	public FileNameSorter(WbConnection con, File sourceDir, final String extension)
	{
		toProcess = new ArrayList<WbFile>();
		dbConn = con;
		FileFilter ff = new FileFilter()
		{
			public boolean accept(File pathname)
			{
				if (pathname.isDirectory()) return false;
				String fname = pathname.getName();
				if (fname == null) return false;
				return (fname.toLowerCase().endsWith(extension.toLowerCase()));
			}
		};
		
		File[] files = sourceDir.listFiles(ff);
		
		for (File f : files)
		{
			toProcess.add(new WbFile(f));
		}
	}
	
	public List<WbFile> getFiles()
	{
		return toProcess;
	}
	
	public List<WbFile> getSortedList()
	{
		List<TableIdentifier> tables = new ArrayList<TableIdentifier>(toProcess.size());
		for (WbFile f : toProcess)
		{
			tables.add(new TableIdentifier(f.getFileName()));
		}
		
		TableDependencySorter sorter = new TableDependencySorter(dbConn);
		List<TableIdentifier> sorted = sorter.sortForInsert(tables);
		
		List<WbFile> result = new LinkedList<WbFile>();
		for (TableIdentifier tbl : sorted)
		{
			File f = this.findFile(tbl.getTableName());
			if (f != null)
			{
				result.add(new WbFile(f));
			}
		}
		return result;
	}
	
	private File findFile(String tablename)
	{
		for (WbFile f : toProcess)
		{
			if (f.getFileName().equalsIgnoreCase(tablename)) return f;
		}
		return null;
	}
}
