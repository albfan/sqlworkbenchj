/*
 * ImportFileLister.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.util.CollectionUtil;
import workbench.util.WbFile;

/**
 *
 * @author Thomas Kellerer
 */
public class ImportFileLister
{
	private List<WbFile> toProcess;
	private List<TableIdentifier> tables;
	private WbConnection dbConn;
	private TablenameResolver resolver;
	private boolean ignoreSchema;
	private WbFile sourceDir;
	private boolean checkDependencies;
	private String extension;

	public ImportFileLister(WbConnection con, File dir, final String ext)
	{
		if (!dir.isDirectory()) throw new IllegalArgumentException(dir + " is not a directory");

		toProcess = new ArrayList<WbFile>();
		dbConn = con;
		sourceDir = new WbFile(dir);
		extension = ext;
		FileFilter ff = new FileFilter()
		{
			@Override
			public boolean accept(File pathname)
			{
				if (pathname.isDirectory()) return false;
				String fname = pathname.getName();
				if (fname == null) return false;
				return fname.toLowerCase().endsWith(ext.toLowerCase());
			}
		};

		File[] files = sourceDir.listFiles(ff);

		for (File f : files)
		{
			if (f.length() > 0)
			{
				toProcess.add(new WbFile(f));
			}
			else
			{
				LogMgr.logWarning("ImportFileLister.<init>", "Ignoring empty file: " + f.getAbsolutePath());
			}
		}

		// Cleanup possible _lob files
		Iterator<WbFile> itr = toProcess.iterator();
		while (itr.hasNext())
		{
			WbFile f = itr.next();
			String fname = f.getFileName();
			if (fname.endsWith("_lobs"))
			{
				WbFile basefile = new WbFile(f.getParent(), f.getName().replace("_lobs", ""));
				if (toProcess.contains(basefile))
				{
					LogMgr.logDebug("ImportFileLister.<init>", "Ignoring lob file: " + f.getFullPath());
					itr.remove();
				}
			}
		}
	}

	public void setCheckDependencies(boolean flag)
	{
		this.checkDependencies = flag;
	}

	public String getExtension()
	{
		return this.extension;
	}

	/**
	 * Specify the Resolver that returns the table name for a filename
	 */
	public void setTableNameResolver(TablenameResolver resolve)
	{
		this.resolver = resolve;
	}

  public boolean containsFiles()
  {
    return toProcess != null && toProcess.size() > 0;
  }

	public String getDirectory()
	{
		if (this.sourceDir == null) return null;
		return this.sourceDir.getFullPath();
	}

	/**
	 * Removes any file that contains the given string
	 *
	 * @param containedNames
	 */
	public void ignoreFiles(List<String> containedNames)
	{
		if (CollectionUtil.isEmpty(containedNames)) return;

		Iterator<WbFile> itr = toProcess.iterator();
		Set<WbFile> toRemove = new TreeSet<WbFile>();
		while (itr.hasNext())
		{
			WbFile f = itr.next();
			String fname = f.getName();
			for (String contained : containedNames)
			{
				if (fname.indexOf(contained) > -1)
				{
					LogMgr.logDebug("ImportFileLister.<init>", "Ignoring file: " + f.getFullPath());
					toRemove.add(f);
				}
			}
		}
		toProcess.removeAll(toRemove);
	}

	public void setIgnoreSchema(boolean flag)
	{
		this.ignoreSchema = flag;
	}

	/**
	 * Return the list of tables to be processed if checkDependencies was enabled.
	 * If checkDependencies is not enabled, this will return <tt>null</tt>
	 */
	public List<TableIdentifier> getTableList()
	{
		// This will only return a list of tables, if check
		if (tables == null) return null;
		return Collections.unmodifiableList(tables);
	}

	public List<WbFile> getFiles()
		throws CycleErrorException
	{
		if (this.checkDependencies)
		{
			return getSortedList();
		}
		else
		{
			return toProcess;
		}
	}

	public TableIdentifier getTableForFile(WbFile file)
	{
		String tablename = this.resolver.getTableName(file);
		TableIdentifier tbl = new TableIdentifier(tablename);
		if (ignoreSchema)
		{
			tbl.setSchema(null);
			tbl.setCatalog(null);
		}
		return tbl;
	}

	protected List<WbFile> getSortedList()
		throws CycleErrorException
	{
		Map<String, WbFile> fileMapping = new HashMap<String, WbFile>(toProcess.size());

		tables = new LinkedList<TableIdentifier>();
		for (WbFile f : toProcess)
		{
			TableIdentifier tbl = getTableForFile(f);
			tables.add(tbl);
			fileMapping.put(tbl.getTableExpression().toLowerCase(), f);
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
			String t = tbl.getTableExpression().toLowerCase();
			WbFile f = fileMapping.get(t);
			if (f != null)
			{
				result.add(f);
			}
		}
		return result;
	}
}
