/*
 * DbSettings.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.StringUtil;

/**
 * @author support@sql-workbench.net
 */
public class DbSettings
{
	private String dbId;
	private boolean caseSensitive;
	private boolean useJdbcCommit;
	private boolean ddlNeedsCommit;
	private boolean trimDefaults = true;
	
	private boolean neverQuoteObjects;
	private boolean reportsRealSizeAsDisplaySize = false;
	private boolean allowExtendedCreateStatement = true;
	
	// This is set to true if identifiers starting with
	// a digit should always be quoted. This will 
	private boolean quoteIdentifierWithDigits = false;
	
	private boolean allowsMultipleGetUpdateCounts = true;
	
	private Map indexTypeMapping;
	public static final String IDX_TYPE_NORMAL = "NORMAL";

	public DbSettings(String id, String productName)
	{
		this.dbId = id;
		Settings settings = Settings.getInstance();
		this.caseSensitive = settings.getCaseSensitivServers().contains(productName);
		this.useJdbcCommit = settings.getServersWhichNeedJdbcCommit().contains(productName);
		this.ddlNeedsCommit = settings.getServersWhereDDLNeedsCommit().contains(productName);
		
		String quote = settings.getProperty("workbench.db.neverquote","");
		this.neverQuoteObjects = quote.indexOf(this.getDbId()) > -1;
		this.trimDefaults = settings.getBoolProperty("workbench.db." + getDbId() + ".trimdefaults", true);
		this.quoteIdentifierWithDigits = settings.getBoolProperty("workbench.db." + getDbId() + ".quotedigits", false);
		this.allowsMultipleGetUpdateCounts = settings.getBoolProperty("workbench.db." + getDbId() + ".multipleupdatecounts", true);
		this.reportsRealSizeAsDisplaySize = settings.getBoolProperty("workbench.db." + getDbId() + ".charsize.usedisplaysize", false);
		this.allowExtendedCreateStatement = settings.getBoolProperty("workbench.db." + getDbId() + ".extended.createstmt", true);
	}
	
	private String getDbId() { return this.dbId; }

	public boolean allowsExtendedCreateStatement() { return allowExtendedCreateStatement; }
	public boolean allowsMultipleGetUpdateCounts() { return this.allowsMultipleGetUpdateCounts; }
	public boolean reportsRealSizeAsDisplaySize() { return this.reportsRealSizeAsDisplaySize; }

	public boolean ddlNeedsCommit() { return ddlNeedsCommit; }
	public boolean neverQuoteObjects() { return neverQuoteObjects; }
	public boolean quoteIdentifierWithDigits() { return quoteIdentifierWithDigits; }
	
	public boolean trimDefaults() { return trimDefaults; }
	public boolean useJdbcCommit() { return useJdbcCommit; }
	public boolean isStringComparisonCaseSensitive() { return this.caseSensitive; }

	/**
	 *	Return the verb which does a DROP ... CASCADE for the given
	 *  object type. If the current DBMS does not support cascaded dropping
	 *  of objects, then null will be returned.
	 *
	 *	@param aType the database object type to drop (TABLE, VIEW etc)
	 *  @return a String which can be appended to a DROP type name command in order to drop dependent objects as well
	 *          or null if the current DBMS does not support this.
	 */
	public String getCascadeConstraintsVerb(String aType)
	{
		if (aType == null) return null;
		String verb = Settings.getInstance().getProperty("workbench.db.drop." + aType.toLowerCase() + ".cascade." + this.dbId, null);
		return verb;
	}
	
	public boolean needsTableForDropIndex()
	{
		boolean needsTable = Settings.getInstance().getBoolProperty("workbench.db." + this.dbId + ".dropindex.needstable", false);
		return needsTable;
	}	
	
	public boolean supportSingleLineCommands()
	{
		String ids = Settings.getInstance().getProperty("workbench.db.checksinglelinecmd", "");
		if ("*".equals(ids)) return true;
		List dbs = StringUtil.stringToList(ids, ",", true, true);
		return dbs.contains(this.getDbId());
	}

	public boolean supportsQueryTimeout()
	{
		boolean result = Settings.getInstance().getBoolProperty("workbench.db." + getDbId() + ".supportquerytimeout", true);
		return result;
	}
	
	public boolean supportsGetPrimaryKeys()
	{
		boolean result = Settings.getInstance().getBoolProperty("workbench.db." + getDbId() + ".supportgetpk", true);
		return result;
	}
	
	public boolean supportShortInclude()
	{
		String ids = Settings.getInstance().getProperty("workbench.db.supportshortinclude", "");
		if ("*".equals(ids)) return true;
		List dbs = StringUtil.stringToList(ids, ",", true, true);
		return dbs.contains(this.getDbId());
	}

	public boolean getStripProcedureVersion()
	{
		String ids = Settings.getInstance().getProperty("workbench.db.stripprocversion", "");
		List l = StringUtil.stringToList(ids, ",", true, true, false);
		return l.contains(this.dbId);
	}
	
	public String getProcVersionDelimiter()
	{
		return Settings.getInstance().getProperty("workbench.db.procversiondelimiter." + this.getDbId(), "");
	}

	public boolean supportsTruncate()
	{
		String s = Settings.getInstance().getProperty("workbench.db.truncatesupported", StringUtil.EMPTY_STRING);
		List l = StringUtil.stringToList(s, ",");
		return l.contains(this.getDbId());
	}

	public boolean isViewType(String type)
	{
		if (type == null) return false;
		type = type.toLowerCase();
		if (type.toUpperCase().indexOf("VIEW") > -1) return true;
		String viewTypes = Settings.getInstance().getProperty("workbench.db." + getDbId() + ".additional.viewtypes", "view").toLowerCase();
		List types = StringUtil.stringToList(viewTypes, ",", true, true, false);
		return (types.contains(type.toLowerCase()));
	}
	
	public boolean isSynonymType(String type)
	{
		if (type == null) return false;
		String synTypes = Settings.getInstance().getProperty("workbench.db." + getDbId() + ".synonymtypes", "synonym").toLowerCase();
		List types = StringUtil.stringToList(synTypes, ",", true, true, false);
		return (types.contains(type.toLowerCase()));
	}
	
	String mapIndexType(int type)
	{
		if (indexTypeMapping == null)
		{
			this.indexTypeMapping = new HashMap();
			String map = Settings.getInstance().getProperty("workbench.db." + getDbId() + ".indextypes", null);
			if (map != null)
			{
				List<String> entries = StringUtil.stringToList(map, ";", true, true);
				for (String entry : entries)
				{
					String[] mapping = entry.split(",");
					if (mapping.length != 2) continue;
					int value = StringUtil.getIntValue(mapping[0], -42);
					if (value != -42)
					{
						indexTypeMapping.put(new Integer(value), mapping[1]);
					}
				}
			}
		}
		String dbmsType = (String)this.indexTypeMapping.get(new Integer(type));
		if (dbmsType == null) 
		{
			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logDebug("DbSettings.mapIndexType()", "No mapping for type = " + type);
			}
			return IDX_TYPE_NORMAL;
		}
		return dbmsType;
	}	
}
