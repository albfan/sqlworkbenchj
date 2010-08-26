/*
 * DbSettings.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
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
import java.util.Set;
import java.util.TreeSet;
import workbench.db.exporter.RowDataConverter;
import workbench.gui.dbobjects.TableSearchPanel;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.sql.commands.SingleVerbCommand;
import workbench.storage.BlobLiteralType;
import workbench.storage.DmlStatement;
import workbench.util.CaseInsensitiveComparator;
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
	private final String dbId;
	private boolean caseSensitive;
	private boolean useJdbcCommit;
	private boolean ddlNeedsCommit;

	private boolean neverQuoteObjects;
	private boolean reportsRealSizeAsDisplaySize;

	private boolean allowsMultipleGetUpdateCounts = true;
	private boolean supportsBatchedStatements;
	private boolean supportsCommentInSql = true;

	private Map<Integer, String> indexTypeMapping;
	public static final String IDX_TYPE_NORMAL = "NORMAL";
	private Set<String> updatingCommands;
	private final String prefix;

	public DbSettings(String id, String productName)
	{
		this.dbId = id;
		prefix = "workbench.db." + id + ".";
		Settings settings = Settings.getInstance();

		this.caseSensitive = settings.getBoolProperty(prefix + "casesensitive", false);
		this.useJdbcCommit = settings.getBoolProperty(prefix + "usejdbccommit", false);
		this.ddlNeedsCommit = settings.getBoolProperty(prefix + "ddlneedscommit", false);
		this.supportsCommentInSql = settings.getBoolProperty(prefix + "sql.embeddedcomments", true);

		List<String> quote = StringUtil.stringToList(settings.getProperty("workbench.db.neverquote",""));
		this.neverQuoteObjects = quote.contains(this.getDbId());
		this.allowsMultipleGetUpdateCounts = settings.getBoolProperty(prefix + "multipleupdatecounts", true);
		this.reportsRealSizeAsDisplaySize = settings.getBoolProperty(prefix + "charsize.usedisplaysize", false);
		this.supportsBatchedStatements = settings.getBoolProperty(prefix + "batchedstatements", false);
	}

	public String getDbId()
	{
		return this.dbId;
	}

	public boolean supportsCommentInSql()
	{
		return this.supportsCommentInSql;
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
			this.updatingCommands = new TreeSet<String>(new CaseInsensitiveComparator());

			String l = Settings.getInstance().getProperty("workbench.db.updatingcommands", null);
			List<String> commands = StringUtil.stringToList(l, ",", true, true);
			updatingCommands.addAll(commands);
			l = Settings.getInstance().getProperty(prefix + "updatingcommands", null);
			commands = StringUtil.stringToList(l, ",", true, true);
			updatingCommands.addAll(commands);
		}
		return updatingCommands.contains(verb);
	}

	public boolean getUseOracleDBMSMeta(String type)
	{
		if (type == null) return false;
		return Settings.getInstance().getBoolProperty("workbench.db.oracle.use.dbmsmeta." + type.trim().toLowerCase(), false);
	}

	public boolean useGetStringForClobs()
	{
		return Settings.getInstance().getBoolProperty(prefix + "clob.use.getstring", false);
	}

	public boolean useGetBytesForBlobs()
	{
		return Settings.getInstance().getBoolProperty(prefix + "blob.use.getbytes", false);
	}

	public boolean longVarcharIsClob()
	{
		return Settings.getInstance().getBoolProperty(prefix + "clob.longvarchar", true);
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
		return neverQuoteObjects;
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
	 * @see DmlStatement#execute(workbench.db.WbConnection)
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
		String verb = Settings.getInstance().getProperty("workbench.db.drop." + aType.toLowerCase() + ".cascade." + getDbId(), null);
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

		String ddl = Settings.getInstance().getProperty(prefix + "drop." + type.toLowerCase(), null);
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

	public boolean needParametersToDropFunction()
	{
		return Settings.getInstance().getBoolProperty(prefix + "drop.function.includeparameters", false);
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

	public String getValueTemplate(String dbmsType)
	{
		if (dbmsType == null) return null;
		return Settings.getInstance().getProperty(prefix + "valuetemplate." + dbmsType.toLowerCase(), null);
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

	public boolean supportSingleLineCommands()
	{
		String ids = Settings.getInstance().getProperty("workbench.db.checksinglelinecmd", "");
		if ("*".equals(ids)) return true;
		List dbs = StringUtil.stringToList(ids, ",", true, true);
		return dbs.contains(this.getDbId());
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

	public boolean supportShortInclude()
	{
		String ids = Settings.getInstance().getProperty("workbench.db.supportshortinclude", "");
		if ("*".equals(ids)) return true;
		List dbs = StringUtil.stringToList(ids, ",", true, true);
		return dbs.contains(this.getDbId());
	}

	public String getProcVersionDelimiter()
	{
		return Settings.getInstance().getProperty(prefix + "procversiondelimiter", null);
	}

	public boolean truncateNeedsCommit()
	{
		return Settings.getInstance().getBoolProperty(prefix + "truncate.commit", false);
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
		String viewTypes = Settings.getInstance().getProperty(prefix + "additional.viewtypes", "view").toLowerCase();
		List types = StringUtil.stringToList(viewTypes, ",", true, true, false);
		return types.contains(type.toLowerCase());
	}

	public boolean isSynonymType(String type)
	{
		if (type == null) return false;
		String synTypes = Settings.getInstance().getProperty(prefix + "synonymtypes", "synonym").toLowerCase();
		List types = StringUtil.stringToList(synTypes, ",", true, true, false);
		return types.contains(type.toLowerCase());
	}

	String mapIndexType(Object type)
	{
		if (type == null) return null;
		if (type instanceof Number)
		{
			return mapIndexType(((Number)type).intValue());
		}
		if (type instanceof String)
		{
			int t = StringUtil.getIntValue((String)type, Integer.MIN_VALUE);
			if (t == Integer.MIN_VALUE) return (String)type;
			return mapIndexType(t);
		}
		return null;
	}

	String mapIndexType(int type)
	{
		if (indexTypeMapping == null)
		{
			this.indexTypeMapping = new HashMap<Integer, String>();
			String map = Settings.getInstance().getProperty(prefix + "indextypes", null);
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
						indexTypeMapping.put(Integer.valueOf(value), mapping[1]);
					}
				}
			}
		}
		String dbmsType = this.indexTypeMapping.get(Integer.valueOf(type));
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
		String nameCase = Settings.getInstance().getProperty(prefix + "schemaname.case", null);
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
		String nameCase = Settings.getInstance().getProperty(prefix + "objectname.case", null);
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
		String query = Settings.getInstance().getProperty(prefix + "currentcatalog.query", null);
		return query;
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
		return Settings.getInstance().getProperty(prefix + "drop.column", null);
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
		return Settings.getInstance().getProperty(prefix + "drop.column.multi", null);
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
		return Settings.getInstance().getProperty(prefix + "add.column", null);
	}

	public boolean supportsSortedIndex()
	{
		return Settings.getInstance().getBoolProperty(prefix + "index.sorted", true);
	}

	public boolean includeSystemTablesInSelectable()
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
	 * Retrieves a "select" expression for the given datatype.
	 * The DbExplorer will use this expression instead of the "plain" column
	 * name to retrieve data for this data type. This can be used to make
	 * data readable in the DbExplorer for data types that are not natively supported
	 * by the JDBC driver.
	 *
	 * The expression must contain a placeholder for the column name.
	 *
	 * @param cleanType
	 * @return null if nothing is configured
	 * @see workbench.db.TableSelectBuilder#getSelectForTable(workbench.db.TableIdentifier)
	 * @see workbench.db.TableSelectBuilder#COLUMN_PLACEHOLDER
	 */
	public String getDataTypeExpression(String cleanType)
	{
		if (cleanType == null) return null;
		return Settings.getInstance().getProperty(prefix + "selectexpression." + cleanType.toLowerCase(), null);
	}

	/**
	 * Return a customized mapping of JDBC types to native datatypes.
	 * @see TypeMapper
	 */
	public String getJDBCTypeMapping()
	{
		return Settings.getInstance().getProperty(prefix + "typemap", null);
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
		if (!getUseCustomizedCreateTableRetrieval()) return true;
		boolean included = Settings.getInstance().getBoolProperty(prefix + "retrieve.create.table.index_included", false);
		return !included;
	}

	/**
	 * If customized SQL is configured to retrieve the source of a table, this setting controls if table and column
	 * comments should be gnerated by SQL Workbench.
	 * <br/>
	 * If no SQL is configured, this method always returns true. Otherwise the value of the config property <br/>
	 * <tt>workbench.db.[dbid].retrieve.create.table.comments_included</tt> is checked. If that is true to indicate that
	 * comments <i>are</i> returned by the custom SQL, this method returns false.
	 * <br/>
	 * 
	 * @see #getUseCustomizedCreateTableRetrieval()
	 * @return true if table comments (including columns) should be generated
	 */
	public boolean getGenerateTableComments()
	{
		if (!getUseCustomizedCreateTableRetrieval()) return true;
		boolean included = Settings.getInstance().getBoolProperty(prefix + "retrieve.create.table.comments_included", false);
		return !included;
	}

	/**
	 *
	 * @see #getUseCustomizedCreateTableRetrieval()
	 * @see workbench.db.TableSourceBuilder#getTableSource(workbench.db.TableIdentifier, java.util.List, workbench.storage.DataStore, workbench.storage.DataStore, boolean, java.lang.String, boolean)
	 * @return true if table grants should be generated even if the table source is retrieved by a customized statement
	 */
	public boolean getGenerateTableGrants()
	{
		if (!getUseCustomizedCreateTableRetrieval()) return true;
		boolean included = Settings.getInstance().getBoolProperty(prefix + "retrieve.create.table.grants_included", false);
		return !included;
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
		return Settings.getInstance().getProperty(prefix + "retrieve.create.table.query", null);
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
		return Settings.getInstance().getProperty(prefix + "alter.column.type", null);
	}

	public String getRenameColumnSql()
	{
		return Settings.getInstance().getProperty(prefix + "alter.column.rename", null);
	}

	public String getAlterColumnSetNotNull()
	{
		return Settings.getInstance().getProperty(prefix + "alter.column.notnull.set", null);
	}

	public String getAlterColumnDropNotNull()
	{
		return Settings.getInstance().getProperty(prefix + "alter.column.notnull.drop", null);
	}

	/**
	 * The SQL to alter a column's default. If this returns null, getSetColumnDefault()
	 * and getDropColumnDefaultSql() should also be checked because some DBMS only
	 * allow setting or removing the column default
	 */
	public String getAlterColumnDefaultSql()
	{
		return Settings.getInstance().getProperty(prefix + "alter.column.default", null);
	}

	public String getSetColumnDefaultSql()
	{
		return Settings.getInstance().getProperty(prefix + "alter.column.default.set", null);
	}

	public String getDropColumnDefaultSql()
	{
		return Settings.getInstance().getProperty(prefix + "alter.column.default.drop", null);
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
		return Settings.getInstance().getProperty(prefix + "alter." + type.trim().toLowerCase() + ".rename", null);
	}

	/**
	 * Returns the SQL to drop a primary key of a database object
	 * @param type the type of the object. e.g. table, materialized view
	 */
	public String getDropPrimaryKeySql(String type)
	{
		if (StringUtil.isBlank(type)) return null;
		return Settings.getInstance().getProperty(prefix + "alter." + type.trim().toLowerCase() + ".drop.pk", null);
	}

	/**
	 * Returns the SQL to drop a constriant from a data object
	 * @param type the type of the object. e.g. table, materialized view
	 */
	public String getDropConstraint(String type)
	{
		if (StringUtil.isBlank(type)) return null;
		return Settings.getInstance().getProperty(prefix + "alter." + type.trim().toLowerCase() + ".drop.constraint", null);
	}

	/**
	 * Returns the SQL to add a primary key to an object
	 *
	 * @param type the type of the object. e.g. table, materialized view
	 */
	public String getAddPK(String type)
	{
		if (StringUtil.isBlank(type)) return null;
		return Settings.getInstance().getProperty(prefix + "alter." + type.trim().toLowerCase() + ".add.pk", null);
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

	public boolean isClobType(String dbmsType)
	{
		if (dbmsType == null) return false;
		return Settings.getInstance().getBoolProperty(prefix + "isclob." + dbmsType, false);
	}

	public boolean pkIndexHasTableName()
	{
		return Settings.getInstance().getBoolProperty(prefix + ".pkconstraint.is_table_name", false);
	}
}
