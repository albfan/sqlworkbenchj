/*
 * DbMetadata.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import workbench.WbManager;
import workbench.db.firebird.FirebirdProcedureReader;
import workbench.db.firstsql.FirstSqlMetadata;
import workbench.db.hsqldb.HsqlSequenceReader;
import workbench.db.ingres.IngresMetadata;
import workbench.db.mckoi.McKoiMetadata;
import workbench.db.mssql.SqlServerConstraintReader;
import workbench.db.mssql.SqlServerMetadata;
import workbench.db.mysql.EnumReader;
import workbench.db.mysql.MySqlProcedureReader;
import workbench.db.oracle.DbmsOutput;
import workbench.db.oracle.OracleConstraintReader;
import workbench.db.oracle.OracleIndexReader;
import workbench.db.oracle.OracleMetadata;
import workbench.db.oracle.OracleSynonymReader;
import workbench.db.postgres.PostgresDDLFilter;
import workbench.db.postgres.PostgresIndexReader;
import workbench.db.postgres.PostgresSequenceReader;
import workbench.db.postgres.PostgresConstraintReader;
import workbench.db.postgres.PostgresProcedureReader;
import workbench.util.ExceptionUtil;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;
import workbench.util.StrBuffer;
import workbench.util.StringUtil;
import workbench.util.WbPersistence;
import workbench.db.hsqldb.HsqlConstraintReader;
import workbench.db.firebird.FirebirdConstraintReader;

/**
 * Retrieve meta data information from the database.
 * This class returns more information then the generic JDBC DatabaseMetadata.
 *  @author  support@sql-workbench.net
 */
public class DbMetadata
	implements PropertyChangeListener
{

	// <editor-fold defaultstate="collapsed" desc="Placeholders for statement templates">
	public static final String TABLE_NAME_PLACEHOLDER = "%tablename%";
	public static final String INDEX_NAME_PLACEHOLDER = "%indexname%";
	public static final String PK_NAME_PLACEHOLDER = "%pk_name%";
	public static final String UNIQUE_PLACEHOLDER = "%unique_key% ";
	public static final String COLUMNLIST_PLACEHOLDER = "%columnlist%";
	public static final String FK_NAME_PLACEHOLDER = "%constraintname%";
	public static final String FK_TARGET_TABLE_PLACEHOLDER = "%targettable%";
	public static final String FK_TARGET_COLUMNS_PLACEHOLDER = "%targetcolumnlist%";
	public static final String COMMENT_TABLE_PLACEHOLDER = "%table%";
	public static final String COMMENT_COLUMN_PLACEHOLDER = "%column%";
	public static final String COMMENT_PLACEHOLDER = "%comment%";
	public static final String FK_UPDATE_RULE = "%fk_update_rule%";
	public static final String FK_DELETE_RULE = "%fk_delete_rule%";
	public static final String GENERAL_SQL = "All";
	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="HashMaps containing statement templates">
	private static HashMap procSourceSql;
	private static HashMap viewSourceSql;
	private static HashMap triggerSourceSql;
	private static HashMap triggerList;
	private static HashMap pkStatements;
	private static HashMap idxStatements;
	private static HashMap fkStatements;
	private static HashMap columnCommentStatements;
	private static HashMap tableCommentStatements;
	private static boolean templatesRead;
	// </editor-fold>

	public static final String MVIEW_NAME = "MATERIALIZED VIEW";
	private String schemaTerm;
	private String catalogTerm;
	private String productName;
	private String dbId;

	private DatabaseMetaData metaData;
	private WbConnection dbConnection;

	private OracleMetadata oracleMetaData;

	private ConstraintReader constraintReader;
	private SynonymReader synonymReader;
	private SequenceReader sequenceReader;
	private ProcedureReader procedureReader;
	private ErrorInformationReader errorInfoReader;
	private SchemaInformationReader schemaInfoReader;
	private IndexReader indexReader;
	private DDLFilter ddlFilter;

	private DbmsOutput oraOutput;

  private boolean caseSensitive;
	private boolean useJdbcCommit;
	private boolean ddlNeedsCommit;
  private boolean isOracle;
	private boolean isPostgres;
	private boolean isFirstSql;
	private boolean isHsql;
	private boolean isFirebird;
	private boolean isSqlServer;
	private boolean isMySql;
	private boolean isCloudscape;
	private boolean isApacheDerby;

	private boolean trimDefaults = true;
	private boolean createInlineConstraints;
	private boolean useNullKeyword = true;
	private boolean fixOracleDateBug = false;
	private boolean columnsListInViewDefinitionAllowed = true;
	
	// This is set to true if identifiers starting with
	// a digit should always be quoted. This will 
	// be initialized through the Settings object
	private boolean quoteIdentifierWithDigits = false;
	
	private String quoteCharacter;
	private String dbVersion;
	private SqlKeywordHandler keywordHandler;
	
	private static final String SELECT_INTO_PG = "(?i)(?s)SELECT.*INTO\\p{Print}*\\s*FROM.*";
	private static final String SELECT_INTO_INFORMIX = "(?i)(?s)SELECT.*FROM.*INTO\\s*\\p{Print}*";
	private Pattern selectIntoPattern = null;

	private static final int UPPERCASE_NAMES = 1;
	private static final int LOWERCASE_NAMES = 2;
	private int objectCaseStorage = -1;
	private int schemaCaseStorage = -1;

	private String tableTypeName;
	private boolean neverQuoteObjects;

	public static final String TABLE_TYPE_VIEW = "VIEW";

	private String[] TABLE_TYPES_VIEW = new String[] {TABLE_TYPE_VIEW};
	private String[] TABLE_TYPES_TABLE; 
	private String[] TABLE_TYPES_SELECTABLE;
	private List objectsWithData = null;
	private List schemasToIgnore;
	private List catalogsToIgnore;

	public DbMetadata(WbConnection aConnection)
		throws SQLException
	{
		Settings settings = Settings.getInstance();
		Connection c = aConnection.getSqlConnection();
		this.dbConnection = aConnection;
		this.metaData = c.getMetaData();

		if (!templatesRead)
		{
			readTemplates();
		}

		try
		{
			this.schemaTerm = this.metaData.getSchemaTerm();
		}
		catch (SQLException e)
		{
			LogMgr.logWarning("DbMetadata.<init>", "Could not retrieve Schema term", e);
			this.schemaTerm = "Schema";
		}

		try
		{
			this.catalogTerm = this.metaData.getCatalogTerm();
		}
		catch (SQLException e)
		{
			LogMgr.logWarning("DbMetadata.<init>", "Could not retrieve Catalog term", e);
			this.catalogTerm = "Catalog";
		}

		// Some JDBC drivers do not return a value for getCatalogTerm() or getSchemaTerm()
		// and don't throw an Exception. This is to ensure that our getCatalogTerm() will
		// always return something usable.
		if (StringUtil.isEmptyString(this.schemaTerm)) this.schemaTerm = "Schema";
		if (StringUtil.isEmptyString(this.catalogTerm))	this.catalogTerm = "Catalog";

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

		if (productLower.indexOf("oracle") > -1)
		{
			this.isOracle = true;
			this.oracleMetaData = new OracleMetadata(this, this.dbConnection);
			this.constraintReader = new OracleConstraintReader();
			this.synonymReader = new OracleSynonymReader();

			// register with the Settings Object to be notified for
			// changes to the "enable dbms output" property
			settings.addPropertyChangeListener(this);
			
			this.sequenceReader = this.oracleMetaData;
			this.procedureReader = this.oracleMetaData;
			this.errorInfoReader = this.oracleMetaData;
			this.fixOracleDateBug = Settings.getInstance().getBoolProperty("workbench.db.oracle.date.usetimestamp", true);
			this.indexReader = new OracleIndexReader(this);
		}
		else if (productLower.indexOf("postgres") > - 1)
		{
			this.isPostgres = true;
			this.selectIntoPattern = Pattern.compile(SELECT_INTO_PG);
			this.constraintReader = new PostgresConstraintReader();
			this.sequenceReader = new PostgresSequenceReader(this.dbConnection.getSqlConnection());
			this.procedureReader = new PostgresProcedureReader(this);
			this.indexReader = new PostgresIndexReader(this);
			this.ddlFilter = new PostgresDDLFilter();
		}
		else if (productLower.indexOf("hsql") > -1)
		{
			this.isHsql = true;
			this.constraintReader = new HsqlConstraintReader(this.dbConnection.getSqlConnection());
			this.sequenceReader = new HsqlSequenceReader(this.dbConnection.getSqlConnection());
			try
			{
				int major = metaData.getDatabaseMajorVersion();
				int minor = metaData.getDriverMinorVersion();
				if (major == 1 && minor <= 7)
				{
					// HSQLDB 1.7.x does not support a column list in the view definition
					this.columnsListInViewDefinitionAllowed = false;
				}
			}
			catch (Exception e)
			{
				this.columnsListInViewDefinitionAllowed = false;
			}
		}
		else if (productLower.indexOf("firebird") > -1)
		{
			this.isFirebird = true;
			this.constraintReader = new FirebirdConstraintReader();
			this.procedureReader = new FirebirdProcedureReader(this);
			// Jaybird 2.0 reports the Firebird version in the 
			// productname. To ease the DBMS handling we'll use the same
			// product name that is reported with the 1.5 driver. 
			this.productName = "Firebird";
		}
		else if (productLower.indexOf("sql server") > -1)
		{
			this.isSqlServer = true;
			this.constraintReader = new SqlServerConstraintReader();
			boolean useJdbc = Settings.getInstance().getBoolProperty("workbench.db.mssql.usejdbcprocreader", true);
			if (!useJdbc)
			{
				this.procedureReader = new SqlServerMetadata(this);
			}
		}
		else if (productLower.indexOf("adaptive server") > -1)
		{
			this.constraintReader = new ASAConstraintReader();
		}
		else if (productLower.indexOf("mysql") > -1)
		{
			this.procedureReader = new MySqlProcedureReader(this, this.dbConnection);
			this.isMySql = true;
		}
		else if (productLower.indexOf("informix") > -1)
		{
			this.selectIntoPattern = Pattern.compile(SELECT_INTO_INFORMIX);
		}
		else if (productLower.indexOf("cloudscape") > -1)
		{
			this.isCloudscape = true;
			this.constraintReader = new CloudscapeConstraintReader();
		}
		else if (productLower.indexOf("derby") > -1)
		{
			this.isApacheDerby = true;
			this.constraintReader = new CloudscapeConstraintReader();
		}
		else if (productLower.indexOf("ingres") > -1)
		{
			IngresMetadata imeta = new IngresMetadata(this.dbConnection.getSqlConnection());
			this.synonymReader = imeta;
			this.sequenceReader = imeta;
		}
		else if (productLower.indexOf("mckoi") > -1)
		{
			// McKoi reports the version in the database product name
			// which makes setting up the meta data stuff lookups
			// too complicated, so we'll strip the version info
			int pos = this.productName.indexOf('(');
			if (pos == -1) pos = this.productName.length() - 1;
			this.productName = this.productName.substring(0, pos).trim();
			this.sequenceReader = new McKoiMetadata(this.dbConnection.getSqlConnection());
		}
		else if (productLower.indexOf("firstsql") > -1)
		{
			this.constraintReader = new FirstSqlMetadata();
			this.isFirstSql = true;
		}

		// if the DBMS does not need a specific ProcedureReader
		// we use the default implementation
		if (this.procedureReader == null)
		{
			this.procedureReader = new JdbcProcedureReader(this);
		}

		if (this.indexReader == null)
		{
			this.indexReader = new JdbcIndexReader(this);
		}
		
		if (this.schemaInfoReader == null)
		{
			this.schemaInfoReader = new GenericSchemaInfoReader(this.getDbId());
		}
		
		try
		{
			this.quoteCharacter = this.metaData.getIdentifierQuoteString();
		}
		catch (Exception e)
		{
			this.quoteCharacter = null;
		}
		if (this.quoteCharacter == null || this.quoteCharacter.length() == 0) this.quoteCharacter = "\"";

		try
		{
			this.dbVersion = this.metaData.getDatabaseProductVersion();
		}
		catch (Exception e)
		{
			LogMgr.logWarning("DbMetadata.<init>", "errro calling getDatabaseProductVersion()", e);
		}

		this.caseSensitive = settings.getCaseSensitivServers().contains(this.productName);
		this.useJdbcCommit = settings.getServersWhichNeedJdbcCommit().contains(this.productName);
		this.ddlNeedsCommit = settings.getServersWhereDDLNeedsCommit().contains(this.productName);
		this.createInlineConstraints = settings.getServersWithInlineConstraints().contains(this.productName);

		this.useNullKeyword = !settings.getServersWithNoNullKeywords().contains(this.getDbId());

		String regex = settings.getProperty("workbench.sql.selectnewtablepattern." + this.getDbId(), null);
		if (regex != null)
		{
			try
			{
				this.selectIntoPattern = Pattern.compile(regex);
			}
			catch (Exception e)
			{
				this.selectIntoPattern = null;
				LogMgr.logError("DbMetadata.<init>", "Invalid pattern to identify a SELECT INTO a new table: " + regex, e);
			}
		}

		String nameCase = settings.getProperty("workbench.db.objectname.case." + this.getDbId(), null);
		if (nameCase != null)
		{
			if ("lower".equals(nameCase))
			{
				this.objectCaseStorage = LOWERCASE_NAMES;
			}
			else if ("upper".equals(nameCase))
			{
				this.objectCaseStorage = UPPERCASE_NAMES;
			}
		}
		nameCase = settings.getProperty("workbench.db.schemaname.case." + this.getDbId(), null);
		if (nameCase != null)
		{
			if ("lower".equals(nameCase))
			{
				this.schemaCaseStorage = LOWERCASE_NAMES;
			}
			else if ("upper".equals(nameCase))
			{
				this.schemaCaseStorage = UPPERCASE_NAMES;
			}
		}

		tableTypeName = settings.getProperty("workbench.db.basetype.table." + this.getDbId(), "TABLE");
		TABLE_TYPES_TABLE = new String[] {tableTypeName};
		if (this.isOracle)
		{
			TABLE_TYPES_SELECTABLE = new String[] {tableTypeName, TABLE_TYPE_VIEW, "SYNONYM"};
		}
		else
		{
			TABLE_TYPES_SELECTABLE = new String[] {tableTypeName, TABLE_TYPE_VIEW};
		}

		String quote = settings.getProperty("workbench.db.neverquote","");
		this.neverQuoteObjects = quote.indexOf(this.getDbId()) > -1;
		this.trimDefaults = settings.getBoolProperty("workbench.db." + getDbId() + ".trimdefaults", true);
		this.quoteIdentifierWithDigits = settings.getBoolProperty("workbench.db." + getDbId() + ".quotedigits", false);
	}

	public String getTableTypeName() { return tableTypeName; }
	public String getViewTypeName() { return TABLE_TYPE_VIEW; }
	public String[] getTypeListView() { return TABLE_TYPES_VIEW; }
	public String[] getTypeListTable() { return TABLE_TYPES_TABLE; }
	public String[] getTypeListSelectable() { return TABLE_TYPES_SELECTABLE; }

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
	
	public DatabaseMetaData getJdbcMetadata()
	{
		return this.metaData;
	}

	public WbConnection getWbConnection() { return this.dbConnection; }
	
	public Connection getSqlConnection()
	{
		return this.dbConnection.getSqlConnection();
	}

	/**
	 * Check if the given DB object type can contain data. i.e. if
	 * a SELECT FROM can be run against this type
	 */
	public boolean objectTypeCanContainData(String type)
	{
		if (type == null) return false;
		if (this.objectsWithData == null)
		{
			String keyPrefix = "workbench.db.objecttype.selectable.";
			String s = Settings.getInstance().getProperty(keyPrefix + getDbId(), null);
			if (s == null) 
			{
				s = Settings.getInstance().getProperty(keyPrefix + "default", null);
				if (s != null) 
				{
					// As we now support materialized views, we silently add
					// them to the default list. 
					if (s.indexOf(MVIEW_NAME.toLowerCase()) == -1)
					{
						s += "," + MVIEW_NAME.toLowerCase();
						Settings.getInstance().setProperty(keyPrefix + "default", s);
					}
				}
			}

			if (s == null)
			{
				// if not property is set in the configuration
				// setup a sensible default 
				objectsWithData = new ArrayList();
				objectsWithData.add("table");
				objectsWithData.add("system table");
				objectsWithData.add("view");
				objectsWithData.add("system view");
				objectsWithData.add("synonym");
				if (this.isPostgres) objectsWithData.add("sequence");
				if (this.isOracle) objectsWithData.add(MVIEW_NAME.toLowerCase());
			}
			else
			{
				objectsWithData = StringUtil.stringToList(s.toLowerCase(), ",", true, true);
			}
		}

		String ltype = type.toLowerCase();
		int count = objectsWithData.size();
		for (int i=0; i < count; i++)
		{
			String t = (String)objectsWithData.get(i);
			if (ltype.equals(t)) return true;
		}
		return false;
	}

	/**
	 *	Return the name of the DBMS as reported by the JDBC driver
	 */
	public String getProductName()
	{
		return this.productName;
	}

	/**
	 * 	Return a clean version of the productname.
	 *  @see #getProductName()
	 */
	public String getDbId()
	{
		if (this.dbId == null)
		{
			this.dbId = this.productName.replaceAll("[ \\(\\)\\[\\]\\/$,.]", "_").toLowerCase();
			LogMgr.logInfo("DbMetadata", "Using DBID=" + this.dbId);
		}
		return this.dbId;
	}

	public String getDbVersion() { return this.dbVersion; }
	public boolean getDDLNeedsCommit() { return ddlNeedsCommit; }
	public boolean getUseJdbcCommit() { return this.useJdbcCommit; }
  public boolean isStringComparisonCaseSensitive() { return this.caseSensitive; }

	public boolean reportsRealSizeAsDisplaySize()
	{
		return this.isHsql;
	}

	private static void readTemplates()
	{
		synchronized (GENERAL_SQL)
		{
			if (templatesRead) return;
			procSourceSql = readStatementTemplates("ProcSourceStatements.xml");
			viewSourceSql = readStatementTemplates("ViewSourceStatements.xml");
			fkStatements = readStatementTemplates("CreateFkStatements.xml");
			pkStatements = readStatementTemplates("CreatePkStatements.xml");
			idxStatements = readStatementTemplates("CreateIndexStatements.xml");
			triggerList = readStatementTemplates("ListTriggersStatements.xml");
			triggerSourceSql = readStatementTemplates("TriggerSourceStatements.xml");
			columnCommentStatements = readStatementTemplates("ColumnCommentStatements.xml");
			tableCommentStatements = readStatementTemplates("TableCommentStatements.xml");
			templatesRead = true;
		}
	}

	public boolean supportSingleLineCommands()
	{
		String ids = Settings.getInstance().getProperty("workbench.db.checksinglelinecmd", "");
		if ("*".equals(ids)) return true;
		List dbs = StringUtil.stringToList(ids, ",", true, true);
		return dbs.contains(this.getDbId());
	}
	
	public boolean supportShortInclude()
	{
		String ids = Settings.getInstance().getProperty("workbench.db.supportshortinclude", "");
		if ("*".equals(ids)) return true;
		List dbs = StringUtil.stringToList(ids, ",", true, true);
		return dbs.contains(this.getDbId());
	}
	
	/**
	 *	Returns true if the current DBMS supports a SELECT syntax
	 *	which creates a new table (e.g. SELECT .. INTO new_table FROM old_table)
	 */
	public boolean supportsSelectIntoNewTable()
	{
		return this.selectIntoPattern != null;
	}

	/**
	 *	Checks if the given SQL string is actually some kind of table
	 *	creation "disguised" as a SELECT. This will always return false
	 *	if supportsSelectIntoNewTable() returns false.
	 *	Otherwise it will check for the DB specific syntax.
	 */
	public boolean isSelectIntoNewTable(String sql)
	{
		if (sql == null || sql.length() == 0) return false;
		if (this.selectIntoPattern == null) return false;
		Matcher m = this.selectIntoPattern.matcher(sql);
		return m.find();
	}

	public boolean isMySql() { return this.isMySql; }
	public boolean isPostgres() { return this.isPostgres; }
  public boolean isOracle() { return this.isOracle; }
	public boolean isHsql() { return this.isHsql; }
	public boolean isFirebird() { return this.isFirebird; }
	public boolean isSqlServer() { return this.isSqlServer; }
	public boolean isCloudscape() { return this.isCloudscape; }
	public boolean isApacheDerby() { return this.isApacheDerby; }

	public String filterDDL(String sql)
	{
		if (this.ddlFilter == null) return sql;
		return this.ddlFilter.adjustDDL(sql);
	}
	
	public boolean ignoreSchema(String schema)
	{
		if (schema == null) return true;
		if (schemasToIgnore == null)
		{
			String ids = Settings.getInstance().getProperty("workbench.sql.ignoreschema." + this.getDbId(), null);
			if (ids != null)
			{
				schemasToIgnore = StringUtil.stringToList(ids, ",");
			}
			else
			{
				 schemasToIgnore = Collections.EMPTY_LIST;
			}
		}
		return schemasToIgnore.contains("*") || schemasToIgnore.contains(schema);
	}

	/**
	 * Check if the given {@link TableIdentifier} requires
	 * the usage of the schema for a DML (select, insert, update, delete)
	 * statement. By default this is not required for an Oracle
	 * connetion where the schema is the current user.
	 * For all other DBMS, the usage can be disabled by setting
	 * a property in the configuration file
	 */
	public boolean needSchemaInDML(TableIdentifier table)
	{
		try
		{
			String tblSchema = table.getSchema();
			if (ignoreSchema(tblSchema)) return false;

			if (this.isOracle)
			{
				// In oracle we don't need the schema if the it is the current user
				if (tblSchema == null) return false;
				return !this.getUserName().equalsIgnoreCase(tblSchema);
			}
		}
		catch (Throwable th)
		{
			return false;
		}
		return true;
	}

	public boolean needCatalogInDML(TableIdentifier table)
	{
		if (!this.supportsCatalogs()) return false;
		String cat = table.getCatalog();
		if (StringUtil.isEmptyString(cat)) return false;
		String currentCat = getCurrentCatalog();
		if (StringUtil.isEmptyString(currentCat)) return false;
		return !cat.equalsIgnoreCase(currentCat);
	}
	
	public boolean ignoreCatalog(String catalog)
	{
		if (catalog == null) return true;
		if (catalogsToIgnore == null)
		{
			String cats = Settings.getInstance().getProperty("workbench.sql.ignorecatalog." + this.getDbId(), null);
			if (cats != null)
			{
				catalogsToIgnore = StringUtil.stringToList(cats, ",");
			}
			else
			{
				 catalogsToIgnore = Collections.EMPTY_LIST;
			}
		}
		return catalogsToIgnore.contains("*") || catalogsToIgnore.contains(catalog);
	}

	private static HashMap readStatementTemplates(String aFilename)
	{
		HashMap result = null;

		BufferedInputStream in = new BufferedInputStream(DbMetadata.class.getResourceAsStream(aFilename));
		Object value;
		try
		{
			WbPersistence reader = new WbPersistence();
			value = reader.readObject(in);
		}
		catch (Exception e)
		{
			LogMgr.logError("DbMetadata.readStatementTemplate()", "Error reading templates file " + aFilename,e);
			value = null;
		}

		if (value != null && value instanceof HashMap)
		{
			result = (HashMap)value;
		}

		// When running tests, the WbManager is not necessarily available
		WbManager mgr = WbManager.getInstance();
		File baseDir = null;;
		if (mgr == null)
		{
			baseDir = new File(".");
		}
		else
		{
			baseDir = new File(mgr.getJarPath());
		}
			
		// Try to read the file in the directory where the jar file is locateds
		File f = new File(baseDir, aFilename);
		if (f.exists())
		{
			LogMgr.logDebug("DbMetadata.readStatementTemplates()", "Reading user define template file " + aFilename);
			// try to read additional definitions from local file
			try
			{
				WbPersistence reader = new WbPersistence(aFilename);
				value = reader.readObject();
			}
			catch (Exception e)
			{
				LogMgr.logDebug("DbMetadata.readStatementTemplate()", "Error reading template file " + aFilename, e);
			}
			if (value != null && value instanceof HashMap)
			{
				HashMap m = (HashMap)value;
				if (result != null)
				{
					result.putAll(m);
				}
				else
				{
					result = m;
				}
			}
		}
		return result;
	}

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
		String verb = Settings.getInstance().getProperty("workbench.db.drop." + aType.toLowerCase() + ".cascade." + this.getDbId(), null);
		return verb;
	}

	public Set getDbFunctions()
	{
		Set dbFunctions = new HashSet();
		try
		{
			String funcs = this.metaData.getSystemFunctions();
			this.addStringList(dbFunctions, funcs);

			funcs = this.metaData.getStringFunctions();
			this.addStringList(dbFunctions, funcs);

			funcs = this.metaData.getNumericFunctions();
			this.addStringList(dbFunctions, funcs);

			funcs = this.metaData.getTimeDateFunctions();
			this.addStringList(dbFunctions, funcs);
		}
		catch (Exception e)
		{
			LogMgr.logError("DbMetadata.getDbFunctions()", "Error retrieving function list from DB", e);
		}
		return dbFunctions;
	}

	private void addStringList(Set target, String list)
	{
		if (list == null) return;
		StringTokenizer tok = new StringTokenizer(list, ",");
		while (tok.hasMoreTokens())
		{
			String keyword = tok.nextToken();
			target.add(keyword.toUpperCase().trim());
		}
	}

	/**
	 * Drop given table. If this is successful and the
	 * DBMS requires a COMMIT for DDL statements then
	 * the DROP will be commited (or rolled back in case
	 * of an error
	 */
	public void dropTable(TableIdentifier aTable)
		throws SQLException
	{
		Statement stmt = null;
		try
		{
			StringBuffer sql = new StringBuffer();
			sql.append("DROP TABLE ");
			sql.append(aTable.getTableExpression());
			String cascade = this.getCascadeConstraintsVerb("TABLE");
			if (cascade != null)
			{
				sql.append(' ');
				sql.append(cascade);
			}
			stmt = this.dbConnection.createStatement();
			stmt.executeUpdate(sql.toString());
			if (this.ddlNeedsCommit && !this.dbConnection.getAutoCommit())
			{
				this.dbConnection.commit();
			}
		}
		catch (SQLException e)
		{
			if (this.ddlNeedsCommit && !this.dbConnection.getAutoCommit())
			{
				try { this.dbConnection.rollback(); } catch (Throwable th) {}
			}
			throw e;
		}
		finally
		{
			try { stmt.close(); } catch (Throwable th) {}
		}
	}

	public String getObjectType(TableIdentifier table)
	{
		String type = null;
		ResultSet rs = null;
		try
		{
			TableIdentifier tbl = table.createCopy();
			tbl.adjustCase(this.dbConnection);
			rs = this.metaData.getTables(tbl.getCatalog(), tbl.getSchema(), tbl.getTableName(), null);
			if (rs.next())
			{
				type = rs.getString("TABLE_TYPE");
			}
		}
		catch (Exception e)
		{
			type = null;
		}
		finally
		{
			SqlUtil.closeResult(rs);
		}
		return type;
	}
	public String getUserName()
	{
		try
		{
			return this.metaData.getUserName();
		}
		catch (Exception e)
		{
			return StringUtil.EMPTY_STRING;
		}
	}

	public String getExtendedViewSource(TableIdentifier tbl, boolean includeDrop)
		throws SQLException
	{
		return this.getExtendedViewSource(tbl, null, includeDrop);
	}

	/**
	 * Returns a complete SQL statement to (re)create the given view.
	 */
	public String getExtendedViewSource(TableIdentifier view, DataStore viewTableDefinition, boolean includeDrop)
		throws SQLException
	{
		GetMetaDataSql sql = (GetMetaDataSql)viewSourceSql.get(this.productName);
		if (sql == null)
		{
			SourceStatementsHelp help = new SourceStatementsHelp();
			return help.explainMissingViewSourceSql(this.getProductName());
		}

		if (viewTableDefinition == null)
		{
			viewTableDefinition = this.getTableDefinition(view);
		}
		String source = this.getViewSource(view);
		
		if (source == null || source.length() == 0) return StringUtil.EMPTY_STRING;

		// ThinkSQL returns the full CREATE VIEW statement
		if (source.toLowerCase().startsWith("create")) return source;

		StrBuffer result = new StrBuffer(source.length() + 100);

		result.append(generateCreateObject(includeDrop, view.getType(), view.getTableName()));

		if (columnsListInViewDefinitionAllowed && !MVIEW_NAME.equalsIgnoreCase(view.getType()))
		{
			result.append("\n(\n");
			int rows = viewTableDefinition.getRowCount();
			for (int i=0; i < rows; i++)
			{
				String colName = viewTableDefinition.getValueAsString(i, DbMetadata.COLUMN_IDX_TABLE_DEFINITION_COL_NAME);
				result.append("  ");
				result.append(quoteObjectname(colName));
				if (i < rows - 1)
				{
					result.append(",");
					result.append("\n");
				}
			}
			result.append("\n)");
		}
		
		result.append("\nAS \n");
		result.append(source);
		result.append("\n");
		if (this.ddlNeedsCommit)
		{
			result.append("COMMIT;\n");
		}
		return result.toString();
	}

	/**
	 *	Return the source of a view definition as it is stored in the database.
	 *	Usually (depending on how the meta data is stored in the database) the DBMS
	 *	only stores the underlying SELECT statement, and that will be returned by this method.
	 *	To create a complete SQL to re-create a view, use {@linke getExtendedViewSource(String, String, String, DataStore, boolean)}
	 *
	 *	@return the view source as stored in the database.
	 */
	public String getViewSource(TableIdentifier viewId)
	{
		if (viewId == null) return null;

		if (this.isOracle && MVIEW_NAME.equalsIgnoreCase(viewId.getType()))
		{
			return oracleMetaData.getSnapshotSource(viewId);
		}
		
		StrBuffer source = new StrBuffer(500);
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			GetMetaDataSql sql = (GetMetaDataSql)viewSourceSql.get(this.productName);
			if (sql == null) return StringUtil.EMPTY_STRING;
			TableIdentifier tbl = viewId.createCopy();
			tbl.adjustCase(this.dbConnection);
			sql.setSchema(tbl.getSchema());
			sql.setObjectName(tbl.getTableName());
			sql.setCatalog(tbl.getCatalog());
			stmt = this.dbConnection.createStatement();
			String query = this.adjustHsqlQuery(sql.getSql());
			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logInfo("DbMetadata.getViewSource()", "Using query=\n" + query);
			}
			rs = stmt.executeQuery(query);
			while (rs.next())
			{
				String line = rs.getString(1);
				if (line != null)
				{
					source.append(line.replaceAll("\r", StringUtil.EMPTY_STRING));
				}
			}
			source.rtrim();
			if (!source.endsWith(';')) source.append(';');
		}
		catch (Exception e)
		{
			LogMgr.logWarning("DbMetadata.getViewSource()", "Could not retrieve view definition for " + viewId.getTableExpression(), e);
			source = new StrBuffer(ExceptionUtil.getDisplay(e));
			if (this.isPostgres) try { this.dbConnection.rollback(); } catch (Throwable th) {}
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return source.toString();
	}

	private StrBuffer generateCreateObject(boolean includeDrop, String type, String name)
	{
		StrBuffer result = new StrBuffer();
		boolean replaced = false;

		String prefix = "workbench.db.";
		String suffix = "." + type.toLowerCase() + ".sql." + this.getDbId();

		String replace = Settings.getInstance().getProperty(prefix + "replace" + suffix, null);
		if (replace != null)
		{
			replace = replace.replaceAll("%name%", name);
			result.append(replace);
			replaced = true;
		}

		if (includeDrop && !replaced)
		{
			String drop = Settings.getInstance().getProperty(prefix + "drop" + suffix, null);
			if (drop == null)
			{
				result.append("DROP ");
				result.append(type.toUpperCase());
				result.append(" ");
				result.append(name);
				String cascade = this.getCascadeConstraintsVerb(type);
				if (cascade != null)
				{
					result.append(" ");
					result.append(cascade);
				}
				result.append(";\n");
			}
			else
			{
				drop = drop.replaceAll("%name%", name);
				result.append(drop);
			}
			result.append("\n");
		}

		if (!replaced)
		{
			String create = Settings.getInstance().getProperty(prefix + "create" + suffix, null);
			if (create == null)
			{
				result.append("CREATE ");
				result.append(type.toUpperCase());
				result.append(" ");
				result.append(name);
			}
			else
			{
				create = create.replaceAll("%name%", name);
				result.append(create);
			}
		}
		return result;
	}

	public String getProcedureSource(String aCatalog, String aSchema, String aProcname, int type)
	{
		if (aProcname == null) return null;
		if (aProcname.length() == 0) return null;

		GetMetaDataSql sql = (GetMetaDataSql)procSourceSql.get(this.productName);
		if (sql == null)
		{
			SourceStatementsHelp help = new SourceStatementsHelp();
			return help.explainMissingProcSourceSql(this.getProductName());
		}

		// this is for MS SQL Server, which appends a ;1 to
		// the end of the procedure name
		int i = aProcname.indexOf(';');
		if (i > -1 && isSqlServer)
		{
			aProcname = aProcname.substring(0, i);
		}

		StrBuffer source = new StrBuffer();

		if (this.procedureReader != null)
		{
			source.append(this.procedureReader.getProcedureHeader(aCatalog, aSchema, aProcname, type));
		}

		Statement stmt = null;
		ResultSet rs = null;
    int linecount = 0;
		
		try
		{
			aProcname = this.adjustObjectnameCase(aProcname);
			sql.setSchema(aSchema);
			sql.setObjectName(aProcname);
			sql.setCatalog(aCatalog);
			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logInfo("DbMetadata.getProcedureSource()", "Using query=\n" + sql.getSql());
			}

			stmt = this.dbConnection.createStatementForQuery();
			rs = stmt.executeQuery(sql.getSql());
			while (rs.next())
			{
				String line = rs.getString(1);
				if (line != null)
        {
          linecount ++;
          source.append(line);
        }
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("DbMetadata.getProcedureSource()", "Error retrieving procedure source", e);
			source = new StrBuffer(ExceptionUtil.getDisplay(e));
			if (this.isPostgres) try { this.dbConnection.rollback(); } catch (Throwable th) {}
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}

		boolean isPackage = false;

		try
		{
      if (this.isOracle && linecount == 0)
      {
				source = this.oracleMetaData.getPackageSource(aSchema, aCatalog);
				isPackage = (source != null && source.length() > 0);
      }
		}
		catch (Exception e)
		{
			LogMgr.logError("DbMetadata.getProcedureSource()", "Error retrieving package source", e);
			source = new StrBuffer(ExceptionUtil.getDisplay(e));
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}

		if (!isPackage && !source.endsWith(';') && proceduresNeedTerminator())
		{
			source.append(';');
		}
		return source.toString();
	}

	public boolean proceduresNeedTerminator()
	{
		String value = Settings.getInstance().getProperty("workbench.db.noprocterminator", null);
		if (value == null) return true;
		List l = StringUtil.stringToList(value, ",");
		return !l.contains(this.getDbId());
	}
	
	private void initKeywordHandler()
	{
		this.keywordHandler = new SqlKeywordHandler(this.dbConnection.getSqlConnection(), this.getDbId());
	}
	
	public boolean isKeyword(String name)
	{
		if (this.keywordHandler == null) this.initKeywordHandler();
		return this.keywordHandler.isKeyword(name);
	}
	
	public Collection getSqlKeywords()
	{
		if (this.keywordHandler == null) this.initKeywordHandler();
		return this.keywordHandler.getSqlKeywords();
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
	public String quoteObjectname(String aName)
	{
		if (aName == null) return null;
		if (aName.length() == 0) return aName;
		
		// already quoted?
		if (aName.startsWith("\"")) return aName;

		if (this.neverQuoteObjects) return StringUtil.trimQuotes(aName);

		try
		{
			boolean needQuote = false;

			// Oracle and HSQL require identifiers starting with a number 
			// have to be quoted always. 
			if (this.quoteIdentifierWithDigits)
			{
				needQuote = (Character.isDigit(aName.charAt(0)));
			}
			
			if (needQuote || isKeyword(aName))
			{
				StringBuffer result = new StringBuffer(aName.length() + 4);
				result.append(this.quoteCharacter);
				result.append(aName.trim());
				result.append(this.quoteCharacter);
				return result.toString();
			}
		}
		catch (Exception e)
		{
			LogMgr.logWarning("DbMetadata.quoteObjectName()", "Error when retrieving DB information", e);
		}

		// if it is not a keyword, we have to check for special characters such
		// as a space, $ etec
		return SqlUtil.quoteObjectname(aName);
	}

	/**
	 * Adjusts the case of the given schema name to the
	 * case in which the server stores objects
	 * This is needed e.g. when the user types a
	 * table name, and that value is used to retrieve
	 * the table definition. Usually the getColumns()
	 * method is case sensitiv. If no special case
	 * for schemas is configured then this method
	 * is simply delegating to {@link adjustObjectNameCase(String)
	 */
	public String adjustSchemaNameCase(String schema)
	{
		if (schema == null) return null;
		if (this.schemaCaseStorage == -1)
		{
			return this.adjustObjectnameCase(schema);
		}
		schema = StringUtil.trimQuotes(schema);
		try
		{
			if (this.storesLowerCaseSchemas())
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
		return schema.trim();
	}

	/**
	 * Adjusts the case of the given object to the
	 * case in which the server stores objects
	 * This is needed e.g. when the user types a
	 * table name, and that value is used to retrieve
	 * the table definition. Usually the getColumns()
	 * method is case sensitiv.
	 */
	public String adjustObjectnameCase(String name)
	{
		if (name == null) return null;
		// if we have quotes, keep them...
		if (name.indexOf("\"") > -1) return name.trim();

		name = StringUtil.trimQuotes(name);
		try
		{
			if (this.storesUpperCaseIdentifiers())
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
	 * Returns the "active" schema. Currently this is only
	 * implemented for Oracle where the "current" schema
	 * is the user name.
	 * Note that in Oracle this could be changed
	 * using ALTER SESSION SET SCHEMA=...
	 * This is not taken into account in this method
	 */
	public String getCurrentSchema()
	{
		if (this.schemaInfoReader != null)
		{
			return this.schemaInfoReader.getCurrentSchema(this.dbConnection);
		}
		return null;
	}

	/**
	 * Returns the schema that should be used for the current user
	 * This essential call {@link getCurrentSchema()}. The method 
	 * then checks if the schema should be ignored for the current
	 * dbms by calling {@link ignoreSchema(String)}. If the 
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
	 * Try to find out to which schema a table belongs.
	 */
	public String findSchemaForTable(String aTablename)
		throws SQLException
	{
		if (this.ignoreSchema("*")) return null;

		aTablename = this.adjustObjectnameCase(aTablename);

		// First we try the current user as the schema name...
		String schema = this.adjustObjectnameCase(this.getUserName());
		ResultSet rs = this.metaData.getTables(null, schema, aTablename, null);
		String table = null;
		try
		{
			if (rs.next())
			{
				table = rs.getString(2);
			}
			else
			{
				schema = null;
			}
		}
		finally
		{
			SqlUtil.closeResult(rs);
		}

		if (table == null)
		{
			try
			{
				// ok, no table found for the current user, so let's
				// try to find the table without the schema qualifier...
				rs = this.metaData.getTables(null, schema, aTablename, null);
				if (rs.next())
				{
					schema = rs.getString(2);
				}
				// check if there are more rows in the result set
				// if yes, we have no way of identifying the real
				// schema name, so we'll return null
				if (rs.next())
				{
					schema = null;
				}
			}
			finally
			{
				SqlUtil.closeResult(rs);
			}
		}
		if (this.ignoreSchema(schema)) return null;
		return schema;
	}

	/**
	 * The column index of the column in the DataStore returned by getTables()
	 * the stores the table's name
	 */
	public final static int COLUMN_IDX_TABLE_LIST_NAME = 0;

	/**
	 * The column index of the column in the DataStore returned by getTables()
	 * the stores the table's type. The available types can be retrieved
	 * using {@link #getTableTypes()}
	 */
	public final static int COLUMN_IDX_TABLE_LIST_TYPE = 1;

	/**
	 * The column index of the column in the DataStore returned by getTables()
	 * the stores the table's catalog
	 */
	public final static int COLUMN_IDX_TABLE_LIST_CATALOG = 2;

	/**
	 * The column index of the column in the DataStore returned by getTables()
	 * the stores the table's schema
	 */
	public final static int COLUMN_IDX_TABLE_LIST_SCHEMA = 3;

	/**
	 * The column index of the column in the DataStore returned by getTables()
	 * the stores the table's comment
	 */
	public final static int COLUMN_IDX_TABLE_LIST_REMARKS = 4;

	public DataStore getTables()
		throws SQLException
	{
		String user = this.getCurrentSchema();
		return this.getTables(null, user, (String[])null);
	}

	public DataStore getTables(String schema, String[] types)
		throws SQLException
	{
		return this.getTables(null, schema, null, types);
	}

	public DataStore getTables(String aCatalog, String aSchema, String[] types)
		throws SQLException
	{
		return getTables(aCatalog, aSchema, null, types);
	}

	public DataStore getTables(String aCatalog, String aSchema, String tables, String[] types)
		throws SQLException
	{
		if ("*".equals(aSchema) || "%".equals(aSchema)) aSchema = null;
		if ("*".equals(tables) || "%".equals(tables)) tables = null;

		if (aSchema != null) aSchema = StringUtil.replace(aSchema, "*", "%");
		if (tables != null) tables = StringUtil.replace(tables, "*", "%");
		String[] cols = new String[] {"NAME", "TYPE", catalogTerm.toUpperCase(), schemaTerm.toUpperCase(), "REMARKS"};
		int coltypes[] = {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
		int sizes[] = {30,12,10,10,20};

		DataStore result = new DataStore(cols, coltypes, sizes);
		aSchema = adjustObjectnameCase(aSchema);
		aCatalog = adjustObjectnameCase(aCatalog);
		boolean sequencesReturned = false;
		boolean checkOracleInternalSynonyms = (isOracle && typeIncluded("SYNONYM", types));
		boolean checkOracleSnapshots = (isOracle && Settings.getInstance().getBoolProperty("workbench.db.oracle.detectsnapshots", true) && typeIncluded("TABLE", types));
		
		
		String excludeSynsRegex = Settings.getInstance().getProperty("workbench.db.oracle.exclude.synonyms", null);
		Pattern synPattern = null;
		if (checkOracleInternalSynonyms && excludeSynsRegex != null)
		{
			try
			{
				synPattern = Pattern.compile(excludeSynsRegex);
			}
			catch (Exception e)
			{
				LogMgr.logError("DbMetadata.getTables()", "Invalid RegEx for excluding public synonyms specified. RegEx ignored", e);
				synPattern = null;
			}
		}

		if (isPostgres && types == null)
		{
			// The current PG drivers to not adhere to the JDBC javadocs
			// and return nothing when passing null for the types
			// so we retrieve all possible types, and pass them 
			// as this is the meaning of "null" for the types parameter
			Collection typeList = this.getTableTypes();
			types = StringUtil.toArray(typeList);
		}
		
		Set snapshotList = Collections.EMPTY_SET;
		if (checkOracleSnapshots)
		{
			snapshotList = this.oracleMetaData.getSnapshots(aSchema);
		}
		
		boolean hideIndexes = hideIndexes();

		ResultSet tableRs = null;
		try
		{
			tableRs = this.metaData.getTables(aCatalog, aSchema, tables, types);
			while (tableRs.next())
			{
				String cat = tableRs.getString(1);
				String schem = tableRs.getString(2);
				String name = tableRs.getString(3);
				if (name == null) continue;

				// filter out "internal" synonyms for Oracle
				if (checkOracleInternalSynonyms)
				{
					if (name.indexOf("/") > -1) continue;
					if (synPattern != null)
					{
						Matcher m = synPattern.matcher(name);
						if (m.matches()) continue;
					}
				}
				String ttype = tableRs.getString(4);
				if (hideIndexes && isIndexType(ttype)) continue;
				
				if (checkOracleSnapshots)
				{
					StringBuffer t = new StringBuffer(30);
					t.append(schem);
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
				result.setValue(row, COLUMN_IDX_TABLE_LIST_SCHEMA, schem);
				result.setValue(row, COLUMN_IDX_TABLE_LIST_REMARKS, rem);
				if (!sequencesReturned && "SEQUENCE".equals(ttype)) sequencesReturned = true;
			}
		}
		finally
		{
			SqlUtil.closeResult(tableRs);
		}

		if (this.sequenceReader != null && typeIncluded("SEQUENCE", types) &&
				"true".equals(Settings.getInstance().getProperty("workbench.db." + this.getDbId() + ".retrieve_sequences", "true"))
				&& !sequencesReturned)
		{
			//LogMgr.logDebug("DbMetadata.getTables()", "Retrieving sequences...");
			List seq = this.sequenceReader.getSequenceList(aSchema);
			int count = seq.size();
			for (int i=0; i < count; i++)
			{
				int row = result.addRow();

				result.setValue(row, COLUMN_IDX_TABLE_LIST_NAME, (String)seq.get(i));
				result.setValue(row, COLUMN_IDX_TABLE_LIST_TYPE, "SEQUENCE");
				result.setValue(row, COLUMN_IDX_TABLE_LIST_CATALOG, null);
				result.setValue(row, COLUMN_IDX_TABLE_LIST_SCHEMA, aSchema);
				result.setValue(row, COLUMN_IDX_TABLE_LIST_REMARKS, null);
			}
		}

		boolean retrieveSyns = (this.synonymReader != null && Settings.getInstance().getBoolProperty("workbench.db." + this.getDbId() + ".retrieve_synonyms", false));
		if (retrieveSyns && typeIncluded("SYNONYM", types) )
		{
			LogMgr.logDebug("DbMetadata.getTables()", "Retrieving synonyms...");
			List syns = this.synonymReader.getSynonymList(aSchema);
			int count = syns.size();
			for (int i=0; i < count; i++)
			{
				int row = result.addRow();

				result.setValue(row, COLUMN_IDX_TABLE_LIST_NAME, (String)syns.get(i));
				result.setValue(row, COLUMN_IDX_TABLE_LIST_TYPE, "SYNONYM");
				result.setValue(row, COLUMN_IDX_TABLE_LIST_CATALOG, null);
				result.setValue(row, COLUMN_IDX_TABLE_LIST_SCHEMA, aSchema);
				result.setValue(row, COLUMN_IDX_TABLE_LIST_REMARKS, null);
			}
		}
		return result;
	}

	private boolean typeIncluded(String type, String[] types)
	{
		if (types == null) return true;
		if (type == null) return false;
		int l = types.length;
		for (int i=0; i < l; i++)
		{
			if (type.equalsIgnoreCase(types[i])) return true;
		}
		return false;
	}

	/**
	 * Check if the given table exists in the database
	 */
	public boolean tableExists(TableIdentifier aTable)
	{
		return objectExists(aTable, TABLE_TYPES_TABLE);
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
		if (aTable == null) return false;
		boolean exists = false;
		ResultSet rs = null;
		try
		{
      aTable.adjustCase(this.dbConnection);
			String c = StringUtil.trimQuotes(aTable.getCatalog());
			String s = StringUtil.trimQuotes(aTable.getSchema());
			String t = StringUtil.trimQuotes(aTable.getTableName());
			rs = this.metaData.getTables(c, s, t, types);
			exists = rs.next();
		}
		catch (Exception e)
		{
			LogMgr.logError("DbMetadata.tableExists()", "Error checking table existence", e);
		}
		finally
		{
			SqlUtil.closeResult(rs);
		}
		return exists;
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
		if (this.objectCaseStorage != -1)
		{
			return this.objectCaseStorage == LOWERCASE_NAMES;
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

	public boolean storesUpperCaseSchemas()
	{
		return this.schemaCaseStorage == UPPERCASE_NAMES;
	}

	public boolean storesLowerCaseSchemas()
	{
		return this.schemaCaseStorage == LOWERCASE_NAMES;
	}

	/**
	 * Returns true if the server stores identifiers in upper case.
	 * Usually this is delegated to the JDBC driver, but as some drivers
	 * (e.g. Frontbase) implement this incorrectly, this can be overriden
	 * in workbench.settings
	 */
	public boolean storesUpperCaseIdentifiers()
	{
		if (this.objectCaseStorage != -1)
		{
			return this.objectCaseStorage == UPPERCASE_NAMES;
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

	public DataStore getProcedureColumns(String aCatalog, String aSchema, String aProcname)
		throws SQLException
	{
		return this.procedureReader.getProcedureColumns(aCatalog, aSchema, aProcname);
	}

	/**
	 * Construct the SQL verb for the given SQL datatype.
	 * This is used when re-recreating the source for a table
	 */
	public static String getSqlTypeDisplay(String aTypeName, int sqlType, int size, int digits)
	{
		String display = aTypeName;

		switch (sqlType)
		{
			case Types.VARCHAR:
			case Types.CHAR:
				if (size > 0) display = aTypeName + "(" + size + ")";
				break;
			case Types.DECIMAL:
			case Types.DOUBLE:
			case Types.NUMERIC:
			case Types.FLOAT:
				if (aTypeName.equalsIgnoreCase("money")) // SQL Server
				{
					display = aTypeName;
				}
				else if ((aTypeName.indexOf('(') == -1))
				{
					if (digits > 0 && size > 0)
					{
						display = aTypeName + "(" + size + "," + digits + ")";
					}
					else if (size <= 0 && digits > 0)
					{
						display = aTypeName + "(" + digits + ")";
					}
					else if (size > 0 && digits <= 0)
					{
						display = aTypeName + "(" + size + ")";
					}
//					else if (size > 0 && ("NUMBER".equals(aTypeName) || "FLOAT".equals(aTypeName))) // Oracle specific
//					{
//						display = aTypeName + "(" + size + ")";
//					}
				}
				break;

			case Types.OTHER:
				// Oracle specific datatypes
				if ("NVARCHAR2".equalsIgnoreCase(aTypeName))
				{
					display = aTypeName + "(" + size + ")";
				}
				else if ("NCHAR".equalsIgnoreCase(aTypeName))
				{
					display = aTypeName + "(" + size + ")";
				}
				else if ("UROWID".equalsIgnoreCase(aTypeName))
				{
					display = aTypeName + "(" + size + ")";
				}
				else if ("RAW".equalsIgnoreCase(aTypeName))
				{
					display = aTypeName + "(" + size + ")";
				}
				break;
			default:
				display = aTypeName;
				break;
		}
		return display;
	}

	/**
	 * Return a list of stored procedures that are available
	 * in the database. This call is delegated to the
	 * currently defined {@link ProcedureReader}
	 * If no DBMS specific reader is used, this is the {@link JdbcProcedureReader}
	 * @return a DataStore with the list of procedures.
	 */
	public DataStore getProcedures(String aCatalog, String aSchema)
		throws SQLException
	{
		return this.procedureReader.getProcedures(aCatalog, aSchema);
	}

	/**
	 * Enable Oracle's DBMS_OUTPUT package with a default buffer size
	 * @see workbench.db.oracle.DbmsOutput#enable(int)
	 */
	public void enableOutput()
	{
		this.enableOutput(-1);
	}

	/**
	 * Enable Oracle's DBMS_OUTPUT package.
	 * @see workbench.db.oracle.DbmsOutput#enable(int)
	 */
	public void enableOutput(long aLimit)
	{
    if (!this.isOracle)	return;

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
		Settings.getInstance().removePropertyChangeLister(this);
		if (this.oraOutput != null) this.oraOutput.close();
		if (this.oracleMetaData != null) this.oracleMetaData.columnsProcessed();
	}


	/**
	 *	Return a list of ColumnIdentifier's for the given table
	 */
	public List getTableColumns(TableIdentifier aTable)
		throws SQLException
	{
		ColumnIdentifier[] cols = getColumnIdentifiers(aTable);
		List result = new ArrayList(cols.length);
		for (int i=0; i < cols.length; i++)
		{
			result.add(cols[i]);
		}
		return result;
	}

	public int fixColumnType(int type)
	{
		if (!this.fixOracleDateBug) return type;
		if (type == Types.DATE) return Types.TIMESTAMP;
		return type;
	}
	
	public ColumnIdentifier[] getColumnIdentifiers(TableIdentifier table)
		throws SQLException
	{
		String type = table.getType();
		//if (type == null) type = tableTypeName;
		TableIdentifier tbl = table.createCopy();
		tbl.adjustCase(this.dbConnection);
		DataStore ds = this.getTableDefinition(tbl.getCatalog(), tbl.getSchema(), tbl.getTableName(), type, false);
		return createColumnIdentifiers(ds);
	}

	private ColumnIdentifier[] createColumnIdentifiers(DataStore ds)
	{
		int count = ds.getRowCount();
		ColumnIdentifier[] result = new ColumnIdentifier[count];
		for (int i=0; i < count; i++)
		{
			String col = ds.getValueAsString(i, COLUMN_IDX_TABLE_DEFINITION_COL_NAME);
			int type = ds.getValueAsInt(i, COLUMN_IDX_TABLE_DEFINITION_JAVA_SQL_TYPE, Types.OTHER);
			boolean pk = "YES".equals(ds.getValueAsString(i, COLUMN_IDX_TABLE_DEFINITION_PK_FLAG));
			ColumnIdentifier ci = new ColumnIdentifier(col, fixColumnType(type), pk);
			int size = ds.getValueAsInt(i, COLUMN_IDX_TABLE_DEFINITION_SIZE, 0);
			int digits = ds.getValueAsInt(i, COLUMN_IDX_TABLE_DEFINITION_DIGITS, 0);
			String nullable = ds.getValueAsString(i, COLUMN_IDX_TABLE_DEFINITION_NULLABLE);
			int position = ds.getValueAsInt(i, COLUMN_IDX_TABLE_DEFINITION_POSITION, 0);
			String dbmstype = ds.getValueAsString(i, COLUMN_IDX_TABLE_DEFINITION_DATA_TYPE);
			String comment = ds.getValueAsString(i, COLUMN_IDX_TABLE_DEFINITION_REMARKS);
			String def = ds.getValueAsString(i, COLUMN_IDX_TABLE_DEFINITION_DEFAULT);
			ci.setColumnSize(size);
			ci.setDecimalDigits(digits);
			ci.setIsNullable(StringUtil.stringToBool(nullable));
			ci.setDbmsType(dbmstype);
			ci.setComment(comment);
			ci.setDefaultValue(def);
			ci.setPosition(position);
			result[i] = ci;
		}
		return result;
	}

	/** The column index for a {@link workbench.storage.DataStore} returned
	 *  by {@link #getTableDefinition(String, String, String)} that holds
	 *  the column name
	 */
	public final static int COLUMN_IDX_TABLE_DEFINITION_COL_NAME = 0;

	/** The column index for a {@link workbench.storage.DataStore} returned
	 *  by {@link #getTableDefinition(String, String, String)} that holds
	 *  the DBMS specific data type string
	 */
	public final static int COLUMN_IDX_TABLE_DEFINITION_DATA_TYPE = 1;

	/** The column index for a {@link workbench.storage.DataStore} returned
	 *  by {@link #getTableDefinition(String, String, String)} that holds
	 *  the primary key flag
	 */
	public final static int COLUMN_IDX_TABLE_DEFINITION_PK_FLAG = 2;

	/** The column index for a {@link workbench.storage.DataStore} returned
	 *  by {@link #getTableDefinition(String, String, String)} that holds
	 *  the nullable flag
	 */
	public final static int COLUMN_IDX_TABLE_DEFINITION_NULLABLE = 3;

	/** The column index for a {@link workbench.storage.DataStore} returned
	 *  by {@link #getTableDefinition(String, String, String)} that holds
	 *  the default value for this column
	 */
	public final static int COLUMN_IDX_TABLE_DEFINITION_DEFAULT = 4;

	/** The column index for a {@link workbench.storage.DataStore} returned
	 *  by {@link #getTableDefinition(String, String, String)} that holds
	 *  the remark for this column
	 */
	public final static int COLUMN_IDX_TABLE_DEFINITION_REMARKS = 5;

	/** The column index for a {@link workbench.storage.DataStore} returned
	 *  by {@link #getTableDefinition(String, String, String)} that holds
	 *  the integer value of the java datatype from {@link java.sql.Types}
	 */
	public final static int COLUMN_IDX_TABLE_DEFINITION_JAVA_SQL_TYPE = 6;
	public final static int COLUMN_IDX_TABLE_DEFINITION_SCALE = 7;
	public final static int COLUMN_IDX_TABLE_DEFINITION_SIZE = 7;
	public final static int COLUMN_IDX_TABLE_DEFINITION_PRECISION = 8;
	public final static int COLUMN_IDX_TABLE_DEFINITION_DIGITS = 8;

	public final static int COLUMN_IDX_TABLE_DEFINITION_POSITION = 9;

	public DataStore getTableDefinition(String aCatalog, String aSchema, String aTable, boolean adjustNames)
		throws SQLException
	{
		return this.getTableDefinition(aCatalog, aSchema, aTable, tableTypeName, adjustNames);
	}

	public DataStore getTableDefinition(String aCatalog, String aSchema, String aTable)
		throws SQLException
	{
		return this.getTableDefinition(aCatalog, aSchema, aTable, tableTypeName, true);
	}

 /**
  * Retrieve the column definition of the given table
  * @param aCatalog the catalog of the table (may be null)
  * @param aSchema  the schema of the table (may be null)
  * @param aTable   the table name
  * @param aType    the table type. One of the types retrurned {@link #getTableTypes()}
  * @throws SQLException
  * @return a DataStore with the columns of the table
  */
	public DataStore getTableDefinition(String aCatalog, String aSchema, String aTable, String aType)
		throws SQLException
	{
		return this.getTableDefinition(aCatalog, aSchema, aTable, aType, true);
	}

	public DataStore getTableDefinition(TableIdentifier aTable)
		throws SQLException
	{
		return getTableDefinition(aTable, true);
	}
	/**
  * Returns the definition of the given
  * table in a {@link workbench.storage.DataStore }
  * @return definiton of the datastore
  * @param aTable The identifier of the table
  * @throws SQLException If the table was not found or an error occurred
  */
	public DataStore getTableDefinition(TableIdentifier id, boolean adjustCase)
		throws SQLException
	{
		if (id == null) return null;
		String type = id.getType();
		if (type == null) type = tableTypeName;
		return this.getTableDefinition(id.getCatalog(), id.getSchema(), id.getTableName(), type, adjustCase);
	}

	public static final String[] TABLE_DEFINITION_COLS = {"COLUMN_NAME", "DATA_TYPE", "PK", "NULLABLE", "DEFAULT", "REMARKS", "java.sql.Types", "SCALE/SIZE", "PRECISION", "POSITION"};
	private DataStore createTableDefinitionDataStore()
	{
		final int[] types = {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.INTEGER};
		final int[] sizes = {20, 18, 5, 8, 10, 25, 18, 2, 2, 2};
		DataStore ds = new DataStore(TABLE_DEFINITION_COLS, types, sizes);
		return ds;
	}
	/** Return a DataStore containing the definition of the given table.
	 * @param aCatalog The catalog in which the table is defined. This should be null if the DBMS does not support catalogs
	 * @param aSchema The schema in which the table is defined. This should be null if the DBMS does not support schemas
	 * @param aTable The name of the table
	 * @param aType The type of the table
	 * @param adjustNames If true the object names will be quoted if necessary
	 * @throws SQLException
	 * @return A DataStore with the table definition.
	 * The individual columns should be accessed using the
	 * COLUMN_IDX_TABLE_DEFINITION_xxx constants.
	 */
	public DataStore getTableDefinition(String aCatalog, String aSchema, String aTable, String aType, boolean adjustNames)
		throws SQLException
	{
		if (aTable == null) throw new IllegalArgumentException("Tablename may not be null!");

		DataStore ds = this.createTableDefinitionDataStore();

		int pos = aTable.indexOf(".");

		if (pos > -1 && aSchema == null)
		{
			aSchema = aTable.substring(0, pos);
			aTable = aTable.substring(pos + 1);
		}

		aCatalog = StringUtil.trimQuotes(aCatalog);
		aSchema = StringUtil.trimQuotes(aSchema);
		aTable = StringUtil.trimQuotes(aTable);

		if (aSchema == null && this.isOracle()) 
		{
			aSchema = this.getSchemaToUse();
		}
		
		if (adjustNames)
		{
			aCatalog = this.adjustObjectnameCase(aCatalog);
			aSchema = this.adjustObjectnameCase(aSchema);
			aTable = this.adjustObjectnameCase(aTable);
		}

		if (this.sequenceReader != null && "SEQUENCE".equalsIgnoreCase(aType))
		{
			DataStore seqDs = this.sequenceReader.getSequenceDefinition(aSchema, aTable);
			if (seqDs != null) return seqDs;
		}

		if ("SYNONYM".equalsIgnoreCase(aType))
		{
			TableIdentifier id = this.getSynonymTable(aSchema, aTable);
			if (id != null)
			{
				aSchema = id.getSchema();
				aTable = id.getTableName();
				aCatalog = null;
			}
		}

		ArrayList keys = new ArrayList();
		ResultSet keysRs = null;
		try
		{
			keysRs = this.metaData.getPrimaryKeys(aCatalog, aSchema, aTable);
			while (keysRs.next())
			{
				keys.add(keysRs.getString("COLUMN_NAME").toLowerCase());
			}
		}
		catch (Throwable e)
		{
			LogMgr.logWarning("DbMetaData.getTableDefinition()", "Error retrieving key columns", e);
		}
		finally
		{
			SqlUtil.closeResult(keysRs);
		}

		boolean hasEnums = false;

		ResultSet rs = null;

		try
		{
			// Oracle's JDBC driver does not return varchar lengths
			// correctly if the NLS_LENGTH_SEMANTICS is set to CHARACTER (and not byte)
			// so we'll need to use our own statement
			if (this.oracleMetaData != null)
			{
				rs = this.oracleMetaData.getColumns(aCatalog, aSchema, aTable, "%");
			}
			else
			{
				rs = this.metaData.getColumns(aCatalog, aSchema, aTable, "%");
			}

			//rs = this.metaData.getColumns(aCatalog, aSchema, aTable, "%");

			while (rs.next())
			{
				int row = ds.addRow();

				String colName = rs.getString("COLUMN_NAME");
				int sqlType = rs.getInt("DATA_TYPE");
				String typeName = rs.getString("TYPE_NAME");
				if (this.isMySql && !hasEnums)
				{
					hasEnums = typeName.startsWith("enum") || typeName.startsWith("set");
				}

				int size = rs.getInt("COLUMN_SIZE");
				int digits = rs.getInt("DECIMAL_DIGITS");
				String rem = rs.getString("REMARKS");
				String def = rs.getString("COLUMN_DEF");
				if (def != null && this.trimDefaults)
				{
					def = def.trim();
				}
				int position = rs.getInt("ORDINAL_POSITION");
				String nul = rs.getString("IS_NULLABLE");

				String display = getSqlTypeDisplay(typeName, sqlType, size, digits);
				ds.setValue(row, COLUMN_IDX_TABLE_DEFINITION_COL_NAME, colName);
				ds.setValue(row, COLUMN_IDX_TABLE_DEFINITION_DATA_TYPE, display);
				if (keys.contains(colName.toLowerCase()))
					ds.setValue(row, COLUMN_IDX_TABLE_DEFINITION_PK_FLAG, "YES");
				else
					ds.setValue(row, COLUMN_IDX_TABLE_DEFINITION_PK_FLAG, "NO");

				ds.setValue(row, COLUMN_IDX_TABLE_DEFINITION_NULLABLE, nul);

				ds.setValue(row, COLUMN_IDX_TABLE_DEFINITION_DEFAULT, def);
				ds.setValue(row, COLUMN_IDX_TABLE_DEFINITION_REMARKS, rem);
				ds.setValue(row, COLUMN_IDX_TABLE_DEFINITION_JAVA_SQL_TYPE, new Integer(sqlType));
				ds.setValue(row, COLUMN_IDX_TABLE_DEFINITION_SIZE, new Integer(size));
				ds.setValue(row, COLUMN_IDX_TABLE_DEFINITION_DIGITS, new Integer(digits));
				ds.setValue(row, COLUMN_IDX_TABLE_DEFINITION_POSITION, new Integer(position));
			}
		}
		finally
		{
			SqlUtil.closeResult(rs);
			if (this.oracleMetaData != null)
			{
				this.oracleMetaData.columnsProcessed();
			}
		}

		if (hasEnums)
		{
			EnumReader.updateEnumDefinition(aTable, ds, this.dbConnection);
		}

		return ds;
	}

	public static final int COLUMN_IDX_TABLE_INDEXLIST_INDEX_NAME = 0;
	public static final int COLUMN_IDX_TABLE_INDEXLIST_UNIQUE_FLAG = 1;
	public static final int COLUMN_IDX_TABLE_INDEXLIST_PK_FLAG = 2;
	public static final int COLUMN_IDX_TABLE_INDEXLIST_COL_DEF = 3;

	public DataStore getTableIndexInformation(TableIdentifier table)
	{
		String[] cols = {"INDEX_NAME", "UNIQUE", "PK", "DEFINITION"};
		final int types[] =   {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
		final int sizes[] =   {40, 7, 6, 50};
		DataStore idxData = new DataStore(cols, types, sizes);
		ResultSet idxRs = null;

		TableIdentifier tbl = table.createCopy();
		tbl.adjustCase(this.dbConnection);
		try
		{
			// Retrieve the name of the PK index
			String pkName = "";
			ResultSet keysRs = null;
			try
			{
				keysRs = this.metaData.getPrimaryKeys(tbl.getCatalog(), tbl.getSchema(), tbl.getTableName());
				while (keysRs.next())
				{
					pkName = keysRs.getString("PK_NAME");
				}
			}
			catch (Exception e)
			{
				LogMgr.logWarning("DbMetadata.getTableIndexInformation()", "Error retrieving PK information", e);
			}
			finally
			{
				try { keysRs.close(); } catch (Throwable th) {}
			}

			// the idxInfo will hold an ArrayList with
			// information for each index. The first entry
			// int he ArrayList will have the unique/non-unique
			// flag, the rest will be the column list
			HashMap idxInfo = new HashMap();
			
			// A list of index names that are functional indexes for Oracle
			List funcIndexNames = null;

			if (this.isOracle)
			{
				funcIndexNames = new ArrayList();
			}
			
			idxRs = this.indexReader.getIndexInfo(tbl, false);
			
			while (idxRs.next())
			{
				boolean unique = idxRs.getBoolean("NON_UNIQUE");
				String indexName = idxRs.getString("INDEX_NAME");
				if (idxRs.wasNull()) continue;
				if (indexName == null) continue;
				String colName = idxRs.getString("COLUMN_NAME");
				String dir = idxRs.getString("ASC_OR_DESC");
				ArrayList colInfo = (ArrayList)idxInfo.get(indexName);
				if (colInfo == null)
				{
					colInfo = new ArrayList(10);
					idxInfo.put(indexName, colInfo);
					if (unique)
						colInfo.add("NO");
					else
						colInfo.add("YES");
				}
				if (dir != null)
					colInfo.add(colName + " " + dir);
				else
					colInfo.add(colName);

				if (this.isOracle)
				{
					String type = idxRs.getString("INDEX_TYPE");
					if (type != null && type.startsWith("FUNCTION-BASED"))
					{
						if (!funcIndexNames.contains(indexName))
						{
							funcIndexNames.add(indexName);
						}
					}
				}
			}

			if (this.isOracle && funcIndexNames.size() > 0)
			{
				Map funcIndex = ((OracleIndexReader)this.indexReader).readFunctionIndexDefinition(tbl.getSchema(), tbl.getTableName(), funcIndexNames);
				Iterator defs = funcIndex.entrySet().iterator();
				while (defs.hasNext())
				{
					Entry entry = (Entry)defs.next();
					String index = (String)entry.getKey();
					ArrayList old = (ArrayList)idxInfo.get(index);
					ArrayList newList = (ArrayList)entry.getValue();
					newList.add(0, old.get(0));
					idxInfo.put(index, newList);
				}
			}

			Iterator itr = idxInfo.entrySet().iterator();
			while (itr.hasNext())
			{
				Entry entry = (Entry)itr.next();
				ArrayList colist = (ArrayList)entry.getValue();
				String index = (String)entry.getKey();
				int row = idxData.addRow();
				if (colist != null && colist.size() > 1)
				{
					idxData.setValue(row, 0, index);

					String unique = (String)colist.get(0);
					idxData.setValue(row, 1, unique);
					StrBuffer def = new StrBuffer();
					for (int i=1; i < colist.size(); i++)
					{
						if (i > 1) def.append(", ");
						def.append((String)colist.get(i));
					}
					if (pkName != null && pkName.equalsIgnoreCase(index))
					{
						idxData.setValue(row, 2, "YES");
					}
					else
					{
						idxData.setValue(row, 2, "NO");
					}
					idxData.setValue(row, 3, def.toString());
				}
			}
			idxData.sortByColumn(0, true);
		}
		catch (Exception e)
		{
			LogMgr.logWarning("DbMetadata.getTableIndexInformation()", "Could not retrieve indexes!", e);
			// clear any entries which might have made into the DataStore
			idxData.reset();
		}
		finally
		{
			try { idxRs.close(); } catch (Throwable th) {}
			this.indexReader.indexInfoProcessed();
		}
		return idxData;
	}

	public List getTableList(String schema, String[] types)
		throws SQLException
	{
		if (schema == null) schema = this.getCurrentSchema();
		return getTableList(null, schema, types);
	}
	
	public List getTableList()
		throws SQLException
	{
		String schema = this.getCurrentSchema();
		return getTableList(null, schema, TABLE_TYPES_TABLE);
	}

	public List getTableList(String table, String schema)
		throws SQLException
	{
		return getTableList(table, schema, TABLE_TYPES_TABLE);
	}

	public List getSelectableObjectsList(String schema)
		throws SQLException
	{
		return getTableList(null, schema, TABLE_TYPES_SELECTABLE, false);
	}

	public List getTableList(String table, String schema, String[] types)
		throws SQLException
	{
		return getTableList(table, schema, types, false);
	}
		/**
	 * Return a list of tables for the given schema
	 * if the schema is null, all tables will be returned
	 */
	public List getTableList(String table, String schema, String[] types, boolean returnAllSchemas)
		throws SQLException
	{
		DataStore ds = getTables(null, schema, table, types);
		int count = ds.getRowCount();
		List tables = new ArrayList(count);
		for (int i=0; i < count; i++)
		{
			String t = ds.getValueAsString(i, COLUMN_IDX_TABLE_LIST_NAME);
			String s = ds.getValueAsString(i, COLUMN_IDX_TABLE_LIST_SCHEMA);
			String c = ds.getValueAsString(i, COLUMN_IDX_TABLE_LIST_CATALOG);
			if (!returnAllSchemas && this.ignoreSchema(s))
			{
				s = null;
			}
			if (this.ignoreCatalog(c))
			{
				c = null;
			}
			TableIdentifier tbl = new TableIdentifier(c, s, t);
			tbl.setType(ds.getValueAsString(i, COLUMN_IDX_TABLE_LIST_TYPE));
			tables.add(tbl);
		}
		return tables;
	}

	public IndexDefinition[] getIndexList(TableIdentifier tbl)
	{
		DataStore ds = this.getTableIndexInformation(tbl);
		int count = ds.getRowCount();
		IndexDefinition[] result = new IndexDefinition[count];
		for (int i=0; i < count; i ++)
		{
			String name = ds.getValueAsString(i, COLUMN_IDX_TABLE_INDEXLIST_INDEX_NAME);
			String coldef = ds.getValueAsString(i, COLUMN_IDX_TABLE_INDEXLIST_COL_DEF);
			boolean unique = "YES".equalsIgnoreCase(ds.getValueAsString(i, COLUMN_IDX_TABLE_INDEXLIST_UNIQUE_FLAG));
			boolean pk = "YES".equalsIgnoreCase(ds.getValueAsString(i, COLUMN_IDX_TABLE_INDEXLIST_PK_FLAG));
			IndexDefinition def = new IndexDefinition(name, coldef);
			def.setUnique(unique);
			def.setPrimaryKeyIndex(pk);
			result[i] = def;
		}
		return result;
	}
	/** 	Return the current catalog for this connection. If no catalog is defined
	 * 	or the DBMS does not support catalogs, an empty string is returned.
	 *
	 * 	This method works around a bug in Microsoft's JDBC driver which does
	 *  not return the correct database (=catalog) after the database has
	 *  been changed with the USE <db> command from within the Workbench.
	 * @return The name of the current catalog or an empty String if there is no current catalog
	 */
	public String getCurrentCatalog()
	{
		String catalog = null;

		if (this.isSqlServer)
		{
			// for some reason, getCatalog() does not return the correct
			// information when using Microsoft's JDBC driver.
			// So we are using SQL Server's db_name() function to retrieve
			// the current catalog
			Statement stmt = null;
			ResultSet rs = null;
			try
			{
				stmt = this.dbConnection.createStatement();
				rs = stmt.executeQuery("SELECT db_name()");
				if (rs.next()) catalog = rs.getString(1);
			}
			catch (Exception e)
			{
				LogMgr.logError("DbMetadata.getCurrentCatalog()", "Error retrieving catalog", e);
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
				catalog = StringUtil.EMPTY_STRING;
			}
		}
		if (catalog == null) catalog = StringUtil.EMPTY_STRING;

		return catalog;
	}

	public boolean supportsTruncate()
	{
		String s = Settings.getInstance().getProperty("workbench.db.truncatesupported", StringUtil.EMPTY_STRING);
		List l = StringUtil.stringToList(s, ",");
		return l.contains(this.getDbId());
	}

	public boolean supportsCatalogs()
	{
		boolean supportsCatalogs = false;
		try
		{
			supportsCatalogs = metaData.supportsCatalogsInDataManipulation()
		                  || metaData.supportsCatalogsInTableDefinitions();
		}
		catch (Exception e)
		{
			supportsCatalogs = false;
		}
		return supportsCatalogs;
	}
	
	/**
	 * Changes the current catalog using Connection.setCatalog()
	 * and notifies the connection object about the change.
	 *
	 * @param newCatalog the name of the new catalog/database that should be selected
	 * @see WbConnection#catalogChanged(String, String)
	 */
	public boolean setCurrentCatalog(String newCatalog)
		throws SQLException
	{
		if (StringUtil.isEmptyString(newCatalog)) return false;
	
		String old = getCurrentCatalog();
		boolean useSetCatalog = Settings.getInstance().getBoolProperty("workbench.db." + this.getDbId() + ".usesetcatalog", true);
		
		// MySQL does not seem to like changing the current database by executing a USE command
		// through Statement.execute(), so we'll use setCatalog() instead
		// which seems to work with SQL Server as well. 
		// If for some reason this does not work, it could be turned off
		if (useSetCatalog)
		{
			this.dbConnection.getSqlConnection().setCatalog(trimQuotes(newCatalog));
		}
		else
		{
			Statement stmt = null;
			try 
			{
				stmt = this.dbConnection.createStatement();
				stmt.execute("USE " + newCatalog);
			}
			finally
			{
				SqlUtil.closeStatement(stmt);
			}
		}
		
		String newCat = getCurrentCatalog();
		if (!StringUtil.equalString(old, newCat))
		{
			this.dbConnection.catalogChanged(old, newCatalog);
		}
		LogMgr.logDebug("DbMetadata.setCurrentCatalog", "Catalog changed to " + newCat);
		
		return true;
	}
	
	/**
	 * Remove quotes from an object's name. 
	 * For MS SQL Server this also removes [] brackets
	 * around the identifier.
	 */
	private String trimQuotes(String s)
	{
		if (s.length() < 2) return s;
		if (this.isSqlServer)
		{
			String clean = s.trim();
			int len = clean.length();
			if (clean.charAt(0)=='[' && clean.charAt(len-1)==']')
				return clean.substring(1,len-1);
		}
		
		return StringUtil.trimQuotes(s);
}
	/**
	 *	Returns a list of all catalogs in the database.
	 *	Some DBMS's do not support catalogs, in this case the method
	 *	will return an empty Datastore.
	 */
	public DataStore getCatalogInformation()
	{

		String[] cols = { this.getCatalogTerm().toUpperCase() };
		int[] types = { Types.VARCHAR };
		int[] sizes = { 10 };

		DataStore result = new DataStore(cols, types, sizes);
		ResultSet rs = null;
		try
		{
			rs = this.metaData.getCatalogs();
			while (rs.next())
			{
				String cat = rs.getString(1);
				if (cat != null)
				{
					int row = result.addRow();
					result.setValue(row, 0, cat);
				}
			}
		}
		catch (Exception e)
		{
		}
		finally
		{
			SqlUtil.closeResult(rs);
		}
		
		if (result.getRowCount() == 1)
		{
			String cat = result.getValueAsString(0, 0);
			if (cat.equals(this.getCurrentCatalog()))
			{
				result.reset();
			}
		}
		
		return result;
	}

	/**
	 *	The column index in the DataStore returned by getTableTriggers which identifies
	 *  the name of the trigger.
	 */
	public static final int COLUMN_IDX_TABLE_TRIGGERLIST_TRG_NAME = 0;
	/**
	 *	The column index in the DataStore returned by getTableTriggers which identifies
	 *  the type (INSERT, UPDATE etc) of the trigger.
	 */
	public static final int COLUMN_IDX_TABLE_TRIGGERLIST_TRG_TYPE = 1;
	/**
	 *	The column index in the DataStore returned by getTableTriggers which identifies
	 *  the event (before, after) of the trigger.
	 */
	public static final int COLUMN_IDX_TABLE_TRIGGERLIST_TRG_EVENT = 2;

	/**
	 *	Return the list of defined triggers for the given table.
	 */
	public DataStore getTableTriggers(String aCatalog, String aSchema, String aTable)
		throws SQLException
	{
		String[] cols = {"NAME", "TYPE", "EVENT"};
		final int types[] =   {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
		final int sizes[] =   {30, 30, 20};

		DataStore result = new DataStore(cols, types, sizes);

		if ("*".equals(aCatalog)) aCatalog = null;
		if ("*".equals(aSchema)) aSchema = null;

		aCatalog = this.adjustObjectnameCase(aCatalog);
		aSchema = this.adjustObjectnameCase(aSchema);
		aTable = this.adjustObjectnameCase(aTable);

		GetMetaDataSql sql = (GetMetaDataSql)triggerList.get(this.productName);
		if (sql == null)
		{
			return result;
		}

		sql.setSchema(aSchema);
		sql.setCatalog(aCatalog);
		sql.setObjectName(aTable);
		Statement stmt = this.dbConnection.createStatementForQuery();
		String query = this.adjustHsqlQuery(sql.getSql());

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("DbMetadata.getTableTriggers()", "Using query=\n" + query);
		}
		ResultSet rs = stmt.executeQuery(query);
		try
		{
			while (rs.next())
			{
				int row = result.addRow();
				String value = rs.getString(1);
				if (!rs.wasNull() && value != null) value = value.trim();
				result.setValue(row, COLUMN_IDX_TABLE_TRIGGERLIST_TRG_NAME, value);

				value = rs.getString(2);
				result.setValue(row, COLUMN_IDX_TABLE_TRIGGERLIST_TRG_TYPE, value);

				value = rs.getString(3);
				result.setValue(row, COLUMN_IDX_TABLE_TRIGGERLIST_TRG_EVENT, value);
			}
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}

	/**
	 * Retrieve the SQL Source of the given trigger.
	 * @param aCatalog The catalog in which the trigger is defined. This should be null if the DBMS does not support catalogs
	 * @param aSchema The schema in which the trigger is defined. This should be null if the DBMS does not support schemas
	 * @param aTriggername
	 * @throws SQLException
	 * @return the trigger source
	 */
	public String getTriggerSource(String aCatalog, String aSchema, String aTriggername)
		throws SQLException
	{
		StrBuffer result = new StrBuffer(500);

		if ("*".equals(aCatalog)) aCatalog = null;
		if ("*".equals(aSchema)) aSchema = null;

		GetMetaDataSql sql = (GetMetaDataSql)triggerSourceSql.get(this.productName);
		if (sql == null) return StringUtil.EMPTY_STRING;

		sql.setSchema(aSchema);
		sql.setCatalog(aCatalog);
		sql.setObjectName(aTriggername);
		Statement stmt = this.dbConnection.createStatement();
		String query = this.adjustHsqlQuery(sql.getSql());

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("DbMetadata.getTriggerSource()", "Using query=\n" + query);
		}

		ResultSet rs = null;
		try
		{
			// for some DBMS (e.g. SQL Server)
			// we need to run a exec which might not work 
			// when using executeQuery() (depending on the JDBC driver)
			stmt.execute(query);
			rs = stmt.getResultSet();
			
			if (rs != null)
			{
				int colCount = rs.getMetaData().getColumnCount();
				while (rs.next())
				{
					for (int i=1; i <= colCount; i++)
					{
						result.append(rs.getString(i));
					}
				}
			}
			String warn = SqlUtil.getWarnings(this.dbConnection, stmt, true);
			if (warn != null && result.length() > 0) result.append("\n\n");
			result.append(warn);
		}
		catch (SQLException e)
		{
			LogMgr.logError("DbMetadata.getTriggerSource()", "Error reading trigger source", e);
			if (this.isPostgres) try { this.dbConnection.rollback(); } catch (Throwable th) {}
			result.append(ExceptionUtil.getDisplay(e));
			SqlUtil.closeAll(rs, stmt);
			return result.toString();
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		
		if (this.isCloudscape || this.isFirstSql)
		{
			String r = result.toString().replaceAll("\\\\n", "\n");
			return r;
		}
		else
		{
			return result.toString();
		}
	}

	/** Returns the list of schemas as returned by DatabaseMetadata.getSchemas()
	 * @return List
	 */
	public List getSchemas()
	{
		ArrayList result = new ArrayList();
		ResultSet rs = null;
		try
		{
			rs = this.metaData.getSchemas();
			while (rs.next())
			{
				result.add(rs.getString(1));
			}
		}
		catch (Exception e)
		{
        LogMgr.logWarning("DbMetadata.getSchemas()", "Error retrieving schemas: " + e.getMessage(), null);
		}
		finally
		{
			SqlUtil.closeResult(rs);
		}
		if (this.isOracle)
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

	public Collection getTableTypes()
	{
		TreeSet result = new TreeSet();
		ResultSet rs = null;
		boolean hideIndexes = hideIndexes();

		try
		{
			rs = this.metaData.getTableTypes();
			while (rs.next())
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
			List addTypes = StringUtil.stringToList(additional, ",", true, true);
			result.addAll(addTypes);
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

	public String getSchemaTerm() { return this.schemaTerm; }
	public String getCatalogTerm() { return this.catalogTerm; }

	public static final int COLUMN_IDX_FK_DEF_FK_NAME = 0;
	public static final int COLUMN_IDX_FK_DEF_COLUMN_NAME = 1;
	public static final int COLUMN_IDX_FK_DEF_REFERENCE_COLUMN_NAME = 2;
	public static final int COLUMN_IDX_FK_DEF_UPDATE_RULE = 3;
	public static final int COLUMN_IDX_FK_DEF_DELETE_RULE = 4;
	public static final int COLUMN_IDX_FK_DEF_UPDATE_RULE_VALUE = 5;
	public static final int COLUMN_IDX_FK_DEF_DELETE_RULE_VALUE = 6;

	public DataStore getExportedKeys(String aCatalog, String aSchema, String aTable)
		throws SQLException
	{
		return getRawKeyList(aCatalog, aSchema, aTable, true);
	}

	public DataStore getImportedKeys(String aCatalog, String aSchema, String aTable)
		throws SQLException
	{
		return getRawKeyList(aCatalog, aSchema, aTable, false);
	}

	private DataStore getRawKeyList(String aCatalog, String aSchema, String aTable, boolean exported)
		throws SQLException
	{
		aCatalog = this.adjustObjectnameCase(aCatalog);
		aSchema = this.adjustObjectnameCase(aSchema);
		aTable = this.adjustObjectnameCase(aTable);
		ResultSet rs;
		if (exported)
			rs = this.metaData.getExportedKeys(aCatalog, aSchema, aTable);
		else
			rs = this.metaData.getImportedKeys(aCatalog, aSchema, aTable);

		DataStore ds = new DataStore(rs, false);
		try
		{
			while (rs.next())
			{
				int row = ds.addRow();
				ds.setValue(row, 0, rs.getString(1));
				ds.setValue(row, 1, rs.getString(2));
				ds.setValue(row, 2, rs.getString(3));
				ds.setValue(row, 3, rs.getString(4));
				ds.setValue(row, 4, rs.getString(5));
				ds.setValue(row, 5, rs.getString(6));
				ds.setValue(row, 6, rs.getString(7));
				ds.setValue(row, 7, rs.getString(8));
				ds.setValue(row, 8, new Integer(rs.getInt(9)));
				ds.setValue(row, 9, new Integer(rs.getInt(10)));
				ds.setValue(row, 10, rs.getString(11));
				String fk_name = this.fixFKName(rs.getString(12));
				ds.setValue(row, 11, fk_name);
				ds.setValue(row, 12, rs.getString(13));
				ds.setValue(row, 13, new Integer(rs.getInt(14)));
			}
		}
		finally
		{
			SqlUtil.closeResult(rs);
		}
		return ds;
	}

	/**
	 *	Works around a bug in Postgres' JDBC driver.
	 *	For Postgres strips everything after \000 for any
	 *  other DBMS the given name is returned without change
	 */
	private String fixFKName(String aName)
	{
		if (aName == null) return null;
		if (!this.isPostgres) return aName;
		if (aName.indexOf("\\000") > -1)
		{
			// the Postgres JDBC driver seems to have a bug here,
			// because it appends the whole FK information to the fk name!
			// the actual FK name ends at the first \000
			return aName.substring(0, aName.indexOf("\\000"));
		}
		return aName;
	}

	public DataStore getForeignKeys(String aCatalog, String aSchema, String aTable, boolean includeNumericRuleValue)
	{
		DataStore ds = this.getKeyList(aCatalog, aSchema, aTable, true, includeNumericRuleValue);
		return ds;
	}

	public DataStore getReferencedBy(String aCatalog, String aSchema, String aTable)
	{
		DataStore ds = this.getKeyList(aCatalog, aSchema, aTable, false, false);
		return ds;
	}

	private DataStore getKeyList(String aCatalog, String aSchema, String aTable, boolean getOwnFk, boolean includeNumericRuleValue)
	{
		String cols[];
		String refColName;

		if (getOwnFk)
		{
			refColName = "REFERENCES";
		}
		else
		{
			refColName = "REFERENCED BY";
		}
		int types[];
		int sizes[];

		if (includeNumericRuleValue)
		{
			cols = new String[] { "FK_NAME", "COLUMN", refColName , "UPDATE_RULE", "DELETE_RULE", "UPDATE_RULE_VALUE", "DELETE_RULE_VALUE"};
			types = new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.INTEGER};
			sizes = new int[] {25, 10, 30, 12, 12, 1, 1};
		}
		else
		{
			cols = new String[] { "FK_NAME", "COLUMN", refColName , "UPDATE_RULE", "DELETE_RULE"};
			types = new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
			sizes = new int[] {25, 10, 30, 12, 12};
		}
		DataStore ds = new DataStore(cols, types, sizes);
		ResultSet rs = null;

		try
		{
			if ("*".equals(aCatalog)) aCatalog = null;
			if ("*".equals(aSchema)) aSchema = null;

			aCatalog = this.adjustObjectnameCase(aCatalog);
			aSchema = this.adjustObjectnameCase(aSchema);
			aTable = this.adjustObjectnameCase(aTable);
			int tableCol;
			int fkNameCol;
			int colCol;
			int fkColCol;
			int deleteActionCol;
			int updateActionCol;
			int schemaCol;

			if (getOwnFk)
			{
				rs = this.metaData.getImportedKeys(aCatalog, aSchema, aTable);
				tableCol = 3;
				schemaCol = 2;
				fkNameCol = 12;
				colCol = 8;
				fkColCol = 4;
				updateActionCol = 10;
				deleteActionCol = 11;
			}
			else
			{
				rs = this.metaData.getExportedKeys(aCatalog, aSchema, aTable);
				tableCol = 7;
				schemaCol = 6;
				fkNameCol = 12;
				colCol = 4;
				fkColCol = 8;
				updateActionCol = 10;
				deleteActionCol = 11;
			}
			while (rs.next())
			{
				String table = rs.getString(tableCol);
				String fk_col = rs.getString(fkColCol);
				String col = rs.getString(colCol);
				String fk_name = this.fixFKName(rs.getString(fkNameCol));
				String schema = rs.getString(schemaCol);
				if (schema != null && !schema.equals(this.getCurrentSchema()))
				{
					table = schema + "." + table;
				}
				int updateAction = rs.getInt(updateActionCol);
				String updActionDesc = this.getRuleTypeDisplay(updateAction);
				int deleteAction = rs.getInt(deleteActionCol);
				String delActionDesc = this.getRuleTypeDisplay(deleteAction);
				int row = ds.addRow();
				ds.setValue(row, COLUMN_IDX_FK_DEF_FK_NAME, fk_name);
				ds.setValue(row, COLUMN_IDX_FK_DEF_COLUMN_NAME, col);
				ds.setValue(row, COLUMN_IDX_FK_DEF_REFERENCE_COLUMN_NAME, table + "." + fk_col);
				ds.setValue(row, COLUMN_IDX_FK_DEF_UPDATE_RULE, updActionDesc);
				ds.setValue(row, COLUMN_IDX_FK_DEF_DELETE_RULE, delActionDesc);
				if (includeNumericRuleValue)
				{
					ds.setValue(row, COLUMN_IDX_FK_DEF_DELETE_RULE_VALUE, new Integer(deleteAction));
					ds.setValue(row, COLUMN_IDX_FK_DEF_UPDATE_RULE_VALUE, new Integer(updateAction));
				}
			}
		}
		catch (Exception e)
		{
			ds.reset();
		}
		finally
		{
			try { rs.close(); } catch (Exception e) {}
		}
		return ds;
	}

	/**
	 *	Translates the numberic constants of DatabaseMetaData for trigger rules
	 *	into text (e.g DatabaseMetaData.importedKeyNoAction --> NO ACTION)
	 *
	 *	@param aRule the numeric value for a rule as defined by DatabaseMetaData.importedKeyXXXX constants
	 *	@return String
	 */
	public String getRuleTypeDisplay(int aRule)
	{
		String display = this.getRuleAction(aRule);
		if (display != null) return display;
		switch (aRule)
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

	private String getRuleAction(int rule)
	{
		String key;
		switch (rule)
		{
			case DatabaseMetaData.importedKeyNoAction:
				key = "workbench.sql.fkrule.noaction";
				break;
			case DatabaseMetaData.importedKeyRestrict:
				key = "workbench.sql.fkrule.restrict";
				break;
			case DatabaseMetaData.importedKeySetNull:
				key = "workbench.sql.fkrule.setnull";
				break;
			case DatabaseMetaData.importedKeyCascade:
				key = "workbench.sql.fkrule.cascade";
				break;
			case DatabaseMetaData.importedKeySetDefault:
				key = "workbench.sql.fkrule.setdefault";
				break;
			case DatabaseMetaData.importedKeyInitiallyDeferred:
				key = "workbench.sql.fkrule.initiallydeferred";
				break;
			case DatabaseMetaData.importedKeyInitiallyImmediate:
				key = "workbench.sql.fkrule.initiallyimmediate";
				break;
			case DatabaseMetaData.importedKeyNotDeferrable:
				key = "workbench.sql.fkrule.notdeferrable";
				break;
			default:
				return null;
		}
		key = key + "." + this.getDbId();
		return Settings.getInstance().getProperty(key, null);
	}

	private String getPkIndexName(DataStore anIndexDef)
	{
		if (anIndexDef == null) return null;
		int count = anIndexDef.getRowCount();

		String name = null;
		for (int row = 0; row < count; row ++)
		{
			String is_pk = anIndexDef.getValue(row, COLUMN_IDX_TABLE_INDEXLIST_PK_FLAG).toString();
			if ("YES".equalsIgnoreCase(is_pk))
			{
				name = anIndexDef.getValue(row, COLUMN_IDX_TABLE_INDEXLIST_INDEX_NAME).toString();
				break;
			}
		}
		return name;
	}

	public String getSequenceSource(String fullName)
	{
		String sequenceName = fullName;
		String schema = null;

		int pos = fullName.indexOf('.');
		if (pos > 0)
		{
			sequenceName = fullName.substring(pos);
			schema = fullName.substring(0, pos - 1);
		}
		return this.getSequenceSource(null, schema, sequenceName);
	}

	public String getSequenceSource(String aCatalog, String aSchema, String aSequence)
	{
		if (this.sequenceReader != null)
		{
			if (aSchema == null)
			{
				aSchema = this.getCurrentSchema();
			}
			return this.sequenceReader.getSequenceSource(aSchema, aSequence);
		}
		return StringUtil.EMPTY_STRING;
	}

	/**
	 *	Return the underlying table of a synonym.
	 *
	 *	@return the table to which the synonym points.
	 */
	public TableIdentifier getSynonymTable(String anOwner, String aSynonym)
	{
		if (this.synonymReader == null) return null;
		TableIdentifier id = null;
		try
		{
			id = this.synonymReader.getSynonymTable(this.dbConnection.getSqlConnection(), anOwner, aSynonym);
			if (id == null && this.isOracle)
			{
				id = this.synonymReader.getSynonymTable(this.dbConnection.getSqlConnection(), "PUBLIC", aSynonym);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("DbMetadata.getSynonymTable()", "Could not retrieve table for synonym", e);
		}
		return id;
	}

	/**
	 *	Return the SQL statement to recreate the given synonym.
	 *	@return the SQL to create the synonym.
	 */
	public String getSynonymSource(String anOwner, String aSynonym)
	{
		if (this.synonymReader == null) return StringUtil.EMPTY_STRING;
		String result = null;

		try
		{
			result = this.synonymReader.getSynonymSource(this.dbConnection.getSqlConnection(), anOwner, aSynonym);
		}
		catch (Exception e)
		{
			result = StringUtil.EMPTY_STRING;
		}

		return result;
	}

	/** 	
   * Return the SQL statement to re-create the given table. (in the dialect for the
	 * current DBMS)
   *
	 * @return the SQL statement to create the given table.
	 * @param catalog The catalog in which the table is defined. This should be null if the DBMS does not support catalogs
	 * @param schema The schema in which the table is defined. This should be null if the DBMS does not support schemas
	 * @param table The name of the table
	 * @param includeDrop If true, a DROP TABLE statement will be included in the generated SQL script.
	 * @throws SQLException
	 */
	public String getTableSource(TableIdentifier table, boolean includeDrop)
		throws SQLException
	{
		DataStore tableDef = this.getTableDefinition(table, true);
		DataStore index = this.getTableIndexInformation(table);
		TableIdentifier tbl = table.createCopy();
		tbl.adjustCase(this.dbConnection);
		DataStore fkDef = this.getForeignKeys(tbl.getCatalog(), tbl.getSchema(), tbl.getTableName(), false);
		String source = this.getTableSource(table, tableDef, index, fkDef, includeDrop);
		return source;
	}

	public String getTableSource(TableIdentifier table, DataStore aTableDef, DataStore aIndexDef, DataStore aFkDef, boolean includeDrop)
	{
		ColumnIdentifier[] cols = createColumnIdentifiers(aTableDef);
		return getTableSource(table, cols, aIndexDef, aFkDef, includeDrop, null);
	}

	public String getTableSource(TableIdentifier table, ColumnIdentifier[] columns, String tableNameToUse)
	{
		return getTableSource(table, columns, null, null, false, tableNameToUse);
	}

	public String getTableSource(TableIdentifier table, ColumnIdentifier[] columns, DataStore aIndexDef, DataStore aFkDef, boolean includeDrop, String tableNameToUse)
	{
		if (columns == null || columns.length == 0) return StringUtil.EMPTY_STRING;

		StrBuffer result = new StrBuffer();

		Map columnConstraints = this.getColumnConstraints(table);

		result.append(generateCreateObject(includeDrop, "TABLE", (tableNameToUse == null ? table.getTableName() : tableNameToUse)));
		result.append("\n(\n");
		int count = columns.length;
		StrBuffer pkCols = new StrBuffer(1000);
		int maxColLength = 0;
		int maxTypeLength = 0;

		// calculate the longest column name, so that the display can be formatted
		for (int i=0; i < count; i++)
		{
			String colName = columns[i].getColumnName();
			String type = columns[i].getDbmsType();
			maxColLength = Math.max(maxColLength, colName.length());
			maxTypeLength = Math.max(maxTypeLength, type.length());
		}
		maxColLength++;
		maxTypeLength++;
		
		// Some RDBMS require the "DEFAULT" clause before the [NOT] NULL clause
		boolean defaultBeforeNull = Settings.getInstance().getBoolProperty("workbench.db.defaultbeforenull." + this.getDbId(), false);//this.isOracle || this.isFirebird || this.isIngres;
		String nullKeyword = Settings.getInstance().getProperty("workbench.db.nullkeyword." + getDbId(), "NULL");
		boolean includeCommentInTableSource = Settings.getInstance().getBoolProperty("workbench.db.colcommentinline." + this.getDbId(), false);
			
		for (int i=0; i < count; i++)
		{
			String colName = columns[i].getColumnName();
			String type = columns[i].getDbmsType();
			String def = columns[i].getDefaultValue();

			result.append("   ");
			result.append(quoteObjectname(colName));
			if (columns[i].isPkColumn() && (!this.isFirstSql || this.isFirstSql && !type.equals("sequence")))
			{
				if (pkCols.length() > 0) pkCols.append(',');
				pkCols.append(colName.trim());
			}
			for (int k=0; k < maxColLength - colName.length(); k++) result.append(' ');
			result.append(type);
			for (int k=0; k < maxTypeLength - type.length(); k++) result.append(' ');

			if (defaultBeforeNull && !StringUtil.isEmptyString(def))
			{
				result.append(" DEFAULT ");
				result.append(def.trim());
			}

			if (this.isFirstSql && "sequence".equals(type))
			{
				// with FirstSQL a column of type "sequence" is always the primary key
				result.append(" PRIMARY KEY");
			}
			else if (columns[i].isNullable())
			{
				if (this.useNullKeyword)
				{
					result.append(' ');
					result.append(nullKeyword);
				}
			}
			else
			{
				result.append(" NOT NULL");
			}

			if (!defaultBeforeNull && !StringUtil.isEmptyString(def))
			{
				result.append(" DEFAULT ");
				result.append(def.trim());
			}

			String constraint = (String)columnConstraints.get(colName);
			if (constraint != null && constraint.length() > 0)
			{
				result.append(' ');
				result.append(constraint);
			}
			
			if (includeCommentInTableSource && !StringUtil.isEmptyString(columns[i].getComment()))
			{
				result.append(" COMMENT '");
				result.append(columns[i].getComment());
				result.append('\'');
			}
			
			if (i < count - 1) result.append(',');
			result.append('\n');
		}

		String cons = this.getTableConstraints(table, "   ");
		if (cons != null && cons.length() > 0)
		{
			result.append("   ,");
			result.append(cons);
			result.append('\n');
		}
		String realTable = (tableNameToUse == null ? table.getTableName() : tableNameToUse);

		if (this.createInlineConstraints && pkCols.length() > 0)
		{
			result.append("\n   ,PRIMARY KEY (");
			result.append(pkCols.toString());
			result.append(")\n");

			StrBuffer fk = this.getFkSource(table.getTableName(), aFkDef, tableNameToUse);
			if (fk.length() > 0)
			{
				result.append(fk);
			}
		}

		result.append(");\n");
		if (!this.createInlineConstraints && pkCols.length() > 0)
		{
			String template = this.getSqlTemplate(DbMetadata.pkStatements);
			template = StringUtil.replace(template, TABLE_NAME_PLACEHOLDER, realTable);
			template = StringUtil.replace(template, COLUMNLIST_PLACEHOLDER, pkCols.toString());
			String name = this.getPkIndexName(aIndexDef);
			if (name == null) name = "pk_" + realTable.toLowerCase();
			if (isKeyword(name)) name = "\"" + name + "\"";
			template = StringUtil.replace(template, PK_NAME_PLACEHOLDER, name);
			result.append(template);
			result.append(";\n\n");
		}
		StrBuffer indexSource = this.indexReader.getIndexSource(table, aIndexDef, tableNameToUse);
		result.append(indexSource);
		if (!this.createInlineConstraints) result.append(this.getFkSource(table.getTableName(), aFkDef, tableNameToUse));

		String comments = this.getTableCommentSql(table);
		if (comments != null && comments.length() > 0)
		{
			result.append('\n');
			result.append(comments);
			result.append('\n');
		}

		comments = this.getTableColumnCommentsSql(table, columns);
		if (comments != null && comments.length() > 0)
		{
			result.append('\n');
			result.append(comments);
			result.append('\n');
		}

		StrBuffer grants = this.getTableGrantSource(table);
		if (grants.length() > 0)
		{
			result.append(grants);
		}
		
		if (this.ddlNeedsCommit)
		{
			result.append("\nCOMMIT;\n");
		}

		return result.toString();
	}

	/**
	 * Return constraints defined for each column in the given table.
	 * @param table The table to check
	 * @return A Map with columns and their constraints. The keys to the Map are column names
	 * The value is the SQL source for the column. The actual retrieval is delegated to a {@link ConstraintReader}
	 * @see ConstraintReader#getColumnConstraints(TableIdentifier)
	 */
	public Map getColumnConstraints(TableIdentifier table)
	{
		Map columnConstraints = Collections.EMPTY_MAP;
		if (this.constraintReader != null)
		{
			try
			{
				columnConstraints = this.constraintReader.getColumnConstraints(this.dbConnection.getSqlConnection(), table);
			}
			catch (Exception e)
			{
				if (this.isPostgres) try { this.dbConnection.rollback(); } catch (Throwable th) {}
				columnConstraints = Collections.EMPTY_MAP;
			}
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
	public String getTableConstraints(TableIdentifier tbl, String indent)
	{
		if (this.constraintReader == null) return null;
		String cons = null;
		try
		{
			cons = this.constraintReader.getTableConstraints(dbConnection.getSqlConnection(), tbl, indent);
		}
		catch (SQLException e)
		{
			if (this.isPostgres) try { this.dbConnection.rollback(); } catch (Throwable th) {}
			cons = null;
		}
		return cons;
	}

	/**
	 * Return the SQL that is needed to re-create the comment on the given columns.
	 * The syntax to be used, can be configured in the ColumnCommentStatements.xml file.
	 */
	public String getTableColumnCommentsSql(TableIdentifier table, ColumnIdentifier[] columns)
	{
		String columnStatement = (String)columnCommentStatements.get(this.productName);
		if (columnStatement == null || columnStatement.trim().length() == 0) return null;
		StrBuffer result = new StrBuffer(500);
		int cols = columns.length;
		for (int i=0; i < cols; i ++)
		{
			String column = columns[i].getColumnName();
			String remark = columns[i].getComment();
			if (column == null || remark == null || remark.trim().length() == 0) continue;
			String comment = columnStatement.replaceAll(COMMENT_TABLE_PLACEHOLDER, table.getTableName());
			comment = comment.replaceAll(COMMENT_COLUMN_PLACEHOLDER, column);
			comment = comment.replaceAll(COMMENT_PLACEHOLDER, remark.replaceAll("'", "''"));
			result.append(comment);
			result.append("\n");
		}
		return result.toString();
	}

	/**
	 * Return the SQL that is needed to re-create the comment on the given table.
	 * The syntax to be used, can be configured in the TableCommentStatements.xml file.
	 */
	public String getTableCommentSql(TableIdentifier table)
	{
		String commentStatement = (String)tableCommentStatements.get(this.productName);
		if (commentStatement == null || commentStatement.trim().length() == 0) return null;

		String comment = this.getTableComment(table);
		String result = null;
		if (comment != null && comment.trim().length() > 0)
		{
			result = commentStatement.replaceAll(COMMENT_TABLE_PLACEHOLDER, table.getTableName());
			result = result.replaceAll(COMMENT_PLACEHOLDER, comment);
		}
		return result;
	}

	public String getTableComment(TableIdentifier tbl)
	{
		TableIdentifier table = tbl.createCopy();
		table.adjustCase(this.dbConnection);
		ResultSet rs = null;
		String result = null;
		try
		{
			rs = this.metaData.getTables(table.getCatalog(), table.getSchema(), table.getTableName(), null);
			if (rs.next())
			{
				result = rs.getString("REMARKS");
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("DbMetadata.getTableComment()", "Error retrieving comment for table " + table.getTableExpression(), e);
			result = null;
		}
		finally
		{
			SqlUtil.closeResult(rs);
		}

		return result;
	}

	/**
	 *	Return a SQL script to re-create the Foreign key definition for the given table.
	 *
	 *	@param aTable the tablename for which the foreign keys should be created
	 *  @param aFkDef a DataStore with the FK definition as returned by #getForeignKeys()
	 *
	 *	@return a SQL statement to add the foreign key definitions to the given table
	 */
	public StrBuffer getFkSource(String aTable, DataStore aFkDef, String tableNameToUse)
	{
		if (aFkDef == null) return StrBuffer.EMPTY_BUFFER;
		int count = aFkDef.getRowCount();
		if (count == 0) return StrBuffer.EMPTY_BUFFER;

		String template = (String)DbMetadata.fkStatements.get(this.productName);

		if (template == null)
		{
			if (this.createInlineConstraints)
			{
				template = (String)DbMetadata.fkStatements.get("All-Inline");
			}
			else
			{
				template = (String)DbMetadata.fkStatements.get(GENERAL_SQL);
			}
		}
		// collects all columns from the base table mapped to the
		// defining foreign key constraing.
		// The fk name is the key.
		// to the hashtable. The entry will be a HashSet containing the column names
		// this ensures that each column will only be used once per fk definition
		// (the postgres driver returns some columns twice!)
		HashMap fkCols = new HashMap();

		// this hashmap contains the columns of the referenced table
		HashMap fkTarget = new HashMap();

		HashMap fks = new HashMap();
		HashMap updateRules = new HashMap();
		HashMap deleteRules = new HashMap();

		String name;
		String col;
		String fkCol;
		String updateRule;
		String deleteRule;
		List colList;
		//String entry;

		for (int i=0; i < count; i++)
		{
			//"FK_NAME", "COLUMN_NAME", "REFERENCES"};
			name = aFkDef.getValueAsString(i, COLUMN_IDX_FK_DEF_FK_NAME);
			col = aFkDef.getValueAsString(i, COLUMN_IDX_FK_DEF_COLUMN_NAME);
			fkCol = aFkDef.getValueAsString(i, COLUMN_IDX_FK_DEF_REFERENCE_COLUMN_NAME);
			updateRule = aFkDef.getValueAsString(i, COLUMN_IDX_FK_DEF_UPDATE_RULE);
			deleteRule = aFkDef.getValueAsString(i, COLUMN_IDX_FK_DEF_DELETE_RULE);
			colList = (List)fkCols.get(name);
			if (colList == null)
			{
				colList = new ArrayList();
				fkCols.put(name, colList);
			}
			colList.add(col);
			updateRules.put(name, updateRule);
			deleteRules.put(name, deleteRule);

			colList = (List)fkTarget.get(name);
			if (colList == null)
			{
				colList = new ArrayList();
				fkTarget.put(name, colList);
			}
			colList.add(fkCol);
		}

		// now put the real statements together
		Iterator names = fkCols.keySet().iterator();
		while (names.hasNext())
		{
			name = (String)names.next();

			String stmt = (String)fks.get(name);
			if (stmt == null)
			{
				// first time we hit this FK definition in this loop
				stmt = template;
			}
			String entry = null;
			stmt = StringUtil.replace(stmt, TABLE_NAME_PLACEHOLDER, (tableNameToUse == null ? aTable : tableNameToUse));
			stmt = StringUtil.replace(stmt, FK_NAME_PLACEHOLDER, name);
			colList = (List)fkCols.get(name);
			entry = StringUtil.listToString(colList, ',');
			stmt = StringUtil.replace(stmt, COLUMNLIST_PLACEHOLDER, entry);
			String rule = (String)updateRules.get(name);
			stmt = StringUtil.replace(stmt, FK_UPDATE_RULE, " ON UPDATE " + rule);
			rule = (String)deleteRules.get(name);
			if (this.isOracle())
			{
				// Oracle does not allow ON DELETE RESTRICT, so we'll have to
				// remove the placeholder completely
				if ("restrict".equalsIgnoreCase(rule))
				{
					stmt = StringUtil.replace(stmt, FK_DELETE_RULE, StringUtil.EMPTY_STRING);
				}
				else
				{
					stmt = StringUtil.replace(stmt, FK_DELETE_RULE, " ON DELETE " + rule);
				}
			}
			else
			{
				stmt = StringUtil.replace(stmt, FK_DELETE_RULE, " ON DELETE " + rule);
			}

			colList = (List)fkTarget.get(name);
			
			Iterator itr = colList.iterator();
			StrBuffer colListBuffer = new StrBuffer(30);
			String targetTable = null;
			boolean first = true;
			//while (tok.hasMoreTokens())
			while (itr.hasNext())
			{
				col = (String)itr.next();//tok.nextToken();
				int pos = col.lastIndexOf('.');
				if (targetTable == null)
				{
					targetTable = col.substring(0, pos);
				}
				if (!first)
				{
					colListBuffer.append(',');
				}
				else
				{
					first = false;
				}
				colListBuffer.append(col.substring(pos + 1));
			}
			stmt = StringUtil.replace(stmt, FK_TARGET_TABLE_PLACEHOLDER, targetTable);
			stmt = StringUtil.replace(stmt, FK_TARGET_COLUMNS_PLACEHOLDER, colListBuffer.toString());
			fks.put(name, stmt.trim());
		}
		StrBuffer fk = new StrBuffer();

		Iterator values = fks.values().iterator();
		while (values.hasNext())
		{
			if (this.createInlineConstraints)
			{
				fk.append("   ,");
				fk.append((String)values.next());
				fk.append("\n");
			}
			else
			{
				fk.append((String)values.next());
				fk.append(";\n\n");
			}
		}
		return fk;
	}

	/**
	 * 	Build the SQL statement to create an Index on the given table.
	 * 	@param aTable - The table name for which the index should be constructed
	 * 	@param indexName - The name of the Index
	 * 	@param unique - Should the index be unique
	 *  @param columnList - The columns that should build the index
	 */
	public String buildIndexSource(TableIdentifier aTable, String indexName, boolean unique, String[] columnList)
	{
		return this.indexReader.buildCreateIndexSql(aTable, indexName, unique, columnList);
	}

	/**	The column index for the DataStore returned by getTableGrants() which contains the object's name */
	public static final int COLUMN_IDX_TABLE_GRANTS_OBJECT_NAME = 0;
	/**	The column index for the DataStore returned by getTableGrants() which contains the name of the user which granted the access (GRANTOR) */
	public static final int COLUMN_IDX_TABLE_GRANTS_GRANTOR = 1;
	/**	The column index for the DataStore returned by getTableGrants() which contains the name of the user to which the privilege was granted */
	public static final int COLUMN_IDX_TABLE_GRANTS_GRANTEE = 2;
	/**	The column index for the DataStore returned by getTableGrants() which contains the privilege's name (SELECT, UPDATE etc) */
	public static final int COLUMN_IDX_TABLE_GRANTS_PRIV = 3;
	/** The column index for th DataStore returned by getTableGrants() which contains the information if the GRANTEE may grant the privilege to other users */
	public static final int COLUMN_IDX_TABLE_GRANTS_GRANTABLE = 4;

	/**
	 *	Return a String to recreate the GRANTs given for the passed table.
	 *
	 *	Some JDBC drivers return all GRANT privileges separately even if the original
	 *  GRANT was a GRANT ALL ON object TO user.
	 *
	 *	The COLUMN_IDX_TABLE_GRANTS_xxx constants should be used to access the DataStore's columns.
	 *
	 *	@return a DataStore which contains the grant information.
	 */
	public DataStore getTableGrants(TableIdentifier table)
	{
		String[] columns = new String[] { "TABLENAME", "GRANTOR", "GRANTEE", "PRIVILEGE", "GRANTABLE" };
		int[] colTypes = new int[] { Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR };
		DataStore result = new DataStore(columns, colTypes);
		ResultSet rs = null;
		try
		{
			TableIdentifier tbl = table.createCopy();
			tbl.adjustCase(this.dbConnection);
			rs = this.metaData.getTablePrivileges(tbl.getCatalog(), tbl.getSchema(), tbl.getTableName());
			while (rs.next())
			{
				int row = result.addRow();
				result.setValue(row, COLUMN_IDX_TABLE_GRANTS_OBJECT_NAME, rs.getString(3));
				result.setValue(row, COLUMN_IDX_TABLE_GRANTS_GRANTOR, rs.getString(4));
				result.setValue(row, COLUMN_IDX_TABLE_GRANTS_GRANTEE, rs.getString(5));
				result.setValue(row, COLUMN_IDX_TABLE_GRANTS_PRIV, rs.getString(6));
				result.setValue(row, COLUMN_IDX_TABLE_GRANTS_GRANTABLE, rs.getString(7));
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("DbMetadata.getTableGrants()", "Error when retrieving table privileges",e);
			result.reset();
		}
		finally
		{
			try { rs.close(); } catch (Throwable th) {}
		}
		return result;
	}

	/**
	 *	Creates an SQL Statement which can be used to re-create the GRANTs on the
	 *  given table.
	 *
	 *	@return SQL script to GRANT access to the table.
	 */
	public StrBuffer getTableGrantSource(TableIdentifier table)
	{
		DataStore ds = this.getTableGrants(table);
		StrBuffer result = new StrBuffer(200);
		int count = ds.getRowCount();

		// as several grants to several users can be made, we need to collect them
		// first, in order to be able to build the complete statements
		HashMap grants = new HashMap(count);
		for (int i=0; i < count; i++)
		{
			String grantee = ds.getValueAsString(i, COLUMN_IDX_TABLE_GRANTS_GRANTEE);
			String priv = ds.getValueAsString(i, COLUMN_IDX_TABLE_GRANTS_PRIV);
			StrBuffer privs;
			if (!grants.containsKey(grantee))
			{
				privs = new StrBuffer(priv);
				grants.put(grantee, privs);
			}
			else
			{
				privs = (StrBuffer)grants.get(grantee);
				if (privs == null) privs = new StrBuffer();
				privs.append(", ");
				privs.append(priv);
			}
		}
		Set entries = grants.entrySet();
		Iterator itr = entries.iterator();
		while (itr.hasNext())
		{
			Entry entry = (Entry)itr.next();
			String grantee = (String)entry.getKey();
			StrBuffer privs = (StrBuffer)entry.getValue();
			result.append("GRANT ");
			result.append(privs);
			result.append(" ON ");
			result.append(table.getTableExpression(this.dbConnection));
			result.append(" TO ");
			result.append(grantee);
			result.append(";\n");
		}
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

	/**
	 * With v1.8 of HSQLDB the tables that list table and view
	 * information, are stored in the INFORMATION_SCHEMA schema.
	 * Although the table names are the same, prior to 1.8 you
	 * cannot use the schema, so it needs to be removed
	 */
	private String adjustHsqlQuery(String query)
	{
		if (!this.isHsql) return query;
		if (this.dbVersion.startsWith("1.8")) return query;

		Pattern p = Pattern.compile("\\sINFORMATION_SCHEMA\\.", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(query);
		return m.replaceAll(" ");
	}

	public String getIndexSqlTemplate()
	{
		return getSqlTemplate(DbMetadata.idxStatements);
	}
	
	private String getSqlTemplate(HashMap aMap)
	{
		String template = (String)aMap.get(this.productName);
		if (template == null)
		{
			template = (String)aMap.get(GENERAL_SQL);
		}
		return template;
	}

	public void propertyChange(java.beans.PropertyChangeEvent evt)
	{
		String prop = evt.getPropertyName();
		if ("workbench.sql.enable_dbms_output".equals(prop))
		{
			boolean enable = Settings.getInstance().getEnableDbmsOutput();
			if (enable)
			{
				this.enableOutput();
			}
			else
			{
				this.disableOutput();
			}
		}
	}

}
