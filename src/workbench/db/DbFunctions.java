/*
 * DbFunctions.java
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

import java.sql.DatabaseMetaData;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.StringUtil;

/**
 * @author support@sql-workbench.net
 */
public class DbFunctions 
{
	public Set<String> getDbFunctions(DbMetadata wbMeta)
	{
		DatabaseMetaData metaData = wbMeta.getJdbcMetaData();
		
		Set<String> dbFunctions = new HashSet<String>();
		try
		{
			String funcs = metaData.getSystemFunctions();
			this.addStringList(dbFunctions, funcs);

			funcs = metaData.getStringFunctions();
			this.addStringList(dbFunctions, funcs);

			funcs = metaData.getNumericFunctions();
			this.addStringList(dbFunctions, funcs);

			funcs = metaData.getTimeDateFunctions();
			this.addStringList(dbFunctions, funcs);
			
			// Add Standard ANSI SQL Functions
			this.addStringList(dbFunctions, Settings.getInstance().getProperty("workbench.db.syntax.functions", "COUNT,AVG,SUM,MAX,MIN"));
			
			// Add additional DB specific functions
			this.addStringList(dbFunctions, Settings.getInstance().getProperty("workbench.db." + wbMeta.getDbId() + ".syntax.functions", null));
		}
		catch (Exception e)
		{
			LogMgr.logWarning("DbMetadata.getDbFunctions()", "Error retrieving function list from DB: " + e.getMessage());
		}
		return dbFunctions;
	}

	private void addStringList(Set<String> target, String list)
	{
		if (list == null) return;
		List<String> tokens = StringUtil.stringToList(list, ",", true, true, false);
		for (String keyword : tokens)
		{
			target.add(keyword.toUpperCase().trim());
		}
	}
}
