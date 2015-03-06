/*
 * DbSettings.java
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
package workbench.db;

import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import workbench.log.LogMgr;
import workbench.resource.DbExplorerSettings;
import workbench.resource.Settings;

import workbench.db.exporter.RowDataConverter;

import workbench.gui.dbobjects.TableSearchPanel;

import workbench.storage.BlobLiteralType;
import workbench.storage.DmlStatement;

import workbench.sql.EndReadOnlyTrans;
import workbench.sql.commands.SingleVerbCommand;

import workbench.util.CollectionUtil;
import workbench.util.NumberStringCache;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * Stores and manages db specific settings.
 * <br/>
 * The settings are stored in the global Settings file using
 * {@link workbench.resource.Settings}
 * <br/>
 * Any setting returned from this class will be specific to the DBMS
 * that it was initialized for, identified by the DBID passed to the constructor
 *
 * @author Thomas Kellerer
 * @see DbMetadata#getDbId()
 */
public class DbSettings
{
	public static final String IDX_TYPE_NORMAL = "NORMAL";
	public static final String DEFAULT_CREATE_TABLE_TYPE = "default";
	public static final String DBID_PLACEHOLDER = "[dbid]";

	private static final String NOT_THERE = "$wb$_not_there_$wb$";

	private final String dbId;
	private boolean caseSensitive;
	private boolean useJdbcCommit;
	private boolean ddlNeedsCommit;

	private boolean reportsRealSizeAsDisplaySize;

	private boolean allowsMultipleGetUpdateCounts = true;
	private boolean supportsBatchedStatements;
	private boolean supportsCommentInSql = true;

	private Set<String> updatingCommands;
	private Set<String> noUpdateCountVerbs = CollectionUtil.caseInsensitiveSet();

	private final String prefix;
	private final String prefixMajorVersion;
	private final String prefixFullVersion;

	public static enum GenerateOwnerType
	{
		always,
		whenNeeded,
		never;
	}

	public DbSettings(String id)
	{
		this(id, -1, -1);
	}

	public DbSettings(String id, int majorVersion, int minorVersion)
	{
		dbId = id;
		prefix = "workbench.db." + id + ".";
		prefixMajorVersion = "workbench.db." + id + "_" + NumberStringCache.getNumberString(majorVersion) + ".";
		prefixFullVersion = "workbench.db." + id + "_" + NumberStringCache.getNumberString(majorVersion) + "_" + NumberStringCache.getNumberString(minorVersion) + ".";

		Settings settings = Settings.getInstance();

		this.caseSensitive = settings.getBoolProperty(prefix + "casesensitive", false);
		this.useJdbcCommit = settings.getBoolProperty(prefix + "usejdbccommit", false);
		this.ddlNeedsCommit = settings.getBoolProperty(prefix + "ddlneedscommit", false);
		this.supportsCommentInSql = settings.getBoolProperty(prefix + "sql.embeddedcomments", true);

		List<String> quote = StringUtil.stringToList(settings.getProperty("workbench.db.neverquote",""));
		if (CollectionUtil.isNonEmpty(quote))
		{
			LogMgr.logInfo("DbSettings.<init>", "Migrating deprecated property \"workbench.db.neverquote\" to dbid based properties");
			for (String nid : quote)
			{
				settings.setProperty("workbench.db." + nid + ".neverquote", "true");
			}
			settings.removeProperty("workbench.db.neverquote");
		}
		this.allowsMultipleGetUpdateCounts = settings.getBoolProperty(prefix + "multipleupdatecounts", true);
		this.reportsRealSizeAsDisplaySize = settings.getBoolProperty(prefix + "charsize.usedisplaysize", false);
		this.supportsBatchedStatements = settings.getBoolProperty(prefix + "batchedstatements", false);
		readNoUpdateCountVerbs();
	}

	public final String getDbId()
	{
		return this.dbId;
	}

	public boolean supportsCommentInSql()
	{
		return this.supportsCommentInSql;
	}

	public String getProperty(String prop, String defaultValue)
	{
		return getVersionedString(prop, defaultValue);
	}

	private String getVersionedString(String prop, String defaultValue)
	{
		Settings set = Settings.getInstance();

		String result = set.getProperty(prefixFullVersion + prop, NOT_THERE);
		if (result != NOT_THERE) return result;

		result = set.getProperty(prefixMajorVersion + prop, NOT_THERE);
		if (result != NOT_THERE) return result;
		return set.getProperty(prefix + prop, defaultValue);
	}

	/**
	 * Checks if the given SQL verb updates the database.
	 * In addition to the built-in detected (e.g. UPDATE, DELETE), the user
	 * can configure DB-specific SQL commands that should be considered to update
	 * the database.
	 * <br/>
	 * This is used by the options "confirm updates" and "read only"
	 * in the connection profile
	 * <br/><br/>
	 * The related property is: <tt>workbench.db.[dbid].updatingcommands</tt> (comma separated list)
	 * @param verb the SQL command to check
	 *
	 * @return true if the command was configured
	 *
	 * @see workbench.sql.SqlCommand#isUpdatingCommand()
	 * @see ConnectionProfile#getConfirmUpdates()
	 * @see ConnectionProfile#isReadOnly()
	 */
	public boolean isUpdatingCommand(String verb)
	{
		if (StringUtil.isEmptyString(verb)) return false;
		if (this.updatingCommands == null)
		{
			this.updatingCommands = CollectionUtil.caseInsensitiveSet();

			String l = Settings.getInstance().getProperty("workbench.db.updatingcommands", null);
			List<String> commands = StringUtil.stringToList(l, ",", true, true);
			updatingCommands.addAll(commands);
			l = Settings.getInstance().getProperty(prefix + "updatingcommands", null);
			commands = StringUtil.stringToList(l, ",", true, true);
			updatingCommands.addAll(commands);
		}
		return updatingCommands.contains(verb);
	}

	public static Map<String, String> getDBMSNames()
	{
		Map<String, String> dbmsNames = new HashMap<>();
		dbmsNames.put("h2", "H2");
		dbmsNames.put("oracle", "Oracle");
		dbmsNames.put("hsql_database_engine", "HSQLDB");
		dbmsNames.put("postgresql", "PostgreSQL");
		dbmsNames.put("db2", "DB2 (LUW)");
		dbmsNames.put("db2h", "DB2 Host");
		dbmsNames.put("db2i", "DB2 iSeries");
		dbmsNames.put("mysql", "MySQL");
		dbmsNames.put("firebird", "Firebird SQL");
		dbmsNames.put("informix_dynamic_server", "Informix");
		dbmsNames.put("sql_anywhere", "SQL Anywhere");
		dbmsNames.put("microsoft_sql_server", "Microsoft SQL Server");
		dbmsNames.put("apache_derby", "Apache Derby");
		return dbmsNames;
	}

	public boolean getUseOracleDBMSMeta(String type)
	{
		if (type == null) return false;
		return Settings.getInstance().getBoolProperty("workbench.db.oracle.use.dbmsmeta." + type.trim().toLowerCase(), false);
	}

	public void setUseOracleDBMSMeta(String type, boolean flag)
	{
		if (type == null) return;
		Settings.getInstance().setProperty("workbench.db.oracle.use.dbmsmeta." + type.trim().toLowerCase(), flag);
	}

	public boolean supportsCreateArray()
	{
		return Settings.getInstance().getBoolProperty(prefix + "createarray.supported", true);
	}

	public boolean handleArrayDisplay()
	{
		return Settings.getInstance().getBoolProperty(prefix + "array.adjust.display", false);
	}

	public boolean showArrayType()
	{
		return Settings.getInstance().getBoolProperty(prefix + "array.show.type", true);
	}

	public boolean useGetStringForBit()
	{
		return Settings.getInstance().getBoolProperty(prefix + "bit.use.getstring", false);
	}

	public boolean useGetXML()
	{
		return Settings.getInstance().getBoolProperty(prefix + "xml.use.getsqlxml", false);
	}

	public boolean useGetStringForClobs()
	{
		return Settings.getInstance().getBoolProperty(prefix + "clob.use.getstring", true);
	}

	public boolean useSetStringForClobs()
	{
		return Settings.getInstance().getBoolProperty(prefix + "clob.use.setstring", false);
	}

	public boolean useGetBytesForBlobs()
	{
		return Settings.getInstance().getBoolProperty(prefix + "blob.use.getbytes", false);
	}

	public boolean useSetBytesForBlobs()
	{
		return Settings.getInstance().getBoolProperty(prefix + "blob.use.setbytes", false);
	}

	public boolean longVarcharIsClob()
	{
		return Settings.getInstance().getBoolProperty(prefix + "clob.longvarchar", true);
	}

	public boolean supportsResultSetsWithDML()
	{
		return Settings.getInstance().getBoolProperty(prefix + "dml.supports.results", true);
	}

	public boolean truncateReturnsRowCount()
	{
		return Settings.getInstance().getBoolProperty(prefix + "dml.truncate.returns.rows", true);
	}

	public boolean supportsBatchedStatements()
	{
		return this.supportsBatchedStatements;
	}

	public boolean allowsExtendedCreateStatement()
	{
		return Settings.getInstance().getBoolProperty(prefix + "extended.createstmt", true);
	}

	public boolean allowsMultipleGetUpdateCounts()
	{
		return this.allowsMultipleGetUpdateCounts;
	}

	public boolean reportsRealSizeAsDisplaySize()
	{
		return this.reportsRealSizeAsDisplaySize;
	}

	public int getMaxWarnings()
	{
		return Settings.getInstance().getIntProperty(prefix + "maxwarnings", 5000);
	}

	public int getMaxResults()
	{
		return Settings.getInstance().getIntProperty(prefix + "maxresults", 50000);
	}

	/**
	 * Returns true if the DBMS supports transactional DDL and thus
	 * needs a COMMIT after any DDL statement.
	 * <br/>
	 * The related property is: <tt>workbench.db.[dbid].ddlneedscommit</tt>
	 */
	public boolean ddlNeedsCommit()
	{
		return ddlNeedsCommit;
	}

	/**
	 * Returns true if object names should never be quoted.
	 *
	 */
	public boolean neverQuoteObjects()
	{
		return Settings.getInstance().getBoolProperty(prefix + "neverquote", false);
	}

	/**
	 * Returns true if default values in the table definition should be trimmed
	 * before displaying them to the user.
	 * The default is true
	 * <br/>
	 * The related property is: <tt>workbench.db.[dbid].trimdefaults</tt>
	 */
	public boolean trimDefaults()
	{
		return Settings.getInstance().getBoolProperty(prefix + "trimdefaults", true);
	}

	/**
	 * Returns true if the DataImporter should use setNull() to send NULL values
	 * instead of setObject(int, null).
	 * <br/>
	 * This is also used from within the DataStore when updating data.
	 * <br/>
	 * The related property is workbench.db.[dbid].import.use.setnull
	 *
	 * @see DmlStatement#execute(workbench.db.WbConnection, boolean)
	 */
	public boolean useSetNull()
	{
		return Settings.getInstance().getBoolProperty(prefix + "import.use.setnull", false);
	}

	/**
	 * Some JDBC driver to not allow to run a SQL statement that contains COMMIT or ROLLBACK
	 * as a String. They required to use Connection.commit() or Conneciton.rollback() instead.
	 * <br/>
	 * The related property is: workbench.db.[dbid].usejdbccommit
	 * @see SingleVerbCommand#execute(java.lang.String)
	 */
	public boolean useJdbcCommit()
	{
		return useJdbcCommit;
	}

	/**
	 * Check if string comparisons are case-sensitive by default for the current DBMS.
	 * <br/>
	 * The related property is: <tt>workbench.db.[dbid].casesensitive</tt>
	 *
	 * @return true if the current DBMS is case sensitive
	 * @see TableSearchPanel#searchData()
	 */
	public boolean isStringComparisonCaseSensitive()
	{
		return this.caseSensitive;
	}

	public boolean getDefaultBeforeNull()
	{
		return Settings.getInstance().getBoolProperty(prefix + "defaultbeforenull", false);
	}

	public String getCascadeConstraintsVerb(String aType)
	{
		if (aType == null) return null;
		String verb = Settings.getInstance().getProperty("workbench.db.drop." + getKeyValue(aType) + ".cascade." + getDbId(), null);
		return verb;
	}

	public boolean useFQConstraintName()
	{
		return Settings.getInstance().getBoolProperty(prefix + "constraints.use_fqname", false);
	}

	public boolean useCatalogInDML()
	{
		return Settings.getInstance().getBoolProperty(prefix + "catalog.dml", true);
	}

	public boolean alwaysUseSchema()
	{
		return Settings.getInstance().getBoolProperty(prefix + "schema.always", false);
	}

	public boolean alwaysUseCatalog()
	{
		return Settings.getInstance().getBoolProperty(prefix + "catalog.always", false);
	}

	public boolean needsCatalogIfNoCurrent()
	{
		return Settings.getInstance().getBoolProperty(prefix + "catalog.neededwhenempty", false);
	}

	public String getInsertForImport()
	{
		return Settings.getInstance().getProperty(prefix + "import.insert", null);
	}

	public List<String> getRefCursorTypeNames()
	{
		return Settings.getInstance().getListProperty(prefix + "refcursor.typename", false, null);
	}

	public int getRefCursorDataType()
	{
		return Settings.getInstance().getIntProperty(prefix + "refcursor.typevalue", Integer.MIN_VALUE);
	}

	public boolean useWbProcedureCall()
	{
		return Settings.getInstance().getBoolProperty(prefix + "procs.use.wbcall", false);
	}

	public String getCreateIndexSQL()
	{
		return Settings.getInstance().getProperty(prefix + "create.index", Settings.getInstance().getProperty("workbench.db.sql.create.index", null));
	}

	public String getCreateUniqeConstraintSQL()
	{
		return Settings.getInstance().getProperty(prefix + "create.uniqueconstraint", Settings.getInstance().getProperty("workbench.db.sql.create.uniqueconstraint", null));
	}


	public String getSelectForFunctionSQL()
	{
		return Settings.getInstance().getProperty(prefix + "function.select", null);
	}

	public static String getKeyValue(String value)
	{
		if (value == null) return null;
		return value.toLowerCase().trim().replaceAll("\\s+", "_");
	}

	/**
	 * Return the complete DDL to drop the given type of DB-Object.
	 * <br/>
	 * If includeCascade is true and the DBMS supports dropping this type cascaded,
	 * then the returned DDL will include the necessary CASCADE keyword
	 * <br/>
	 * The cascade keyword will only be used when the SQL template (defined in
	 * default.properties or workbench.settings actually includes the %cascade%
	 * placeholder. If that placeholder is not present in the SQL template,
	 * passing true as includeCascade will not have an effect.
	 * <br/>
	 *
	 * @param type the database object type to drop (TABLE, VIEW etc)
	 * @return the DDL Statement to drop an object of that type. The placeholder %name% must
	 * be replaced with the correct object name
	 */
	public String getDropDDL(String type, boolean includeCascade)
	{
		if (StringUtil.isBlank(type)) return null;
		String cascade = getCascadeConstraintsVerb(type);

		String ddl = getProperty("drop." + getKeyValue(type), null);
		if (ddl == null)
		{
			ddl = "DROP " + type.toUpperCase() + " %name%";
			if (cascade != null && includeCascade)
			{
				ddl += " " + cascade;
			}
		}
		else
		{
			if (includeCascade)
			{
				ddl = ddl.replace("%cascade%", cascade == null ? "" : cascade);
			}
			else
			{
				ddl = ddl.replace("%cascade%", "");
			}
		}
		return ddl;
	}

	public boolean useSpecificNameForDropProcedure()
	{
		return Settings.getInstance().getBoolProperty(prefix + "drop.function.use.specificname", false);
	}

	public boolean useSpecificNameForProcedureColumns()
	{
		return Settings.getInstance().getBoolProperty(prefix + "procedures.use.specificname", true);
	}

	public String getSpecificNameColumn()
	{
		return Settings.getInstance().getProperty(prefix + "procedures.specificname.colname", "SPECIFIC_NAME");
	}

	public boolean needParametersToDropFunction()
	{
		return Settings.getInstance().getBoolProperty(prefix + "drop.function.includeparameters", false);
	}

	public boolean includeOutParameterForDropFunction()
	{
		return Settings.getInstance().getBoolProperty(prefix + "drop.function.include.out.parameters", true);
	}

	/**
	 * Returns if the DataImporter should use savepoints for each statement.
	 * <br/>
	 * The related property is: <tt>workbench.db.[dbid].import.usesavepoint</tt>
	 */
	public boolean useSavepointForImport()
	{
		return Settings.getInstance().getBoolProperty(prefix + "import.usesavepoint", false);
	}

	/**
	 * Returns if the DataImporter should use savepoints for the pre and post tables statements.
	 * <br/>
	 * The related property is: <tt>workbench.db.[dbid].import.tablestmt.usesavepoint</tt>
	 */
	public boolean useSavepointForTableStatements()
	{
		return Settings.getInstance().getBoolProperty(prefix + "import.tablestmt.usesavepoint", false);
	}

	/**
	 * Returns if DML statements should be guarded by savepoints.
	 * <br/>
	 * This affects SQL statements entered by the user and generated when
	 * updating a DataStore
	 * <br/>
	 * The related property is: <tt>workbench.db.[dbid].sql.usesavepoint</tt>
	 */
	public boolean useSavePointForDML()
	{
		return Settings.getInstance().getBoolProperty(prefix + "sql.usesavepoint", false);
	}

	/**
	 * Returns if DDL statements should be guarded by savepoints.
	 * This affects SQL statements entered by the user and generated when
	 * updating a DataStore
	 * <br/>
	 * The related property is: <tt>workbench.db.[dbid].ddl.usesavepoint</tt>
	 */
	public boolean useSavePointForDDL()
	{
		return Settings.getInstance().getBoolProperty(prefix + "ddl.usesavepoint", false);
	}

	/**
	 * Returns the default type for the Blob formatter
	 * @return hex, octal, char
	 * @see BlobLiteralType
	 */
	public String getBlobLiteralType()
	{
		return Settings.getInstance().getProperty(prefix + "blob.literal.type", "hex");
	}

	public String getBlobLiteralPrefix()
	{
		return Settings.getInstance().getProperty(prefix + "blob.literal.prefix", null);
	}

	public String getBlobLiteralSuffix()
	{
		return Settings.getInstance().getProperty(prefix + "blob.literal.suffix", null);
	}

	public boolean getBlobLiteralUpperCase()
	{
		return Settings.getInstance().getBoolProperty(prefix + "blob.literal.upcase", false);
	}

	public boolean getUseIdioticQuotes()
	{
		return Settings.getInstance().getBoolProperty(prefix + "bracket.quoting", false);
	}

	public boolean selectStartsTransaction()
	{
		return Settings.getInstance().getBoolProperty(prefix + "select.startstransaction", false);
	}

	public boolean getUseMySQLShowCreate(String type)
	{
		if (type == null) return false;
		return Settings.getInstance().getBoolProperty("workbench.db.mysql.use.showcreate." + type.trim().toLowerCase(), false);
	}

	/**
	 * Returns the string that is used for line comments if the DBMS does not use
	 * the ANSI comment character (such as MySQL)
	 */
	public String getLineComment()
	{
		return Settings.getInstance().getProperty(prefix + "linecomment", null);
	}

	public boolean supportsQueryTimeout()
	{
		boolean result = Settings.getInstance().getBoolProperty(prefix + "supportquerytimeout", true);
		return result;
	}

	public boolean supportsIndexedViews()
	{
		boolean result = Settings.getInstance().getBoolProperty(prefix + "indexedviews", false);
		return result;
	}

	public boolean supportsGetPrimaryKeys()
	{
		boolean result = Settings.getInstance().getBoolProperty(prefix + "supportgetpk", true);
		return result;
	}

	public boolean supportsTransactions()
	{
		boolean result = Settings.getInstance().getBoolProperty(prefix + "supports.transactions", true);
		return result;
	}

	public String getProcVersionDelimiter()
	{
		return Settings.getInstance().getProperty(prefix + "procversiondelimiter", null);
	}

	public boolean supportsCascadedTruncate()
	{
		String sql = Settings.getInstance().getProperty(prefix + "sql.truncate.cascade", null);
		return sql != null;
	}

	public String getTruncateCommand(boolean cascade)
	{
		String truncate = Settings.getInstance().getProperty(prefix + "sql.truncate", null);
		if (cascade)
		{
			truncate = Settings.getInstance().getProperty(prefix + "sql.truncate.cascade", truncate);
		}
		return truncate;
	}

	public boolean truncateNeedsCommit()
	{
		return Settings.getInstance().getBoolProperty(prefix + "truncate.commit", false);
	}

	public boolean supportsTruncate()
	{
		return getTruncateCommand(false) != null;
	}

	public boolean isViewType(String type)
	{
		if (type == null) return false;
		return getViewTypes().contains(type);
	}

	public boolean isSynonymType(String type)
	{
		if (type == null) return false;
		String synTypes = Settings.getInstance().getProperty(prefix + "synonymtypes", "synonym").toLowerCase();
		List types = StringUtil.stringToList(synTypes, ",", true, true, false);
		return types.contains(type.toLowerCase());
	}

	String mapIndexType(int type)
	{
		switch (type)
		{
			case DatabaseMetaData.tableIndexHashed:
				return "HASH";
			case DatabaseMetaData.tableIndexClustered:
				return "CLUSTERED";
		}
		return IDX_TYPE_NORMAL;
	}

	public IdentifierCase getSchemaNameCase()
	{
		// This allows overriding the default value returned by the JDBC driver
		String nameCase = Settings.getInstance().getProperty(prefix + "schemaname.case", null);
		if (nameCase != null)
		{
			try
			{
				return IdentifierCase.valueOf(nameCase);
			}
			catch (Exception e)
			{
				LogMgr.logWarning("DbSettings.getSchemaNameCase()", "Invalid IdentifierCase value '" + nameCase + "' specified");
			}
		}
		return IdentifierCase.unknown;
	}

	public void setObjectNameCase(String oCase)
	{
		Settings.getInstance().setProperty(prefix + "objectname.case", oCase);
	}

	public IdentifierCase getObjectNameCase()
	{
		// This allows overriding the default value returned by the JDBC driver
		String nameCase = Settings.getInstance().getProperty(prefix + "objectname.case", null);
		if (nameCase != null)
		{
			try
			{
				return IdentifierCase.valueOf(nameCase);
			}
			catch (Exception e)
			{
				LogMgr.logWarning("DbSettings.getObjectNameCase()", "Invalid IdentifierCase value '" + nameCase + "' specified");
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
		return Settings.getInstance().getBoolProperty(prefix + "usesetcatalog", true);
	}

	public boolean isNotDeferrable(String deferrable)
	{
		if (StringUtil.isEmptyString(deferrable)) return true;
		return deferrable.equals(getRuleDisplay(DatabaseMetaData.importedKeyNotDeferrable));
	}

	/**
	 * Retrieve the list of datatypes that should be ignored when mapping datatypes
	 * from one DBMS to another (e.g. when creating a table on the fly using DataCopier
	 * <br/>
	 * The names in that list must match the names returned by DatabaseMetaData.getTypeInfo()
	 */
	public List<String> getDataTypesToIgnore()
	{
		String types = Settings.getInstance().getProperty("workbench.ignoretypes." + getDbId(), null);
		List<String> ignored = StringUtil.stringToList(types, ",", true, true);
		return ignored;
	}

	/**
	 * Return the query to retrieve the current catalog
	 * <br/>
	 * The related property is: <tt>workbench.db.[dbid].currentcatalog.query
	 *
	 * @return null if no query is configured
	 */
	public String getQueryForCurrentCatalog()
	{
		return getProperty("currentcatalog.query", null);
	}

	/**
	 * Returns if the RowDataConverter should format instances of java.util.Date using
	 * the supplied timestamp format (to preserve the time information).
	 * <br/>
	 * The related property is: <tt>workbench.db.[dbid].export.convert.date2ts
	 * <br/>
	 * This property defaults to true for Oracle.
	 *
	 * @return true if java.util.Date should be formated with the Timestamp format
	 * @see RowDataConverter#getValueAsFormattedString(workbench.storage.RowData, int)
	 */
	public boolean getConvertDateInExport()
	{
		return Settings.getInstance().getBoolProperty(prefix + "export.convert.date2ts", false);
	}

	public boolean needsExactClobLength()
	{
		return Settings.getInstance().getBoolProperty(prefix + "exactcloblength", false);
	}

	/**
	 * Check if the source of views (in the DbExplorer) should be formatted after
	 * it is retrieved from the server.
	 *
	 * The related property is: <tt>workbench.db.[dbid].source.view.doformat
	 *
	 * @return true if the source should be formatted (using the SQLFormatter)
	 *
	 * @see workbench.db.ViewReader#getViewSource(workbench.db.TableIdentifier)
	 */
	public boolean getFormatViewSource()
	{
		return Settings.getInstance().getBoolProperty(prefix + "source.view.doformat", false);
	}

	/**
	 * Return the DDL to drop a single column from a table.
	 * The statement must contain placeholders for table and column names.
	 *
	 * The related property is: <tt>workbench.db.[dbid].drop.column
	 *
	 * @return null if no statement is configured.
	 * @see workbench.db.MetaDataSqlManager#TABLE_NAME_PLACEHOLDER
	 * @see workbench.db.MetaDataSqlManager#COLUMN_NAME_PLACEHOLDER
	 */
	public String getDropSingleColumnSql()
	{
		return getProperty("drop.column", null);
	}

	/**
	 * Return the DDL to drop multiple columns from a table.
	 * The statement must contain placeholders for the table name and the column list.
	 *
	 * The related property is: <tt>workbench.db.[dbid].drop.column.multi
	 *
	 * @return null if no statement is configured.
	 * @see workbench.db.MetaDataSqlManager#TABLE_NAME_PLACEHOLDER
	 * @see workbench.db.MetaDataSqlManager#COLUMN_LIST_PLACEHOLDER
	 */
	public String getDropMultipleColumnSql()
	{
		return getProperty("drop.column.multi", null);
	}


	/**
	 * Return the DDL to add a single column to a table.
	 *
	 * The related property is: <tt>workbench.db.[dbid].add.column
	 *
	 * @return null if no statement is configured.
	 * @see workbench.db.MetaDataSqlManager#TABLE_NAME_PLACEHOLDER
	 * @see workbench.db.MetaDataSqlManager#COLUMN_NAME_PLACEHOLDER
	 */
	public String getAddColumnSql()
	{
		return getProperty("add.column", null);
	}

	public boolean supportsSortedIndex()
	{
		return Settings.getInstance().getBoolProperty(prefix + "index.sorted", true);
	}

	public final boolean includeSystemTablesInSelectable()
	{
		return Settings.getInstance().getBoolProperty(prefix + "systemtables.selectable", false);
	}

	public boolean removeNewLinesInSQL()
	{
		return Settings.getInstance().getBoolProperty(prefix + "removenewlines", false);
	}

	public boolean canDropType(String type)
	{
		if (StringUtil.isEmptyString(type)) return false;
		if (type.equalsIgnoreCase("column"))
		{
			return getDropSingleColumnSql() != null;
		}
		return true;
	}

	public void setDataTypeExpression(String cleanType, String expr)
	{
		Settings.getInstance().setProperty(prefix + "selectexpression." + cleanType, expr);
	}

	/**
	 * Retrieves an expression to be used inside a select statement for the given datatype.
	 *
	 * The DbExplorer will use this expression instead of the "plain" column
	 * name to retrieve data for this data type. This can be used to make
	 * data readable in the DbExplorer for data types that are not natively supported
	 * by the JDBC driver.
	 *
	 * The expression must contain the placeholder <tt>${column}</tt> for the column name.
	 *
	 * @param dbmsType
	 * @return null if nothing is configured
	 *
	 * @see workbench.db.TableSelectBuilder#getSelectForTable(workbench.db.TableIdentifier)
	 * @see workbench.db.TableSelectBuilder#COLUMN_PLACEHOLDER
	 */
	public String getDataTypeSelectExpression(String dbmsType)
	{
		if (dbmsType == null) return null;
		return getProperty("selectexpression." + dbmsType.toLowerCase(), null);
	}

	/**
	 * Return an expression to be used in a PreparedStatement as the value placeholder.
	 *
	 * For e.g. Postgres to be able to update an <tt>XML</tt> column, the expression
	 * <tt>cast(? as xml)</tt> is required.
	 *
	 * @param dbmsType  the DBMS data type of the column
	 * @return a DBMS specific expression or null if nothing was defined (or the datatype is null)
	 *
	 * @see #isDmlExpressionDefined(java.lang.String)
	 * @see DmlExpressionBuilder
	 */
	public String getDmlExpressionValue(String dbmsType)
	{
		if (dbmsType == null) return null;
		String cleanType = SqlUtil.getBaseTypeName(dbmsType);
		return Settings.getInstance().getProperty(prefix + "dmlexpression." + cleanType.toLowerCase(), null);
	}

	public boolean isDmlExpressionDefined(String dbmsType)
	{
		return getDmlExpressionValue(dbmsType) != null;
	}

	/**
	 * Return a customized mapping of JDBC types to native datatypes.
	 * @see TypeMapper
	 */
	public String getJDBCTypeMapping()
	{
		return Settings.getInstance().getProperty(prefix + "typemap", null);
	}

	public boolean cleanupTypeMappingNames()
	{
		return Settings.getInstance().getBoolProperty(prefix + "typemap.cleanup", false);
	}

	/**
	 * Returns if setObject() should be used with the target JDBC datatype or
	 * without
	 * <br/>e.g. <tt>setObject(1, "42", Types.OTHER)</tt> which will define
	 * the datatype, or using <tt>setObject(1, "42")</tt> which will pass the
	 * conversion and type detection to the driver.
	 * <br/>
	 * Some drivers to not work properly when dealing with non JDBC Types here
	 * (e.g. Postgres and UUID columns)
	 */
	public boolean getUseTypeWithSetObject()
	{
		return Settings.getInstance().getBoolProperty(prefix + "import.setobject.usetype", false);
	}

	public boolean getRetrieveProcParmsForAutoCompletion()
	{
		return Settings.getInstance().getBoolProperty(prefix + "completion.procs.showparms", true);
	}

	/**
	 * If a customized table source retrieval is enabled, this method
	 * controls if the index source should be generated by SQL Workbench/J or not
	 *
	 * @see #getUseCustomizedCreateTableRetrieval()
	 * @return true if index source should be generated even if the table source is retrieved by a customized statement
	 */
	public boolean getGenerateTableIndexSource()
	{
		if (isTableSourceRetrievalCustomized())
		{
			boolean included = Settings.getInstance().getBoolProperty(prefix + "retrieve.create.table.index_included", false);
			return !included;
		}
		return Settings.getInstance().getBoolProperty(prefix + "generate.tablesource.include.indexes", true);
	}

	/**
	 * If a customized table source retrieval is enabled, this method
	 * controls if the foreign key source should be generated by SQL Workbench/J or not
	 *
	 * @see #getUseCustomizedCreateTableRetrieval()
	 * @return true if index source should be generated even if the table source is retrieved by a customized statement
	 */
	public boolean getGenerateTableFKSource()
	{
		if (isTableSourceRetrievalCustomized())
		{
			boolean included = Settings.getInstance().getBoolProperty(prefix + "retrieve.create.table.fk_included", false);
			return !included;
		}
		return Settings.getInstance().getBoolProperty(prefix + "generate.tablesource.include.fk", true);
	}

	public GenerateOwnerType getGenerateTableOwner()
	{
		String value = Settings.getInstance().getProperty(prefix + "generate.tablesource.include.owner", GenerateOwnerType.whenNeeded.name());
		try
		{
			return GenerateOwnerType.valueOf(value);
		}
		catch (Exception ex)
		{
			return GenerateOwnerType.whenNeeded;
		}
	}

	/**
	 * If customized SQL is configured to retrieve the source of a table, this setting identifies if table and column
	 * comments are included in the generated SQL.
	 *
	 * <br/>
	 * If no SQL is configured, this method always returns true. Otherwise the value of the config property <br/>
	 * <tt>workbench.db.[dbid].retrieve.create.table.comments_included</tt> is checked. If that is true to indicate that
	 * comments <i>are</i> returned by the custom SQL, this method returns false.
	 * <br/>
	 *
	 * @see #getUseCustomizedCreateTableRetrieval()
	 *
	 * @return true if table comments (including columns) should be generated
	 */
	public boolean getGenerateTableComments()
	{
		if (isTableSourceRetrievalCustomized())
		{
			boolean included = Settings.getInstance().getBoolProperty(prefix + "retrieve.create.table.comments_included", false);
			return !included;
		}
		boolean defaultFlag = Settings.getInstance().getBoolProperty("workbench.db.generate.tablesource.generate..comments", true);
		return Settings.getInstance().getBoolProperty(prefix + "generate.tablesource.include.comments", defaultFlag);
	}

	/**
	 * Returns if the table grants are already included in the generated table source.
	 *
	 * @see #getUseCustomizedCreateTableRetrieval()
	 * @see workbench.db.TableSourceBuilder#getTableSource(workbench.db.TableIdentifier, java.util.List)
	 *
	 * @return true if table grants should be generated even if the table source is retrieved by a customized statement
	 */
	public boolean getGenerateTableGrants()
	{
		if (isTableSourceRetrievalCustomized())
		{
			boolean included = Settings.getInstance().getBoolProperty(prefix + "retrieve.create.table.grants_included", false);
			return !included;
		}
		boolean defaultFlag = DbExplorerSettings.getGenerateTableGrants();
		return Settings.getInstance().getBoolProperty(prefix + "generate.tablesource.include.grants", defaultFlag);
	}

	private boolean isTableSourceRetrievalCustomized()
	{
		return (getUseCustomizedCreateTableRetrieval() && getRetrieveTableSourceSql() != null);
	}
	/*
	 * @see workbench.db.TableSourceBuilder#getTableSource(workbench.db.TableIdentifier, java.util.List, workbench.storage.DataStore, workbench.storage.DataStore, boolean, java.lang.String, boolean)
	 */
	protected boolean getUseCustomizedCreateTableRetrieval()
	{
		return Settings.getInstance().getBoolProperty(prefix + "retrieve.create.table.enabled", true);
	}
	/**
	 * Returns the SQL that retrieves the CREATE SQL for a given table directly from the DBMS.
	 * In the returned SQL, the placeholders %table_name%, %schema% and %catalog% must be
	 * replaced with the real values.
	 *
	 * If the table source is not returned in the first column of the result set,
	 * getRetrieveTableSourceCol() will indicate the column index that contains the
	 * actual source.
	 *
	 * @return null if not configured, a SQL to be run to retrieve a CREATE TABLE otherwise
	 * @see #getRetrieveTableSourceCol()
	 * @see #getRetrieveTableSourceNeedsQuotes()
	 * @see #getGenerateTableComments()
	 * @see #getGenerateTableGrants()
	 * @see #getGenerateTableIndexSource()
	 */
	public String getRetrieveTableSourceSql()
	{
		if (!getUseCustomizedCreateTableRetrieval()) return null;
		return getProperty("retrieve.create.table.query", null);
	}

	/**
	 * Returns the result set column in which the table source from getRetrieveTableSourceSql()
	 * is returned (if configured)
	 *
	 * @return the approriate result set column index if configured, 1 otherwise
	 * @see #getRetrieveTableSourceSql()
	 */
	public int getRetrieveTableSourceCol()
	{
		return Settings.getInstance().getIntProperty(prefix + "retrieve.create.table.sourcecol", 1);
	}

	/**
	 * Returns the result set column in which the index source from getRetrieveIndexSourceSql()
	 * is returned (if configured)
	 *
	 * @return the approriate result set column index if configured, 1 otherwise
	 * @see #getRetrieveIndexSourceSql()
	 */
	public int getRetrieveIndexSourceCol()
	{
		return Settings.getInstance().getIntProperty(prefix + "retrieve.create.index.sourcecol", 1);
	}

	protected boolean getUseCustomizedCreateIndexRetrieval()
	{
		return Settings.getInstance().getBoolProperty(prefix + "retrieve.create.index.enabled", true);
	}

	public String getRetrieveIndexSourceSql()
	{
		if (!getUseCustomizedCreateIndexRetrieval()) return null;
		return getProperty("retrieve.create.index.query", null);
	}

	/**
	 * Returns true if the placeholders for retrieving the index source need to be checked
	 * for quoting. This is necessary if the SQL is a SELECT statement, but might not
	 * be necessary if the SQL (defined by getRetrieveIndexSourceSql()) is a procedure call
	 *
	 * @return true if quotes might be needed.
	 * @see #getRetrieveIndexSourceSql()
	 */
	public boolean getRetrieveIndexSourceNeedsQuotes()
	{
		return Settings.getInstance().getBoolProperty(prefix + "retrieve.create.index.checkquotes", true);
	}

	/**
	 * Return all configured "CREATE TABLE" types.
	 *
	 * @see #getCreateTableTemplate(java.lang.String)
	 */
	public static List<CreateTableTypeDefinition> getCreateTableTypes()
	{
		return getCreateTableTypes(null);
	}

	/**
	 * Return configured "CREATE TABLE" types for the specified DBID.
	 *
	 * @see #getCreateTableTemplate(java.lang.String)
	 */
	public static List<CreateTableTypeDefinition> getCreateTableTypes(String dbid)
	{
		List<String> types = Settings.getInstance().getKeysLike(".create.table.");
		List<CreateTableTypeDefinition> result = new ArrayList<>(types.size());
		for (String type : types)
		{
			CreateTableTypeDefinition createType = new CreateTableTypeDefinition(type);
			if (dbid == null || dbid.equals(createType.getDbId()))
			{
				result.add(createType);
			}
		}
		Collections.sort(result);
		return result;
	}

	/**
	 * Checks if a CREATE TABLE for the specific type should be committed.
	 *
	 * @param createType
	 * @return true if this type can/should be committed
	 * @see #getCreateTableTemplate(java.lang.String)
	 */
	public boolean commitCreateTable(String createType)
	{
		if (createType == null) return true;
		String key = prefix + ".create.table."+ createType.toLowerCase() + ".commit";
		return Settings.getInstance().getBoolProperty(key, true);
	}

	/**
	 * The SQL template that is used to create a table of the specified type
	 *
	 * @return the temp table keyword or null
	 */
	public String getCreateTableTemplate(String type)
	{
		final String defaultSql =
			"CREATE TABLE " + MetaDataSqlManager.FQ_TABLE_NAME_PLACEHOLDER +
			"\n(\n" +
			MetaDataSqlManager.COLUMN_LIST_PLACEHOLDER +
			"\n)";

		if (StringUtil.isBlank(type)) type = DEFAULT_CREATE_TABLE_TYPE;

		return getProperty("create.table." + getKeyValue(type), defaultSql);
	}

	public Set<String> getViewTypes()
	{
		List<String> types = Settings.getInstance().getListProperty("workbench.db.viewtypes", false, "VIEW");
		List<String> dbTypes = Settings.getInstance().getListProperty(prefix + "additional.viewtypes", false, null);
		Set<String> allTypes = CollectionUtil.caseInsensitiveSet();
		allTypes.addAll(types);
		allTypes.addAll(dbTypes);
		return allTypes;
	}

	/**
	 * For testing purposes.
	 */
	public void setCreateTableTemplate(String type, String template)
	{
		Settings.getInstance().setProperty(prefix + "create.table." + type.toLowerCase(), template);
	}

	/**
	 * Returns true if the placeholders for retrieving the table source need to be checked
	 * for quoting. This is necessary if the SQL is a SELECT statement, but might not
	 * be necessary if the SQL (defined by getRetrieveTableSourceSql()) is a procedure call
	 *
	 * @return true if quotes might be needed.
	 * @see #getRetrieveTableSourceSql()
	 */
	public boolean getRetrieveTableSourceNeedsQuotes()
	{
		return Settings.getInstance().getBoolProperty(prefix + "retrieve.create.table.checkquotes", true);
	}

	public boolean isSearchable(String dbmsType)
	{
		if (StringUtil.isBlank(dbmsType)) return false;
		List<String> types = Settings.getInstance().getListProperty(prefix + "datatypes.searchable", true);
		return types.contains(dbmsType.toLowerCase());
	}

	public String getAlterColumnDataTypeSql()
	{
		return getProperty("alter.column.type", null);
	}

	public String getRenameColumnSql()
	{
		return getProperty("alter.column.rename", null);
	}

	public String getAlterColumnSetNotNull()
	{
		return getProperty("alter.column.notnull.set", null);
	}

	public String getAlterColumnDropNotNull()
	{
		return getProperty("alter.column.notnull.drop", null);
	}

	/**
	 * The SQL to alter a column's default. If this returns null, getSetColumnDefault()
	 * and getDropColumnDefaultSql() should also be checked because some DBMS only
	 * allow setting or removing the column default
	 */
	public String getAlterColumnDefaultSql()
	{
		return getProperty("alter.column.default", null);
	}

	/**
	 * The SQL to set a column's default.
	 */
	public String getSetColumnDefaultSql()
	{
		return getProperty("alter.column.default.set", null);
	}

	public String getDropColumnDefaultSql()
	{
		return getProperty("alter.column.default.drop", null);
	}

	/**
	 * Returns the ALTER ... template to rename the given object type
	 * (e.g. TABLE, VIEW)
	 *
	 * @param type
	 * @return null if no template was configured for this dbms
	 */
	public String getRenameObjectSql(String type)
	{
		if (StringUtil.isBlank(type)) return null;
		return getProperty("alter." + getKeyValue(type) + ".rename", null);
	}

	/**
	 * Returns the ALTER ... template to change the schema for a given object type
	 *
	 * @param type
	 * @return null if no template was configured for this dbms
	 */
	public String getChangeSchemaSql(String type)
	{
		if (StringUtil.isBlank(type)) return null;
		return getProperty("alter." + getKeyValue(type) + ".change.schema", null);
	}

	/**
	 * Returns the ALTER ... template to change the schema for a given object type
	 *
	 * @param type
	 * @return null if no template was configured for this dbms
	 */
	public String getChangeCatalogSql(String type)
	{
		if (StringUtil.isBlank(type)) return null;
		return getProperty("alter." + getKeyValue(type) + ".change.catalog", null);
	}

	/**
	 * Returns the SQL to drop a primary key of a database object
	 * @param type the type of the object. e.g. table, materialized view
	 */
	public String getDropPrimaryKeySql(String type)
	{
		if (StringUtil.isBlank(type)) return null;
		return getProperty("alter." + getKeyValue(type) + ".drop.pk", null);
	}

	/**
	 * Returns the SQL to drop a constraint from a data object
	 * @param type the type of the object. e.g. table, materialized view
	 */
	public String getDropConstraint(String type)
	{
		if (StringUtil.isBlank(type)) return null;
		return getProperty("alter." + getKeyValue(type) + ".drop.constraint", null);
	}

	/**
	 * Returns the SQL to add a primary key to an object
	 *
	 * @param type the type of the object. e.g. table, materialized view
	 */
	public String getAddPK(String type)
	{
		return getAddPK(type, false);
	}

	public String getAddPK(String type, boolean checkDefault)
	{
		if (StringUtil.isBlank(type)) return null;
		String sql = getProperty("alter." + getKeyValue(type) + ".add.pk", null);
		if (StringUtil.isEmptyString(sql) && checkDefault)
		{
			sql = Settings.getInstance().getProperty("workbench.db.sql.alter." + getKeyValue(type) + ".add.pk", null);
		}
		return sql;
	}

	public boolean useInlineColumnComments()
	{
		return Settings.getInstance().getBoolProperty(prefix + "colcommentinline", false);
	}

	/**
	 * Checks if the current DBMS supports comments for the given DB object type
	 * @param objectType the type to be checked (e.g. TABLE, COLUMN)
	 * @return true if the DBMS supports comments for this type
	 */
	public boolean columnCommentAllowed(String objectType)
	{
		if (StringUtil.isBlank(objectType)) return false;
		String type = objectType.toLowerCase().trim().replace(' ', '_');
		List<String> types = Settings.getInstance().getListProperty(prefix + "columncomment.types", true, "table");
		return types.contains(type);
	}

	/**
	 * Setting to control the display of the auto-generated SELECT rules for views.
	 *
	 * @return true if the auto-generated SELECT rules should be excluded
	 */
	public static boolean getExcludePostgresDefaultRules()
	{
		return Settings.getInstance().getBoolProperty("workbench.db.postgresql.exclude.defaultselectrule", true);
	}

	public boolean useXmlAPI()
	{
		return Settings.getInstance().getBoolProperty(prefix + "use.xmlapi", false);
	}

	public boolean isClobType(String dbmsType)
	{
		if (dbmsType == null) return false;
		return Settings.getInstance().getBoolProperty(prefix + "isclob." + dbmsType, false);
	}

	public boolean pkIndexHasTableName()
	{
		return Settings.getInstance().getBoolProperty(prefix + "pkconstraint.is_table_name", false);
	}

	public boolean createTriggerNeedsAlternateDelimiter()
	{
		return Settings.getInstance().getBoolProperty(prefix + "alternate.delim.create.trigger", true);
	}

	public boolean getSearchAllSchemas()
	{
		return Settings.getInstance().getBoolProperty(prefix + "search.all.schemas", true);
	}

	public String getInlinePKKeyword()
	{
		return getProperty("sql.pk.inline", "PRIMARY KEY");
	}

	/**
	 * Returns a flag if the driver returns "ready-made" expressions for the DEFAULT value of a column.
	 */
	public boolean returnsValidDefaultExpressions()
	{
		return Settings.getInstance().getBoolProperty(prefix + "defaultvalue.isexpression", true);
	}

	/**
	 * Returns true if the JDBC driver returns the correct ResultSetMetadata only by preparing a statement.
	 *
	 * If this is false, the ResultSetMetadata is only returned after actually retrieving data through the
	 * statement.
	 *
	 */
	public boolean usePreparedStatementForQueryInfo()
	{
		return Settings.getInstance().getBoolProperty(prefix + "queryinfo.preparedstatement", false);
	}

	public void setUsePreparedStatementForQueryInfo(boolean flag)
	{
		Settings.getInstance().setProperty(prefix + "queryinfo.preparedstatement", flag);
	}

	public boolean alwaysUseSchemaForCompletion()
	{
		return Settings.getInstance().getBoolProperty(prefix + "completion.always_use.schema", false);
	}

	public boolean alwaysUseCatalogForCompletion()
	{
		return Settings.getInstance().getBoolProperty(prefix + "completion.always_use.catalog", false);
	}

	/**
	 * Returns the sqlstate for a unique (primary) key violation error for the DBMS.
	 *
	 * This value can be compared against SQLException.getSQLState() to test for such an error.
	 *
	 * Some DBMS only return exact information on pk violations through the sqlstate, some through
	 * the error code.
	 *
	 * @return the sql state that is defined for this DBMS or null if none is defined
	 * @see #getUniqueKeyViolationErrorCode()
	 */
	public String getUniqueKeyViolationErrorState()
	{
		return Settings.getInstance().getProperty(prefix + "errorstate.unique", null);
	}

	/**
	 * Returns the error code for a unique (primary) key violation error for the DBMS.
	 * This value can be compared against SQLException.getErrorCode() to test for such an error.
	 *
	 * Some DBMS only return exact information on pk violations through the sqlstate, some through
	 * the error code.
	 *
	 * @return the numeric value of the error code or -1 if none is defined
	 * @see #getUniqueKeyViolationErrorState()
	 */
	public int getUniqueKeyViolationErrorCode()
	{
		return Settings.getInstance().getIntProperty(prefix + "errorcode.unique", -1);
	}

	public boolean supportsResultMetaGetTable()
	{
		return Settings.getInstance().getBoolProperty(prefix + "resultmetadata.gettablename.supported", false);
	}

	/**
	 * Checks if this DBMS supports triggers on views.
	 * It is better to call TriggerReader#supportsTriggersOnViews() instead as that also
	 * checks for the current DBMS version.
	 *
	 * @see TriggerReader#supportsTriggersOnViews()
	 */
	public boolean supportsTriggersOnViews()
	{
		return Settings.getInstance().getBoolProperty(prefix + "view.trigger.supported", false);
	}

	public boolean supportsCatalogs()
	{
		return Settings.getInstance().getBoolProperty(prefix + "catalogs.supported", true);
	}

	public boolean supportsGetSchemaCall()
	{
		return Settings.getInstance().getBoolProperty(prefix + "jdbc.getschame.supported", true);
	}

	public boolean supportsSchemas()
	{
		return Settings.getInstance().getBoolProperty(prefix + "schemas.supported", true);
	}

	/**
	 * Return true if the driver for this DBMS is known to support CallableStatement.getParameterMetaData()
	 */
	public boolean supportsParameterMetaData()
	{
		return Settings.getInstance().getBoolProperty(prefix + "parameter.metadata.supported", true);
	}

	public boolean doEscapeSearchString()
	{
		return Settings.getInstance().getBoolProperty(prefix + "escape.searchstrings", true);
	}

	public String getSearchStringEscape()
	{
		return Settings.getInstance().getProperty(prefix + "searchstringescape", null);
	}

	public boolean fixFKRetrieval()
	{
		return Settings.getInstance().getBoolProperty(prefix + "fixfkretrieval", true);
	}

	public String getIdentifierQuoteString()
	{
		String propName = prefix + "identifier.quote";
		String quote = Settings.getInstance().getProperty(prefix + "quote.escape", null);
		if (quote != null)
		{
			LogMgr.logWarning("DbSettings.getIdentifierQuoteString()", "Deprecated property \"" + prefix + ".quote.escape\" used. Renaming to: " + propName);
			Settings.getInstance().removeProperty(prefix + "quote.escape");
			Settings.getInstance().setProperty(propName, quote);
		}
		else
		{
			quote = Settings.getInstance().getProperty(propName, null);
		}
		return quote;
	}

	public boolean useCacheForObjectInfo()
	{
		boolean global = Settings.getInstance().getBoolProperty("workbench.db.objectinfo.usecache", false);
		return Settings.getInstance().getBoolProperty(prefix + "objectinfo.usecache", global);
	}

	public boolean objectInfoWithFK()
	{
		boolean global = Settings.getInstance().getBoolProperty("workbench.db.objectinfo.includefk", false);
		return Settings.getInstance().getBoolProperty(prefix + "objectinfo.includefk", global);
	}

	public boolean objectInfoWithDependencies()
	{
		boolean global = Settings.getInstance().getBoolProperty("workbench.db.objectinfo.includedeps", false);
		return Settings.getInstance().getBoolProperty(prefix + "objectinfo.includedeps", global);
	}

	public String checkOpenTransactionsQuery()
	{
		return Settings.getInstance().getProperty(prefix + "opentransaction.query", null);
	}

	public String getCatalogSeparator()
	{
		return Settings.getInstance().getProperty(prefix + "separator.catalog", null);
	}

	public boolean useCatalogSeparatorForSchema()
	{
		return Settings.getInstance().getBoolProperty(prefix + "separator.catalog.forschema", false);
	}

	public String getSchemaSeparator()
	{
		return Settings.getInstance().getProperty(prefix + "separator.schema", ".");
	}

	public boolean createInlinePKConstraints()
	{
		return Settings.getInstance().getBoolProperty(prefix + "pk.inline", false);
	}

	public boolean createInlineFKConstraints()
	{
		return Settings.getInstance().getBoolProperty(prefix + "fk.inline", false);
	}

	public boolean supportsFkOption(String action, String type)
	{
		String toUse = type.toLowerCase().replace(' ', '_');
		return Settings.getInstance().getBoolProperty(prefix + "fk." + action.toLowerCase() + "." + toUse +".supported", true);
	}

	public boolean supportsMetaDataWildcards()
	{
		return Settings.getInstance().getBoolProperty(prefix + "metadata.retrieval.wildcards", true);
	}

	public boolean supportsMetaDataSchemaWildcards()
	{
		return supportsMetaDataWildcards("schema");
	}

	public boolean supportsMetaDataCatalogWildcards()
	{
		return supportsMetaDataWildcards("catalog");
	}

	public boolean supportsMetaDataNullPattern()
	{
		return Settings.getInstance().getBoolProperty(prefix + "metadata.pattern.tablename.null.supported", true);
	}

	private boolean supportsMetaDataWildcards(String type)
	{
		return Settings.getInstance().getBoolProperty(prefix + "metadata.retrieval.wildcards." + type, supportsMetaDataWildcards());
	}

	public int getLockTimoutForSqlServer()
	{
		return Settings.getInstance().getIntProperty(prefix + "dbexplorer.locktimeout", 2500);
	}

	public boolean endTransactionAfterConnect()
	{
		return Settings.getInstance().getBoolProperty(prefix + "afterconnect.finishtrans", false);
	}

	public String getTableSelectTemplate(String keyname)
	{
		String general = Settings.getInstance().getProperty("workbench.db.sql." + keyname + ".select", null);
		return getProperty(keyname + ".select", general);
	}

	public boolean getSwitchCatalogInExplorer()
	{
		return Settings.getInstance().getBoolProperty(prefix + "dbexplorer.switchcatalog", DbExplorerSettings.getSwitchCatalogInExplorer());
	}

	public boolean fixSqlServerAutoincrement()
	{
		return Settings.getInstance().getBoolProperty("workbench.db.microsoft_sql_server.autoincrement.fix", true);
	}

	public String getLowerFunctionTemplate()
	{
		return Settings.getInstance().getProperty(prefix + "sql.function.lower", null);
	}

	public String getDisabledConstraintKeyword()
	{
		return getProperty("sql.constraint.disabled", null);
	}

	public String getNoValidateConstraintKeyword()
	{
		return getProperty("sql.constraint.notvalid", null);
	}

	public boolean getUseStreamsForBlobExport()
	{
		return Settings.getInstance().getBoolProperty(prefix + "export.blob.use.streams", true);
	}

	public boolean getUseStreamsForClobExport()
	{
		return Settings.getInstance().getBoolProperty(prefix + "export.clob.use.streams", false);
	}

	public boolean getUseGenericExecuteForSelect()
	{
		boolean global = Settings.getInstance().getUseGenericExecuteForSelect();
		return Settings.getInstance().getBoolProperty(prefix + "select.executegeneric", global);
	}

	public boolean useCleanSQLForPreparedStatements()
	{
		return Settings.getInstance().getBoolProperty(prefix + "preparedstaments.cleansql", false);
	}

	public boolean supportsAutomaticFkIndexes()
	{
		return Settings.getInstance().getBoolProperty(prefix + "fk.index.automatic", false);
	}

	public boolean useReadUncommittedForDbExplorer()
	{
		return Settings.getInstance().getBoolProperty(prefix + "dbexplorer.use.read_uncommitted", false);
	}

	public boolean useFullSearchPathForCompletion()
	{
		return Settings.getInstance().getBoolProperty(prefix + "completion.full.searchpath", false);
	}

	public boolean cleanupTypeList()
	{
		return Settings.getInstance().getBoolProperty(prefix + "metadata.cleanup.types", false);
	}

	public Set<String> verbsWithoutUpdateCount()
	{
		return Collections.unmodifiableSet(noUpdateCountVerbs);
	}

	private void readNoUpdateCountVerbs()
	{
		List<String> verbs = Settings.getInstance().getListProperty(prefix + "no.updatecount.default", true);
		noUpdateCountVerbs.addAll(verbs);

		List<String> userVerbs = Settings.getInstance().getListProperty(prefix + "no.updatecount", true);
		for (String verb : userVerbs)
		{
			if (StringUtil.isEmptyString(verb)) continue;

			if (verb.charAt(0) == '-')
			{
				noUpdateCountVerbs.remove(verb.substring(1));
			}
			else
			{
				noUpdateCountVerbs.add(verb);
			}
		}
	}

	public boolean disableEscapesForDDL()
	{
		return Settings.getInstance().getBoolProperty(prefix + "ddl.disable.escapeprocessing", true);
	}

	public boolean hideOracleIdentitySequences()
	{
		return Settings.getInstance().getBoolProperty(prefix + "sequence.identity.hide", false);
	}

	public boolean useColumnNameForMetadata()
	{
		return Settings.getInstance().getBoolProperty(prefix + "metadata.retrieval.columnnames", true);
	}

	// Currently only used for Postgres
	public boolean returnAccessibleProceduresOnly()
	{
		return Settings.getInstance().getBoolProperty(prefix + "procedurelist.only.accessible", true);
	}

	public boolean getBoolProperty(String prop, boolean defaultValue)
	{
		return Settings.getInstance().getBoolProperty(prefix + prop, defaultValue);
	}

	public boolean generateColumnListInViews()
	{
		boolean all = DbExplorerSettings.getGenerateColumnListInViews();
		return Settings.getInstance().getBoolProperty(prefix + "create.view.columnlist", all);
	}

	public String getErrorColumnInfoRegex()
	{
		return getProperty("errorinfo.regex.column", null);
	}

	public String getErrorLineInfoRegex()
	{
		return getProperty("errorinfo.regex.line", null);
	}

	public String getErrorPosInfoRegex()
	{
		return getProperty("errorinfo.regex.position", null);
	}

	public boolean getErrorPosIsZeroBased()
	{
		return Settings.getInstance().getBoolProperty(prefix + "errorinfo.zerobased", true);
	}

	public boolean getErrorPosIncludesLeadingComments()
	{
		return Settings.getInstance().getBoolProperty(prefix + "errorinfo.leading.comment.included", false);
	}

	public boolean getCheckResultSetReadOnlyCols()
	{
		return Settings.getInstance().getBoolProperty(prefix + "resultset.columns.check.readonly", true);
	}

	public boolean getRetrieveGeneratedKeys()
	{
		return Settings.getInstance().getBoolProperty(prefix + "insert.retrieve.keys", true);
	}

	public Collection<String> getIgnoreCompletionSchemas()
	{
		return Settings.getInstance().getListProperty(prefix + "completion.ignore.schema", false, null);
	}

	public Set<String> getGrantorsToIgnore()
	{
		List<String> names = Settings.getInstance().getListProperty(prefix + "ignore.grantor", false);
		Set<String> result = CollectionUtil.caseInsensitiveSet();
		result.addAll(names);
		return result;
	}

	public Set<String> getGranteesToIgnore()
	{
		List<String> names = Settings.getInstance().getListProperty(prefix + "ignore.grantee", false);
		Set<String> result = CollectionUtil.caseInsensitiveSet();
		result.addAll(names);
		return result;
	}

	public boolean supportsSetSchema()
	{
		return Settings.getInstance().getBoolProperty(prefix + "supports.schema_change", false);
	}

	public EndReadOnlyTrans getAutoCloseReadOnlyTransactions()
	{
		String defaultSetting = Settings.getInstance().getProperty("workbench.sql.transaction.readonly.end", EndReadOnlyTrans.never.name());
		String value = Settings.getInstance().getProperty(prefix + "transaction.readonly.end", defaultSetting);
		if (value != null)
		{
			try
			{
				return EndReadOnlyTrans.valueOf(value);
			}
			catch (Exception e)
			{
				LogMgr.logWarning("DbSettings.getAutoCloseReadOnlyTransactions()", "Invalid end type '" + value + "' specified");
			}
		}
		return EndReadOnlyTrans.never;
	}

	public Set<Integer> getInformationalWarningCodes()
	{
		List<String> ids = Settings.getInstance().getListProperty(prefix + "warning.ignore.codes", false);
		if (ids.isEmpty()) return Collections.emptySet();
		Set<Integer> result = new HashSet<>(ids.size());
		for (String id :ids)
		{
			result.add(StringUtil.getIntValue(id, Integer.MIN_VALUE));
		}
		return result;
	}

	public Set<String> getInformationalWarningStates()
	{
		List<String> ids = Settings.getInstance().getListProperty(prefix + "warning.ignore.sqlstate", false);
		if (ids.isEmpty()) return Collections.emptySet();
		return new HashSet<>(ids);
	}

	public List<String> getSchemasToAdd()
	{
		return Settings.getInstance().getListProperty(prefix + "schemas.additional", false, null);
	}

	public boolean checkUniqueIndexesForPK()
	{
		boolean global = Settings.getInstance().getBoolProperty("workbench.db.pk.retrieval.checkunique", true);
		return Settings.getInstance().getBoolProperty(prefix + "pk.retrieval.checkunique", global);
	}

	public boolean getUpdateTableCheckPkOnly()
	{
		String propName = "updatetable.check.pkonly";
		boolean global = Settings.getInstance().getBoolProperty("workbench.db." + propName, false);
		return Settings.getInstance().getBoolProperty(prefix + propName, global);
	}

	public boolean useCompletionCacheForUpdateTableCheck()
	{
		String propName = "updatetable.check.use.cache";
		boolean global = Settings.getInstance().getBoolProperty("workbench.db." + propName, false);
		return Settings.getInstance().getBoolProperty(prefix + propName, global);
	}

	public String getLimitClause()
	{
		return getVersionedString("select.limit", null);
	}

	public boolean fixStupidMySQLZeroDate()
	{
		return Settings.getInstance().getBoolProperty(prefix + "timestamp.ignore.read.errors", false);
	}

	public boolean addWarningsOnError()
	{
		return Settings.getInstance().getBoolProperty(prefix + "error.include.warning", true);
	}
}
