/*
 * PkMapping.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.StringUtil;

/**
 * A class to hold user-defined primary key mappings for tables (or views)
 * @author support@sql-workbench.net
 */
public class PkMapping
{
	private HashMap columnMapping;
	
	private static PkMapping instance;
	
	public static synchronized boolean isInitialized()
	{
		return instance != null;
	}
	
	public static synchronized PkMapping getInstance()
	{
		if (instance == null)
		{
			instance = new PkMapping();
		}
		return instance;
	}
	
	private PkMapping()
	{
		String filename = Settings.getInstance().getPKMappingFilename();
		loadMapping(filename);
	}

	public synchronized String getMappingAsText()
	{
		if (this.columnMapping == null) return null;
		if (this.columnMapping.size() == 0) return null;
		
		StringBuilder result = new StringBuilder(this.columnMapping.size() * 50);
		Iterator itr = this.columnMapping .entrySet().iterator();
		while (itr.hasNext())
		{
			Map.Entry entry = (Map.Entry)itr.next();
			result.append(entry.getKey().toString() + "=" + (String)entry.getValue());
			result.append("\n");
		}
		return result.toString();
	}
	
	public synchronized void loadMapping(String filename)
	{
		if (filename == null) return;
		Properties props = new Properties();
		
		File f = new File(filename);
		if (!f.exists())
		{
			LogMgr.logError("PkConfig.readMappingFile()", "Mapping file '" + filename + "' not found! Please check workbench.settings", null);
			return;
		}
		InputStream in = null;
		try
		{
			in = new BufferedInputStream(new FileInputStream(filename));
			props.load(in);
		}
		catch (Exception e)
		{
			LogMgr.logError("PkMapping.readMappingFile()", "Error reading mapping file", e);
			this.columnMapping = null;
		}
		finally
		{
			try { in.close(); } catch (Throwable th) {}
		}
		
		LogMgr.logInfo("PkMapping.readMappingFile()", "Using PK mappings from " + f.getAbsolutePath());
		
		this.columnMapping = new HashMap(props.size());
		Iterator itr = props.entrySet().iterator();
		while (itr.hasNext())
		{
			Map.Entry entry = (Map.Entry)itr.next();
			String table = (String)entry.getKey();
			String columns = (String)entry.getValue();
			if (!StringUtil.isEmptyString(columns)) 
			{
				this.columnMapping.put(table, columns);
			}
		}
	}

	public synchronized void removeMapping(WbConnection con, String table)
	{
		if (this.columnMapping == null) return;
		this.columnMapping.remove(table);
	}
	
	public synchronized void addMapping(TableIdentifier table, String columns)
	{
		addMapping(table.getTableExpression(), columns);
	}
	
	public synchronized void addMapping(String table, String columns)
	{
		if (this.columnMapping == null) this.columnMapping = new HashMap();
		if (!StringUtil.isEmptyString(table) && !StringUtil.isEmptyString(columns))
		{
			this.columnMapping.put(table.toLowerCase(), columns);
		}
	}
	
	public synchronized Collection getPKColumns(WbConnection con, TableIdentifier tbl)
	{
		if (this.columnMapping == null) return null;
		String columns = (String) this.columnMapping.get(tbl.getTableName().toLowerCase());
		if (columns == null)
		{
			columns = (String)this.columnMapping.get(tbl.getTableExpression(con).toLowerCase());
		}
		List cols = null;
		if (columns != null)
		{
			cols = StringUtil.stringToList(columns, ",", true, true);
			LogMgr.logInfo("PkMapping.getPKColumns()", "Using PK Columns [" + columns + "]" + " for table [" + tbl.getTableExpression() + "]");
		}
		return cols;
	}

	public synchronized Map getMapping()
	{
		if (this.columnMapping == null) return Collections.EMPTY_MAP;
		return Collections.unmodifiableMap(this.columnMapping);
	}
	
	public synchronized void saveMapping(String filename)
	{
		if (this.columnMapping == null) return;
		BufferedWriter out = null;
		try
		{
			Iterator itr = this.columnMapping.entrySet().iterator();
			File f = new File(filename);
			out = new BufferedWriter(new FileWriter(f));
			out.write("# Primary key mapping for " + ResourceMgr.TXT_PRODUCT_NAME);
			out.newLine();
			while (itr.hasNext())
			{
				Map.Entry entry = (Map.Entry)itr.next();
				String table = (String)entry.getKey();
				String cols = (String)entry.getValue();
				out.write(table);
				out.write('=');
				out.write(cols);
				out.newLine();
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("PkMapping.saveMapping()", "Error saving mapping to properties file", e);
		}
		finally
		{
			try { out.close(); } catch (Throwable th) {}
		}
	}
	
	public synchronized void addMapping(TableIdentifier table, ColumnIdentifier[] cols)
	{
		StringBuilder colNames = new StringBuilder(50);
		for (int i = 0; i < cols.length; i++)
		{
			if (cols[i].isPkColumn())
			{
				if (colNames.length() > 0) colNames.append(',');
				colNames.append(cols[i].getColumnName());
			}
		}
		if (colNames.length() > 0)
		{
			this.addMapping(table, colNames.toString());
		}
	}
}
