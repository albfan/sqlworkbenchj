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

import java.sql.DatabaseMetaData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.StringUtil;

/**
 * Stores and manages db specific settings.
 * 
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
		this.allowsMultipleGetUpdateCounts = settings.getBoolProperty("workbench.db." + getDbId() + ".multipleupdatecounts", true);
		this.reportsRealSizeAsDisplaySize = settings.getBoolProperty("workbench.db." + getDbId() + ".charsize.usedisplaysize", false);
		this.allowExtendedCreateStatement = settings.getBoolProperty("workbench.db." + getDbId() + ".extended.createstmt", true);
	}
	
	String getDbId() { return this.dbId; }

	public boolean allowsExtendedCreateStatement() { return allowExtendedCreateStatement; }
	public boolean allowsMultipleGetUpdateCounts() { return this.allowsMultipleGetUpdateCounts; }
	public boolean reportsRealSizeAsDisplaySize() { return this.reportsRealSizeAsDisplaySize; }

	public boolean ddlNeedsCommit() { return ddlNeedsCommit; }
	public boolean neverQuoteObjects() { return neverQuoteObjects; }
	
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
	
	String mapIndexType(Object type)
	{
		if (type == null) return null;
		if (type instanceof String) return (String)type;
		if (type instanceof Number)
		{
			return mapIndexType(((Number)type).intValue());
		}
		return null;
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
					int value = StringUtil.getIntValue(mapping[0], Integer.MIN_VALUE);
					if (value != Integer.MIN_VALUE)
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
	
	public boolean proceduresNeedTerminator()
	{
		String value = Settings.getInstance().getProperty("workbench.db.noprocterminator", null);
		if (value == null) return true;
		List l = StringUtil.stringToList(value, ",");
		return !l.contains(this.dbId);
	}	

	public IdentifierCase getSchemaNameCase()
	{
		// This allows overriding the default value returned by the JDBC driver
		String nameCase = Settings.getInstance().getProperty("workbench.db."  + this.getDbId() + ".schemaname.case", null);
		if (nameCase != null)
		{
			if ("lower".equals(nameCase))
			{
				return IdentifierCase.lower;
			}
			else if ("upper".equals(nameCase))
			{
				return IdentifierCase.upper;
			}
			else if ("mixed".equals(nameCase))
			{
				return IdentifierCase.mixed;
			}
		}
		return IdentifierCase.unknown;
	}
	
	public IdentifierCase getObjectNameCase()
	{
		// This allows overriding the default value returned by the JDBC driver
		String nameCase = Settings.getInstance().getProperty("workbench.db."  + this.getDbId() + ".objectname.case", null);
		if (nameCase != null)
		{
			if ("lower".equals(nameCase))
			{
				return IdentifierCase.lower;
			}
			else if ("upper".equals(nameCase))
			{
				return IdentifierCase.upper;
			}
			else if ("mixed".equals(nameCase))
			{
				return IdentifierCase.mixed;
			}
		}
		return IdentifierCase.unknown;
	}

	/**
	 *	Translates the numberic constants of DatabaseMetaData for trigger rules
	 *	into text (e.g DatabaseMetaData.importedKeyNoAction --> NO ACTION)
	 *
	 *	@param code the numeric value for a rule as defined by DatabaseMetaData.importedKeyXXXX constants
	 *	@return String
	 */
	public String getRuleDisplay(int code)
	{
		StringBuilder key = new StringBuilder(40);
		switch (code)
		{
			case DatabaseMetaData.importedKeyNoAction:
				key.append("workbench.sql.fkrule.noaction");
				break;
			case DatabaseMetaData.importedKeyRestrict:
				key.append("workbench.sql.fkrule.restrict");
				break;
			case DatabaseMetaData.importedKeySetNull:
				key.append("workbench.sql.fkrule.setnull");
				break;
			case DatabaseMetaData.importedKeyCascade:
				key.append("workbench.sql.fkrule.cascade");
				break;
			case DatabaseMetaData.importedKeySetDefault:
				key.append("workbench.sql.fkrule.setdefault");
				break;
			case DatabaseMetaData.importedKeyInitiallyDeferred:
				key.append("workbench.sql.fkrule.initiallydeferred");
				break;
			case DatabaseMetaData.importedKeyInitiallyImmediate:
				key.append("workbench.sql.fkrule.initiallyimmediate");
				break;
			case DatabaseMetaData.importedKeyNotDeferrable:
				key.append("workbench.sql.fkrule.notdeferrable");
				break;
			default:
				key = null;
		}
		if (key != null)
		{
			key.append('.');
			key.append(this.getDbId());
			String display = Settings.getInstance().getProperty(key.toString(), null);
			if (display != null) return display;
		}
		switch (code)
		{
			case DatabaseMetaData.importedKeyNoAction:
				return "NO ACTION";
			case DatabaseMetaData.importedKeyRestrict:
				return "RESTRICT";
			case DatabaseMetaData.importedKeySetNull:
				return "SET NULL";
			case DatabaseMetaData.importedKeyCascade:
				return "CASCADE";
			case DatabaseMetaData.importedKeySetDefault:
				return "SET DEFAULT";
			case DatabaseMetaData.importedKeyInitiallyDeferred:
				return "INITIALLY DEFERRED";
			case DatabaseMetaData.importedKeyInitiallyImmediate:
				return "INITIALLY IMMEDIATE";
			case DatabaseMetaData.importedKeyNotDeferrable:
				return "NOT DEFERRABLE";
			default:
				return StringUtil.EMPTY_STRING;
		}
	}
	public boolean useSetCatalog()
	{
		return Settings.getInstance().getBoolProperty("workbench.db." + this.getDbId() + ".usesetcatalog", true);
	}
	
	public boolean isNotDeferrable(String deferrable)
	{
		if (StringUtil.isEmptyString(deferrable)) return true;
		return (deferrable.equals(getRuleDisplay(DatabaseMetaData.importedKeyNotDeferrable)));
	}

	/**
	 * Retrieve the list of datatypes that should be ignored for the current 
	 * dbms. The names in that list must match the names returned   
	 * by DatabaseMetaData.getTypeInfo() 
	 */
	public List<String> getDataTypesToIgnore()
	{
		String types = Settings.getInstance().getProperty("workbench.ignoretypes." + getDbId(), null);;
		List<String> ignored = StringUtil.stringToList(types, ",", true, true);
		return ignored;
	}
}
