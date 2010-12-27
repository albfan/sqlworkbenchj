/*
 * DbMetadata.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
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
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import workbench.db.derby.DerbyColumnEnhancer;
import workbench.db.derby.DerbySequenceReader;
import workbench.db.derby.DerbySynonymReader;
import workbench.db.derby.DerbyTypeReader;
import workbench.db.firebird.FirebirdColumnEnhancer;
import workbench.db.firebird.FirebirdDomainReader;
import workbench.db.firebird.FirebirdSequenceReader;
import workbench.db.h2database.H2ColumnEnhancer;
import workbench.db.h2database.H2ConstantReader;
import workbench.db.h2database.H2DomainReader;
import workbench.db.hsqldb.HsqlSequenceReader;
import workbench.db.ibm.Db2SequenceReader;
import workbench.db.ibm.Db2SynonymReader;
import workbench.db.ingres.IngresMetadata;
import workbench.db.mckoi.McKoiSequenceReader;
import workbench.db.mysql.MySqlEnumReader;
import workbench.db.oracle.DbmsOutput;
import workbench.db.oracle.OracleMetadata;
import workbench.db.oracle.OracleSynonymReader;
import workbench.db.postgres.PostgresDDLFilter;
import workbench.db.postgres.PostgresSequenceReader;
import workbench.storage.SortDefinition;
import workbench.util.ExceptionUtil;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.db.h2database.H2SequenceReader;
import workbench.db.hsqldb.HsqlColumnEnhancer;
import workbench.db.ibm.DB2TypeReader;
import workbench.db.ibm.Db2ColumnEnhancer;
import workbench.db.ibm.Db2ProcedureReader;
import workbench.db.mssql.SqlServerColumnEnhancer;
import workbench.db.mssql.SqlServerDataTypeResolver;
import workbench.db.mssql.SqlServerObjectListEnhancer;
import workbench.db.mssql.SqlServerSynonymReader;
import workbench.db.mssql.SqlServerTypeReader;
import workbench.db.mysql.MySQLTableCommentReader;
import workbench.db.oracle.OracleSequenceReader;
import workbench.db.oracle.OracleTypeReader;
import workbench.db.postgres.PostgresDataTypeResolver;
import workbench.db.postgres.PostgresDomainReader;
import workbench.db.postgres.PostgresEnumReader;
import workbench.db.postgres.PostgresRuleReader;
import workbench.db.postgres.PostgresTypeReader;
import workbench.db.sqlite.SQLiteDataTypeResolver;
import workbench.sql.syntax.SqlKeywordHelper;
import workbench.storage.filter.AndExpression;
import workbench.storage.filter.StringEqualsComparator;
import workbench.util.CaseInsensitiveComparator;
import workbench.util.CollectionUtil;

/**
 * Retrieve meta data information from the database.
 * This class returns more information than the generic JDBC DatabaseMetadata.
 *
 * @author Thomas Kellerer
 */
public class DbMetadata
	implements QuoteHandler
{
	public static final String MVIEW_NAME = "MATERIALIZED VIEW";
	private final String[] EMPTY_STRING_ARRAY = new String[]{};
	
	private String schemaTerm;
	private String catalogTerm;
	private String productName;
	private String dbId;

	MetaDataSqlManager metaSqlMgr;
	private DatabaseMetaData metaData;
	private WbConnection dbConnection;

	private OracleMetadata oracleMetaData;

	private ColumnDefinitionEnhancer columnEnhancer;
	private ObjectListEnhancer objectListEnhancer;
	private TableDefinitionReader definitionReader;
	private ConstraintReader constraintReader;
	private DataTypeResolver dataTypeResolver;
	private SynonymReader synonymReader;
	private SequenceReader sequenceReader;
	private ProcedureReader procedureReader;
	private ErrorInformationReader errorInfoReader;
	private SchemaInformationReader schemaInfoReader;
	private IndexReader indexReader;
	private List<ObjectListExtender> extenders = new ArrayList<ObjectListExtender>();
	private DDLFilter ddlFilter;

	private DbmsOutput oraOutput;

	private boolean isOracle;
	private boolean isPostgres;
	private boolean isFirstSql;
	private boolean isHsql;
	private boolean isFirebird;
	private boolean isSqlServer;
	private boolean isMySql;
	private boolean isApacheDerby;
	private boolean isExcel;
	private boolean isAccess;
	private boolean isH2;

	private String quoteCharacter;
	private final Set<String> keywords = new TreeSet<String>(new CaseInsensitiveComparator());

	private Pattern selectIntoPattern;

	private String baseTableTypeName;

	private Set<String> tableTypesList;
	private String[] tableTypes;
	private String[] tableTypesSelectable;
	private Set<String> schemasToIgnore;
	private Set<String> catalogsToIgnore;

	private DbSettings dbSettings;
	private ViewReader viewReader;

	public DbMetadata(WbConnection aConnection)
		throws SQLException
	{
		this.dbConnection = aConnection;
		this.metaData = aConnection.getSqlConnection().getMetaData();

		try
		{
			this.schemaTerm = this.metaData.getSchemaTerm();
		}
		catch (SQLException e)
		{
			LogMgr.logWarning("DbMetadata.<init>", "Could not retrieve Schema term: " + e.getMessage());
			this.schemaTerm = "Schema";
		}

		try
		{
			this.catalogTerm = this.metaData.getCatalogTerm();
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
			this.dbId = null;
		}
		catch (SQLException e)
		{
			LogMgr.logWarning("DbMetadata.<init>", "Could not retrieve Database Product name", e);
			this.productName = aConnection.getProfile().getDriverclass();
		}

		String productLower = this.productName.toLowerCase();

		if (productLower.indexOf("postgres") > - 1)
		{
			this.isPostgres = true;
			this.sequenceReader = new PostgresSequenceReader(this.dbConnection);
			this.dataTypeResolver = new PostgresDataTypeResolver();

			// Starting with the version 8.2 the driver supports the dollar quoting
			// out of the box, so there is no need to use our own workaround
			if (!JdbcUtils.hasMiniumDriverVersion(dbConnection.getSqlConnection(), "8.2"))
			{
				this.ddlFilter = new PostgresDDLFilter();
			}
			extenders.add(new PostgresDomainReader());
			extenders.add(new PostgresEnumReader());
			extenders.add(new PostgresRuleReader());
			extenders.add(new PostgresTypeReader());
		}
		else if (productLower.indexOf("oracle") > -1)
		{
			isOracle = true;
			oracleMetaData = new OracleMetadata(this.dbConnection);
			synonymReader = new OracleSynonymReader();
			sequenceReader = new OracleSequenceReader(this.dbConnection);
			errorInfoReader = oracleMetaData;
			dataTypeResolver = oracleMetaData;
			definitionReader = oracleMetaData;
			extenders.add(new OracleTypeReader());
		}
		else if (productLower.indexOf("hsql") > -1)
		{
			this.isHsql = true;
			if (JdbcUtils.hasMinimumServerVersion(dbConnection, "2.0"))
			{
				// HSQLDB 2.0 has a completely different set of system tables
				// so the dynamically configured queries in the XML files need
				// to be different, therefor the product name is "patched" to include the version number
				productName += " 2.0";
				columnEnhancer = new HsqlColumnEnhancer();
			}
			this.sequenceReader = new HsqlSequenceReader(this.dbConnection.getSqlConnection());
		}
		else if (productLower.indexOf("firebird") > -1)
		{
			this.isFirebird = true;
			// Jaybird 2.0 reports the Firebird version in the
			// productname. To ease the DBMS handling we'll use the same
			// product name that is reported with the 1.5 driver.
			// Otherwise the DBID would look something like:
			// firebird_2_0_wi-v2_0_1_12855_firebird_2_0_tcp__wallace__p10
			productName = "Firebird";
			sequenceReader = new FirebirdSequenceReader(dbConnection);
			extenders.add(new FirebirdDomainReader());
			columnEnhancer = new FirebirdColumnEnhancer();
		}
		else if (productLower.indexOf("sql server") > -1)
		{
			isSqlServer = true;
			if (SqlServerSynonymReader.supportsSynonyms(dbConnection))
			{
				synonymReader = new SqlServerSynonymReader(this);
			}

			if (SqlServerTypeReader.versionSupportsTypes(dbConnection))
			{
				extenders.add(new SqlServerTypeReader());
			}
			columnEnhancer = new SqlServerColumnEnhancer();
			objectListEnhancer = new SqlServerObjectListEnhancer();
			dataTypeResolver = new SqlServerDataTypeResolver();
		}
		else if (productLower.indexOf("db2") > -1)
		{
			synonymReader = new Db2SynonymReader();
			sequenceReader = new Db2SequenceReader(dbConnection, getDbId());
			procedureReader = new Db2ProcedureReader(dbConnection, getDbId());

			// Generated columns are not available on the host version...
			if (getDbId().equals("db2"))
			{
				columnEnhancer = new Db2ColumnEnhancer();
				extenders.add(new DB2TypeReader());
			}
		}
		else if (productLower.indexOf("mysql") > -1)
		{
			this.isMySql = true;
			columnEnhancer = new MySqlEnumReader();
			objectListEnhancer = new MySQLTableCommentReader();
		}
		else if (productLower.indexOf("cloudscape") > -1)
		{
			this.isApacheDerby = true;
		}
		else if (productLower.indexOf("derby") > -1)
		{
			this.isApacheDerby = true;
			this.synonymReader = new DerbySynonymReader();
			if (JdbcUtils.hasMinimumServerVersion(dbConnection, "10.6"))
			{
				sequenceReader = new DerbySequenceReader(dbConnection);
				extenders.add(new DerbyTypeReader());
			}
			columnEnhancer = new DerbyColumnEnhancer();
		}
		else if (productLower.indexOf("ingres") > -1)
		{
			IngresMetadata imeta = new IngresMetadata(dbConnection.getSqlConnection());
			synonymReader = imeta;
			sequenceReader = imeta;
		}
		else if (productLower.indexOf("mckoi") > -1)
		{
			// McKoi reports the version in the database product name
			// which makes setting up the meta data stuff lookups
			// too complicated, so we'll strip the version info
			int pos = this.productName.indexOf('(');
			if (pos == -1) pos = this.productName.length() - 1;
			productName = this.productName.substring(0, pos).trim();
			sequenceReader = new McKoiSequenceReader(this.dbConnection.getSqlConnection());
		}
		else if (productLower.indexOf("sqlite") > -1)
		{
			dataTypeResolver = new SQLiteDataTypeResolver();
		}
		else if (productLower.indexOf("firstsql") > -1)
		{
			isFirstSql = true;
		}
		else if (productLower.indexOf("excel") > -1)
		{
			isExcel = true;
		}
		else if (productLower.indexOf("access") > -1)
		{
			isAccess = true;
		}
		else if (productLower.equals("h2"))
		{
			isH2 = true;
			sequenceReader = new H2SequenceReader(this.dbConnection.getSqlConnection());
			extenders.add(new H2DomainReader());
			extenders.add(new H2ConstantReader());
			columnEnhancer = new H2ColumnEnhancer();
		}

		this.schemaInfoReader = new GenericSchemaInfoReader(this.getDbId());

		if (this.dataTypeResolver == null)
		{
			this.dataTypeResolver = new DefaultDataTypeResolver();
		}

		if (definitionReader == null)
		{
			definitionReader = new JdbcTableDefinitionReader();
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

		if (StringUtil.isBlank(quoteCharacter)) this.quoteCharacter = "\"";

		this.dbSettings = new DbSettings(this.getDbId(), this.productName);

		this.metaSqlMgr = new MetaDataSqlManager(this.getProductName());


		baseTableTypeName = Settings.getInstance().getProperty("workbench.db.basetype.table." + this.getDbId(), "TABLE");

		List<String> ttypes = Settings.getInstance().getListProperty("workbench.db." + getDbId() + ".tabletypes", false, "TABLE");
		tableTypesList = new TreeSet<String>(new CaseInsensitiveComparator());
		tableTypesList.addAll(ttypes);

		tableTypes = tableTypesList.toArray(new String[]{});

		// The tableTypesSelectable array will be used
		// to fill the completion cache. In that case
		// we do not want system tables included (which
		// is done for the objectsWithData as that
		// drives the "Data" tab in the DbExplorer)
		Set<String> types = getObjectsWithData();

		if (!getDbSettings().includeSystemTablesInSelectable())
		{
			Iterator<String> itr = types.iterator();
			while (itr.hasNext())
			{
				String s = itr.next();
				if (s.toUpperCase().indexOf("SYSTEM") > -1)
				{
					itr.remove();
				}
			}
		}

		tableTypesSelectable = StringUtil.toArray(types, true);

		String pattern = Settings.getInstance().getProperty("workbench.db." + getDbId() + ".selectinto.pattern", null);
		if (pattern != null)
		{
			try
			{
				this.selectIntoPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
			}
			catch (Exception e)
			{
				LogMgr.logError("DbMetadata.<init>", "Incorrect Pattern for detecting SELECT ... INTO <new table> specified", e);
				this.selectIntoPattern = null;
			}
		}
	}

	public ProcedureReader getProcedureReader()
	{
		synchronized (ReaderFactory.READER_LOCK)
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
		synchronized (MVIEW_NAME)
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
		return tableTypesList.toArray(EMPTY_STRING_ARRAY);
	}
	
	public String[] getTablesAndViewTypes()
	{
		List<String> types = new ArrayList<String>(tableTypesList);
		types.add(getViewTypeName());
		return types.toArray(EMPTY_STRING_ARRAY);
	}
	
	public List<String> getTableTypes()
	{
		return new ArrayList<String>(tableTypesList);
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
		synchronized (ReaderFactory.READER_LOCK)
		{
			if (indexReader == null)
			{
				indexReader = ReaderFactory.getIndexReader(this);
			}
			return this.indexReader;
		}
	}

	public OracleMetadata getOracleMeta()
	{
		return this.oracleMetaData;
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
	 */
	public boolean objectTypeCanContainData(String type)
	{
		if (type == null) return false;
		return getObjectsWithData().contains(type.toLowerCase());
	}

	private Set<String> getObjectsWithData()
	{
		Set<String> objectsWithData = new TreeSet<String>();
		String keyPrefix = "workbench.db.objecttype.selectable.";
		String defValue = Settings.getInstance().getProperty(keyPrefix + "default", null);
		String types = Settings.getInstance().getProperty(keyPrefix + getDbId(), defValue);

		if (types == null)
		{
			objectsWithData.add("table");
			objectsWithData.add("view");
			objectsWithData.add("synonym");
			objectsWithData.add("system view");
			objectsWithData.add("system table");
		}
		else
		{
			List<String> l = StringUtil.stringToList(types.toLowerCase(), ",", true, true);
			objectsWithData.addAll(l);
		}

		if (this.isPostgres)
		{
			objectsWithData.add("sequence");
		}

		if (this.isOracle)
		{
			objectsWithData.add(MVIEW_NAME.toLowerCase());
		}

		return objectsWithData;
	}

	/**
	 *	Return the name of the DBMS as reported by the JDBC driver.
	 * <br/>
	 * For configuration purposes the DBID should be used as that can be part of a key
	 * in a properties file.
	 * @see #getDbId()
	 */
	public String getProductName()
	{
		return this.productName;
	}

	/**
	 * Return a clean version of the productname that can be used as the part of a properties key.
	 *
	 * @see #getProductName()
	 */
	public String getDbId()
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
			LogMgr.logInfo("DbMetadata.<init>", "Using DBID=" + this.dbId);
		}
		return this.dbId;
	}

	public DbSettings getDbSettings()
	{
		return this.dbSettings;
	}

	/**
	 * Returns true if the current DBMS supports a SELECT syntax
	 * which creates a new table (e.g. SELECT .. INTO new_table FROM old_table)
	 *
	 * It simply checks if a regular expression has been defined to
	 * detect this kind of statements
	 *
	 * @see #isSelectIntoNewTable(String)
	 */
	public boolean supportsSelectIntoNewTable()
	{
		return this.selectIntoPattern != null;
	}

	/**
	 * Checks if the given SQL string is actually some kind of table
	 * creation "disguised" as a SELECT.
	 * Whether a statement is identified as a SELECT into a new table
	 * is defined through the regular expression that can be set for
	 * the DBMS using the property:
	 * <tt>workbench.sql.[dbid].selectinto.pattern</tt>
	 *
	 * This method returns true if a Regex has been defined and matches the given SQL
	 */
	public boolean isSelectIntoNewTable(String sql)
	{
		if (this.selectIntoPattern == null) return false;
		return SqlUtil.isSelectIntoNewTable(this.selectIntoPattern, sql);
	}

	public boolean isMySql() { return this.isMySql; }
	public boolean isPostgres() { return this.isPostgres; }
  public boolean isOracle() { return this.isOracle; }
	public boolean isHsql() { return this.isHsql; }
	public boolean isFirebird() { return this.isFirebird; }
	public boolean isFirstSql() { return this.isFirstSql; }
	public boolean isSqlServer() { return this.isSqlServer; }
	public boolean isApacheDerby() { return this.isApacheDerby; }
	public boolean isH2() { return this.isH2; }

	/**
	 * If a DDLFilter is registered for the current DBMS, this
	 * method will replace all "problematic" characters in the
	 * SQL string, and will return a String that the DBMS will
	 * understand.
	 * <br/>
	 * Currently this is only implemented for PostgreSQL to
	 * fix the lack of $$ in older JDBC drivers
	 *
	 * @see workbench.db.postgres.PostgresDDLFilter
	 * @see workbench.sql.commands.DdlCommand#execute(java.lang.String)
	 */
	public String filterDDL(String sql)
	{
		if (this.ddlFilter == null) return sql;
		return this.ddlFilter.adjustDDL(sql);
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
		if (StringUtil.isEmptyString(schema)) return true;
		if (schemasToIgnore == null)
		{
			String ids = Settings.getInstance().getProperty("workbench.sql.ignoreschema." + this.getDbId(), null);
			if (ids != null)
			{
				schemasToIgnore = new TreeSet<String>(StringUtil.stringToList(ids, ","));
			}
			else
			{
				 schemasToIgnore = Collections.emptySet();
			}
		}
		return schemasToIgnore.contains("*") || schemasToIgnore.contains(schema);
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
	 * If either no current schema is available, or it is the same as the table's schema
	 * the result of ignoreSchema() is checked to leave out e.g. the PUBLIC schema in Postgres or H2
	 *
	 * @see #ignoreSchema(java.lang.String)
	 * @see #supportsSchemas()
	 */
	public boolean needSchemaInDML(TableIdentifier table)
	{
		if (!supportsSchemas()) return false;

		try
		{
			String tblSchema = table.getSchema();

			// Object names may never be prefixed with PUBLIC
			if (this.isOracle && "PUBLIC".equalsIgnoreCase(tblSchema)) return false;

			String currentSchema = getCurrentSchema();

			if (StringUtil.isBlank(currentSchema))
			{
				 return (!ignoreSchema(tblSchema));
			}

			// If the current schema is not the one of the table, the schema is needed in DML statements
			return (!currentSchema.equalsIgnoreCase(tblSchema));
		}
		catch (Throwable th)
		{
			return false;
		}
	}

	/**
	 * Check if the given {@link TableIdentifier} requires
	 * the usage of a catalog name for a DML statement.
	 * <br/>
	 * First the result of ignoreCatalog() is tested. If that is true, then this method returns false.
	 * <br/>
	 * If the current DB engine is Microsoft Access, this method always returns true.
	 * If the current DBMS does not support catalogs, false is returned
	 * <br/>
	 * For all other DBMS, the result of this method depends on the setting if
	 * a catalog is needed in case it's not the current catalog.
   *
	 * @see #ignoreCatalog(java.lang.String)
	 * @see #supportsCatalogs()
	 * @see DbSettings#needsCatalogIfNoCurrent()
	 * @see #getCurrentCatalog()
	 */
	public boolean needCatalogInDML(TableIdentifier table)
	{
		if (this.isAccess) return true;
		if (!this.supportsCatalogs()) return false;
		if (!dbSettings.useCatalogInDML()) return false;

		String cat = table.getCatalog();
		if (StringUtil.isEmptyString(cat)) return false;
		if (ignoreCatalog(cat)) return false;

		String currentCat = getCurrentCatalog();

		if (this.isExcel)
		{
			// Excel puts the directory into the catalog
			// so we need to normalize the directory name
			File c1 = new File(cat);
			File c2 = new File(currentCat);
			if (c1.equals(c2)) return false;
			return true;
		}

		if (StringUtil.isEmptyString(currentCat))
		{
			return this.dbSettings.needsCatalogIfNoCurrent();
		}
		return !cat.equalsIgnoreCase(currentCat);
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
		String c = getCurrentCatalog();
		if (c != null && c.equalsIgnoreCase(catalog)) return true;
		if (catalogsToIgnore == null)
		{
			String cats = Settings.getInstance().getProperty("workbench.sql.ignorecatalog." + this.getDbId(), null);
			if (cats != null)
			{
				catalogsToIgnore = new TreeSet<String>(StringUtil.stringToList(cats, ","));
			}
			else
			{
				 catalogsToIgnore = Collections.emptySet();
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
	 * Returns the type of the passed TableIdentifier. This could
	 * be VIEW, TABLE, SYNONYM, ...
	 * If the JDBC driver does not return the object through the getObjects()
	 * method, null is returned, otherwise the value reported in TABLE_TYPE
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

	public CharSequence generateDrop(String type, String objectName)
	{
		StringBuilder result = new StringBuilder(type.length() + objectName.length() + 15);

		String prefix = "workbench.db.";
		String suffix = "." + type.toLowerCase() + ".sql." + this.getDbId();

		String drop = Settings.getInstance().getProperty(prefix + "drop" + suffix, null);
		if (drop == null)
		{
			result.append("DROP ");
			result.append(type.toUpperCase());
			result.append(' ');
			result.append(quoteObjectname(objectName));
			String cascade = this.dbSettings.getCascadeConstraintsVerb(type);
			if (cascade != null)
			{
				result.append(' ');
				result.append(cascade);
			}
			result.append(";\n");
		}
		else
		{
			drop = StringUtil.replace(drop, "%name%", quoteObjectname(objectName));
			result.append(SqlUtil.addSemicolon(drop));
		}
		return result;
	}

	public StringBuilder generateCreateObject(boolean includeDrop, String type, String name)
	{
		StringBuilder result = new StringBuilder();
		boolean replaceAvailable = false;

		String prefix = "workbench.db.";
		String suffix = "." + type.toLowerCase() + ".sql." + this.getDbId();

		String replace = Settings.getInstance().getProperty(prefix + "replace" + suffix, null);
		if (replace != null)
		{
			result.append(StringUtil.replace(replace, "%name%", name));
			replaceAvailable = true;
		}

		if (includeDrop && !replaceAvailable)
		{
			result.append(generateDrop(type, name));
			result.append('\n');
		}

		if (!replaceAvailable)
		{
			String create = Settings.getInstance().getProperty(prefix + "create" + suffix, null);
			if (create == null)
			{
				result.append("CREATE ");
				result.append(type.toUpperCase());
				result.append(' ');
				result.append(name);
			}
			else
			{
				create = StringUtil.replace(create, "%name%", name);
				result.append(create);
			}
		}
		return result;
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
	public boolean isQuoted(String name)
	{
		if (StringUtil.isEmptyString(name)) return false;
		if (name.startsWith(quoteCharacter)) return true;

		// SQL Server driver claims that a " is the quote character but still
		// accepts those iditotic brackets as quote characters...
		if (this.isSqlServer)
		{
			if (name.charAt(0) == '[' && name.charAt(name.length() - 1) == ']') return true;
		}
		return false;
	}

	public String removeQuotes(String name)
	{
		if (StringUtil.isEmptyString(name)) return name;

		if (this.isSqlServer && name.startsWith("[") && name.endsWith("]"))
		{
			return name.substring(1, name.length() - 1);
		}
		return StringUtil.removeQuotes(name, quoteCharacter);
	}

	public String quoteObjectname(String aName)
	{
		return quoteObjectname(aName, false);
	}


	/**
	 *	Encloses the given object name in double quotes if necessary.
	 *	Quoting of names is necessary if the name is a reserved word in the
	 *	database. To check if the given name is a keyword, it is compared
	 *  to the words returned by getSQLKeywords().
	 *
	 *	If the given name is not a keyword, {@link workbench.util.SqlUtil#quoteObjectname(String)}
	 *  will be called to check if the name contains special characters which require
	 *	double quotes around the object name.
	 *
	 *  For Oracle and HSQL strings starting with a digit will
	 *  always be quoted.
	 */
	public String quoteObjectname(String name, boolean quoteAlways)
	{
		if (name == null) return null;
		if (name.length() == 0) return name;

		// already quoted?
		if (isQuoted(name)) return name;

		if (this.dbSettings.neverQuoteObjects()) return removeQuotes(name);

		boolean needQuote = quoteAlways;

		// Excel does not support the standard rules for SQL identifiers
		// Basically anything that does not contain only characters needs to
		// be quoted.
		if (this.isExcel)
		{
			Pattern chars = Pattern.compile("[A-Za-z0-9]+");
			Matcher m = chars.matcher(name);
			needQuote = !m.matches();
		}

		try
		{
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

			if (needQuote || isKeyword(name))
			{
				StringBuilder result = new StringBuilder(name.length() + 4);
				result.append(this.quoteCharacter);
				result.append(name.trim());
				result.append(this.quoteCharacter);
				return result.toString();
			}

		}
		catch (Exception e)
		{
			LogMgr.logWarning("DbMetadata.quoteObjectName()", "Error when retrieving DB information", e);
		}

		// if it is not a keyword, we have to check for special characters such
		// as a space, $, digits at the beginning etc
		return SqlUtil.quoteObjectname(name);
	}

	public String adjustSchemaNameCase(String schema)
	{
		return adjustSchemaNameCase(schema, false);
	}

	/**
	 * Adjusts the case of the given schema name to the
	 * case in which the server stores schema names.
	 *
	 * This is needed e.g. when the user types a
	 * table name, and that value is used to retrieve
	 * the table definition.
	 *
	 * @param schema the schema name to adjust
	 * @return the adjusted schema name
	 */
	public String adjustSchemaNameCase(String schema, boolean respectQuotes)
	{
		if (StringUtil.isBlank(schema)) return null;
		if (respectQuotes && isQuoted(schema)) return schema;

		schema = StringUtil.trimQuotes(schema).trim();
		try
		{
			if (this.storesUpperCaseSchemas())
			{
				return schema.toUpperCase();
			}
			else if (this.storesLowerCaseSchemas())
			{
				return schema.toLowerCase();
			}
		}
		catch (Exception e)
		{
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

		if (supportsMixedCaseIdentifiers()) return true;

		boolean isUpper = StringUtil.isUpperCase(name);
		boolean isLower = StringUtil.isLowerCase(name);

		if (isUpper && storesUpperCaseIdentifiers())  return true;
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
	 */
	public String getCurrentSchema()
	{
		if (!supportsSchemas()) return null;
		if (this.schemaInfoReader != null)
		{
			return this.schemaInfoReader.getCurrentSchema(this.dbConnection);
		}
		return null;
	}

	/**
	 * Returns the schema that should be used for the current user
	 * This essential call {@link #getCurrentSchema()}. The method
	 * then checks if the schema should be ignored for the current
	 * dbms by calling {@link #ignoreSchema(String)}. If the
	 * Schema should not be ignored, the it's returned, otherwise
	 * the method will return null
	 */
	public String getSchemaToUse()
	{
		String schema = this.getCurrentSchema();
		if (schema == null) return null;
		if (this.ignoreSchema(schema)) return null;
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

	public DataStore getObjects(String catalogPattern, String schemaPattern, String namePattern, String[] types)
		throws SQLException
	{
		if ("*".equals(schemaPattern) || "%".equals(schemaPattern)) schemaPattern = null;
		if ("*".equals(namePattern) || "%".equals(namePattern)) namePattern = null;

		if (schemaPattern != null) schemaPattern = StringUtil.replace(schemaPattern, "*", "%");
		if (namePattern != null) namePattern = StringUtil.replace(namePattern, "*", "%");
		String[] cols = getTableListColumns();
		int coltypes[] = {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
		int sizes[] = {30, 12, 10, 10, 20};

		DataStore result = new DataStore(cols, coltypes, sizes);

		boolean sequencesReturned = false;
		boolean checkOracleSnapshots = (isOracle && Settings.getInstance().getBoolProperty("workbench.db.oracle.detectsnapshots", true) && typeIncluded("TABLE", types));
		boolean synRetrieved = false;
		boolean synonymsRequested = typeIncluded("SYNONYM", types);

		ObjectListFilter filter = new ObjectListFilter(getDbId());

		Set<String> snapshotList = Collections.emptySet();
		if (checkOracleSnapshots)
		{
			snapshotList = oracleMetaData.getSnapshots(schemaPattern);
		}

		boolean hideIndexes = hideIndexes();

		ResultSet tableRs = null;
		try
		{
			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logDebug("DbMetadata.getObjects()", "Calling getTables() using: catalog="+ catalogPattern + ", schema=" + schemaPattern + ", name=" + namePattern + ", types=" + (types == null ? "NULL" : Arrays.asList(types).toString()));
			}
			tableRs = metaData.getTables(StringUtil.trimQuotes(catalogPattern), StringUtil.trimQuotes(schemaPattern), StringUtil.trimQuotes(namePattern), types);
			if (tableRs == null)
			{
				LogMgr.logError("DbMetadata.getTables()", "Driver returned a NULL ResultSet from getTables()",null);
				return result;
			}

			while (tableRs.next())
			{
				String cat = tableRs.getString(1);
				String schema = tableRs.getString(2);
				String name = tableRs.getString(3);
				String ttype = tableRs.getString(4);
				if (name == null) continue;

				if (filter.isExcluded(ttype, name)) continue;

				boolean isSynoym = "SYNONYM".equals(ttype);

				// prevent duplicate retrieval of SYNONYMs if the driver
				// returns them already, but the Settings have enabled
				// Synonym retrieval as well
				// (e.g. because an upgraded Driver now returns the synonyms)
				if (!synRetrieved && isSynoym)
				{
					synRetrieved = true;
				}

				if (hideIndexes && isIndexType(ttype)) continue;

				if (checkOracleSnapshots)
				{
					StringBuilder t = new StringBuilder(30);
					t.append(schema);
					t.append('.');
					t.append(name);
					if (snapshotList.contains(t.toString()))
					{
						ttype = MVIEW_NAME;
					}
				}

				String rem = tableRs.getString(5);
				int row = result.addRow();
				result.setValue(row, COLUMN_IDX_TABLE_LIST_NAME, name);
				result.setValue(row, COLUMN_IDX_TABLE_LIST_TYPE, ttype);
				result.setValue(row, COLUMN_IDX_TABLE_LIST_CATALOG, cat);
				result.setValue(row, COLUMN_IDX_TABLE_LIST_SCHEMA, schema);
				result.setValue(row, COLUMN_IDX_TABLE_LIST_REMARKS, rem);
				if (!sequencesReturned && "SEQUENCE".equals(ttype)) sequencesReturned = true;
			}
		}
		finally
		{
			SqlUtil.closeResult(tableRs);
		}

		boolean sortNeeded = false;

		if (this.sequenceReader != null && typeIncluded("SEQUENCE", types) &&
				Settings.getInstance().getBoolProperty("workbench.db." + this.getDbId() + ".retrieve_sequences", true)
				&& !sequencesReturned)
		{
			List<SequenceDefinition> sequences = sequenceReader.getSequences(catalogPattern, schemaPattern, namePattern);
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
			sortNeeded = true;
		}

		boolean retrieveSyns = (this.synonymReader != null && Settings.getInstance().getBoolProperty("workbench.db." + this.getDbId() + ".retrieve_synonyms", false));
		if (retrieveSyns && !synRetrieved && synonymsRequested)
		{
			List<TableIdentifier> syns = synonymReader.getSynonymList(dbConnection, schemaPattern, namePattern);
			for (TableIdentifier synonym : syns)
			{
				int row = result.addRow();

				result.setValue(row, COLUMN_IDX_TABLE_LIST_NAME, synonym.getTableName());
				result.setValue(row, COLUMN_IDX_TABLE_LIST_TYPE, synonym.getType());
				result.setValue(row, COLUMN_IDX_TABLE_LIST_CATALOG, synonym.getCatalog());
				result.setValue(row, COLUMN_IDX_TABLE_LIST_SCHEMA, synonym.getSchema());
				result.setValue(row, COLUMN_IDX_TABLE_LIST_REMARKS, synonym.getComment());
			}
			sortNeeded = true;
		}

		for (ObjectListExtender extender : extenders)
		{
			if (extender.handlesType(types))
			{
				if (extender.extendObjectList(dbConnection, result, catalogPattern, schemaPattern, namePattern, types))
				{
					sortNeeded = true;
				}
			}
		}

		if (objectListEnhancer != null)
		{
			objectListEnhancer.updateObjectList(dbConnection, result, catalogPattern, schemaPattern, namePattern, types);
		}

		if (sortNeeded)
		{
			SortDefinition def = new SortDefinition();
			def.addSortColumn(COLUMN_IDX_TABLE_LIST_TYPE, true);
			def.addSortColumn(COLUMN_IDX_TABLE_LIST_SCHEMA, true);
			def.addSortColumn(COLUMN_IDX_TABLE_LIST_NAME, true);
			result.sort(def);
		}

		result.resetStatus();
		return result;
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

			DataStore ds = getObjects(catalog, schema, table.getRawTableName(), null);
			String[] cols = getTableListColumns();

			if (ds.getRowCount() == 0 && this.isOracle)
			{
				// try again with PUBLIC, maybe it's a public synonym
				ds = getObjects(null, "PUBLIC", table.getRawTableName(), null);
			}

			if (ds.getRowCount() == 0 && schemaWasNull && searchAllSchemas)
			{
				ds = getObjects(null, null, table.getRawTableName(), null);
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
			LogMgr.logError("DbMetadata.tableExists()", "Error checking table existence", e);
		}
		return result;
	}

	/**
	 * Check if the given table exists in the database
	 */
	public boolean tableExists(TableIdentifier aTable)
	{
		return objectExists(aTable, tableTypes);
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
		return findTable(tbl, tableTypesSelectable, false);
	}

	public TableIdentifier findTable(TableIdentifier tbl, boolean searchAllSchemas)
	{
		return findTable(tbl, tableTypes, searchAllSchemas);
	}

	public TableIdentifier findTable(TableIdentifier tbl)
	{
		return findTable(tbl, tableTypes, false);
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

			DataStore ds = getObjects(catalog, schema, table.getRawTableName(), types);

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
			LogMgr.logError("DbMetadata.tableExists()", "Error checking table existence", e);
		}
		return result;
	}

	protected boolean supportsMixedCaseIdentifiers()
	{
		try
		{
			return this.metaData.supportsMixedCaseIdentifiers();
		}
		catch (Exception e)
		{
			return false;
		}
	}

	/**
	 * Returns true if the server stores identifiers in mixed case.
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

	public boolean isCaseSensitive()
	{
		try
		{
			// According to the JDBC docs, supportsMixedCaseIdentifiers()
			// should only return true if the server is case sensitive...
			return this.metaData.supportsMixedCaseIdentifiers();
		}
		catch (SQLException ex)
		{
			LogMgr.logWarning("DbMetadata.isCaseSensitive()", "Error when calling supportsMixedCaseIdentifiers()", ex);
			// Standard SQL identifiers are not case sensitive.
			return false;
		}
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
			return this.metaData.storesLowerCaseIdentifiers();
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
			return this.metaData.storesUpperCaseIdentifiers();
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
	 * Release any resources for this object. After a call
	 * to close(), this object should not be used any longer
	 */
	public void close()
	{
		if (this.oraOutput != null) this.oraOutput.close();
		if (this.oracleMetaData != null) this.oracleMetaData.done();
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

	/**
	 * Return the column list for the given table.
	 * @param table the table for which to retrieve the column definition
	 * @see #getTableDefinition(workbench.db.TableIdentifier)
	 */
	public List<ColumnIdentifier> getTableColumns(TableIdentifier table)
		throws SQLException
	{
		TableDefinition def = this.getTableDefinition(table);
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
		if (def == null && "SEQUENCE".equalsIgnoreCase(table.getObjectType()))
		{
			TableIdentifier tbl = table.createCopy();
			tbl.adjustCase(this.dbConnection);
			String schema = StringUtil.trimQuotes(table.getSchema());
			String seqname = StringUtil.trimQuotes(table.getObjectName());
			String catalog = StringUtil.trimQuotes(table.getCatalog());
			def = getSequenceReader().getRawSequenceDefinition(catalog, schema, seqname);
		}
		else if (def == null)
		{
			TableDefinition tdef = getTableDefinition(table);
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
	 */
	public TableDefinition getTableDefinition(TableIdentifier toRead)
		throws SQLException
	{
		if (toRead == null) return null;

		TableIdentifier table = toRead.createCopy();
		table.adjustCase(dbConnection);

		String catalog = StringUtil.trimQuotes(table.getCatalog());
		String schema = StringUtil.trimQuotes(table.getSchema());
		String tablename = StringUtil.trimQuotes(table.getTableName());

		if (schema == null)
		{
			schema = getCurrentSchema();
			table.setSchema(schema);
		}

		if (catalog == null)
		{
			catalog = getCurrentCatalog();
			table.setCatalog(catalog);
		}

		TableIdentifier retrieve = table;

		if ("SYNONYM".equalsIgnoreCase(table.getType()))
		{
			TableIdentifier id = getSynonymTable(schema, tablename);
			if (id != null)
			{
				schema = id.getSchema();
				tablename = id.getTableName();
				catalog = null;
				retrieve = table.createCopy();
				retrieve.setSchema(schema);
				retrieve.setTable(tablename);
				retrieve.setCatalog(null);
			}
		}

		List<String> keys = new ArrayList<String>();
		String pkname = null;

		if (dbSettings.supportsGetPrimaryKeys())
		{
			ResultSet keysRs = null;
			try
			{
				keysRs = metaData.getPrimaryKeys(catalog, schema, tablename);
				while (keysRs.next())
				{
					keys.add(keysRs.getString("COLUMN_NAME").toLowerCase());
					pkname = keysRs.getString("PK_NAME");
				}
			}
			catch (Throwable e)
			{
				LogMgr.logWarning("JdbcTableDefinitionReader.getTableDefinition()", "Error retrieving key columns: " + e.getMessage());
			}
			finally
			{
				SqlUtil.closeResult(keysRs);
			}
		}
		table.setPrimaryKeyName(pkname);

		List<ColumnIdentifier> columns = definitionReader.getTableColumns(retrieve, keys, dbConnection, dataTypeResolver);

		table.setNewTable(false);
		TableDefinition result = new TableDefinition(table, columns);
		if (columnEnhancer != null)
		{
			columnEnhancer.updateColumnDefinition(result, dbConnection);
		}

		return result;
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
	 * The types used are those returned by #getT
	 * @throws SQLException
	 */
	public List<TableIdentifier> getTableList()
		throws SQLException
	{
		return getObjectList(null, getCurrentSchema(), tableTypes);
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
		return getObjectList(table, schema, tableTypes);
	}

	/**
	 * Returns a list of objects from which a SELECT can be run.
	 * <br/>
	 * Typically these are tables, views and materialized views.
	 *
	 * @param name a pattern to search for object names
	 * @param schema
	 * @throws SQLException
	 */
	public List<TableIdentifier> getSelectableObjectsList(String name, String schema)
		throws SQLException
	{
		return getObjectList(name, schema, tableTypesSelectable);
	}

	/**
	 * Return a list of tables for the given schema
	 * if the table name is null, all tables will be returned
	 *
	 * @see #getObjects(java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
	 */
	public List<TableIdentifier> getObjectList(String table, String schema, String[] types)
		throws SQLException
	{
		DataStore ds = getObjects(null, schema, table, types);
		int count = ds.getRowCount();
		List<TableIdentifier> tables = new ArrayList<TableIdentifier>(count);
		for (int i=0; i < count; i++)
		{
			TableIdentifier tbl = buildTableIdentifierFromDs(ds, i);
			if (ignoreSchema(tbl.getSchema()))
			{
				tbl.setSchema(null);
			}

			if (ignoreCatalog(tbl.getCatalog()))
			{
				tbl.setCatalog(null);
			}
			tables.add(tbl);
		}
		return tables;
	}

	private TableIdentifier buildTableIdentifierFromDs(DataStore ds, int row)
	{
		String t = ds.getValueAsString(row, COLUMN_IDX_TABLE_LIST_NAME);
		String s = ds.getValueAsString(row, COLUMN_IDX_TABLE_LIST_SCHEMA);
		String c = ds.getValueAsString(row, COLUMN_IDX_TABLE_LIST_CATALOG);
		TableIdentifier tbl = new TableIdentifier(c, s, t);
		tbl.setNeverAdjustCase(true);
		tbl.setType(ds.getValueAsString(row, COLUMN_IDX_TABLE_LIST_TYPE));
		tbl.setComment(ds.getValueAsString(row, COLUMN_IDX_TABLE_LIST_REMARKS));
		return tbl;
	}

	/**
	 * Return the current catalog for this connection. If no catalog is defined
	 * or the DBMS does not support catalogs, an empty string is returned.
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
	 *
	 * @return The name of the current catalog or an empty String if there is no current catalog
	 */
	public String getCurrentCatalog()
	{
		if (!this.supportsCatalogs()) return null;

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
				LogMgr.logWarning("DbMetadata.getCurrentCatalog()", "Error retrieving current catalog using query=[" + query + "]", e);
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
				catalog = StringUtil.EMPTY_STRING;
			}
		}
		if (catalog == null) catalog = StringUtil.EMPTY_STRING;

		return catalog;
	}

	protected boolean supportsSchemas()
	{
		boolean supportsSchemas = false;
		try
		{
			supportsSchemas = metaData.supportsSchemasInDataManipulation()
		                  || metaData.supportsSchemasInTableDefinitions()
											|| metaData.supportsSchemasInProcedureCalls();
		}
		catch (Exception e)
		{
			supportsSchemas = false;
		}
		return supportsSchemas;

	}

	public boolean supportsCatalogs()
	{
		boolean supportsCatalogs = false;
		try
		{
			supportsCatalogs = metaData.supportsCatalogsInDataManipulation()
		                  || metaData.supportsCatalogsInTableDefinitions()
											|| metaData.supportsCatalogsInProcedureCalls();
		}
		catch (Exception e)
		{
			supportsCatalogs = false;
		}
		return supportsCatalogs;
	}

	/**
	 *	Returns a list of all catalogs in the database.
	 *	Some DBMS's do not support catalogs, in this case the method
	 *	will return an empty List.
	 * <br/>
	 * The list of catalogs will not be filtered.
	 */
	public List<String> getCatalogInformation()
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

		ResultSet rs = null;
		try
		{
			rs = this.metaData.getCatalogs();
			while (rs.next())
			{
				String cat = rs.getString(1);
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
		if (result.size() == 1)
		{
			if (result.get(0).equals(this.getCurrentCatalog()))
			{
				result.clear();
			}
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
		ArrayList<String> result = new ArrayList<String>();
		ResultSet rs = null;

		try
		{
			rs = this.metaData.getSchemas();
			while (rs.next())
			{
				String schema = rs.getString(1);
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

		// The Oracle driver does not return the "PUBLIC"
		// schema (which is - strictly speaking - correct as
		// there is no user PUBLIC in the database), but to view
		// public synonyms we need the entry for the DbExplorer
		if (isOracle)
		{
			result.add("PUBLIC");
			Collections.sort(result);
		}
		return result;
	}

	private boolean isIndexType(String type)
	{
		if (type == null) return false;
		return (type.indexOf("INDEX") > -1);
	}

	private boolean hideIndexes()
	{
		return (isPostgres && Settings.getInstance().getBoolProperty("workbench.db.postgres.hideindex", true));
	}

	/**
	 * Return a list of types that identify tables in the target database.
	 * e.g. TABLE, SYSTEM TABLE, ...
	 */
	public Collection<String> getObjectTypes()
	{
		Set<String> result = CollectionUtil.caseInsensitiveSet();
		ResultSet rs = null;
		boolean hideIndexes = hideIndexes();

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

				if (hideIndexes && isIndexType(type)) continue;
				result.add(type);
			}
			String additional = Settings.getInstance().getProperty("workbench.db." + this.getDbId() + ".additional.tabletypes",null);
			List<String> addTypes = StringUtil.stringToList(additional, ",", true, true);
			result.addAll(addTypes);

			if (this.synonymReader != null)
			{
				result.add("SYNONYM");
			}

			if (sequenceReader != null)
			{
				result.add("SEQUENCE");
			}

			for (ObjectListExtender extender : extenders)
			{
				result.addAll(extender.supportedTypes());
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("DbMetadata.getTableTypes()", "Error retrieving table types", e);
		}
		finally
		{
			try { rs.close(); }	 catch (Throwable e) {}
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
		return this.sequenceReader;
	}

	public boolean isTableType(String type)
	{
		if (type == null) return false;
		return tableTypesList.contains(type);
	}

	/**
	 * Checks if the current DBMS supports synonyms.
	 * @return true if the synonym support is available (basically if synonymReader != null)
	 */
	public boolean supportsSynonyms()
	{
		return this.synonymReader != null;
	}

	/**
	 *	Return the underlying table of a synonym.
	 * @param synonym the synonym definition
	 *
	 * @return the table to which the synonym points or null if the passed
	 *         name does not reference a synonym or if the DBMS does not support synonyms
	 * @see #getSynonymTable(String, String)
	 */
	public TableIdentifier getSynonymTable(TableIdentifier synonym)
	{
		if (this.synonymReader == null) return null;
		TableIdentifier tbl = synonym.createCopy();
		tbl.adjustCase(this.dbConnection);
		return getSynonymTable(tbl.getSchema(), tbl.getTableName());
	}

	/**
	 * Return the underlying table of a synonym.
	 *
	 * @param schema the schema of the synonym
	 * @param synonym the name of the synonym
	 *
	 * @return the table to which the synonym points or null if the passed
	 *         name does not reference a synonym or if the DBMS does not support synonyms
	 * @see #getSynonymTable(String, String)
	 */
	public TableIdentifier getSynonymTable(String schema, String synonym)
	{
		if (this.synonymReader == null) return null;
		TableIdentifier id = null;
		try
		{
			id = this.synonymReader.getSynonymTable(this.dbConnection, schema, synonym);
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
		return this.synonymReader;
	}

	protected String getMViewSource(TableIdentifier table, List<ColumnIdentifier> columns, DataStore aIndexDef, boolean includeDrop)
	{
		StringBuilder result = new StringBuilder(250);

		try
		{
			TableDefinition def = new TableDefinition(table, columns);
			result.append(getViewReader().getExtendedViewSource(def, includeDrop, false));
		}
		catch (SQLException e)
		{
			result.append(ExceptionUtil.getDisplay(e));
		}
		result.append("\n\n");

		StringBuilder indexSource = getIndexReader().getIndexSource(table, aIndexDef, table.getTableName());

		if (indexSource != null) result.append(indexSource);
		if (this.dbSettings.ddlNeedsCommit())
		{
			result.append('\n');
			result.append("COMMIT;");
			result.append('\n');
		}
		return result.toString();
	}

	protected boolean isSystemConstraintName(String name)
	{
		if (name == null) return false;
		String regex = Settings.getInstance().getProperty("workbench.db." + this.getDbId() + ".constraints.systemname", null);
		if (StringUtil.isEmptyString(regex)) return false;

		try
		{
			Pattern p = Pattern.compile(regex);
			Matcher m = p.matcher(name);
			return m.matches();
		}
		catch (Exception e)
		{
			LogMgr.logError("DbMetadata.isSystemConstraintName()", "Error in regex", e);
		}
		return false;
	}

	public synchronized ConstraintReader getConstraintReader()
	{
		synchronized (ReaderFactory.READER_LOCK)
		{
			if (constraintReader == null)
			{
				constraintReader = ReaderFactory.getConstraintReader(this);
			}
			return constraintReader;
		}
	}

	/**
	 * Return constraints defined for each column in the given table.
	 * @param table The table to check
	 * @return A Map with columns and their constraints. The keys to the Map are column names
	 * The value is the SQL source for the column. The actual retrieval is delegated to a {@link ConstraintReader}
	 * @see ConstraintReader#getColumnConstraints(java.sql.Connection, TableIdentifier)
	 */
	public Map<String, String> getColumnConstraints(TableIdentifier table)
	{
		Map<String, String> columnConstraints = Collections.emptyMap();
		ConstraintReader reader = this.getConstraintReader();
		if (reader == null) return columnConstraints;

		Savepoint sp = null;
		try
		{
			if (dbSettings.useSavePointForDML())
			{
				sp = this.dbConnection.setSavepoint();
			}
			columnConstraints = reader.getColumnConstraints(this.dbConnection.getSqlConnection(), table);
			dbConnection.releaseSavepoint(sp);
		}
		catch (Exception e)
		{
			LogMgr.logError("DbMetadata.getTableConstraints()", "Error retrieving table constraints", e);
			dbConnection.rollback(sp);
			columnConstraints = Collections.emptyMap();
		}

		return columnConstraints;
	}

	/**
	 * Return the SQL source for check constraints defined for the table. This is
	 * delegated to a {@link ConstraintReader}
	 * @return A String with the table constraints. If no constrains exist, a null String is returned
	 * @param tbl The table to check
	 * @param indent A String defining the indention for the source code
	 */
	public String getTableConstraintSource(TableIdentifier tbl, String indent)
	{
		List<TableConstraint> cons = getTableConstraints(tbl);
		return getTableConstraintSource(cons, indent);
	}

	public String getTableConstraintSource(List<TableConstraint> cons, String indent)
	{
		ConstraintReader reader = this.getConstraintReader();
		if (reader == null) return null;

		return reader.getConstraintSource(cons, indent);
	}


	/**
	 * Return the SQL source for check constraints defined for the table. This is
	 * delegated to a {@link ConstraintReader}
	 * @return A String with the table constraints. If no constrains exist, a null String is returned
	 * @param tbl The table to check
	 */
	public List<TableConstraint> getTableConstraints(TableIdentifier tbl)
	{
		ConstraintReader reader = this.getConstraintReader();
		if (reader == null) return null;

		List<TableConstraint> result = reader.getTableConstraints(dbConnection, tbl);
		return result;
	}

	/**
	 * Returns the errors available for the given object and type. This call
	 * is delegated to the available {@link ErrorInformationReader}
	 * @return extended error information if the current DBMS is Oracle. An empty string otherwise.
	 * @see ErrorInformationReader
	 */
	public String getExtendedErrorInfo(String schema, String objectName, String objectType)
	{
		if (this.errorInfoReader == null) return StringUtil.EMPTY_STRING;
		return this.errorInfoReader.getErrorInfo(schema, objectName, objectType);
	}

}
