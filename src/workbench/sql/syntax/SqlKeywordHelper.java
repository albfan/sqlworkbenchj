/*
 * SqlKeywordHelper.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
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
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.CollectionUtil;
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
 * <br/>
 * For DBMS specific keywords, this class looks for the above filenames with the
 * corresponding dbid as a prefix (e.g. postgresql.functions.wb)
 * <br/>
 * The DBMS specific keywords will be added to the global ones.
 *
 * @author Thomas Kellerer
 */
public class SqlKeywordHelper
{
	private String dbId;

	/**
	 * Read dbms-independent keywords.
	 */
	public SqlKeywordHelper()
	{
		this(null);
	}

	/**
	 * Read keywords specific for the DBMS identified by the given DBID
	 * (dbms-independent keywords will be included).
	 * @param id the DBID for the dbms
	 */
	public SqlKeywordHelper(String id)
	{
		this.dbId = id;
	}

	public Set<String> getKeywords()
	{
		Set<String> keywords = loadKeywordsFromFile("keywords.wb");
		if (this.dbId != null)
		{
			// old way of customizing DB specific keywords
			String key = "workbench.db." + dbId + ".syntax.keywords";
			List<String> addwords = StringUtil.stringToList(Settings.getInstance().getProperty(key, ""), ",", true, true);
			keywords.addAll(addwords);
		}
		return keywords;
	}

	public Set<String> getDataTypes()
	{
		return loadKeywordsFromFile("datatypes.wb");
	}

	public Set<String> getOperators()
	{
		return loadKeywordsFromFile("operators.wb");
	}

	public Set<String> getSqlFunctions()
	{
		Set<String> functions = loadKeywordsFromFile("functions.wb");
		if (this.dbId != null)
		{
			// old way of customizing DB specific functions
			String key = "workbench.db." + dbId + ".syntax.functions";
			List<String> addfuncs = StringUtil.stringToList(Settings.getInstance().getProperty(key, ""), ",", true, true);
			functions.addAll(addfuncs );
		}
		return functions;
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
		InputStream s = getClass().getResourceAsStream(filename);
		Set<String> result = CollectionUtil.caseInsensitiveSet();
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
			LogMgr.logInfo("SqlKeywordHelper.readFile()", "Reading keywords from: " + f.getAbsolutePath());
			try
			{
				BufferedReader customFile = new BufferedReader(new FileReader(f));
				Collection<String> custom = FileUtil.getLines(customFile, true);
				result.addAll(custom);
			}
			catch (Exception e)
			{
				LogMgr.logError("SqlKeywordHelper.readFile()", "Error reading external file", e);
			}
		}

		return result;
	}
}
