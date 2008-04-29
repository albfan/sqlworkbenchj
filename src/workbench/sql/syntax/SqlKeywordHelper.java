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
import java.util.HashSet;
import java.util.Set;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.FileUtil;

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

	public SqlKeywordHelper()
	{
	}
	
	public Set<String> getKeywords()
	{
		return loadKeywordsFromFile("keywords.wb");
	}
	
	public Set<String> getDataTypes()
	{
		return loadKeywordsFromFile("datatypes.wb");
	}
	
	public Set<String> getOperators()
	{
		return loadKeywordsFromFile("operators.wb");
	}
	
	public Set<String> getSystemFunctions()
	{
		return loadKeywordsFromFile("functions.wb");
	}
	
	private Set<String> loadKeywordsFromFile(String filename)
	{
		// First read the built-in functions
		InputStream s = SqlKeywordHelper.class.getResourceAsStream(filename);
		BufferedReader in = new BufferedReader(new InputStreamReader(s));
		
		Collection<String> builtin = FileUtil.getLines(in);
		Set<String> result = new HashSet<String>(builtin.size());
		result.addAll(builtin);
		
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
				Collection<String> custom = FileUtil.getLines(customFile);
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
