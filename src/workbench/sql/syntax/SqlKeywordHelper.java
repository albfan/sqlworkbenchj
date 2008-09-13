/*
 * SqlKeywordHelper.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.syntax;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.CaseInsensitiveComparator;
import workbench.util.FileUtil;
import workbench.util.StringUtil;

/**
 * Manage SQL keywords to support built-in keywords and user-defined keywords.
 * By default the files 
 * <ul>
 *   <li>keywords.wb (general SQL keywords)</li>
 *   <li>datatypes.wb (SQL datatypes)</li>
 *   <li>functions.wb (SQL functions)</li>
 *   <li>operators.wb (SQL operators)</li>
 * </ul>
 * are read from this package. 
 * If any of those files exist in the config directory, their contents
 * is read as well and merged with the predefined keywords.
 * @author support@sql-workbench.net
 */
public class SqlKeywordHelper 
{
	private String dbId;
	private CaseInsensitiveComparator comparator = new CaseInsensitiveComparator();
	private Set<String> keywords;
	private Set<String> operators;
	private Set<String> functions;
	private Set<String> datatypes;
	
	public SqlKeywordHelper()
	{
		this(null);
	}

	public SqlKeywordHelper(String id)
	{
		this.dbId = id;
	}

	public Set<String> getKeywords()
	{
		if (keywords == null)
		{
			keywords = loadKeywordsFromFile("keywords.wb");
			if (this.dbId != null)
			{
				// old way of customizing DB specific keywords
				String key = "workbench.db." + dbId + ".syntax.keywords";
				List<String> addwords = StringUtil.stringToList(Settings.getInstance().getProperty(key, ""), ",", true, true);
				keywords.addAll(addwords);
			}
		}
		return keywords;
	}
	
	public Set<String> getDataTypes()
	{
		if (datatypes == null)
		{
			datatypes = loadKeywordsFromFile("datatypes.wb");
		}
		return datatypes;
	}
	
	public Set<String> getOperators()
	{
		if (operators == null)
		{
			operators = loadKeywordsFromFile("operators.wb");
		}
		return operators;
	}
	
	public Set<String> getSqlFunctions()
	{
		if (functions == null)
		{
			functions = loadKeywordsFromFile("functions.wb");
			if (this.dbId != null)
			{
				// old way of customizing DB specific functions
				String key = "workbench.db." + dbId + ".syntax.functions";
				List<String> addfuncs = StringUtil.stringToList(Settings.getInstance().getProperty(key, ""), ",", true, true);
				functions.addAll(addfuncs );
			}
		}
		return functions;
	}

	public boolean isFunction(String func)
	{
		return getSqlFunctions().contains(func);
	}

	public boolean isKeyword(String word)
	{
		return getKeywords().contains(word);
	}

	private Set<String> loadKeywordsFromFile(String filename)
	{
		Set<String> result = readFile(filename);
		if (this.dbId != null)
		{
			Set<String> dbms = readFile(this.dbId + "." + filename);
			if (dbms != null)
			{
				result.addAll(dbms);
			}
		}
		return result;
	}

	private Set<String> readFile(String filename)
	{
		// First read the built-in functions
		InputStream s = SqlKeywordHelper.class.getResourceAsStream(filename);
		Set<String> result = new TreeSet<String>(comparator);
		if (s != null)
		{
			BufferedReader in = new BufferedReader(new InputStreamReader(s));
			Collection<String> builtin = FileUtil.getLines(in, true);
			result.addAll(builtin);
		}
		
		// Try to read the file in the current directory.
		File f = new File(filename);
		if (!f.exists())
		{
			// nothing in the current directory, try the config dir
			f = new File(Settings.getInstance().getConfigDir(), filename);
		}

		if (f.exists())
		{
			try
			{
				BufferedReader customFile = new BufferedReader(new FileReader(f));
				Collection<String> custom = FileUtil.getLines(customFile, true);
				result.addAll(custom);
			}
			catch (Exception e)
			{
				LogMgr.logError("SqlKeywordHelper.loadKeywordsFromfile()", "Error reading external file", e);
			}
		}

		return result;
	}
}
