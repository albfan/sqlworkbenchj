/*
 * DbMetadata.java
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

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.derby.DerbyTypeReader;
import workbench.db.firebird.FirebirdDomainReader;
import workbench.db.h2database.H2ConstantReader;
import workbench.db.h2database.H2DomainReader;
import workbench.db.hsqldb.HsqlTypeReader;
import workbench.db.ibm.DB2TypeReader;
import workbench.db.ibm.Db2ProcedureReader;
import workbench.db.ibm.InformixDataTypeResolver;
import workbench.db.mssql.SqlServerDataTypeResolver;
import workbench.db.mssql.SqlServerObjectListEnhancer;
import workbench.db.mssql.SqlServerRuleReader;
import workbench.db.mssql.SqlServerSchemaInfoReader;
import workbench.db.mssql.SqlServerTypeReader;
import workbench.db.mssql.SqlServerUtil;
import workbench.db.mysql.MySQLTableCommentReader;
import workbench.db.nuodb.NuoDBDomainReader;
import workbench.db.oracle.DbmsOutput;
import workbench.db.oracle.OracleDataTypeResolver;
import workbench.db.oracle.OracleObjectListEnhancer;
import workbench.db.oracle.OracleTableDefinitionReader;
import workbench.db.oracle.OracleTypeReader;
import workbench.db.postgres.PostgresDataTypeResolver;
import workbench.db.postgres.PostgresDomainReader;
import workbench.db.postgres.PostgresEnumReader;
import workbench.db.postgres.PostgresRangeTypeReader;
import workbench.db.postgres.PostgresRuleReader;
import workbench.db.postgres.PostgresTypeReader;
import workbench.db.sqlite.SQLiteDataTypeResolver;

import workbench.storage.DataStore;
import workbench.storage.DatastoreTransposer;
import workbench.storage.RowDataListSorter;
import workbench.storage.SortDefinition;
import workbench.storage.filter.AndExpression;
import workbench.storage.filter.StringEqualsComparator;

import workbench.sql.syntax.SqlKeywordHelper;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * Retrieve meta data information from the database.
 * This class returns more information than the generic JDBC DatabaseMetadata.
 *
 * @author Thomas Kellerer
 */
public class DbMetadata
	implements QuoteHandler
{
	public static final String DBID_ORA = "oracle";
	public static final String DBID_PG = "postgresql";
	public static final String DBID_MS = "microsoft_sql_server";
	public static final String DBID_VERTICA = "vertica_database";
	public static final String DBID_MYSQL = "mysql";
	public static final String DBID_FIREBIRD = "firebird";
	public static final String DBID_DB2_LUW = "db2";
	public static final String DBID_TERADATA = "teradata";
	public static final String DBID_H2 = "h2";

	public static final String MVIEW_NAME = "MATERIALIZED VIEW";
	private final String[] EMPTY_STRING_ARRAY = new String[]{};

	private final Object readerLock = new Object();

	private String schemaTerm;
	private String catalogTerm;
	private String productName;
	private String dbId;

	private MetaDataSqlManager metaSqlMgr;
	private DatabaseMetaData metaData;
	private WbConnection dbConnection;

	private SynonymReader synonymReader;
	private ObjectListEnhancer objectListEnhancer;
	private TableDefinitionReader definitionReader;
	private DataTypeResolver dataTypeResolver;
	private SequenceReader sequenceReader;
	private ProcedureReader procedureReader;
	private SchemaInformationReader schemaInfoReader;
	private IndexReader indexReader;
	private List<ObjectListExtender> extenders = new ArrayList<>();

	private DbmsOutput oraOutput;

	private boolean isOracle;
	private boolean isPostgres;
	private boolean isHsql;
	private boolean isFirebird;
	private boolean isSqlServer;
	private boolean isMySql;
	private boolean isApacheDerby;
	private boolean isExcel;
	private boolean isAccess;
	private boolean isH2;

	private String quoteCharacter;
	private final Set<String> keywords = CollectionUtil.caseInsensitiveSet();
	private final Set<String> reservedWords = CollectionUtil.caseInsensitiveSet();

	private String baseTableTypeName;

	private Set<String> tableTypesList;
	private String[] tableTypesArray;
	private String[] selectableTypes;
	private Set<String> schemasToIgnore;
	private Set<String> catalogsToIgnore;

	private DbSettings dbSettings;
	private ViewReader viewReader;
	private final char catalogSeparator;
	private SelectIntoVerifier selectIntoVerifier;
	private Set<String> tableTypesFromDriver;
	private int maxTableNameLength;

	private boolean supportsGetSchema;

	public DbMetadata(WbConnection aConnection)
		throws SQLException
	{
		this.dbConnection = aConnection;
		this.metaData = aConnection.getSqlConnection().getMetaData();

		try
		{
			this.schemaTerm = this.metaData.getSchemaTerm();
			LogMgr.logDebug("DbMetadata.<init>", "Schema term: " + schemaTerm);
		}
		catch (SQLException e)
		{
			LogMgr.logWarning("DbMetadata.<init>", "Could not retrieve Schema term: " + e.getMessage());
			this.schemaTerm = "Schema";
		}

		try
		{
			this.catalogTerm = this.metaData.getCatalogTerm();
			LogMgr.logDebug("DbMetadata.<init>", "Catalog term: " + catalogTerm);
		}
		catch (SQLException e)
		{
			LogMgr.logWarning("DbMetadata.<init>", "Could not retrieve Catalog term: " + e.getMessage());
			this.catalogTerm = "Catalog";
		}

		// Some JDBC drivers do not return a value for getCatalogTerm() or getSchemaTerm()
		// and don't throw an Exception. This is to ensure that our getCatalogTerm() will
		// always return something usable.
		if (StringUtil.isBlank(this.schemaTerm)) this.schemaTerm = "Schema";
		if (StringUtil.isBlank(this.catalogTerm))	this.catalogTerm = "Catalog";

		try
		{
			this.productName = this.metaData.getDatabaseProductName();
		}
		catch (SQLException e)
		{
			LogMgr.logWarning("DbMetadata.<init>", "Could not retrieve Database Product name", e);
			this.productName = aConnection.getProfile().getDriverclass();
		}

		String productLower = this.productName.toLowerCase();

		if (productLower.contains("postgres"))
		{
			this.isPostgres = true;
			this.dataTypeResolver = new PostgresDataTypeResolver();

			extenders.add(new PostgresDomainReader());
			if (JdbcUtils.hasMinimumServerVersion(dbConnection, "8.3"))
			{
				extenders.add(new PostgresEnumReader());
			}
			extenders.add(new PostgresRuleReader());
			PostgresTypeReader typeReader = new PostgresTypeReader();
			objectListEnhancer = typeReader;
			extenders.add(typeReader);
			if (JdbcUtils.hasMinimumServerVersion(dbConnection, "9.2") && PostgresRangeTypeReader.retrieveRangeTypes())
			{
				extenders.add(new PostgresRangeTypeReader());
			}
		}
		else if (productLower.contains("oracle") && !productLower.contains("lite ordbms"))
		{
			isOracle = true;
			dataTypeResolver = new OracleDataTypeResolver(aConnection);
			definitionReader = new OracleTableDefinitionReader(aConnection);
			extenders.add(new OracleTypeReader());
			objectListEnhancer = new OracleObjectListEnhancer(); // to cleanup MVIEW type information
		}
		else if (productLower.contains("hsql"))
		{
			this.isHsql = true;
			if (JdbcUtils.hasMinimumServerVersion(dbConnection, "2.2"))
			{
				extenders.add(new HsqlTypeReader());
			}
		}
		else if (productLower.contains("firebird"))
		{
			this.isFirebird = true;
			// Jaybird 2.x reports the Firebird version in the
			// productname. To ease the DBMS handling we'll use the same
			// product name that is reported with the 1.5 driver.
			// Otherwise the DBID would look something like:
			// firebird_2_0_wi-v2_0_1_12855_firebird_2_0_tcp__wallace__p10
			dbId = "firebird";

			// because the dbId is already initialized, we need to log it here
			LogMgr.logInfo("DbMetadata.<init>", "Using DBID=" + this.dbId);
			extenders.add(new FirebirdDomainReader());
		}
		else if (productLower.contains("microsoft") && productLower.contains("sql server"))
		{
			// checking for "microsoft" is important, because apparently the jTDS driver
			// confusingly identifies a Sybase server as "SQL Server"
			isSqlServer = true;

			if (SqlServerTypeReader.versionSupportsTypes(dbConnection))
			{
				extenders.add(new SqlServerTypeReader());
			}

			if (SqlServerUtil.isSqlServer2000(dbConnection))
			{
				extenders.add(new SqlServerRuleReader());
			}

			objectListEnhancer = new SqlServerObjectListEnhancer();
			dataTypeResolver = new SqlServerDataTypeResolver();
			if (Settings.getInstance().getBoolProperty("workbench.db.microsoft_sql_server.use.schemareader", true)
				  && SqlServerUtil.isSqlServer2008(dbConnection))
			{
				// SqlServerSchemaInfoReader will cache the user's default schema
				schemaInfoReader = new SqlServerSchemaInfoReader(dbConnection);
			}
		}
		else if (productLower.contains("db2"))
		{
			procedureReader = new Db2ProcedureReader(dbConnection, getDbId());

			// Generated columns are not available on the host version...
			if (getDbId().equals("db2"))
			{
				extenders.add(new DB2TypeReader());
			}
		}
		else if (productLower.contains("mysql"))
		{
			this.objectListEnhancer = new MySQLTableCommentReader();
			this.isMySql = true;
		}
		else if (productLower.contains("cloudscape"))
		{
			this.isApacheDerby = true;
		}
		else if (productLower.contains("derby"))
		{
			this.isApacheDerby = true;
			if (JdbcUtils.hasMinimumServerVersion(dbConnection, "10.6"))
			{
				extenders.add(new DerbyTypeReader());
			}
		}
		else if (productLower.equals("nuodb"))
		{
			extenders.add(new NuoDBDomainReader());
		}
		else if (productLower.contains("sqlite"))
		{
			dataTypeResolver = new SQLiteDataTypeResolver();
		}
		else if (productLower.contains("excel"))
		{
			isExcel = true;
		}
		else if (productLower.contains("access"))
		{
			isAccess = true;
		}
		else if (productLower.equals("h2"))
		{
			isH2 = true;
			extenders.add(new H2DomainReader());
			extenders.add(new H2ConstantReader());
		}
		else if (productLower.contains("informix"))
		{
			this.dataTypeResolver = new InformixDataTypeResolver();
		}

		if (schemaInfoReader == null)
		{
			this.schemaInfoReader = new GenericSchemaInfoReader(this.dbConnection, this.getDbId());
		}

		if (this.dataTypeResolver == null)
		{
			this.dataTypeResolver = new DefaultDataTypeResolver();
		}

		if (definitionReader == null)
		{
			definitionReader = new JdbcTableDefinitionReader(dbConnection);
		}

		try
		{
			this.quoteCharacter = this.metaData.getIdentifierQuoteString();
			LogMgr.logDebug("DbMetadata.<init>", "Identifier quote character obtained from driver: " + quoteCharacter);
		}
		catch (Exception e)
		{
			this.quoteCharacter = null;
			LogMgr.logError("DbMetadata.<init>", "Error when retrieving identifier quote character", e);
		}

		this.dbSettings = new DbSettings(this.getDbId());

		String quote = dbSettings.getIdentifierQuoteString();
		if (quote != null)
		{
			if ("<none>".equals(quote))
			{
				this.quoteCharacter = "";
			}
			else
			{
				this.quoteCharacter = quote;
			}
			LogMgr.logDebug("DbMetadata.<init>", "Using configured identifier quote character: >" + quoteCharacter + "<");
		}

		if (StringUtil.isBlank(quoteCharacter))
		{
			this.quoteCharacter = "\"";
		}
		LogMgr.logInfo("DbMetadata.<init>", "Using identifier quote character: " + quoteCharacter);
		LogMgr.logInfo("DbMetadata.<init>", "Using search string escape character: " + getSearchStringEscape());

		baseTableTypeName = Settings.getInstance().getProperty("workbench.db.basetype.table." + this.getDbId(), "TABLE");

		Collection<String> ttypes = Settings.getInstance().getListProperty("workbench.db." + getDbId() + ".tabletypes", false, null);
		if (ttypes.isEmpty())
		{
			ttypes.addAll(retrieveTableTypes());
			Iterator<String> itr = ttypes.iterator();
			while (itr.hasNext())
			{
				String type = itr.next().toLowerCase();
				// we don't want regular views in this list
				if (type.contains("view") && !type.equalsIgnoreCase("materialized view"))
				{
					itr.remove();
				}
			}
			LogMgr.logDebug("DbMetadata.<init>", "Using table types returned by the JDBC driver: " + ttypes);
		}
		else
		{
			LogMgr.logInfo("DbMetadata.<init>", "Using configured table types: " + ttypes);
		}

		tableTypesList = CollectionUtil.caseInsensitiveSet();
		tableTypesList.addAll(ttypes);

		tableTypesArray = StringUtil.toArray(tableTypesList, true);

		// The selectableTypes array will be used
		// to fill the completion cache. In that case
		// we do not want system tables included (which
		// is done for the objectsWithData as that
		// drives the "Data" tab in the DbExplorer)
		Set<String> types = getObjectsWithData();

		if (!dbSettings.includeSystemTablesInSelectable())
		{
			Iterator<String> itr = types.iterator();
			while (itr.hasNext())
			{
				String s = itr.next();
				if (s.toUpperCase().contains("SYSTEM"))
				{
					itr.remove();
				}
			}
		}

		selectableTypes = StringUtil.toArray(types, true);
		selectIntoVerifier = new SelectIntoVerifier(getDbId());

		String sep = getDbSettings().getCatalogSeparator();
		if (sep == null)
		{
			try
			{
				sep = metaData.getCatalogSeparator();
			}
			catch (Exception e)
			{
				LogMgr.logError("DbMetadata.<init>", "Could not retrieve catalog separator", e);
			}
		}

		if (StringUtil.isEmptyString(sep))
		{
			catalogSeparator = '.';
		}
		else
		{
			catalogSeparator = sep.charAt(0);
		}

		try
		{
			this.maxTableNameLength = metaData.getMaxTableNameLength();
		}
		catch (SQLException sql)
		{
			LogMgr.logWarning("DbMetadata.<init>", "Driver does not support getMaxTableNameLength()", sql);
			this.maxTableNameLength = 0;
		}

		supportsGetSchema = dbSettings.supportsGetSchemaCall();

		LogMgr.logInfo("DbMetadata.<init>", "Using catalog separator: " + catalogSeparator);
	}

	public int getMaxTableNameLength()
	{
		return maxTableNameLength;
	}

	/**
	 * Wrapper around DatabaseMetadata.getSearchStringEscape() that does not throw an exception.
	 *
	 * It also allows overwriting the driver's escape character through DbSettings in case
	 * the driver returns the wrong information (like Oracle's driver did for some time).
	 *
	 * @return the escape characters to mask wildcards in a string literal
	 */
	public final String getSearchStringEscape()
	{
		// using a config property to override the driver's behaviour
		// is necessary because some Oracle drivers return the wrong escape character
		String escape = getDbSettings().getSearchStringEscape();
		if (escape != null)
		{
			return escape;
		}

		try
		{
			return metaData.getSearchStringEscape();
		}
		catch (Throwable e)
		{
			// Should not happen
			return null;
		}
	}

	public char getCatalogSeparator()
	{
		return catalogSeparator;
	}

	public char getSchemaSeparator()
	{
		if (dbSettings.useCatalogSeparatorForSchema()) return getCatalogSeparator();
		String sep = dbSettings.getSchemaSeparator();
		if (sep == null) return '.';
		return sep.charAt(0);
	}

	public MetaDataSqlManager getMetaDataSQLMgr()
	{
		if (this.metaSqlMgr == null)
		{
			this.metaSqlMgr = new MetaDataSqlManager(productName, this.dbConnection.getDatabaseVersion());
		}
		return this.metaSqlMgr;
	}

	public ProcedureReader getProcedureReader()
	{
		synchronized (readerLock)
		{
			if (this.procedureReader == null)
			{
				this.procedureReader = ReaderFactory.getProcedureReader(this);
			}
			return procedureReader;
		}
	}

	public ViewReader getViewReader()
	{
		synchronized (readerLock)
		{
			if (this.viewReader == null)
			{
				viewReader = ViewReaderFactory.createViewReader(this.dbConnection);
			}
			return viewReader;
		}
	}

	public String getQuoteCharacter()
	{
		return this.quoteCharacter;
	}

	public String getBaseTableTypeName()
	{
		return baseTableTypeName;
	}

	public String[] getTableTypesArray()
	{
		return Arrays.copyOf(tableTypesArray, tableTypesArray.length);
	}

	public String[] getTablesAndViewTypes()
	{
		List<String> types = new ArrayList<>(tableTypesList);
		types.add(getViewTypeName());
		return types.toArray(EMPTY_STRING_ARRAY);
	}

	public String[] getSelectableTypes()
	{
		return Arrays.copyOf(selectableTypes, selectableTypes.length);
	}

	public List<String> getTableTypes()
	{
		return new ArrayList<>(tableTypesList);
	}

	public String getMViewTypeName()
	{
		return MVIEW_NAME;
	}

	public String getViewTypeName()
	{
		return "VIEW";
	}

	public DataTypeResolver getDataTypeResolver()
	{
		return this.dataTypeResolver;
	}

	public DatabaseMetaData getJdbcMetaData()
	{
		return this.metaData;
	}

	public WbConnection getWbConnection()
	{
		return this.dbConnection;
	}

	public Connection getSqlConnection()
	{
		return this.dbConnection.getSqlConnection();
	}

	public IndexReader getIndexReader()
	{
		synchronized (readerLock)
		{
			if (indexReader == null)
			{
				indexReader = ReaderFactory.getIndexReader(this);
			}
			return this.indexReader;
		}
	}

	public TableDefinitionReader getTableDefinitionReader()
	{
		return definitionReader;
	}

	/**
	 * Check if the given DB object type can contain data. i.e. if
	 * a SELECT FROM can be run against this type.
	 * <br/>
	 * By default these are objects of type
	 * <ul>
	 *	<li>table</li>
	 *  <li>view</li>
	 *  <li>synonym</li>
	 *  <li>system view</li>
	 *  <li>system table</li>
	 * </ul>
	 * <br/>
	 * The list of types can be defined per DBMS using the property
	 * <literal>workbench.db.objecttype.selectable.[dbid]</literal>.
	 * <br/>
	 * If that property is empty, the above defaults are used.
	 *
	 * @see #getObjectsWithData()
	 * @see #retrieveTableTypes()
	 */
	public boolean objectTypeCanContainData(String type)
	{
		if (type == null) return false;
		return getObjectsWithData().contains(type);
	}

	public final Set<String> getObjectsWithData()
	{
		Set<String> objectsWithData = CollectionUtil.caseInsensitiveSet();
		objectsWithData.addAll(retrieveTableTypes());

		String keyPrefix = "workbench.db.objecttype.selectable.";
		String defValue = Settings.getInstance().getProperty(keyPrefix + "default", null);
		String types = Settings.getInstance().getProperty(keyPrefix + getDbId(), defValue);

		if (types != null)
		{
			List<String> l = StringUtil.stringToList(types.toLowerCase(), ",", true, true);
			objectsWithData.addAll(l);
		}

		if (objectsWithData.isEmpty())
		{
			// make sure we have at some basic information in here
			objectsWithData.add("table");
			objectsWithData.add("view");
			objectsWithData.add("synonym");
			objectsWithData.add("system view");
			objectsWithData.add("system table");
		}

		List<String> notSelectable = Settings.getInstance().getListProperty("workbench.db.objecttype.not.selectable." + getDbId(), false);
		objectsWithData.removeAll(notSelectable);

		return objectsWithData;
	}

	/**
	 *	Return the name of the DBMS as reported by the JDBC driver.
	 * <br/>
	 * For configuration purposes the DBID should be used as that can be part of a key
	 * in a properties file.
	 * @see #getDbId()
	 */
	public final String getProductName()
	{
		return this.productName;
	}

	/**
	 * Return a clean version of the productname that can be used as the part of a properties key.
	 *
	 * @see #getProductName()
	 */
	public final String getDbId()
	{
		if (this.dbId == null)
		{
			this.dbId = this.productName.replaceAll("[ \\(\\)\\[\\]/$,.'=\"]", "_").toLowerCase();

			if (productName.startsWith("DB2"))
			{
				// DB/2 for Host-Systems
				// apparently DB2 for z/OS identifies itself as "DB2" whereas
				// DB2 for AS/400 identifies itself as "DB2 UDB for AS/400"
				if (productName.contains("AS/400") || productName.contains("iSeries"))
				{
					dbId = "db2i";
				}
				else if(productName.equals("DB2"))
				{
					dbId = "db2h";
				}
				else
				{
					// Everything else is LUW (Linux, Unix, Windows)
					dbId = "db2";
				}
			}
			else if (productName.startsWith("HSQL"))
			{
				// As the version number is appended to the productname
				// we need to ignore that here. The properties configured
				// in workbench.settings using the DBID are (currently) identically
				// for all HSQL versions.
				dbId = "hsql_database_engine";
			}
			else if (productName.toLowerCase().contains("ucanaccess"))
			{
				dbId = "ucanaccess";
			}
			LogMgr.logInfo("DbMetadata.<init>", "Using DBID=" + this.dbId);
		}
		return this.dbId;
	}

	public final DbSettings getDbSettings()
	{
		return this.dbSettings;
	}

	public boolean isSelectIntoNewTable(String sql)
	{
		return selectIntoVerifier.isSelectIntoNewTable(sql);
	}

	public boolean supportsSelectIntoNewTable()
	{
		return selectIntoVerifier != null && selectIntoVerifier.supportsSelectIntoNewTable();
	}

	public boolean isMySql() { return this.isMySql; }
	public boolean isPostgres() { return this.isPostgres; }
  public boolean isOracle() { return this.isOracle; }
	public boolean isHsql() { return this.isHsql; }
	public boolean isFirebird() { return this.isFirebird; }
	public boolean isSqlServer() { return this.isSqlServer; }
	public boolean isApacheDerby() { return this.isApacheDerby; }
	public boolean isH2() { return this.isH2; }

	/**
	 * Clears the cached list of catalogs to ignore.
	 * Intended for testing purposes.
	 * The list is re-read from the global settings if needed
	 */
	public void clearIgnoredCatalogs()
	{
		catalogsToIgnore = null;
	}

	/**
	 * Clears the cached list of schemas to ignore.
	 * Intended for testing purposes.
	 * The list is re-read from the global settings if needed
	 */
	public void clearIgnoredSchemas()
	{
		schemasToIgnore = null;
	}

	/**
	 * Returns true if the given schema name can be ignored for the current DBMS.
	 * <br/>
	 * The information which schema names can be ignored for the current DBMS is
	 * retrieved from the settings file through the property
	 * <literal>workbench.sql.ignoreschema.[dbid]</literal>
	 *
	 * @param schema
	 * @return true if the supplied schema name should not be used
	 */
	public boolean ignoreSchema(String schema)
	{
		return ignoreSchema(schema, null);
	}

	public boolean ignoreSchema(String schema, String currentSchema)
	{
		if (StringUtil.isEmptyString(schema)) return true;
		if (dbSettings.alwaysUseSchema()) return false;

		if (schemasToIgnore == null)
		{
			schemasToIgnore = readIgnored("schema", null);
		}

		if (schemasToIgnore.contains("$current"))
		{
			String current = (currentSchema == null ? getCurrentSchema() : currentSchema);
			if (current != null)
			{
				return SqlUtil.objectNamesAreEqual(schema, current);
			}
		}
		return schemasToIgnore.contains("*") || schemasToIgnore.contains(schema);
	}

	private Set<String> readIgnored(String type, String defaultList)
	{
		Set<String> result;
		String ids = Settings.getInstance().getProperty("workbench.sql.ignore" + type + "." + this.getDbId(), defaultList);
		if (ids != null)
		{
			result = new TreeSet<>(StringUtil.stringToList(ids, ","));
		}
		else
		{
			 result = Collections.emptySet();
		}
		return result;
	}

	/**
	 * For testing purposes only
	 */
	public void resetSchemasToIgnores()
	{
		schemasToIgnore = null;
	}

	/**
	 * Check if the given {@link TableIdentifier} requires
	 * the usage of the schema for a DML or DDL statement
	 * statement.
	 * <br/>
	 * If the current DBMS does not support schemas, it returns false.
	 * <br/>
	 * If the current  schema is different to the table's schema, it returns true.
	 * <br/>
	 * If either no current schema is available, or if is the same as the table's schema
	 * the result of ignoreSchema() is checked to leave out e.g. the PUBLIC schema in Postgres or H2
	 *
	 * @see #ignoreSchema(java.lang.String)
	 * @see DbSettings#supportsSchemas()
	 */
	public boolean needSchemaInDML(TableIdentifier table)
	{
		try
		{
			String tblSchema = table.getSchema();

			// Object names may never be prefixed with PUBLIC in Oracle
			if (this.isOracle && "PUBLIC".equalsIgnoreCase(tblSchema)) return false;

			if (!dbSettings.supportsSchemas()) return false;
			if (dbSettings.alwaysUseSchema()) return true;

			String currentSchema = getCurrentSchema();

			// If the current schema is not the one of the table, the schema is needed in DML statements
			// to avoid wrong implicit schema resolution
			if (currentSchema != null && !currentSchema.equalsIgnoreCase(tblSchema)) return true;

			// otherwise check if the schema should be ignored
		  return (!ignoreSchema(tblSchema, currentSchema));
		}
		catch (Throwable th)
		{
			return true;
		}
	}

	/**
	 * Check if the given {@link TableIdentifier} requires the usage of a catalog name for a DML statement.
	 * <br/>
	 * If the current DB engine is Microsoft Access, this method always returns true.
	 * <br/>
   *
	 * @see #ignoreCatalog(java.lang.String)
	 * @see DbSettings#alwaysUseCatalog()
	 * @see DbSettings#useCatalogInDML()
	 * @see DbSettings#needsCatalogIfNoCurrent()
	 */
	public boolean needCatalogInDML(TableIdentifier table)
	{
		if (this.isAccess) return true;

		String cat = table.getCatalog();
		if (StringUtil.isEmptyString(cat)) return false;

		if (this.isExcel)
		{
			String currentCat = getCurrentCatalog();
			if (StringUtil.isEmptyString(currentCat)) return true;

			// Excel puts the directory into the catalog
			// so we need to normalize the directory name
			File c1 = new File(cat);
			File c2 = new File(currentCat);
			return !c1.equals(c2);
		}

		if (!dbSettings.supportsCatalogs()) return false;
		if (!dbSettings.useCatalogInDML()) return false;
		if (dbSettings.alwaysUseCatalog()) return true;

		if (this.dbSettings.needsCatalogIfNoCurrent())
		{
			String currentCat = getCurrentCatalog();
			if (StringUtil.isEmptyString(currentCat))
			{
				return true;
			}
		}
		return !ignoreCatalog(cat);
	}

	/**
	 * Checks if the given catalog name should be ignored
	 * in SQL statements.
	 *
	 * @param catalog the catalog name to check
	 * @return true if the catalog is not needed in SQL statements
	 */
	public boolean ignoreCatalog(String catalog)
	{
		if (catalog == null) return true;
		if (dbSettings.alwaysUseCatalog()) return false;
		if (!dbSettings.supportsCatalogs()) return true;

		if (catalogsToIgnore == null)
		{
			catalogsToIgnore = readIgnored("catalog", "$current");
		}
		if (catalogsToIgnore.contains("$current"))
		{
			String current = getCurrentCatalog();
			if (current != null)
			{
				return SqlUtil.objectNamesAreEqual(current, catalog);
			}
		}
		return catalogsToIgnore.contains("*") || catalogsToIgnore.contains(catalog);
	}

	/**
	 * Wrapper for DatabaseMetaData.supportsBatchUpdates() that throws
	 * no exception. If any error occurs, false will be returned
	 */
	public boolean supportsBatchUpdates()
	{
		try
		{
			return this.metaData.supportsBatchUpdates();
		}
		catch (SQLException e)
		{
			return false;
		}
	}

	/**
	 * Returns the type of the passed TableIdentifier.
	 * <br/>
	 * This could be VIEW, TABLE, SYNONYM, ...
	 * <br/>
	 * If the JDBC driver does not return the object through the getObjects()
	 * method, null is returned, otherwise the value reported in TABLE_TYPE
	 * <br/>
	 * If there is more than object with the same name but different types
	 * (is there a DB that supports that???) than the type of the first object found
   * will be returned.
	 * @see #getObjects(String, String, String, String[])
	 */
	public String getObjectType(TableIdentifier table)
	{
		String type = null;
		try
		{
			TableIdentifier tbl = table.createCopy();
			tbl.adjustCase(this.dbConnection);
			tbl.checkIsQuoted(this);
			TableIdentifier target = findObject(tbl);
			if (target != null)
			{
				type = target.getType();
			}
		}
		catch (Exception e)
		{
			type = null;
		}
		return type;
	}

	public boolean isReservedWord(String name)
	{
		synchronized (reservedWords)
		{
			if (reservedWords.isEmpty())
			{
				SqlKeywordHelper helper = new SqlKeywordHelper(this.getDbId());
				reservedWords.addAll(helper.getReservedWords());
				reservedWords.addAll(helper.getOperators());
			}
			return this.reservedWords.contains(name);
		}
	}

	public boolean isKeyword(String name)
	{
		synchronized (keywords)
		{
			if (keywords.isEmpty())
			{
				SqlKeywordHelper helper = new SqlKeywordHelper(this.getDbId());
				keywords.addAll(helper.getKeywords());
				keywords.addAll(helper.getOperators());

				try
				{
					String words = metaData.getSQLKeywords();
					if (StringUtil.isNonBlank(words))
					{
						List<String> driverKeywords = StringUtil.stringToList(words, ",");
						keywords.addAll(driverKeywords);
					}
				}
				catch (SQLException sql)
				{
					LogMgr.logDebug("DbMetadata.isKeyword()", "Could not read SQL keywords from driver", sql);
				}
			}
			return this.keywords.contains(name);
		}
	}

	/**
	 * Checks if the given name is already quoted according to the SQL rules
	 * for the current DBMS. This takes non-standard DBMS into account.
	 *
	 * @param name
	 * @return true if the values is already quoted.
	 */
	@Override
	public boolean isQuoted(String name)
	{
		if (name == null) return false;
		name = name.trim();
		if (name.isEmpty()) return false;

		if (name.startsWith(quoteCharacter)) return true;

		// SQL Server driver claims that a " is the quote character but still
		// accepts those iditotic brackets as quote characters...
		if (this.isSqlServer)
		{
			if (name.charAt(0) == '[' && name.charAt(name.length() - 1) == ']') return true;
		}
		return false;
	}

	@Override
	public String removeQuotes(String name)
	{
		if (StringUtil.isEmptyString(name)) return name;

		if (this.isSqlServer && name.startsWith("[") && name.endsWith("]"))
		{
			return name.substring(1, name.length() - 1);
		}
		return StringUtil.removeQuotes(name, quoteCharacter);
	}

	@Override
	public String quoteObjectname(String aName)
	{
		return quoteObjectname(aName, false);
	}


	/**
	 * Quotes the given object name if necessary.
	 *
	 * Quoting of names is necessary if the name is a reserved word in the
	 * database or if the names contains invalid characters or if the name is not
	 * in default case as the database uses.
	 *
	 * @see #needsQuotes(java.lang.String)
	 * @see #getQuoteCharacter()
	 */
	public String quoteObjectname(String name, boolean quoteAlways)
	{
		if (StringUtil.isEmptyString(name)) return null;

		// already quoted?
		if (isQuoted(name)) return name;

		if (this.dbSettings.neverQuoteObjects()) return removeQuotes(name);

		boolean needQuote = quoteAlways || needsQuotes(name);

		if (needQuote)
		{
			StringBuilder result = new StringBuilder(name.length() + quoteCharacter.length() * 2);
			result.append(this.quoteCharacter);
			result.append(name.trim());
			result.append(this.quoteCharacter);
			return result.toString();
		}
		return name;
	}

	/**
	 * Check if the given object name needs quoting for this DBMS.
	 *
	 * @param name the name to check
	 * @return true if the name needs quotes (or if quoted names should always be used for this DBMS)
	 *
	 * @see #isReservedWord(java.lang.String)
	 * @see #storesLowerCaseIdentifiers()
	 * @see #storesMixedCaseIdentifiers()
	 * @see #storesUpperCaseIdentifiers()
	 */
	@Override
	public boolean needsQuotes(String name)
	{
		if (name == null) return false;
		if (name.length() == 0) return false;

		// already quoted?
		if (isQuoted(name)) return false;

		boolean needQuote = name.indexOf(' ') > -1;

		// Excel does not support the standard rules for SQL identifiers
		// Basically anything that does not contain only characters needs to
		// be quoted.
		if (this.isExcel)
		{
			Pattern chars = Pattern.compile("[A-Za-z0-9]+");
			Matcher m = chars.matcher(name);
			needQuote = !m.matches();
		}
		else if (!needQuote)
		{
			Matcher matcher = SqlUtil.SQL_IDENTIFIER.matcher(name);
			needQuote = !matcher.matches();
		}

		if (!needQuote && !this.storesMixedCaseIdentifiers())
		{
			if (this.storesUpperCaseIdentifiers() && !StringUtil.isUpperCase(name))
			{
				needQuote = true;
			}
			else if (this.storesLowerCaseIdentifiers() && !StringUtil.isLowerCase(name))
			{
				needQuote = true;
			}
		}

		if (!needQuote)
		{
			needQuote = isReservedWord(name);
		}
		return needQuote;
	}

	/**
	 * Adjusts the case of the given schema name to the
	 * case in which the server stores schema names.
	 *
	 * This is needed e.g. when the user types a
	 * table name, and that value is used to retrieve
	 * the table definition.
	 *
	 * If the schema name is quoted, then the case will not be adjusted.
	 *
	 * @param schema the schema name to adjust
	 * @return the adjusted schema name
	 */
	public String adjustSchemaNameCase(String schema)
	{
		if (StringUtil.isBlank(schema)) return null;
		if (isQuoted(schema)) return schema.trim();

		schema = SqlUtil.removeObjectQuotes(schema).trim();

		if (this.storesUpperCaseSchemas())
		{
			return schema.toUpperCase();
		}
		else if (this.storesLowerCaseSchemas())
		{
			return schema.toLowerCase();
		}
		return schema;
	}

	/**
	 * Returns true if the given object name needs quoting due
	 * to mixed case writing or because the case of the name
	 * does not match the case in which the database stores its objects
	 */
	public boolean isDefaultCase(String name)
	{
		if (name == null) return true;

		if (storesMixedCaseIdentifiers()) return true;

		boolean isUpper = StringUtil.isUpperCase(name);
		boolean isLower = StringUtil.isLowerCase(name);

		if (isUpper && storesUpperCaseIdentifiers()) return true;
		if (isLower && storesLowerCaseIdentifiers()) return true;

		return false;
	}


	/**
	 * Adjusts the case of the given object to the
	 * case in which the server stores objects
	 * This is needed e.g. when the user types a
	 * table name, and that value is used to retrieve
	 * the table definition. Usually the getColumns()
	 * method is case sensitiv.
	 *
	 * @param name the object name to adjust
	 * @return the adjusted object name
	 */
	public String adjustObjectnameCase(String name)
	{
		if (name == null) return null;
		// if we have quotes, keep them...
		if (isQuoted(name)) return name.trim();

		try
		{
			if (this.storesMixedCaseIdentifiers())
			{
				return name;
			}
			else if (this.storesUpperCaseIdentifiers())
			{
				return name.toUpperCase();
			}
			else if (this.storesLowerCaseIdentifiers())
			{
				return name.toLowerCase();
			}
		}
		catch (Exception e)
		{
		}
		return name.trim();
	}

	/**
	 * Returns the current schema.
	 *
	 * @see SchemaInformationReader#getCurrentSchema()
	 */
	public String getCurrentSchema()
	{
		if (dbSettings.supportsSchemas() && this.schemaInfoReader != null && schemaInfoReader.isSupported())
		{
			return this.schemaInfoReader.getCurrentSchema();
		}

		String schema = null;
		if (supportsGetSchema)
		{
			try
			{
				schema = this.dbConnection.getSqlConnection().getSchema();
			}
			catch (Throwable ex)
			{
				supportsGetSchema = false;
			}
		}
		return schema;
	}

	public void clearCachedSchemaInformation()
	{
		if (dbSettings.supportsSchemas() && this.schemaInfoReader != null)
		{
			this.schemaInfoReader.clearCache();
		}
	}

	/**
	 * Returns the schema that should be used for the current user
	 * This essentially calls {@link #getCurrentSchema()}. The method
	 * then checks if the schema should be ignored for the current
	 * dbms by calling {@link #ignoreSchema(String, String)}. If the
	 * Schema should not be ignored, then it's returned, otherwise
	 * the method will return null
	 */
	public String getSchemaToUse()
	{
		String schema = this.getCurrentSchema();
		if (schema == null) return null;
		if (this.ignoreSchema(schema, schema)) return null;
		return schema;
	}

	/**
	 * The column index of the column in the DataStore returned by getObjects()
	 * the stores the table's name
	 */
	public final static int COLUMN_IDX_TABLE_LIST_NAME = 0;

	/**
	 * The column index of the column in the DataStore returned by getObjects()
	 * that stores the table's type. The available types can be retrieved
	 * using {@link #getObjectTypes()}
	 */
	public final static int COLUMN_IDX_TABLE_LIST_TYPE = 1;

	/**
	 * The column index of the column in the DataStore returned by getObjects()
	 * the stores the table's catalog
	 */
	public final static int COLUMN_IDX_TABLE_LIST_CATALOG = 2;

	/**
	 * The column index of the column in the DataStore returned by getObjects()
	 * the stores the table's schema
	 */
	public final static int COLUMN_IDX_TABLE_LIST_SCHEMA = 3;

	/**
	 * The column index of the column in the DataStore returned by getObjects()
	 * the stores the table's comment
	 */
	public final static int COLUMN_IDX_TABLE_LIST_REMARKS = 4;

	public DataStore getObjects(String aCatalog, String aSchema, String[] types)
		throws SQLException
	{
		return getObjects(aCatalog, aSchema, null, types);
	}


	public String[] getTableListColumns()
	{
		return new String[] {"NAME", "TYPE", catalogTerm.toUpperCase(), schemaTerm.toUpperCase(), "REMARKS"};
	}

	public static String cleanupWildcards(String pattern)
	{
		if (StringUtil.isEmptyString(pattern)) return null;
		if ("*".equals(pattern) || "%".equals(pattern)) return null;
		return SqlUtil.removeObjectQuotes(StringUtil.replace(pattern, "*", "%"));
	}

	private String[] cleanupTypes(String[] types)
	{
		if (types == null || types.length == 0) return types;

		List<String> typesToUse = new ArrayList<>(types.length);

		Collection<String> nativeTypes = retrieveTableTypes();
		for (String type : types)
		{
			if (nativeTypes.contains(type))
			{
				typesToUse.add(type);
			}
		}

		String[] cleanTypes = new String[typesToUse.size()];
		cleanTypes = typesToUse.toArray(cleanTypes);
		return cleanTypes;
	}

	public DataStore createTableListDataStore()
	{
		String[] cols = getTableListColumns();
		int coltypes[] = {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
		int sizes[] = {30, 12, 10, 10, 20};

		final boolean sortMViewAsTable = isOracle && Settings.getInstance().getBoolProperty("workbench.db.oracle.sortmviewsastable", true);

		DataStore result = new DataStore(cols, coltypes, sizes)
		{
			@Override
			protected RowDataListSorter createSorter(SortDefinition sort)
			{
				TableListSorter sorter = new TableListSorter(sort);
				sorter.setSortMViewAsTable(sortMViewAsTable);
				return sorter;
			}
		};

		return result;
	}

	public DataStore getObjects(String catalogPattern, String schemaPattern, String namePattern, String[] types)
		throws SQLException
	{
		catalogPattern = cleanupWildcards(catalogPattern);
		schemaPattern = cleanupWildcards(schemaPattern);
		namePattern = cleanupWildcards(namePattern);

		DataStore result = createTableListDataStore();

		boolean sequencesReturned = false;
		boolean synRetrieved = false;
		boolean synonymsRequested = typeIncluded(SynonymReader.SYN_TYPE_NAME, types);

		ObjectListFilter filter = new ObjectListFilter(getDbId());

		// When TABLE and MATERIALIZED VIEW is specified for getTables() the Oracle driver returns
		// materialized views twice, so we need to get rid of them.
		boolean detectDuplicates = isOracle && typeIncluded("TABLE", types) && typeIncluded(MVIEW_NAME, types);
		Set<String> processed = new TreeSet<>();

		String escape = dbConnection.getSearchStringEscape();

		String escapedNamePattern = namePattern;
		String escapedSchema = schemaPattern;
		String escapedCatalog = SqlUtil.removeObjectQuotes(catalogPattern);

		if (getDbSettings().supportsMetaDataWildcards())
		{
			escapedNamePattern = SqlUtil.escapeUnderscore(namePattern, escape);
		}

		if (getDbSettings().supportsMetaDataSchemaWildcards())
		{
			escapedSchema = SqlUtil.escapeUnderscore(schemaPattern, escape);
		}

		if (getDbSettings().supportsMetaDataCatalogWildcards())
		{
			escapedCatalog = SqlUtil.escapeUnderscore(catalogPattern, escape);
		}

		String sequenceType = getSequenceReader() != null ? getSequenceReader().getSequenceTypeName() : null;
		final SynonymReader synReader = this.getSynonymReader();
		String synTypeName = SynonymReader.SYN_TYPE_NAME;

		if (synReader != null)
		{
			synTypeName = synReader.getSynonymTypeName();
		}

		if (!getDbSettings().supportsMetaDataNullPattern() && escapedNamePattern == null)
		{
			escapedNamePattern = "%";
		}

		ResultSet tableRs = null;
		try
		{
			String[] typesToUse = types;
			if (getDbSettings().cleanupTypeList())
			{
				typesToUse = cleanupTypes(types);
			}

			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logDebug("DbMetadata.getObjects()", "Calling getTables() using: catalog="+ escapedCatalog +
					", schema=" + escapedSchema +
					", name=" + escapedNamePattern +
					", types=" + (typesToUse == null ? "null" : Arrays.asList(typesToUse).toString()));
			}

			long start = System.currentTimeMillis();

			// if the types are cleaned up, an empty array can be returned
			// in that case this means that only non-native types are requested
			// which are handled by one of the extenders. In that case there is no
			// need to call getTables()
			if (typesToUse == null || typesToUse.length > 0)
			{
				tableRs = metaData.getTables(escapedCatalog, escapedSchema, escapedNamePattern, typesToUse);
				if (tableRs == null)
				{
					LogMgr.logError("DbMetadata.getTables()", "Driver returned a NULL ResultSet from getTables()",null);
					return result;
				}
			}

			long duration = System.currentTimeMillis() - start;
			LogMgr.logDebug("DbMetadata.getObjects()", "Retrieving table list took: " + duration + "ms");


			if (tableRs != null && Settings.getInstance().getDebugMetadataSql())
			{
				SqlUtil.dumpResultSetInfo("DatabaseMetaData.getTables() returned:", tableRs.getMetaData());
			}

			boolean useColumnNames = dbSettings.useColumnNameForMetadata();

			while (tableRs != null && tableRs.next())
			{
				String cat = useColumnNames ? tableRs.getString("TABLE_CAT") : tableRs.getString(1);
				String schema = useColumnNames ? tableRs.getString("TABLE_SCHEM") : tableRs.getString(2);
				String name = useColumnNames ? tableRs.getString("TABLE_NAME") : tableRs.getString(3);
				String ttype = useColumnNames ? tableRs.getString("TABLE_TYPE") : tableRs.getString(4);
				if (name == null) continue;

				if (filter.isExcluded(ttype, name)) continue;

				String remarks = useColumnNames ? tableRs.getString("REMARKS") : tableRs.getString(5);

				boolean isSynoym = synRetrieved || synTypeName.equals(ttype);

				// prevent duplicate retrieval of SYNONYMs if the driver
				// returns them already, but the Settings have enabled
				// Synonym retrieval as well
				// (e.g. because an upgraded Driver now returns the synonyms)
				if (isSynoym)
				{
					synRetrieved = true;
				}

				if (isIndexType(ttype)) continue;

				if (detectDuplicates)
				{
					// As this name is only used to workaround an Oracle problem,
					// schema and table name are enough for a fully qualified name
					String fqName = schema + "." + name;
					if (processed.contains(fqName)) continue;
					processed.add(fqName);
				}

				int row = result.addRow();
				result.setValue(row, COLUMN_IDX_TABLE_LIST_NAME, name);
				result.setValue(row, COLUMN_IDX_TABLE_LIST_TYPE, ttype);
				result.setValue(row, COLUMN_IDX_TABLE_LIST_CATALOG, cat);
				result.setValue(row, COLUMN_IDX_TABLE_LIST_SCHEMA, schema);
				result.setValue(row, COLUMN_IDX_TABLE_LIST_REMARKS, remarks);
				if (!sequencesReturned && StringUtil.equalString(sequenceType, ttype)) sequencesReturned = true;
			}
		}
		finally
		{
			SqlUtil.closeResult(tableRs);
		}

		// Synonym and Sequence retrieval is handled differently to "regular" ObjectListExtenders
		// because some JDBC driver versions do retrieve this information automatically some don't
		SequenceReader seqReader = getSequenceReader();
		if (seqReader != null && typeIncluded(seqReader.getSequenceTypeName(), types) &&
				Settings.getInstance().getBoolProperty("workbench.db." + this.getDbId() + ".retrieve_sequences", true)
				&& !sequencesReturned)
		{
			List<SequenceDefinition> sequences = seqReader.getSequences(catalogPattern, schemaPattern, namePattern);
			for (SequenceDefinition sequence : sequences)
			{
				int row = result.addRow();

				result.setValue(row, COLUMN_IDX_TABLE_LIST_NAME, sequence.getSequenceName());
				result.setValue(row, COLUMN_IDX_TABLE_LIST_TYPE, sequence.getObjectType());
				result.setValue(row, COLUMN_IDX_TABLE_LIST_CATALOG, sequence.getCatalog());
				result.setValue(row, COLUMN_IDX_TABLE_LIST_SCHEMA, sequence.getSchema());
				result.setValue(row, COLUMN_IDX_TABLE_LIST_REMARKS, sequence.getComment());
				result.getRow(row).setUserObject(sequence);
			}
		}

		boolean retrieveSyns = (synReader != null && Settings.getInstance().getBoolProperty("workbench.db." + this.getDbId() + ".retrieve_synonyms", false));
		if (retrieveSyns && !synRetrieved && synonymsRequested)
		{
			List<TableIdentifier> syns = synReader.getSynonymList(dbConnection, catalogPattern, schemaPattern, namePattern);
			for (TableIdentifier synonym : syns)
			{
				int row = result.addRow();

				result.setValue(row, COLUMN_IDX_TABLE_LIST_NAME, synonym.getTableName());
				result.setValue(row, COLUMN_IDX_TABLE_LIST_TYPE, synonym.getType());
				result.setValue(row, COLUMN_IDX_TABLE_LIST_CATALOG, synonym.getCatalog());
				result.setValue(row, COLUMN_IDX_TABLE_LIST_SCHEMA, synonym.getSchema());
				result.setValue(row, COLUMN_IDX_TABLE_LIST_REMARKS, synonym.getComment());
			}
		}

		for (ObjectListExtender extender : extenders)
		{
			if (extender.handlesType(types))
			{
				extender.extendObjectList(dbConnection, result, catalogPattern, schemaPattern, namePattern, types);
			}
		}

		if (objectListEnhancer != null)
		{
			objectListEnhancer.updateObjectList(dbConnection, result, catalogPattern, schemaPattern, namePattern, types);
		}

		result.resetStatus();

		return result;
	}

	public static SortDefinition getTableListSort()
	{
		SortDefinition def = new SortDefinition();
		def.addSortColumn(COLUMN_IDX_TABLE_LIST_TYPE, true);
		def.addSortColumn(COLUMN_IDX_TABLE_LIST_CATALOG, true);
		def.addSortColumn(COLUMN_IDX_TABLE_LIST_SCHEMA, true);
		def.addSortColumn(COLUMN_IDX_TABLE_LIST_NAME, true);
		return def;
	}

	/**
	 * Checks if the given type is contained in the passed array.
	 * If at least one entry in the types array is * then this
	 * method will always return true
	 * @param type the type to check for
	 * @param types the list of types to be checked
	 * @return true, if type is contained in types
	 */
	public static boolean typeIncluded(String type, String[] types)
	{
		if (types == null) return true;
		if (type == null) return false;
		int l = types.length;
		for (int i=0; i < l; i++)
		{
			if (types[i] == null) continue;
			if (types[i].equals("*")) return true;
			if (types[i].equals("%")) return true;
			if (type.equalsIgnoreCase(types[i])) return true;
		}
		return false;
	}

	public TableIdentifier findObject(TableIdentifier tbl)
	{
		return findObject(tbl, true, false);
	}

	public TableIdentifier findObject(TableIdentifier tbl, boolean adjustCase, boolean searchAllSchemas)
	{
		if (tbl == null) return null;
		TableIdentifier result = null;
		TableIdentifier table = tbl.createCopy();
		if (adjustCase) table.adjustCase(dbConnection);

		try
		{
			boolean schemaWasNull = false;

			String schema = table.getSchema();
			if (schema == null)
			{
				schemaWasNull = true;
				schema = getCurrentSchema();
			}
			else
			{
				searchAllSchemas = false;
			}

			String catalog = table.getCatalog();
			if (catalog == null)
			{
				catalog = getCurrentCatalog();
			}

			String tablename = table.getRawTableName(); // SqlUtil.escapeUnderscore(, dbConnection);
			// schema = SqlUtil.escapeUnderscore(schema, dbConnection);

			DataStore ds = getObjects(catalog, schema, tablename, null);

			String[] cols = getTableListColumns();

			if (ds.getRowCount() == 0 && this.isOracle)
			{
				// try again with PUBLIC, maybe it's a public synonym
				ds = getObjects(null, "PUBLIC", tablename, null);
			}

			if (ds.getRowCount() == 0 && schemaWasNull && searchAllSchemas)
			{
				ds = getObjects(null, null, tablename, null);
			}

			if (ds.getRowCount() == 1)
			{
				result = buildTableIdentifierFromDs(ds, 0);
			}
			else if (ds.getRowCount() > 1)
			{
				AndExpression filter = new AndExpression();
				StringEqualsComparator comp = new StringEqualsComparator();
				filter.addColumnExpression(cols[COLUMN_IDX_TABLE_LIST_NAME], comp, table.getRawTableName(), true);
				if (StringUtil.isNonBlank(schema))
				{
					filter.addColumnExpression(cols[COLUMN_IDX_TABLE_LIST_SCHEMA], comp, schema, true);
				}
				if (StringUtil.isNonBlank(catalog))
				{
					filter.addColumnExpression(cols[COLUMN_IDX_TABLE_LIST_CATALOG], comp, catalog, true);
				}
				ds.applyFilter(filter);
				if (ds.getRowCount() == 1)
				{
					result = buildTableIdentifierFromDs(ds, 0);
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("DbMetadata.findObject()", "Error checking table existence", e);
		}
		return result;
	}

	/**
	 * Check if the given table exists in the database
	 */
	public boolean tableExists(TableIdentifier aTable)
	{
		return objectExists(aTable, tableTypesArray);
	}

	public boolean objectExists(TableIdentifier aTable, String type)
	{
		String[] types = null;
		if (type != null)
		{
			types = new String[] { type };
		}
		return objectExists(aTable, types);
	}

	public boolean objectExists(TableIdentifier aTable, String[] types)
	{
		return findTable(aTable, types, false) != null;
	}

	public TableIdentifier findSelectableObject(TableIdentifier tbl)
	{
		return findTable(tbl, selectableTypes, false);
	}

	public TableIdentifier searchSelectableObjectOnPath(TableIdentifier tbl)
	{
		return searchObjectOnPath(tbl, selectableTypes);
	}

	public TableIdentifier searchTableOnPath(TableIdentifier table)
	{
		return searchObjectOnPath(table, tableTypesArray);
	}

	public TableIdentifier searchObjectOnPath(TableIdentifier table, String[] types)
	{
		if (table.getSchema() != null)
		{
			return findTable(table, types, false);
		}

		List<String> searchPath = DbSearchPath.Factory.getSearchPathHandler(this.dbConnection).getSearchPath(this.dbConnection, null);

		if (searchPath.isEmpty())
		{
			return findTable(table, types, false);
		}

		LogMgr.logDebug("DbMetaData.searchObjectOnPath()", "Looking for table " + table.getRawTableName() + " in schemas: " + searchPath);
		for (String checkSchema  : searchPath)
		{
			TableIdentifier toSearch = table.createCopy();
			toSearch.setSchema(checkSchema);

			TableIdentifier found = findTable(toSearch, types, false);
			if (found != null)
			{
				LogMgr.logDebug("DbMetaData.searchObjectOnPath()", "Found table " + found.getTableExpression());
				return found;
			}
		}
		return null;
	}

	public TableIdentifier findTable(TableIdentifier tbl, boolean searchAllSchemas)
	{
		return findTable(tbl, tableTypesArray, searchAllSchemas);
	}

	public TableIdentifier findTable(TableIdentifier tbl)
	{
		return findTable(tbl, tableTypesArray, false);
	}

	public TableIdentifier findTable(TableIdentifier tbl, String[] types)
	{
		return findTable(tbl, types == null || types.length == 0 ? tableTypesArray : types, false);
	}

	private TableIdentifier findTable(TableIdentifier tbl, String[] types, boolean searchAllSchemas)
	{
		if (tbl == null) return null;

		TableIdentifier result = null;
		TableIdentifier table = tbl.createCopy();
		table.adjustCase(dbConnection);
		try
		{
			String schema = table.getSchema();
			if (schema == null && !searchAllSchemas)
			{
				schema = getCurrentSchema();
			}

			String catalog = table.getCatalog();
			if (catalog == null)
			{
				catalog = getCurrentCatalog();
			}

			String tablename = table.getRawTableName();

			DataStore ds = getObjects(catalog, schema, tablename, types);

			if (ds.getRowCount() == 1)
			{
				result = buildTableIdentifierFromDs(ds, 0);
				return result;
			}

			// Nothing found, try again with the original catalog and schema information
			ds = getObjects(table.getRawCatalog(), table.getRawSchema(), table.getRawTableName(), types);
			if (ds.getRowCount() == 0)
			{
				return null;
			}
			else if (ds.getRowCount() == 1)
			{
				result = buildTableIdentifierFromDs(ds, 0);
				return result;
			}

			// if nothing was found there is nothing we can do to guess the correct
			// "searching strategy" for the current DBMS
		}
		catch (Exception e)
		{
			LogMgr.logError("DbMetadata.findTable()", "Error checking table existence", e);
		}
		return result;
	}

	/**
	 * Returns true if the server stores identifiers in mixed case.
	 *
	 * Usually this is delegated to the JDBC driver, but as some drivers
	 * (e.g. Frontbase) implement this incorrectly, this can be overriden
	 * in workbench.settings with the property:
	 * workbench.db.[dbid].objectname.case
	 */
	public boolean storesMixedCaseIdentifiers()
	{
		IdentifierCase ocase = this.dbSettings.getObjectNameCase();
		if (ocase != IdentifierCase.unknown)
		{
			return ocase == IdentifierCase.mixed;
		}
		try
		{
			boolean upper = this.metaData.storesUpperCaseIdentifiers();
			boolean lower = this.metaData.storesLowerCaseIdentifiers();
			boolean mixed = this.metaData.storesMixedCaseIdentifiers();

			return mixed || (upper && lower);
		}
		catch (SQLException e)
		{
			return false;
		}
	}

	public boolean storesUpperCaseSchemas()
	{
		IdentifierCase ocase = this.dbSettings.getSchemaNameCase();
		if (ocase == IdentifierCase.unknown)
		{
			return storesUpperCaseIdentifiers();
		}
		return ocase == IdentifierCase.upper;
	}

	public boolean storesLowerCaseSchemas()
	{
		IdentifierCase ocase = this.dbSettings.getSchemaNameCase();
		if (ocase == IdentifierCase.unknown)
		{
			return storesLowerCaseIdentifiers();
		}
		return ocase == IdentifierCase.lower;
	}

	/**
	 * Returns true if the server stores identifiers in lower case.
	 * Usually this is delegated to the JDBC driver, but as some drivers
	 * (e.g. Frontbase) implement this incorrectly, this can be overriden
	 * in workbench.settings with the property:
	 * workbench.db.objectname.case.<dbid>
	 */
	public boolean storesLowerCaseIdentifiers()
	{
		IdentifierCase ocase = this.dbSettings.getObjectNameCase();
		if (ocase != IdentifierCase.unknown)
		{
			return ocase == IdentifierCase.lower;
		}
		try
		{
			boolean lower = this.metaData.storesLowerCaseIdentifiers();
			return lower;
		}
		catch (SQLException e)
		{
			return false;
		}
	}

	/**
	 * Returns true if the server stores identifiers in upper case.
	 * Usually this is delegated to the JDBC driver, but as some drivers
	 * (e.g. Frontbase) implement this incorrectly, this can be overriden
	 * in workbench.settings
	 */
	public boolean storesUpperCaseIdentifiers()
	{
		IdentifierCase ocase = this.dbSettings.getObjectNameCase();
		if (ocase != IdentifierCase.unknown)
		{
			return ocase == IdentifierCase.upper;
		}
		try
		{
			boolean upper = this.metaData.storesUpperCaseIdentifiers();
			return upper;
		}
		catch (SQLException e)
		{
			return false;
		}
	}

	/**
	 * Enable Oracle's DBMS_OUTPUT package with a default buffer size
	 * @see #enableOutput(long)
	 */
	public void enableOutput()
	{
		this.enableOutput(-1);
	}

	/**
	 * Enable Oracle's DBMS_OUTPUT package.
	 * @see workbench.db.oracle.DbmsOutput#enable(long)
	 */
	public void enableOutput(long aLimit)
	{
		if (!this.isOracle)
		{
			return;
		}

		if (this.oraOutput == null)
		{
			try
			{
				this.oraOutput = new DbmsOutput(this.dbConnection.getSqlConnection());
			}
			catch (Exception e)
			{
				LogMgr.logError("DbMetadata.enableOutput()", "Could not create DbmsOutput", e);
				this.oraOutput = null;
			}
		}

		if (this.oraOutput != null)
		{
			try
			{
				this.oraOutput.enable(aLimit);
			}
			catch (Throwable e)
			{
				LogMgr.logError("DbMetadata.enableOutput()", "Error when enabling DbmsOutput", e);
			}
		}
	}

	/**
	 * Disable Oracle's DBMS_OUTPUT package
	 * @see workbench.db.oracle.DbmsOutput#disable()
	 */
	public void disableOutput()
	{
    if (!this.isOracle) return;

		if (this.oraOutput != null)
		{
			try
			{
				this.oraOutput.disable();
        this.oraOutput = null;
			}
			catch (Throwable e)
			{
				LogMgr.logError("DbMetadata.disableOutput()", "Error when disabling DbmsOutput", e);
			}
		}
	}

	/**
	 * Return any server side messages. Currently this is only implemented
	 * for Oracle (and is returning messages that were "printed" using
	 * the DBMS_OUTPUT package
	 */
	public String getOutputMessages()
	{
		String result = StringUtil.EMPTY_STRING;

		if (this.oraOutput != null)
		{
			try
			{
				result = this.oraOutput.getResult();
			}
			catch (Throwable th)
			{
				LogMgr.logError("DbMetadata.getOutputMessages()", "Error when retrieving Output Messages", th);
				result = StringUtil.EMPTY_STRING;
			}
		}
		return result;
	}

	/**
	 * Release any resources for this object.
	 *
	 * After a call to close(), this instance should not be used any longer.
	 * @see DbmsOutput#close()
	 * @see SchemaInformationReader#dispose()
	 */
	public void close()
	{
		if  (this.dbConnection != null && !this.dbConnection.isBusy())
		{
			if (this.oraOutput != null) 	this.oraOutput.close();
			if (this.schemaInfoReader != null) this.schemaInfoReader.dispose();
		}
	}

	public boolean isExtendedObject(DbObject o)
	{
		for (ObjectListExtender extender : extenders)
		{
			if (extender.handlesType(o.getObjectType())) return true;
		}
		return false;
	}

	/**
	 * Retrieves the object source for the given object
	 * using a registered ObjectListExtender.
	 *
	 * @param o the object to retrieve
	 * @return the source of the object or null, if this object is not handled by an extender
	 * @see #isExtendedObject(workbench.db.DbObject)
	 */
	public String getObjectSource(DbObject o)
	{
		if (o == null) return null;
		for (ObjectListExtender extender : extenders)
		{
			if (extender.handlesType(o.getObjectType()))
			{
				return extender.getObjectSource(dbConnection, o);
			}
		}
		return null;
	}

	public List<ColumnIdentifier> getTableColumns(TableIdentifier table)
		throws SQLException
	{
		return getTableColumns(table, true);
	}

	/**
	 * Return the column list for the given table.
	 *
	 * @param table             the table for which to retrieve the column definition
	 * @param readPkDefinition  if true, the PK definition of the table will also be retrieved
	 *
	 * @see #getTableDefinition(workbench.db.TableIdentifier)
	 */
	public List<ColumnIdentifier> getTableColumns(TableIdentifier table, boolean readPkDefinition)
		throws SQLException
	{
		TableDefinition def = definitionReader.getTableDefinition(table, readPkDefinition);
		if (def == null) return Collections.emptyList();
		return def.getColumns();
	}

	public DbObject getObjectDefinition(TableIdentifier table)
	{
		for (ObjectListExtender extender : extenders)
		{
			if (extender.handlesType(table.getObjectType()))
			{
				return extender.getObjectDefinition(dbConnection, table);
			}
		}
		return null;
	}

	public DataStore getExtendedObjectDetails(DbObject object)
	{
		if (object == null) return null;
		DataStore def = null;
		for (ObjectListExtender extender : extenders)
		{
			if (extender.handlesType(object.getObjectType()))
			{
				def = extender.getObjectDetails(dbConnection, object);
				break;
			}
		}
		return def;
	}

	public boolean isSynonym(TableIdentifier table)
	{
		if (table == null) return false;
		SynonymReader reader = getSynonymReader();
		if (reader == null) return false;
		return reader.getSynonymTypeName().equalsIgnoreCase(table.getType());
	}

	public boolean isSequenceType(String type)
	{
		if (type == null) return false;
		SequenceReader reader = getSequenceReader();
		if (reader == null) return false;
		return StringUtil.equalStringIgnoreCase(type, reader.getSequenceTypeName());
	}

	public DataStore getObjectDetails(TableIdentifier table)
		throws SQLException
	{
		DataStore def = null;
		for (ObjectListExtender extender : extenders)
		{
			if (extender.handlesType(table.getObjectType()))
			{
				def = extender.getObjectDetails(dbConnection, table);
				break;
			}
		}
		if (def == null && isSequenceType(table.getObjectType()))
		{
			TableIdentifier tbl = table.createCopy();
			tbl.adjustCase(this.dbConnection);
			String schema = SqlUtil.removeObjectQuotes(table.getSchema());
			String seqname = SqlUtil.removeObjectQuotes(table.getObjectName());
			String catalog = SqlUtil.removeObjectQuotes(table.getCatalog());
			DataStore seqDef = getSequenceReader().getRawSequenceDefinition(catalog, schema, seqname);
			if (GuiSettings.getTransformSequenceDisplay() && seqDef != null && seqDef.getRowCount() == 1)
			{
				DatastoreTransposer transpose = new DatastoreTransposer(seqDef);

				// No need to show the remarks as a row in the sequence details
				transpose.setColumnsToExclude(CollectionUtil.caseInsensitiveSet("remarks"));

				def = transpose.transposeRows(new int[]{0});
				def.getColumns()[0].setColumnName(ResourceMgr.getString("TxtAttribute"));
				def.getColumns()[1].setColumnName(ResourceMgr.getString("TxtValue"));
			}
			else
			{
				def = seqDef;
			}
		}
		else if (def == null)
		{
			TableDefinition tdef = definitionReader.getTableDefinition(table, true);
			def = new TableColumnsDatastore(tdef);
		}
		if (def != null) def.resetStatus();
		return def;
	}

	/**
	 * Return the definition of the given table.
	 * <br/>
	 * To display the columns for a table in a DataStore create an
	 * instance of {@link TableColumnsDatastore}.
	 *
	 * @param toRead The table for which the definition should be retrieved
	 *
	 * @throws SQLException
	 * @return the definition of the table.
	 * @see TableColumnsDatastore
	 * @see TableDefinitionReader#getTableDefinition(workbench.db.TableIdentifier)
	 */
	public TableDefinition getTableDefinition(TableIdentifier toRead)
		throws SQLException
	{
		return getTableDefinition(toRead, true);
	}

	public TableDefinition getTableDefinition(TableIdentifier toRead, boolean includePkInformation)
		throws SQLException
	{
		if (toRead == null) return null;
		return definitionReader.getTableDefinition(toRead, includePkInformation);
	}

	/**
	 * If the passed TableIdentifier is a Synonym and the current
	 * DBMS supports synonyms, a TableIdentifier for the "real"
	 * table is returned.
	 *
	 * Otherwise the passed TableIdentifier is returned
	 */
	public TableIdentifier resolveSynonym(TableIdentifier tbl)
	{
		if (tbl == null) return null;
		if (!supportsSynonyms()) return tbl;
		String type = tbl.getType();
		if (type != null && !dbSettings.isSynonymType(type)) return tbl;
		TableIdentifier syn = getSynonymTable(tbl);
		if (syn == null) return tbl;
		return syn;
	}

	/**
	 * Returns a list of all tables in the current schema.
	 * <br/>
	 * The types used are those returned by {@link #getTableTypes()}
	 * @throws SQLException
	 */
	public List<TableIdentifier> getTableList()
		throws SQLException
	{
		return getObjectList(null, getCurrentSchema(), tableTypesArray);
	}

	public List<TableIdentifier> getObjectList(String schema, String[] types)
		throws SQLException
	{
		if (schema == null) schema = this.getCurrentSchema();
		return getObjectList(null, schema, types);
	}

	public List<TableIdentifier> getTableList(String table, String schema)
		throws SQLException
	{
		return getObjectList(table, schema, tableTypesArray);
	}

	/**
	 * Returns a list of objects from which a SELECT can be run.
	 * <br/>
	 * Typically these are tables, views and materialized views.
	 *
	 * @param namePattern      a pattern to search for object names
	 * @param schemaOrCatalog  the schema or catalog pattern (if the DBMS does not support schemas)
	 * @see #getObjects(java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
	 * @throws SQLException
	 */
	public List<TableIdentifier> getSelectableObjectsList(String namePattern, String schemaOrCatalog)
		throws SQLException
	{
		return getSelectableObjectsList(namePattern, schemaOrCatalog, selectableTypes);
	}

	public List<TableIdentifier> getSelectableObjectsList(String namePattern, String schemaOrCatalog, String[] types)
		throws SQLException
	{
		if (getDbSettings().supportsSchemas())
		{
			return getObjectList(namePattern, null, schemaOrCatalog, types);
		}
		else if (getDbSettings().supportsCatalogs())
		{
			return getObjectList(namePattern, schemaOrCatalog, null, types);
		}
		return getObjectList(namePattern, null, null, types);
	}

	/**
	 * Return a list of tables for the given schema
	 * if the table name is null, all tables will be returned
	 *
	 * @param namePattern  the search pattern for the objects to be retrieved
	 * @param schema       the schema or catalog pattern (if the DBMS does not support schemas)
	 * @see #getObjects(java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
	 */
	public List<TableIdentifier> getObjectList(String namePattern, String schema, String[] types)
		throws SQLException
	{
		if (getDbSettings().supportsSchemas())
		{
			return getObjectList(namePattern, null, schema, types);
		}
		else if (getDbSettings().supportsCatalogs())
		{
			return getObjectList(namePattern, schema, null, types);
		}
		return getObjectList(namePattern, null, null, types);
	}

	/**
	 * Return a list of tables for the given schema
	 * if the table name is null, all tables will be returned
	 *
	 * @param namePattern     the (wildcard) name of a table
	 * @param catalogPattern  the catalog for which to retrieve the objects (may be null)
	 * @param schemaPattern   the schema for which to retrieve the objects (may be null)
	 *
	 * @see #getObjects(java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
	 */
	public List<TableIdentifier> getObjectList(String namePattern, String catalogPattern, String schemaPattern, String[] types)
		throws SQLException
	{
		DataStore ds = getObjects(catalogPattern, schemaPattern, namePattern, types);
		int count = ds.getRowCount();
		List<TableIdentifier> tables = new ArrayList<>(count);
		for (int i=0; i < count; i++)
		{
			TableIdentifier tbl = buildTableIdentifierFromDs(ds, i);
			tables.add(tbl);
		}
		return tables;
	}

	public TableIdentifier buildTableIdentifierFromDs(DataStore ds, int row)
	{
		String t = ds.getValueAsString(row, COLUMN_IDX_TABLE_LIST_NAME);
		String s = ds.getValueAsString(row, COLUMN_IDX_TABLE_LIST_SCHEMA);
		String c = ds.getValueAsString(row, COLUMN_IDX_TABLE_LIST_CATALOG);
		TableIdentifier tbl = new TableIdentifier(c, s, t, false);
		tbl.setNeverAdjustCase(true);
		tbl.setType(ds.getValueAsString(row, COLUMN_IDX_TABLE_LIST_TYPE));
		tbl.setComment(ds.getValueAsString(row, COLUMN_IDX_TABLE_LIST_REMARKS));
		return tbl;
	}

	/**
	 * Return the current catalog for this connection.
	 *
	 * If no catalog is defined or the DBMS does not support catalogs, null is returned.
	 *
	 * This method works around a bug in Microsoft's JDBC driver which does
	 * not return the correct database (=catalog) after the database has
	 * been changed with the USE <db> command from within the Workbench.
	 *
	 * If no query has been configured for the current DBMS, DatabaseMetaData.getCatalog()
	 * is used, otherwise the query that is configured with the property
	 * workbench.db.[dbid].currentcatalog.query
	 *
	 * @see DbSettings#getQueryForCurrentCatalog()
	 * @see DbSettings#supportsCatalogs()
	 *
	 * @return The name of the current catalog or null if there is no current catalog
	 */
	public String getCurrentCatalog()
	{
		if (!dbSettings.supportsCatalogs())
		{
			return null;
		}

		String catalog = null;

		String query = this.dbSettings.getQueryForCurrentCatalog();
		if (query != null)
		{
			// for some reason, getCatalog() does not return the correct
			// information when using Microsoft's JDBC driver.
			// If this is the case, a SQL query can be defined that is
			// used instead of the JDBC call, e.g. SELECT db_name()
			Statement stmt = null;
			ResultSet rs = null;
			try
			{
				stmt = this.dbConnection.createStatementForQuery();
				rs = stmt.executeQuery(query);
				if (rs.next()) catalog = rs.getString(1);
			}
			catch (Exception e)
			{
				LogMgr.logWarning("DbMetadata.getCurrentCatalog()", "Error retrieving current catalog using query:\n" + query, e);
				catalog = null;
			}
			finally
			{
				SqlUtil.closeAll(rs, stmt);
			}
		}

		if (catalog == null)
		{
			try
			{
				catalog = this.dbConnection.getSqlConnection().getCatalog();
			}
			catch (Exception e)
			{
				LogMgr.logWarning("DbMetadata.getCurrentCatalog", "Could not retrieve catalog using getCatalog()", e);
				catalog = null;
			}
		}

		return catalog;
	}

	/**
	 *	Returns a list of all catalogs in the database.
	 *	Some DBMS's do not support catalogs, in this case the method
	 *	will return an empty List.
	 * <br/>
	 * The list of catalogs will not be filtered.
	 */
	public List<String> getCatalogs()
	{
		return getCatalogInformation(null);
	}

	/**
	 * Return a filtered list of catalogs in the database.
	 * <br/>
	 * Some DBMS's do not support catalogs, in this case the method
	 * will return an empty List.
	 * The list is obtained by calling DatabaseMetaData.getCatalogs().
	 * <br/>
	 * If the filter is not null, all entries that are matched by the
	 * filter are removed from the result.
	 *
	 * @param filter the ObjectNameFilter to apply
	 * @return a list of available catalogs if supported by the database
	 * @see ObjectNameFilter#isExcluded(java.lang.String)
	 */
	public List<String> getCatalogInformation(ObjectNameFilter filter)
	{
		List<String> result = CollectionUtil.arrayList();

		boolean useColumnNames = dbSettings.useColumnNameForMetadata();
		ResultSet rs = null;
		try
		{
			rs = this.metaData.getCatalogs();
			while (rs.next())
			{
				String cat = useColumnNames ? rs.getString("TABLE_CAT") : rs.getString(1);
				if (cat == null) continue;

				if (filter == null || !filter.isExcluded(cat))
				{
					result.add(cat);
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("DbMetadata.getCatalogInformation()", "Error retrieving catalog information", e);
		}
		finally
		{
			SqlUtil.closeResult(rs);
		}

		// If only a single catalog is returned and that is the current one,
		// is is assumed, that the system does not have catalogs.
		// this is mainly for Postgres and DB2 which do return the current database as a catalog
		// but don't actually allow switching databases or cross-database queries.
		String currentCatalog = this.getCurrentCatalog();
		if (result.size() == 1 && (currentCatalog == null || result.get(0).equals(currentCatalog)))
		{
			result.clear();
		}
		Collections.sort(result);
		return result;
	}

	/**
	 * Returns the list of schemas as returned by DatabaseMetadata.getSchemas()
	 * @return a list of schema names
	 */
	public List<String> getSchemas()
	{
		return getSchemas(null);
	}

	/**
	 * Return a filtered list of schemas in the database.
	 * <br/>
	 * The list is obtained by calling DatabaseMetadata.getSchemas().
	 * <br/>
	 * If the filter is not null, all entries that are matched by the
	 * filter are removed from the result.
	 *
	 * @param filter the ObjectNameFilter to apply
	 * @return a list of available schemas if supported by the database
	 * @see ObjectNameFilter#isExcluded(java.lang.String)
	 */
	public List<String> getSchemas(ObjectNameFilter filter)
	{
		ArrayList<String> result = new ArrayList<>();
		ResultSet rs = null;

		boolean useColumnNames = dbSettings.useColumnNameForMetadata();
		try
		{
			rs = this.metaData.getSchemas();
			while (rs.next())
			{
				String schema = useColumnNames ? rs.getString("TABLE_SCHEM") : rs.getString(1);
				if (schema == null)	continue;
				if (filter == null || !filter.isExcluded(schema))
				{
					result.add(schema);
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("DbMetadata.getSchemas()", "Error retrieving schemas: " + e.getMessage(), null);
		}
		finally
		{
			SqlUtil.closeResult(rs);
		}

		// This is mainly for Oracle because the Oracle driver does not return the "PUBLIC" schema
		// which is - strictly speaking - correct as there is no user PUBLIC in the database.
		// but to view public synonyms we need the entry for the DbExplorer
		List<String> additionalSchemas = dbSettings.getSchemasToAdd();
		if (additionalSchemas != null)
		{
			result.addAll(additionalSchemas);
			Collections.sort(result);
		}
		return result;
	}

	private boolean isIndexType(String type)
	{
		if (type == null) return false;
		return (type.contains("INDEX"));
	}

	private synchronized Collection<String> retrieveTableTypes()
	{
		if (tableTypesFromDriver != null) return tableTypesFromDriver;

		Set<String> types = CollectionUtil.caseInsensitiveSet();
		ResultSet rs = null;

		try
		{
			rs = this.metaData.getTableTypes();
			while (rs != null && rs.next())
			{
				String type = rs.getString(1);
				if (type == null) continue;
				// for some reason oracle sometimes returns
				// the types padded to a fixed length. I'm assuming
				// it doesn't harm for other DBMS as well to
				// trim the returned value...
				type = type.trim();

				if (isIndexType(type)) continue;
				types.add(type.toUpperCase());
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("DbMetadata.getTableTypes()", "Error retrieving table types. Using default values", e);
			types = CollectionUtil.caseInsensitiveSet("table", "system table");
		}
		finally
		{
			SqlUtil.closeResult(rs);
		}

		tableTypesFromDriver = Collections.unmodifiableSet(types);

		return tableTypesFromDriver;
	}

	/**
	 * Return a list of object types available in the database.
	 *
	 * e.g. TABLE, SYSTEM TABLE, ...
	 */
	public Collection<String> getObjectTypes()
	{
		Set<String> result = CollectionUtil.caseInsensitiveSet();
		result.addAll(retrieveTableTypes());

		String types = System.getProperty("workbench.db." + this.getDbId() + ".additional.objecttypes", "");
		if (StringUtil.isNonBlank(types))
		{
			List<String> addTypes = StringUtil.stringToList(types.toUpperCase(), ",", true, true, false, false);
			result.addAll(addTypes);
		}

		if (supportsSynonyms())
		{
			result.add("SYNONYM");
		}

		SequenceReader reader = getSequenceReader();
		if (reader != null)
		{
			result.add(reader.getSequenceTypeName());
		}

		for (ObjectListExtender extender : extenders)
		{
			if (!extender.isDerivedType())
			{
				result.addAll(extender.supportedTypes());
			}
		}
		return result;
	}


	public String getSchemaTerm()
	{
		return this.schemaTerm;
	}

	public String getCatalogTerm()
	{
		return this.catalogTerm;
	}

	public SequenceReader getSequenceReader()
	{
		synchronized (this.readerLock)
		{
			if (sequenceReader == null)
			{
				sequenceReader = ReaderFactory.getSequenceReader(this.dbConnection);
			}
			return this.sequenceReader;
		}
	}

	/**
	 * Checks if the given type is a regular table type or an extended table type.
	 *
	 * @param type the type to check
	 * @return true if it's a table of some kind.
	 * @see #isTableType(java.lang.String)
	 */
	public boolean isExtendedTableType(String type)
	{
		if (isTableType(type)) return true;
		List<String> types = Settings.getInstance().getListProperty("workbench.db." + this.getDbId() + ".additional.tabletypes",false);
		return types.contains(type);
	}

	public boolean isTableType(String type)
	{
		if (type == null) return false;
		return tableTypesList.contains(type.trim());
	}

	/**
	 * Checks if the current DBMS supports synonyms.
	 * @return true if the synonym support is available (basically if synonymReader != null)
	 */
	public boolean supportsSynonyms()
	{
		return getSynonymReader() != null;
	}

	/**
	 *	Return the underlying table of a synonym.
	 * @param synonym the synonym definition
	 *
	 * @return the table to which the synonym points or null if the passed
	 *         name does not reference a synonym or if the DBMS does not support synonyms
	 * @see #getSynonymTable(java.lang.String, java.lang.String, java.lang.String)
	 */
	public TableIdentifier getSynonymTable(TableIdentifier synonym)
	{
		TableIdentifier tbl = synonym.createCopy();
		tbl.adjustCase(this.dbConnection);
		return getSynonymTable(tbl.getCatalog(), tbl.getSchema(), tbl.getTableName());
	}

	/**
	 * Return the underlying table of a synonym.
	 *
	 * @param schema the schema of the synonym
	 * @param synonym the name of the synonym
	 *
	 * @return the table to which the synonym points or null if the passed
	 *         name does not reference a synonym or if the DBMS does not support synonyms
	 * @see #getSynonymTable(workbench.db.TableIdentifier)
	 */
	public TableIdentifier getSynonymTable(String catalog, String schema, String synonym)
	{
		SynonymReader reader = getSynonymReader();
		if (reader == null) return null;
		TableIdentifier id = null;
		try
		{
			id = reader.getSynonymTable(this.dbConnection, catalog, schema, synonym);
			if (id != null && id.getType() == null)
			{
				String type = getObjectType(id);
				id.setType(type);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("DbMetadata.getSynonymTable()", "Could not retrieve table for synonym", e);
		}
		return id;
	}

	public SynonymReader getSynonymReader()
	{
		synchronized (readerLock)
		{
			if (synonymReader == null)
			{
				synonymReader = SynonymReader.Factory.getSynonymReader(dbConnection);
			}
		}
		return synonymReader;
	}
}
