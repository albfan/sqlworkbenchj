/*
 * SqlDataTypesHandler.java
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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.StringUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class SqlDataTypesHandler
{
	private Set<String> dataTypes;
	
	public SqlDataTypesHandler(String dbId)
	{
		readTypes(dbId);
	}
	
	public Set<String> getDataTypes()
	{
		return Collections.unmodifiableSet(this.dataTypes);
	}
	
	/**
	 * Read the keywords for the current DBMS that the JDBC driver returns.
	 * If the driver does not return all keywords, this list can be manually
	 * extended by defining the property workbench.db.<dbid>.syntax.keywords
	 * with a comma separated list of additional keywords
	 */
	private void readTypes(String dbId)
	{
		this.dataTypes = new TreeSet<String>();
		if (dbId != null)
		{
			try
			{
				String keys = Settings.getInstance().getProperty("workbench.db.default.syntax.datatypes", 
								"CHAR,VARCHAR,DECIMAL,NUMERIC,INTEGER,BOOLEAN,FLOAT,REAL,NUMBER,CLOB,BLOB,INT,SMALLINT,BIT,NCHAR,NVARCHAR,DATE,TIME,TIMESTAMP");
				if (keys != null)
				{
					List<String> l = StringUtil.stringToList(keys.toUpperCase(), ",");
					this.dataTypes.addAll(l);
				}

				if (dbId != null)
				{
					keys = Settings.getInstance().getProperty("workbench.db." + dbId + ".syntax.datatypes", null);
					if (keys != null)
					{
						List<String> l = StringUtil.stringToList(keys.toUpperCase(), ",");
						this.dataTypes.addAll(l);
					}
				}
			}
			catch (Exception e)
			{
				LogMgr.logError("SqlKeywordHandler.readKeywords", "Error reading SQL data types", e);
			}
		}
	}

	public boolean isDataType(String verb)
	{
		if (verb == null) return false;
		if (this.dataTypes == null) return false;
		return this.dataTypes.contains(verb.trim().toUpperCase());
	}
	
}
