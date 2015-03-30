/*
 * SqlKeywordHelper.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
 *
 * By default the files
 * <ul>
 *   <li>reserved_words.wb (general SQL reserved words)</li>
 *   <li>keywords.wb (general SQL keywords)</li>
 *   <li>datatypes.wb (SQL datatypes)</li>
 *   <li>functions.wb (SQL functions)</li>
 *   <li>operators.wb (SQL operators)</li>
 * </ul>
 * are read from this package.
 *
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

	private static class LazyHolder
	{
		private static Set<String> keywords = new SqlKeywordHelper().getReservedWords();
	}

	public static Set<String> getDefaultReservedWords()
	{
		return LazyHolder.keywords;
	}

	/**
	 * Read dbms-independent keywords.
	 */
	public SqlKeywordHelper()
	{
		this(null);
	}

	/**
	 * Read keywords specific for the DBMS identified by the given DBID.
	 * (dbms-independent keywords will be included).
	 *
	 * @param id the DBID for the DBMS, may be null. In that case only standard keywords are used.
	 */
	public SqlKeywordHelper(String id)
	{
		this.dbId = id;
	}

	public Set<String> getReservedWords()
	{
		return loadKeywordsFromFile("reserved_words.wb");
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

  public Set<String> getCreateTableTypes()
  {
    return loadKeywordsFromFile("create_table_types.wb");
  }

  public Set<String> getCreateViewTypes()
  {
    return loadKeywordsFromFile("create_view_types.wb");
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

	/**
	 * Loads the keywords from the keyword file.
	 * First the passed filename is read, then the database specific one.
	 *
	 * After reading the built-in definitions, the file is also searched
	 * for in the config directory.
	 *
	 * @param filename
	 */
	public Set<String> loadKeywordsFromFile(String filename)
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
		removeItems(result);
		return result;
	}

	/**
	 * Remove any item from the set that starts with '-'.
	 *
	 * @param items
	 */
	private void removeItems(Set<String> items)
	{
		Set<String> toRemove = CollectionUtil.caseInsensitiveSet();
		for (String value : items)
		{
			if (value.charAt(0) == '-')
			{
				toRemove.add(value.substring(1));
				toRemove.add(value);
			}
		}
		items.removeAll(toRemove);
	}

	private Set<String> readFile(String filename)
	{
		// First read the built-in functions
		InputStream s = getClass().getResourceAsStream(filename);
		Set<String> result = CollectionUtil.caseInsensitiveSet();
		if (s != null)
		{
			BufferedReader in = new BufferedReader(new InputStreamReader(s));
			Collection<String> builtin = FileUtil.getLines(in, true, true);
			result.addAll(builtin);
		}

		// Try to read the file from the config directory.
		File f = f = new File(Settings.getInstance().getConfigDir(), filename);

		if (f.exists())
		{
			LogMgr.logInfo("SqlKeywordHelper.readFile()", "Reading keywords from: " + f.getAbsolutePath());
			try
			{
				BufferedReader customFile = new BufferedReader(new FileReader(f));
				Collection<String> custom = FileUtil.getLines(customFile, true, true);
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
