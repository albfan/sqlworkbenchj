/*
 * PkMapping.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.CollectionUtil;
import workbench.util.FileUtil;
import workbench.util.StringUtil;

/**
 * A class to hold user-defined primary key mappings for tables (or views)
 * @author Thomas Kellerer
 */
public class PkMapping
{
	private final Map<String, String> columnMapping = new HashMap<>();

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

	/**
	 * For testing purposes only
	 * @param source
	 */
	PkMapping(String source)
	{
		loadMapping(source);
	}

	private PkMapping()
	{
		String filename = Settings.getInstance().getPKMappingFilename();
		loadMapping(filename);
	}

	public synchronized void clear()
	{
		if (columnMapping != null)
		{
			columnMapping.clear();
		}
	}
	public synchronized String getMappingAsText()
	{
		if (CollectionUtil.isEmpty(this.columnMapping)) return null;

		StringBuilder result = new StringBuilder(this.columnMapping.size() * 50);
		Iterator<Entry<String, String>> itr = this.columnMapping.entrySet().iterator();
		while (itr.hasNext())
		{
			Map.Entry entry = itr.next();
			result.append(entry.getKey());
			result.append('=');
			result.append(entry.getValue());
			result.append('\n');
		}
		return result.toString();
	}

	public final synchronized void loadMapping(String filename)
	{
		if (filename == null) return;
		Properties props = new Properties();

		File f = new File(filename);
		if (!f.exists())
		{
			LogMgr.logWarning("PkConfig.readMappingFile()", "Mapping file '" + filename + "' not found! Please check workbench.settings", null);
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
			this.columnMapping.clear();
		}
		finally
		{
			FileUtil.closeQuietely(in);
		}

		LogMgr.logInfo("PkMapping.readMappingFile()", "Using PK mappings from " + f.getAbsolutePath());

		Iterator<Entry<Object, Object>> itr = props.entrySet().iterator();
		while (itr.hasNext())
		{
			Entry<Object, Object> entry = itr.next();
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

	/**
	 * Defines a PK mapping for the specified table name
	 */
	public synchronized void addMapping(String table, String columns)
	{
		if (!StringUtil.isEmptyString(table) && !StringUtil.isEmptyString(columns))
		{
			this.columnMapping.put(table.toLowerCase(), columns);
		}
	}

	public synchronized List<String> getPKColumns(TableIdentifier tbl)
	{
		if (this.columnMapping == null) return null;
		String tname = tbl.getTableName().toLowerCase();

		String columns = this.columnMapping.get(tname);
		if (columns == null)
		{
			String fullname = tbl.getTableExpression().toLowerCase();
			columns = this.columnMapping.get(fullname);
		}
		List<String> cols = null;
		if (columns != null)
		{
			cols = StringUtil.stringToList(columns, ",", true, true);
			LogMgr.logInfo("PkMapping.getPKColumns()", "Using PK Columns [" + columns + "]" + " for table [" + tbl.getTableExpression() + "]");
		}
		return cols;
	}

	public synchronized Map<String, String> getMapping()
	{
		if (this.columnMapping == null) return Collections.emptyMap();
		return Collections.unmodifiableMap(this.columnMapping);
	}

	public synchronized void saveMapping(String filename)
	{
		if (this.columnMapping == null) return;
		BufferedWriter out = null;
		try
		{
			Iterator<Entry<String, String>> itr = this.columnMapping.entrySet().iterator();
			File f = new File(filename);
			out = new BufferedWriter(new FileWriter(f));
			out.write("# Primary key mapping for " + ResourceMgr.TXT_PRODUCT_NAME);
			out.newLine();
			while (itr.hasNext())
			{
				Map.Entry<String, String> entry = itr.next();
				String table = entry.getKey();
				String cols = entry.getValue();
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
			FileUtil.closeQuietely(out);
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
