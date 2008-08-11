/*
 * SqlKeywordHandler.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import workbench.WbManager;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.StringUtil;
import workbench.util.TextlistReader;

/**
 *
 * @author support@sql-workbench.net
 */
public class SqlKeywordHandler
{
	private Set<String> keywords;
	
	public SqlKeywordHandler(Connection con)
	{
		readKeywords(con, null);
	}
	
	public SqlKeywordHandler(Connection con, String dbid)
	{
		readKeywords(con, dbid);
	}

	public Collection<String> getSqlKeywords()
	{
		return this.keywords;
	}
	
	/**
	 * Read the keywords for the current DBMS that the JDBC driver returns.
	 * If the driver does not return all keywords, this list can be manually
	 * extended by defining the property workbench.db.<dbid>.syntax.keywords
	 * with a comma separated list of additional keywords
	 */
	private void readKeywords(Connection con, String dbId)
	{
		this.keywords = new TreeSet<String>();
		if (dbId != null)
		{
			try
			{
				String keys = con.getMetaData().getSQLKeywords();
				List<String> keyList = StringUtil.stringToList(keys, ",");
				this.keywords.addAll(keyList);
			}
			catch (Exception e)
			{
				LogMgr.logWarning("SqlKeywordHandler.readKeywords", "Error reading SQL keywords: " + e.getMessage());
			}
			
			String userKeys = Settings.getInstance().getProperty("workbench.db. + " + dbId + ".syntax.keywords", null);
			if (userKeys != null)
			{
				List<String> l = StringUtil.stringToList(userKeys.toUpperCase(), ",");
				this.keywords.addAll(l);
			}
		}

		// Read the base set of keywords (which cannot be configured)
		try
		{
			InputStream in = this.getClass().getResourceAsStream("SqlKeywords.txt");
			
			// TextlistReader will close the input stream
			TextlistReader reader = new TextlistReader(in);
			Collection<String> values = reader.getValues();
			if (values != null) this.keywords.addAll(values);
			
			// When running tests, the WbManager is not necessarily available
			WbManager mgr = WbManager.getInstance();
			if (mgr != null)
			{
				File baseDir = new File(mgr.getJarPath());
				File f = new File(baseDir, "SqlKeywords.txt");
				if (f.exists()) 
				{
					LogMgr.logDebug("SqlKeywordHandler.readKeywords()", "Reading addtional keywords from " + f.getCanonicalPath());
					in = new FileInputStream(f);
					reader = new TextlistReader(in);
					values = reader.getValues();
					if (values != null) this.keywords.addAll(values);
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("SqlKeywordHandler.readKeywords", "Error reading SQL keywords", e);
		}
		
		// Now remove any keywords that the user defined.
		
		try
		{
			List<String> l = Settings.getInstance().getListProperty("workbench.db." + dbId + ".syntax.nokeywords", false, null);
			this.keywords.removeAll(l);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public boolean isKeyword(String verb)
	{
		if (verb == null) return false;
		if (this.keywords == null) return false;
		return this.keywords.contains(verb.trim().toUpperCase());
	}
	
}
