/*
 * FileNameSorter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.importer;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
	private List<TableIdentifier> tables;
	private WbConnection dbConn;
	private TablenameResolver resolver;
	
	public FileNameSorter(WbConnection con, File sourceDir, final String extension, TablenameResolver resolve)
	{
		toProcess = new ArrayList<WbFile>();
		this.resolver = resolve;
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
	
	public List<TableIdentifier> getTableList()
	{
		return Collections.unmodifiableList(tables);
	}
	
	public List<WbFile> getFiles()
	{
		return toProcess;
	}
	
	public List<WbFile> getSortedList()
		throws CycleErrorException
	{
		Map<String, WbFile> fileMapping = new HashMap<String, WbFile>(toProcess.size());
		
		tables = new LinkedList<TableIdentifier>();
		for (WbFile f : toProcess)
		{
			String tablename = this.resolver.getTableName(f);
			tables.add(new TableIdentifier(tablename));
			fileMapping.put(tablename.toLowerCase(), f);
		}
		
		TableDependencySorter sorter = new TableDependencySorter(dbConn);
		List<TableIdentifier> sorted = sorter.sortForInsert(tables);
		if (sorter.hasErrors())
		{
			throw new CycleErrorException(sorter.getErrorTables().get(0));
		}
		
		List<WbFile> result = new LinkedList<WbFile>();
		for (TableIdentifier tbl : sorted)
		{
			String t = tbl.getTableName().toLowerCase();
			WbFile f = fileMapping.get(t);
			if (f != null)
			{
				result.add(f);
			}
		}
		return result;
	}
}
