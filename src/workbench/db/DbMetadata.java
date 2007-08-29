/*
 * DbMetadata.java
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

import java.beans.PropertyChangeListener;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import workbench.db.derby.DerbyConstraintReader;
import workbench.db.derby.DerbySynonymReader;
import workbench.db.firebird.FirebirdProcedureReader;
import workbench.db.firstsql.FirstSqlMetadata;
import workbench.db.hsqldb.HsqlSequenceReader;
import workbench.db.ibm.Db2SequenceReader;
import workbench.db.ibm.Db2SynonymReader;
import workbench.db.ingres.IngresMetadata;
import workbench.db.mckoi.McKoiMetadata;
import workbench.db.mssql.SqlServerConstraintReader;
import workbench.db.mssql.SqlServerProcedureReader;
import workbench.db.mysql.EnumReader;
import workbench.db.mysql.MySqlProcedureReader;
import workbench.db.oracle.DbmsOutput;
import workbench.db.oracle.OracleConstraintReader;
import workbench.db.oracle.OracleIndexReader;
import workbench.db.oracle.OracleMetadata;
import workbench.db.oracle.OracleProcedureReader;
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
import workbench.db.hsqldb.HsqlConstraintReader;
import workbench.db.firebird.FirebirdConstraintReader;
import workbench.db.h2database.H2ConstraintReader;
import workbench.db.h2database.H2SequenceReader;

/**
 * Retrieve meta data information from the database.
 * This class returns more information than the generic JDBC DatabaseMetadata.
 * 
 *  @author  support@sql-workbench.net
 */
public class DbMetadata
	implements PropertyChangeListener
{
	public static final String MVIEW_NAME = "MATERIALIZED VIEW";
	private String schemaTerm;
	private String catalogTerm;
	private String productName;
	private String dbId;

	protected MetaDataSqlManager metaSqlMgr;
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

	private boolean isOracle;
	private boolean isPostgres;
	private boolean isFirstSql;
	private boolean isHsql;
	private boolean isFirebird;
	private boolean isSqlServer;
	private boolean isMySql;
	private boolean isCloudscape;
	private boolean isApacheDerby;
	private boolean isExcel; 
	private boolean isAccess;
	
	private boolean createInlineConstraints;
	private boolean useNullKeyword = true;
	private boolean fixOracleDateBug = false;
	private boolean columnsListInViewDefinitionAllowed = true;
	
	private String quoteCharacter;
	private String dbVersion;
	private SqlKeywordHandler keywordHandler;
	
	private static final String SELECT_INTO_PG = "(?i)(?s)SELECT.*INTO\\p{Print}*\\s*FROM.*";
	private static final String SELECT_INTO_INFORMIX = "(?i)(?s)SELECT.*FROM.*INTO\\s*\\p{Print}*";
	private Pattern selectIntoPattern = null;

	private String tableTypeName;

	private String[] tableTypesTable; 
	private String[] tableTypesSelectable;
	private Set<String> objectsWithData = null;
	private List schemasToIgnore;
	private List catalogsToIgnore;
	
	private DbSettings dbSettings;
	
	public DbMetadata(WbConnection aConnection)
		throws SQLException
	{
		Settings settings = Settings.getInstance();
		this.dbConnection = aConnection;
		this.metaData = aConnection.getSqlConnection().getMetaData();

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
			this.oracleMetaData = new OracleMetadata(this.dbConnection);
			this.constraintReader = new OracleConstraintReader();
			this.synonymReader = new OracleSynonymReader();

			// register with the Settings Object to be notified for
			// changes to the "enable dbms output" property
			settings.addPropertyChangeListener(this, "workbench.sql.enable_dbms_output");
			
			this.sequenceReader = this.oracleMetaData;
			this.procedureReader = new OracleProcedureReader(this.dbConnection);
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
			this.procedureReader = new PostgresProcedureReader(this.dbConnection);
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
			this.procedureReader = new FirebirdProcedureReader(this.dbConnection);
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
				this.procedureReader = new SqlServerProcedureReader(this.dbConnection);
			}
		}
		else if (productLower.indexOf("db2") > -1)
		{
			this.synonymReader = new Db2SynonymReader();
			this.sequenceReader = new Db2SequenceReader(this.dbConnection);
		}
		else if (productLower.indexOf("adaptive server") > -1) 
		{
			// this covers adaptive server Enterprise and Anywhere
			this.constraintReader = new ASAConstraintReader();
		}
		else if (productLower.indexOf("mysql") > -1)
		{
			this.procedureReader = new MySqlProcedureReader(this.dbConnection);
			this.isMySql = true;
		}
		else if (productLower.indexOf("informix") > -1)
		{
			this.selectIntoPattern = Pattern.compile(SELECT_INTO_INFORMIX);
		}
		else if (productLower.indexOf("cloudscape") > -1)
		{
			this.isCloudscape = true;
			this.constraintReader = new DerbyConstraintReader();
		}
		else if (productLower.indexOf("derby") > -1)
		{
			this.isApacheDerby = true;
			this.constraintReader = new DerbyConstraintReader();
			this.synonymReader = new DerbySynonymReader(this);
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
		else if (productLower.indexOf("excel") > -1)
		{
			this.isExcel = true;
		}
		else if (productLower.indexOf("access") > -1)
		{
			this.isAccess = true;
		}
		else if (productLower.equals("h2"))
		{
			this.sequenceReader = new H2SequenceReader(this.dbConnection.getSqlConnection());
			this.constraintReader = new H2ConstraintReader();
		}

		// if the DBMS does not need a specific ProcedureReader
		// we use the default implementation
		if (this.procedureReader == null)
		{
			this.procedureReader = new JdbcProcedureReader(this.dbConnection);
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
		if (StringUtil.isEmptyString(quoteCharacter)) this.quoteCharacter = "\"";

		try
		{
			this.dbVersion = this.metaData.getDatabaseProductVersion();
		}
		catch (Exception e)
		{
			LogMgr.logWarning("DbMetadata.<init>", "errro calling getDatabaseProductVersion()", e);
		}

		this.dbSettings = new DbSettings(this.getDbId(), this.productName);
		this.createInlineConstraints = settings.getServersWithInlineConstraints().contains(productName);
		this.useNullKeyword = !settings.getServersWithNoNullKeywords().contains(this.getDbId());
		
		this.metaSqlMgr = new MetaDataSqlManager(this.getProductName());

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

		tableTypeName = settings.getProperty("workbench.db.basetype.table." + this.getDbId(), "TABLE");
		tableTypesTable = new String[] {tableTypeName};
		
		// The tableTypesSelectable array will be used
		// to fill the completion cache. In that case 
		// we do not want system tables included (which 
		// is done for the objectsWithData as that 
		// drives the "Data" tab in the DbExplorer)
		Set<String> types = getObjectsWithData();
		List<String> realTypes = new ArrayList<String>(types.size());
		
		Iterator itr = types.iterator();
		for (String s : types)
		{
			if (s.toUpperCase().indexOf("SYSTEM") == -1)
			{
				realTypes.add(s);
			}
		}
		tableTypesSelectable = new String[realTypes.size()];
		int i = 0;
		for (String s : realTypes)
		{
			tableTypesSelectable[i++] = s.toUpperCase();
		}
	}

	public String getTableTypeName() { return tableTypeName; }
	public String getMViewTypeName() 
	{
		return MVIEW_NAME;
	}
	
	public String getViewTypeName() 
	{ 
		return "VIEW"; 
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
		return objectsWithData.contains(type.toLowerCase());
	}

	private Set<String> getObjectsWithData()
	{
		if (this.objectsWithData == null)
		{
			String keyPrefix = "workbench.db.objecttype.selectable.";
			String defValue = Settings.getInstance().getProperty(keyPrefix + "default", null);
			String types = Settings.getInstance().getProperty(keyPrefix + getDbId(), defValue);
			
			objectsWithData = new HashSet<String>(7);
			
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
		}
		return objectsWithData;
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

	public DbSettings getDbSettings() { return this.dbSettings; }
	
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
	 * 
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

	/**
	 * If a DDLFilter is registered for the current DBMS, this
	 * method will replace all "problematic" characters in the 
	 * SQL string, and will return a String that the DBMS will
	 * understand. 
	 * Currently this is only implemented for PostgreSQL to 
	 * mimic pgsql's $$ quoting for stored procedures
	 * 
	 * @see workbench.db.postgres.PostgresDDLFilter
	 */
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
				// The current schema can be changed in Oracle using ALTER SESSION
				// in that case the current user is still the one used to log-in
				// but the current schema is different, and we do need to qualify
				// objects with the schema. 
				return !getCurrentSchema().equalsIgnoreCase(tblSchema);
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
		if (this.isAccess) return true;
		if (!this.supportsCatalogs()) return false;
		String cat = table.getCatalog();
		if (StringUtil.isEmptyString(cat)) return false;
		String currentCat = getCurrentCatalog();
		
		if (this.isExcel)
		{
			// Excel puts the directory into the catalog
			// so we need to normalize the directory name
			File c1 = new File(cat);
			File c2 = new File(currentCat);
			if (c1.equals(c2)) return false;
		}
		else
		{
			if (StringUtil.isEmptyString(currentCat)) return false;
		}
		return !cat.equalsIgnoreCase(currentCat);
	}
	
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
				catalogsToIgnore = StringUtil.stringToList(cats, ",");
			}
			else
			{
				 catalogsToIgnore = Collections.EMPTY_LIST;
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

	public Set<String> getDbFunctions()
	{
		Set<String> dbFunctions = new HashSet<String>();
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
			
			// Add Standard ANSI SQL Functions
			this.addStringList(dbFunctions, Settings.getInstance().getProperty("workbench.db.syntax.functions", "COUNT,AVG,SUM,MAX,MIN"));
			
			// Add additional DB specific functions
			this.addStringList(dbFunctions, Settings.getInstance().getProperty("workbench.db." + getDbId() + ".syntax.functions", null));
		}
		catch (Exception e)
		{
			LogMgr.logError("DbMetadata.getDbFunctions()", "Error retrieving function list from DB", e);
		}
		return dbFunctions;
	}

	private void addStringList(Set<String> target, String list)
	{
		if (list == null) return;
		List<String> tokens = StringUtil.stringToList(list, ",", true, true, false);
		Iterator itr = tokens.iterator();
		while (itr.hasNext())
		{
			String keyword = (String)itr.next();
			target.add(keyword.toUpperCase().trim());
		}
	}

	/**
	 * Returns the type of the passed TableIdentifier. This could 
	 * be VIEW, TABLE, SYNONYM, ...
	 * If the JDBC driver does not return the object through the getTables()
	 * method, null is returned, otherwise the value reported in TABLE_TYPE
	 * If there is more than object with the same name but different types
	 * (is there a DB that supports that???) than the first object found 
   * will be returned.
	 * @see #getTables(String, String, String, String[])
	 */
	public String getObjectType(TableIdentifier table)
	{
		String type = null;
		try
		{
			TableIdentifier tbl = table.createCopy();
			tbl.adjustCase(this.dbConnection);
			DataStore ds = getTables(tbl.getCatalog(), tbl.getSchema(), tbl.getTableName(), null);
			if (ds.getRowCount() > 0)
			{
				type = ds.getValueAsString(0, COLUMN_IDX_TABLE_LIST_TYPE);
			}
		}
		catch (Exception e)
		{
			type = null;
		}
		return type;
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
		GetMetaDataSql sql = metaSqlMgr.getViewSourceSql();
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
		
		if (StringUtil.isEmptyString(source)) return StringUtil.EMPTY_STRING;

		StringBuilder result = new StringBuilder(source.length() + 100);

		String lineEnding = Settings.getInstance().getInternalEditorLineEnding();
		String verb = SqlUtil.getSqlVerb(source);
		
		// ThinkSQL and DB2 return the full CREATE VIEW statement
		if (verb.equalsIgnoreCase("CREATE"))
		{
			String type = SqlUtil.getCreateType(source);
			result.append("DROP ");
			result.append(type);
			result.append(' ');
			result.append(view.getTableName());
			result.append(';');
			result.append(lineEnding);
			result.append(lineEnding);
			result.append(source);
			if (this.dbSettings.ddlNeedsCommit())
			{
				result.append(lineEnding);
				result.append(lineEnding);
				result.append("COMMIT;");
			}
			return result.toString();
		}

		result.append(generateCreateObject(includeDrop, view.getType(), view.getTableName()));

		if (columnsListInViewDefinitionAllowed && !MVIEW_NAME.equalsIgnoreCase(view.getType()))
		{
			result.append(lineEnding + "(" + lineEnding);
			int rows = viewTableDefinition.getRowCount();
			for (int i=0; i < rows; i++)
			{
				String colName = viewTableDefinition.getValueAsString(i, DbMetadata.COLUMN_IDX_TABLE_DEFINITION_COL_NAME);
				result.append("  ");
				result.append(quoteObjectname(colName));
				if (i < rows - 1)
				{
					result.append(',');
					result.append(lineEnding);
				}
			}
			result.append(lineEnding + ")");
		}
		
		result.append(lineEnding + "AS " + lineEnding);
		result.append(source);
		result.append(lineEnding);
		
		// Oracle and MS SQL Server support materialized views. For those
		// the index definitions are of interest as well.
		DataStore indexInfo = this.getTableIndexInformation(view);
		if (indexInfo.getRowCount() > 0)
		{
			StringBuilder idx = this.indexReader.getIndexSource(view, indexInfo, null);
			if (idx.length() > 0)
			{
				result.append(lineEnding);
				result.append(lineEnding);
				result.append(idx);
				result.append(lineEnding);
			}
		}
		
		if (this.dbSettings.ddlNeedsCommit())
		{
			result.append("COMMIT;");
		}
		return result.toString();
	}

	/**
	 *	Return the source of a view definition as it is stored in the database.
	 *	Usually (depending on how the meta data is stored in the database) the DBMS
	 *	only stores the underlying SELECT statement, and that will be returned by this method.
	 *	To create a complete SQL to re-create a view, use {@link #getExtendedViewSource(TableIdentifier, DataStore, boolean)}
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
			GetMetaDataSql sql = metaSqlMgr.getViewSourceSql();
			if (sql == null) return StringUtil.EMPTY_STRING;
			TableIdentifier tbl = viewId.createCopy();
			tbl.adjustCase(this.dbConnection);
			sql.setSchema(tbl.getSchema());
			sql.setObjectName(tbl.getTableName());
			sql.setCatalog(tbl.getCatalog());
			stmt = this.dbConnection.createStatementForQuery();
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

	private StringBuilder generateCreateObject(boolean includeDrop, String type, String name)
	{
		StringBuilder result = new StringBuilder();
		boolean replaced = false;

		String prefix = "workbench.db.";
		String suffix = "." + type.toLowerCase() + ".sql." + this.getDbId();

		String replace = Settings.getInstance().getProperty(prefix + "replace" + suffix, null);
		if (replace != null)
		{
			replace = StringUtil.replace(replace, "%name%", quoteObjectname(name));
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
				result.append(' ');
				result.append(quoteObjectname(name));
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
				drop = StringUtil.replace(drop, "%name%", quoteObjectname(name));
				result.append(drop);
			}
			result.append('\n');
		}

		if (!replaced)
		{
			String create = Settings.getInstance().getProperty(prefix + "create" + suffix, null);
			if (create == null)
			{
				result.append("CREATE ");
				result.append(type.toUpperCase());
				result.append(' ');
				result.append(quoteObjectname(name));
			}
			else
			{
				create = StringUtil.replace(create, "%name%", quoteObjectname(name));
				result.append(create);
			}
		}
		return result;
	}

	public String getProcedureSource(String aCatalog, String aSchema, String aProcname, int type)
	{
		try
		{
			ProcedureDefinition def = new ProcedureDefinition(aCatalog, aSchema, aProcname, type);
			readProcedureSource(def);
			return def.getSource();
		}
		catch (NoConfigException e)
		{
			SourceStatementsHelp help = new SourceStatementsHelp();
			return help.explainMissingProcSourceSql(this.getProductName());
		}
	}	
	
	public void readProcedureSource(ProcedureDefinition def)
		throws NoConfigException
	{
		if (procedureReader != null)
		{
			this.procedureReader.readProcedureSource(def);
		}
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
	
	public Collection<String> getSqlKeywords()
	{
		if (this.keywordHandler == null) this.initKeywordHandler();
		return this.keywordHandler.getSqlKeywords();
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
	public String quoteObjectname(String aName, boolean quoteAlways)
	{
		if (aName == null) return null;
		if (aName.length() == 0) return aName;
		
		// already quoted?
		if (aName.startsWith("\"")) return aName;

		if (this.dbSettings.neverQuoteObjects()) return StringUtil.trimQuotes(aName);

		try
		{
			boolean needQuote = quoteAlways;

			if (!needQuote && !this.storesMixedCaseIdentifiers())
			{
				if (this.storesUpperCaseIdentifiers() && !StringUtil.isUpperCase(aName))
				{
					needQuote = true;
				}
				else if (this.storesLowerCaseIdentifiers() && !StringUtil.isLowerCase(aName))
				{
					needQuote = true;
				}
			}
			
			if (needQuote || isKeyword(aName))
			{
				StringBuilder result = new StringBuilder(aName.length() + 4);
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
		// as a space, $, digits at the beginning etc
		return SqlUtil.quoteObjectname(aName);
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
	public String adjustSchemaNameCase(String schema)
	{
		if (schema == null) return null;
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
		if (name.indexOf("\"") > -1) return name.trim();
		
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
	 * Returns the current schema. 
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
	 * The column index of the column in the DataStore returned by getTables()
	 * the stores the table's name
	 */
	public final static int COLUMN_IDX_TABLE_LIST_NAME = 0;

	/**
	 * The column index of the column in the DataStore returned by getTables()
	 * that stores the table's type. The available types can be retrieved
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

	public String getTableType(TableIdentifier table)
		throws SQLException
	{
		TableIdentifier tbl = table.createCopy();
		tbl.adjustCase(this.dbConnection);
		DataStore ds = getTables(tbl.getCatalog(), tbl.getSchema(), tbl.getTableName(), null);
		if (ds == null) return this.tableTypeName;
		if (ds.getRowCount() != 1) return null;
		return ds.getValueAsString(0, COLUMN_IDX_TABLE_LIST_TYPE);
	}
	
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
		
		boolean sequencesReturned = false;
		boolean checkOracleInternalSynonyms = (isOracle && typeIncluded("SYNONYM", types));
		boolean checkOracleSnapshots = (isOracle && Settings.getInstance().getBoolProperty("workbench.db.oracle.detectsnapshots", true) && typeIncluded("TABLE", types));
		boolean checkSyns = typeIncluded("SYNONYM", types);
		boolean synRetrieved = false;
		
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
		
		String excludeTablesRegex = Settings.getInstance().getProperty("workbench.db." + getDbId() + ".exclude.tables", null);
		Pattern excludeTablePattern = null;
		if (excludeTablesRegex != null && typeIncluded("TABLE", types))
		{
			try
			{
				excludeTablePattern = Pattern.compile(excludeTablesRegex);
			}
			catch (Exception e)
			{
				LogMgr.logError("DbMetadata.getTables()", "Invalid RegEx for excluding tables. RegEx '" + excludeTablesRegex + "' ignored", e);
				excludeTablePattern = null;
			}
			LogMgr.logInfo("DbMetadata.getTables()", "Excluding tables that match the following regex: " + excludeTablesRegex);
		}

		if (isPostgres && types == null)
		{
			// The current PG drivers to not adhere to the JDBC javadocs
			// and return nothing when passing null for the types
			// so we retrieve all possible types, and pass them 
			// as this is the meaning of "null" for the types parameter
			Collection<String> typeList = this.getTableTypes();
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
			tableRs = this.metaData.getTables(StringUtil.trimQuotes(aCatalog), StringUtil.trimQuotes(aSchema), StringUtil.trimQuotes(tables), types);
			if (tableRs == null)
			{
				LogMgr.logError("DbMetadata.getTables()", "Driver returned a NULL ResultSet from getTables()",null);
				return result;
			}
			
			while (tableRs.next())
			{
				String cat = tableRs.getString(1);
				String schem = tableRs.getString(2);
				String name = tableRs.getString(3);
				String ttype = tableRs.getString(4);
				if (name == null) continue;

				// filter out "internal" synonyms for Oracle
				if (checkOracleInternalSynonyms)
				{
					if (name.indexOf('/') > -1) continue;
					if (synPattern != null)
					{
						Matcher m = synPattern.matcher(name);
						if (m.matches()) continue;
					}
				}
			
				// prevent duplicate retrieval of SYNONYMS if the driver
				// returns them already, but the Settings have enabled
				// Synonym retrieval as well
				// (e.g. because an upgraded Driver now returns the synonyms)
				if (checkSyns && !synRetrieved && "SYNONYM".equals(ttype))
				{
					synRetrieved = true;
				}
				
				if (excludeTablePattern != null && ttype.equalsIgnoreCase("TABLE"))
				{
					Matcher m = excludeTablePattern.matcher(name);
					if (m.matches()) continue;
				}
				
				if (hideIndexes && isIndexType(ttype)) continue;
				
				if (checkOracleSnapshots)
				{
					StringBuilder t = new StringBuilder(30);
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
			List<String> seq = this.sequenceReader.getSequenceList(aSchema);
			for (String seqName : seq)
			{
				int row = result.addRow();

				result.setValue(row, COLUMN_IDX_TABLE_LIST_NAME, seqName);
				result.setValue(row, COLUMN_IDX_TABLE_LIST_TYPE, "SEQUENCE");
				result.setValue(row, COLUMN_IDX_TABLE_LIST_CATALOG, null);
				result.setValue(row, COLUMN_IDX_TABLE_LIST_SCHEMA, aSchema);
				result.setValue(row, COLUMN_IDX_TABLE_LIST_REMARKS, null);
			}
		}

		boolean retrieveSyns = (this.synonymReader != null && Settings.getInstance().getBoolProperty("workbench.db." + this.getDbId() + ".retrieve_synonyms", false));
		if (retrieveSyns && typeIncluded("SYNONYM", types) && !synRetrieved)
		{
			LogMgr.logDebug("DbMetadata.getTables()", "Retrieving synonyms...");
			List<String> syns = this.synonymReader.getSynonymList(this.dbConnection.getSqlConnection(), aSchema);
			for (String synName : syns)
			{
				int row = result.addRow();

				result.setValue(row, COLUMN_IDX_TABLE_LIST_NAME, synName);
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
			if (types[i].equals("*")) return true;
			if (type.equalsIgnoreCase(types[i])) return true;
		}
		return false;
	}

	/**
	 * Check if the given table exists in the database
	 */
	public boolean tableExists(TableIdentifier aTable)
	{
		return objectExists(aTable, tableTypesTable);
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
		TableIdentifier tbl = aTable.createCopy();
		try
		{
      tbl.adjustCase(this.dbConnection);
			String c = tbl.getRawCatalog();
			String s = tbl.getRawSchema();
			String t = tbl.getRawTableName();
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
	
	protected boolean supportsMixedCaseQuotedIdentifiers()
	{
		try
		{
			return this.metaData.supportsMixedCaseQuotedIdentifiers();
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
			
			return  mixed || (upper && lower);
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

	public DataStore getProcedureColumns(String aCatalog, String aSchema, String aProcname)
		throws SQLException
	{
		return this.procedureReader.getProcedureColumns(aCatalog, aSchema, aProcname);
	}

	public boolean procedureExists(ProcedureDefinition def)
	{
		return procedureReader.procedureExists(def.getCatalog(), def.getSchema(), def.getProcedureName(), def.getResultType());
	}
	
	/**
	 * Return a list of stored procedures that are available
	 * in the database. This call is delegated to the
	 * currently defined {@link workbench.db.ProcedureReader}
	 * If no DBMS specific reader is used, this is the {@link workbench.db.JdbcProcedureReader}
	 * 
	 * @return a DataStore with the list of procedures.
	 */
	public DataStore getProcedures(String aCatalog, String aSchema)
		throws SQLException
	{
		return this.procedureReader.getProcedures(aCatalog, aSchema);
	}

	/**
	 * Return a list of stored procedures that are available
	 * in the database. This call is delegated to the
	 * currently defined {@link workbench.db.ProcedureReader}
	 * If no DBMS specific reader is used, this is the {@link workbench.db.JdbcProcedureReader}
	 * 
	 * @return a DataStore with the list of procedures.
	 */
	public DataStore getProceduresAndTriggers(String aCatalog, String aSchema)
		throws SQLException
	{
		DataStore ds = this.procedureReader.getProcedures(aCatalog, aSchema);
		return ds;
	}	
	
	/**
	 * Return a List of {@link workbench.db.ProcedureDefinition} objects
	 * for Oracle only one object per definition is returned (although
	 * the DbExplorer will list each function of the packages.
	 */
	public List<ProcedureDefinition> getProcedureList(String aCatalog, String aSchema)
		throws SQLException
	{
		assert(procedureReader != null);
		
		List<ProcedureDefinition> result = new LinkedList<ProcedureDefinition>();
		DataStore procs = this.procedureReader.getProcedures(aCatalog, aSchema);
		if (procs == null || procs.getRowCount() == 0) return result;
		procs.sortByColumn(ProcedureReader.COLUMN_IDX_PROC_LIST_NAME, true);
		int count = procs.getRowCount();
		Set<String> oraPackages = new HashSet<String>();
		
		for (int i = 0; i < count; i++)
		{
			String schema  = procs.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_LIST_SCHEMA);
			String cat = procs.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_LIST_CATALOG);
			String procName = procs.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_LIST_NAME);
			int type = procs.getValueAsInt(i, ProcedureReader.COLUMN_IDX_PROC_LIST_TYPE, DatabaseMetaData.procedureNoResult);
			ProcedureDefinition def = null;
			if (this.isOracle && cat != null)
			{
				// The package name for Oracle is reported in the catalog column.
				// each function/procedure of the package is listed separately,
				// but we only want to create one ProcedureDefinition for the whole package
				if (!oraPackages.contains(cat))
				{
					def = ProcedureDefinition.createOraclePackage(schema, cat);
					oraPackages.add(cat);
				}
			}
			else
			{
				def = new ProcedureDefinition(cat, schema, procName, type);
			}
			if (def != null) result.add(def);
		}
		return result;
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

	public int fixColumnType(int type)
	{
		if (this.fixOracleDateBug) 
		{
			if (type == Types.DATE) return Types.TIMESTAMP;
		}
		
		// Oracle reports TIMESTAMP WITH TIMEZONE with the numeric 
		// value -101 (which is not an official java.sql.Types value
		// TIMESTAMP WITH LOCAL TIMEZONE is reported as -102
		if (this.isOracle && (type == -101 || type == -102)) return Types.TIMESTAMP;
		
		return type;
	}
	
	/**
	 * Return the column list for the given table.
	 * @param table the table for which to retrieve the column definition
	 * @see #getTableDefinition(String, String, String, String)
	 */
	public List<ColumnIdentifier> getTableColumns(TableIdentifier table)
		throws SQLException
	{
		DataStore ds = this.getTableDefinition(table);
		return createColumnIdentifiers(ds);
	}
	
	private List<ColumnIdentifier> createColumnIdentifiers(DataStore ds)
	{
		int count = ds.getRowCount();
		List<ColumnIdentifier> result = new ArrayList<ColumnIdentifier>(count);
		for (int i=0; i < count; i++)
		{
			String col = ds.getValueAsString(i, COLUMN_IDX_TABLE_DEFINITION_COL_NAME);
			int type = ds.getValueAsInt(i, COLUMN_IDX_TABLE_DEFINITION_JAVA_SQL_TYPE, Types.OTHER);
			boolean pk = "YES".equals(ds.getValueAsString(i, COLUMN_IDX_TABLE_DEFINITION_PK_FLAG));
			ColumnIdentifier ci = new ColumnIdentifier(SqlUtil.quoteObjectname(col), fixColumnType(type), pk);
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
			result.add(ci);
		}
		return result;
	}

	/** The column index for a {@link workbench.storage.DataStore} returned
	 *  by {@link #getTableDefinition(TableIdentifier)} that holds
	 *  the column name
	 */
	public final static int COLUMN_IDX_TABLE_DEFINITION_COL_NAME = 0;

	/** The column index for a {@link workbench.storage.DataStore} returned
	 *  by {@link #getTableDefinition(TableIdentifier)} that holds
	 *  the DBMS specific data type string
	 */
	public final static int COLUMN_IDX_TABLE_DEFINITION_DATA_TYPE = 1;

	/** The column index for a {@link workbench.storage.DataStore} returned
	 *  by {@link #getTableDefinition(TableIdentifier)} that holds
	 *  the primary key flag
	 */
	public final static int COLUMN_IDX_TABLE_DEFINITION_PK_FLAG = 2;

	/** The column index for a {@link workbench.storage.DataStore} returned
	 *  by {@link #getTableDefinition(TableIdentifier)} that holds
	 *  the nullable flag
	 */
	public final static int COLUMN_IDX_TABLE_DEFINITION_NULLABLE = 3;

	/** The column index for a {@link workbench.storage.DataStore} returned
	 *  by {@link #getTableDefinition(TableIdentifier)} that holds
	 *  the default value for this column
	 */
	public final static int COLUMN_IDX_TABLE_DEFINITION_DEFAULT = 4;

	/** The column index for a {@link workbench.storage.DataStore} returned
	 *  by {@link #getTableDefinition(TableIdentifier)} that holds
	 *  the remark for this column
	 */
	public final static int COLUMN_IDX_TABLE_DEFINITION_REMARKS = 5;

	/** The column index for a {@link workbench.storage.DataStore} returned
	 *  by {@link #getTableDefinition(TableIdentifier)} that holds
	 *  the integer value of the java datatype from {@link java.sql.Types}
	 */
	public final static int COLUMN_IDX_TABLE_DEFINITION_JAVA_SQL_TYPE = 6;
	public final static int COLUMN_IDX_TABLE_DEFINITION_SIZE = 7;
	public final static int COLUMN_IDX_TABLE_DEFINITION_DIGITS = 8;
	public final static int COLUMN_IDX_TABLE_DEFINITION_POSITION = 9;

	/**
	 * Returns the definition of the given
	 * table in a {@link workbench.storage.DataStore }
	 * @return definiton of the datastore
	 * @param id The identifier of the table
	 * @throws SQLException If the table was not found or an error occurred 
	 * @see #getTableDefinition(String, String, String, String)
	 */
	public DataStore getTableDefinition(TableIdentifier id)
		throws SQLException
	{
		if (id == null) return null;
		String type = id.getType();
		if (type == null) type = tableTypeName;
		TableIdentifier tbl = id.createCopy();
		tbl.adjustCase(dbConnection);
		return this.getTableDefinition(tbl.getRawCatalog(), tbl.getRawSchema(), tbl.getRawTableName(), type);
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
	 * @throws SQLException
	 * @return A DataStore with the table definition.
	 * The individual columns should be accessed using the
	 * COLUMN_IDX_TABLE_DEFINITION_xxx constants.
	 */
	protected DataStore getTableDefinition(String aCatalog, String aSchema, String aTable, String aType)
		throws SQLException
	{
		if (aTable == null) throw new IllegalArgumentException("Tablename may not be null!");

		DataStore ds = this.createTableDefinitionDataStore();

		aCatalog = StringUtil.trimQuotes(aCatalog);
		aSchema = StringUtil.trimQuotes(aSchema);
		aTable = StringUtil.trimQuotes(aTable);

		if (aSchema == null && this.isOracle()) 
		{
			aSchema = this.getSchemaToUse();
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

		ArrayList<String> keys = new ArrayList<String>();
		if (this.dbSettings.supportsGetPrimaryKeys())
		{
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
		}
		
		boolean hasEnums = false;

		ResultSet rs = null;
		
		boolean checkOracleCharSemantics = this.isOracle && Settings.getInstance().useOracleCharSemanticsFix();
		
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

			while (rs != null && rs.next())
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
				if (this.isPostgres && (sqlType == java.sql.Types.NUMERIC || sqlType == java.sql.Types.DECIMAL))
				{
					if (size == 65535) size = 0;
					if (digits == 65531) digits = 0;
				}
				String rem = rs.getString("REMARKS");
				String def = rs.getString("COLUMN_DEF");
				if (def != null && this.dbSettings.trimDefaults())
				{
					def = def.trim();
				}
				int position = -1;
				try
				{
					position = rs.getInt("ORDINAL_POSITION");
				}
				catch (SQLException e)
				{
					LogMgr.logError("DbMetadata", "JDBC driver does not suport ORDINAL_POSITION column for getColumns()", e);
					position = -1;
				}
				
				String nul = rs.getString("IS_NULLABLE");

				String display = null;
				
				// Hack to get Oracle's VARCHAR2(xx Byte) or VARCHAR2(xxx Char) display correct
				// Our own statement to retrieve column information in OracleMetaData
				// will return the byte/char semantics in the field SQL_DATA_TYPE
				// Oracle's JDBC driver does not supply this information (because
				// the JDBC standard does not define a column for this)
				if (checkOracleCharSemantics && sqlType == Types.VARCHAR && this.oracleMetaData != null)
				{
					int byteOrChar = rs.getInt("SQL_DATA_TYPE");
					display = this.oracleMetaData.getVarcharType(typeName, size, byteOrChar);
					if (display == null)
					{
						display = SqlUtil.getSqlTypeDisplay(typeName, sqlType, size, digits);
					}
				}
				else
				{
					display = SqlUtil.getSqlTypeDisplay(typeName, sqlType, size, digits);
				}
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
	public static final int COLUMN_IDX_TABLE_INDEXLIST_TYPE = 4;
	
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
	 * Return the index information for a table as a DataStore. This is 
	 * delegated to getTableIndexList() and from the resulting collection
	 * the datastore is created.
	 * 
	 * @param table the table to get the indexes for
	 * @see #getTableIndexList(TableIdentifier)
	 */
	public DataStore getTableIndexInformation(TableIdentifier table)
	{
		String[] cols = {"INDEX_NAME", "UNIQUE", "PK", "DEFINITION", "TYPE"};
		final int types[] =   {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
		final int sizes[] =   {30, 7, 6, 40, 10};
		DataStore idxData = new DataStore(cols, types, sizes);
		if (table == null) return idxData;
		Collection<IndexDefinition> indexes = getTableIndexList(table);
		for (IndexDefinition idx : indexes)
		{
			int row = idxData.addRow();
			idxData.setValue(row, COLUMN_IDX_TABLE_INDEXLIST_INDEX_NAME, idx.getName());
			idxData.setValue(row, COLUMN_IDX_TABLE_INDEXLIST_UNIQUE_FLAG, (idx.isUnique() ? "YES" : "NO"));
			idxData.setValue(row, COLUMN_IDX_TABLE_INDEXLIST_PK_FLAG, (idx.isPrimaryKeyIndex() ? "YES" : "NO"));
			idxData.setValue(row, COLUMN_IDX_TABLE_INDEXLIST_COL_DEF, idx.getExpression());
			idxData.setValue(row, COLUMN_IDX_TABLE_INDEXLIST_TYPE, idx.getIndexType());
		}
		idxData.sortByColumn(0, true);
		return idxData;
	}
	
	/**
	 * Returns a list of indexes defined for the given table
	 * @param table the table to get the indexes for
	 */
	public Collection<IndexDefinition> getTableIndexList(TableIdentifier table)
	{
		ResultSet idxRs = null;
		TableIdentifier tbl = table.createCopy();
		tbl.adjustCase(this.dbConnection);
		
		// This will map an indexname to an IndexDefinition object
		// getIndexInfo() returns one row for each column
		HashMap<String, IndexDefinition> defs = new HashMap<String, IndexDefinition>();
		
		try
		{
			// Retrieve the name of the PK index
			String pkName = "";
			if (this.dbSettings.supportsGetPrimaryKeys())
			{
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
					pkName = "";
				}
				finally
				{
					SqlUtil.closeResult(keysRs);
				}
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
				
				IndexDefinition def = defs.get(indexName);
				if (def == null)
				{
					def = new IndexDefinition(indexName, null);
					def.setUnique(!unique);
					def.setPrimaryKeyIndex(pkName.equals(indexName));
					defs.put(indexName, def);
					Object type = idxRs.getObject("TYPE");
					def.setIndexType(dbSettings.mapIndexType(type));
				}

				def.addColumn(colName, dir);
			}
			
			this.indexReader.processIndexList(tbl, defs.values());
		}
		catch (Exception e)
		{
			LogMgr.logWarning("DbMetadata.getTableIndexInformation()", "Could not retrieve indexes!", e);
		}
		finally
		{
			try { idxRs.close(); } catch (Throwable th) {}
			this.indexReader.indexInfoProcessed();
		}
		return defs.values();
	}

	public List<TableIdentifier> getTableList(String schema, String[] types)
		throws SQLException
	{
		if (schema == null) schema = this.getCurrentSchema();
		return getTableList(null, schema, types);
	}
	
	public List<TableIdentifier> getTableList(String table, String schema)
		throws SQLException
	{
		return getTableList(table, schema, tableTypesTable);
	}

	public List<TableIdentifier> getSelectableObjectsList(String schema)
		throws SQLException
	{
		return getTableList(null, schema, tableTypesSelectable, false);
	}

	public List<TableIdentifier> getTableList(String table, String schema, String[] types)
		throws SQLException
	{
		return getTableList(table, schema, types, false);
	}
		/**
	 * Return a list of tables for the given schema
	 * if the schema is null, all tables will be returned
	 */
	public List<TableIdentifier> getTableList(String table, String schema, String[] types, boolean returnAllSchemas)
		throws SQLException
	{
		DataStore ds = getTables(null, schema, table, types);
		int count = ds.getRowCount();
		List<TableIdentifier> tables = new ArrayList<TableIdentifier>(count);
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
			tbl.setNeverAdjustCase(true);
			tbl.setType(ds.getValueAsString(i, COLUMN_IDX_TABLE_LIST_TYPE));
			tables.add(tbl);
		}
		return tables;
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
		boolean useSetCatalog = dbSettings.useSetCatalog();
		boolean clearWarnings = Settings.getInstance().getBoolProperty("workbench.db." + this.getDbId() + ".setcatalog.clearwarnings", true);
		
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
				if (clearWarnings) stmt.clearWarnings();
			}
			finally
			{
				SqlUtil.closeStatement(stmt);
			}
		}
		
		if (clearWarnings) this.dbConnection.clearWarnings();
		
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
	 * Return a list of triggers available in the given schema.
	 */
	public DataStore getTriggers(String catalog, String schema)
		throws SQLException
	{
		return getTriggers(catalog, schema, null);
	}
	
	/**
	 *	Return the list of defined triggers for the given table.
	 */
	public DataStore getTableTriggers(TableIdentifier table)
		throws SQLException
	{
		TableIdentifier tbl = table.createCopy();
		tbl.adjustCase(this.dbConnection);
		return getTriggers(tbl.getCatalog(), tbl.getSchema(), tbl.getTableName());
	}
	
	protected DataStore getTriggers(String catalog, String schema, String tableName)
		throws SQLException
	{
		final String[] cols = {"NAME", "TYPE", "EVENT"};
		final int types[] =   {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
		final int sizes[] =   {30, 30, 20};

		DataStore result = new DataStore(cols, types, sizes);
		
		GetMetaDataSql sql = metaSqlMgr.getListTriggerSql();
		if (sql == null)
		{
			return result;
		}

		sql.setSchema(schema);
		sql.setCatalog(catalog);
		sql.setObjectName(tableName);

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
	 * 
	 * @param aCatalog The catalog in which the trigger is defined. This should be null if the DBMS does not support catalogs
	 * @param aSchema The schema in which the trigger is defined. This should be null if the DBMS does not support schemas
	 * @param aTriggername
	 * @throws SQLException
	 * @return the trigger source
	 */
	public String getTriggerSource(String aCatalog, String aSchema, String aTriggername)
		throws SQLException
	{
		StringBuilder result = new StringBuilder(500);

		if ("*".equals(aCatalog)) aCatalog = null;
		if ("*".equals(aSchema)) aSchema = null;

		GetMetaDataSql sql = metaSqlMgr.getTriggerSourceSql();
		if (sql == null) return StringUtil.EMPTY_STRING;

		sql.setSchema(aSchema);
		sql.setCatalog(aCatalog);
		sql.setObjectName(aTriggername);
		Statement stmt = this.dbConnection.createStatementForQuery();
		String query = this.adjustHsqlQuery(sql.getSql());

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("DbMetadata.getTriggerSource()", "Using query=\n" + query);
		}
		
		String nl = Settings.getInstance().getInternalEditorLineEnding();
		
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
			CharSequence warn = SqlUtil.getWarnings(this.dbConnection, stmt, true);
			if (warn != null && result.length() > 0) result.append(nl + nl);
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
		
		boolean replaceNL = Settings.getInstance().getBoolProperty("workbench.db." + getDbId() + ".replacenl.triggersource", false);

		String source = result.toString();
		if (replaceNL)
		{
			source = StringUtil.replace(source, "\\n", nl);
		}
		return source;
	}

	/** Returns the list of schemas as returned by DatabaseMetadata.getSchemas()
	 * @return List
	 */
	public List<String> getSchemas()
	{
		ArrayList<String> result = new ArrayList<String>();
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

	public Collection<String> getTableTypes()
	{
		TreeSet<String> result = new TreeSet<String>();
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
	public static final int COLUMN_IDX_FK_DEF_DEFERRABLE = 5;
	public static final int COLUMN_IDX_FK_DEF_UPDATE_RULE_VALUE = 6;
	public static final int COLUMN_IDX_FK_DEF_DELETE_RULE_VALUE = 7;
	public static final int COLUMN_IDX_FK_DEF_DEFERRABLE_RULE_VALUE = 8;

	public DataStore getExportedKeys(TableIdentifier tbl)
		throws SQLException
	{
		return getRawKeyList(tbl, true);
	}

	public DataStore getImportedKeys(TableIdentifier tbl)
		throws SQLException
	{
		return getRawKeyList(tbl, false);
	}

	private DataStore getRawKeyList(TableIdentifier tbl, boolean exported)
		throws SQLException
	{
		TableIdentifier table = tbl.createCopy();
		table.adjustCase(this.dbConnection);
			
		List<TableIdentifier> l = getTableList(table.getTableName(), table.getSchema());
		
		ResultSet rs;
		if (exported)
			rs = this.metaData.getExportedKeys(table.getCatalog(), table.getSchema(), table.getTableName());
		else
			rs = this.metaData.getImportedKeys(table.getCatalog(), table.getSchema(), table.getTableName());

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
		int pos = aName.indexOf("\\000");
		if (pos > -1)
		{
			// the Postgres JDBC driver seems to have a bug here,
			// because it appends the whole FK information to the fk name!
			// the actual FK name ends at the first \000
			return aName.substring(0, pos);
		}
		return aName;
	}

	public DataStore getForeignKeys(TableIdentifier table, boolean includeNumericRuleValue)
	{
		DataStore ds = this.getKeyList(table, true, includeNumericRuleValue);
		return ds;
	}

	public DataStore getReferencedBy(TableIdentifier table)
	{
		DataStore ds = this.getKeyList(table, false, false);
		return ds;
	}

	private DataStore getKeyList(TableIdentifier tableId, boolean getOwnFk, boolean includeNumericRuleValue)
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
			cols = new String[] { "FK_NAME", "COLUMN", refColName , "UPDATE_RULE", "DELETE_RULE", "DEFERRABLE", "UPDATE_RULE_VALUE", "DELETE_RULE_VALUE", "DEFER_RULE_VALUE"};
			types = new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.INTEGER};
			sizes = new int[] {25, 10, 30, 12, 12, 15, 1, 1, 1};
		}
		else
		{
			cols = new String[] { "FK_NAME", "COLUMN", refColName , "UPDATE_RULE", "DELETE_RULE", "DEFERRABLE"};
			types = new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
			sizes = new int[] {25, 10, 30, 12, 12, 15};
		}
		DataStore ds = new DataStore(cols, types, sizes);
		if (tableId == null) return ds;
		
		ResultSet rs = null;

		try
		{
			TableIdentifier tbl = tableId.createCopy();
			tbl.adjustCase(this.dbConnection);
			
			int tableCol;
			int fkNameCol;
			int colCol;
			int fkColCol;
			int deleteActionCol;
			int updateActionCol;
			int schemaCol;

			if (getOwnFk)
			{
				rs = this.metaData.getImportedKeys(tbl.getCatalog(), tbl.getSchema(), tbl.getTableName());
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
				rs = this.metaData.getExportedKeys(tbl.getCatalog(), tbl.getSchema(), tbl.getTableName());
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
				if (!this.ignoreSchema(schema))
				{
					table = schema + "." + table;
				}
				int updateAction = rs.getInt(updateActionCol);
				String updActionDesc = this.dbSettings.getRuleDisplay(updateAction);
				int deleteAction = rs.getInt(deleteActionCol);
				String delActionDesc = this.dbSettings.getRuleDisplay(deleteAction);
				
				int deferrableCode = rs.getInt(14);
				String deferrable = this.dbSettings.getRuleDisplay(deferrableCode);
				
				int row = ds.addRow();
				ds.setValue(row, COLUMN_IDX_FK_DEF_FK_NAME, fk_name);
				ds.setValue(row, COLUMN_IDX_FK_DEF_COLUMN_NAME, col);
				ds.setValue(row, COLUMN_IDX_FK_DEF_REFERENCE_COLUMN_NAME, table + "." + fk_col);
				ds.setValue(row, COLUMN_IDX_FK_DEF_UPDATE_RULE, updActionDesc);
				ds.setValue(row, COLUMN_IDX_FK_DEF_DELETE_RULE, delActionDesc);
				ds.setValue(row, COLUMN_IDX_FK_DEF_DEFERRABLE, deferrable);
				if (includeNumericRuleValue)
				{
					ds.setValue(row, COLUMN_IDX_FK_DEF_DELETE_RULE_VALUE, new Integer(deleteAction));
					ds.setValue(row, COLUMN_IDX_FK_DEF_UPDATE_RULE_VALUE, new Integer(updateAction));
					ds.setValue(row, COLUMN_IDX_FK_DEF_DEFERRABLE_RULE_VALUE, new Integer(deferrableCode));
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("DbMetadata.getKeyList()", "Error when retrieving foreign keys", e);
			ds.reset();
		}
		finally
		{
			SqlUtil.closeResult(rs);
		}
		return ds;
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

	public boolean isTableType(String type)
	{
		for (String t : tableTypesTable)
		{
			if (t.equalsIgnoreCase(type)) return true;
		}
		return false;
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
	protected TableIdentifier getSynonymTable(String schema, String synonym)
	{
		if (this.synonymReader == null) return null;
		TableIdentifier id = null;
		try
		{
			id = this.synonymReader.getSynonymTable(this.dbConnection.getSqlConnection(), schema, synonym);
			if (id != null && id.getType() == null)
			{
				String type = getTableType(id);
				id.setType(type);
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
	public String getSynonymSource(TableIdentifier synonym)
	{
		if (this.synonymReader == null) return StringUtil.EMPTY_STRING;
		String result = null;
		TableIdentifier tbl = synonym.createCopy();
		tbl.adjustCase(dbConnection);
		try
		{
			result = this.synonymReader.getSynonymSource(this.dbConnection.getSqlConnection(), tbl.getSchema(), tbl.getTableName());
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
	 * @param table the table for which the source should be retrievedcatalog The catalog in which the table is defined. This should be null if the DBMS does not support catalogs
	 * @param includeDrop If true, a DROP TABLE statement will be included in the generated SQL script.
	 * @param includeFk if true, the foreign key constraints will be added after the CREATE TABLE
	 * @throws SQLException
	 */
	public String getTableSource(TableIdentifier table, boolean includeDrop, boolean includeFk)
		throws SQLException
	{
		if (getViewTypeName().equalsIgnoreCase(table.getType())) return getExtendedViewSource(table, includeDrop);
		List<ColumnIdentifier> cols = getTableColumns(table);
		DataStore index = this.getTableIndexInformation(table);
		TableIdentifier tbl = table.createCopy();
		tbl.adjustCase(this.dbConnection);
		DataStore fkDef = null;
		if (includeFk) fkDef = this.getForeignKeys(tbl, false);
		String source = this.getTableSource(table, cols, index, fkDef, includeDrop, null, includeFk);
		return source;
	}

	public String getTableSource(TableIdentifier table, List<ColumnIdentifier> columns, String tableNameToUse)
	{
		return getTableSource(table, columns, null, null, false, tableNameToUse, true);
	}

	public String getTableSource(TableIdentifier table, DataStore columns, DataStore aIndexDef, DataStore aFkDef, boolean includeDrop, String tableNameToUse)
	{
		List<ColumnIdentifier> cols = this.createColumnIdentifiers(columns);
		return getTableSource(table, cols, aIndexDef, aFkDef, includeDrop, tableNameToUse, true);
	}
	
	protected String getMViewSource(TableIdentifier table, List<ColumnIdentifier> columns, DataStore aIndexDef, boolean includeDrop)
	{
		StringBuilder result = new StringBuilder(250);
	
		try
		{
			result.append(getExtendedViewSource(table, includeDrop));
		}
		catch (SQLException e)
		{
			result.append(ExceptionUtil.getDisplay(e));
		}
		result.append("\n\n");
		StringBuilder indexSource = this.indexReader.getIndexSource(table, aIndexDef, table.getTableName());
		result.append(indexSource);
		if (this.dbSettings.ddlNeedsCommit())
		{
			result.append('\n');
			result.append("COMMIT;");
			result.append('\n');
		}
		return result.toString();
	}
	
	public String getTableSource(TableIdentifier table, List<ColumnIdentifier> columns, DataStore aIndexDef, DataStore aFkDef, boolean includeDrop, String tableNameToUse, boolean includeFk)
	{
		if (columns == null || columns.size() == 0) return StringUtil.EMPTY_STRING;

		if (table.getType().equals(MVIEW_NAME))
		{
			return getMViewSource(table, columns, aIndexDef, includeDrop);
		}
		
		StringBuilder result = new StringBuilder(250);

		Map<String, String> columnConstraints = this.getColumnConstraints(table);

		result.append(generateCreateObject(includeDrop, "TABLE", (tableNameToUse == null ? table.getTableName() : tableNameToUse)));
		result.append("\n(\n");

		List<String> pkCols = new LinkedList<String>();
		int maxColLength = 0;
		int maxTypeLength = 0;

		// calculate the longest column name, so that the display can be formatted
		for (ColumnIdentifier column : columns)
		{
			String colName = quoteObjectname(column.getColumnName());
			String type = column.getDbmsType();
			maxColLength = Math.max(maxColLength, colName.length());
			maxTypeLength = Math.max(maxTypeLength, type.length());
		}
		maxColLength++;
		maxTypeLength++;
		
		// Some RDBMS require the "DEFAULT" clause before the [NOT] NULL clause
		boolean defaultBeforeNull = this.dbSettings.getDefaultBeforeNull(); 
		String nullKeyword = Settings.getInstance().getProperty("workbench.db.nullkeyword." + getDbId(), "NULL");
		boolean includeCommentInTableSource = Settings.getInstance().getBoolProperty("workbench.db.colcommentinline." + this.getDbId(), false);
		
		String lineEnding = Settings.getInstance().getInternalEditorLineEnding();
		
		Iterator<ColumnIdentifier> itr = columns.iterator();
		while (itr.hasNext())
		{
			ColumnIdentifier column = itr.next();
			String colName = column.getColumnName();
			String quotedColName = quoteObjectname(colName);
			String type = column.getDbmsType();
			String def = column.getDefaultValue();

			result.append("   ");
			result.append(quotedColName);
			if (column.isPkColumn() && (!this.isFirstSql || this.isFirstSql && !type.equals("sequence")))
			{
				pkCols.add(colName.trim());
			}
			
			for (int k=0; k < maxColLength - quotedColName.length(); k++) result.append(' ');
			result.append(type);
			
			// Check if any additional keywords are coming after
			// the datatype. If yes, we fill the line with spaces
			// to align the keywords properly
			if ( !StringUtil.isEmptyString(def) || 
				   (!column.isNullable()) ||
				   (column.isNullable() && this.useNullKeyword)
					)
			{
				for (int k=0; k < maxTypeLength - type.length(); k++) result.append(' ');
			}
			

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
			else if (column.isNullable())
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

			String constraint = columnConstraints.get(colName);
			if (constraint != null && constraint.length() > 0)
			{
				result.append(' ');
				result.append(constraint);
			}
			
			if (includeCommentInTableSource && !StringUtil.isEmptyString(column.getComment()))
			{
				result.append(" COMMENT '");
				result.append(column.getComment());
				result.append('\'');
			}
			
			if (itr.hasNext()) result.append(',');
			result.append(lineEnding);
		}

		String cons = this.getTableConstraints(table, "   ");
		if (cons != null && cons.length() > 0)
		{
			result.append("   ,");
			result.append(cons);
			result.append(lineEnding);
		}
		String realTable = (tableNameToUse == null ? table.getTableName() : tableNameToUse);

		if (this.createInlineConstraints && pkCols.size() > 0)
		{
			result.append(lineEnding + "   ,PRIMARY KEY (");
			result.append(StringUtil.listToString(pkCols, ','));
			result.append(")" + lineEnding);

			if (includeFk)
			{
				StringBuilder fk = this.getFkSource(table.getTableName(), aFkDef, tableNameToUse);
				if (fk.length() > 0)
				{
					result.append(fk);
				}
			}
		}

		result.append(");" + lineEnding); // end of CREATE TABLE
		
		if (!this.createInlineConstraints && pkCols.size() > 0)
		{
			String name = this.getPkIndexName(aIndexDef);
			StringBuilder pkSource = getPkSource(realTable, pkCols, name);
			result.append(pkSource);
			result.append(lineEnding);
			result.append(lineEnding);
		}
		StringBuilder indexSource = this.indexReader.getIndexSource(table, aIndexDef, tableNameToUse);
		result.append(indexSource);
		if (!this.createInlineConstraints && includeFk) result.append(this.getFkSource(table.getTableName(), aFkDef, tableNameToUse));

		String tableComment = this.getTableCommentSql(table);
		if (!StringUtil.isEmptyString(tableComment))
		{
			result.append(lineEnding);
			result.append(tableComment);
			result.append(lineEnding);
		}

		StringBuilder colComments = this.getTableColumnCommentsSql(table, columns);
		if (colComments != null && colComments.length() > 0)
		{
			result.append(lineEnding);
			result.append(colComments);
			result.append(lineEnding);
		}

		StringBuilder grants = this.getTableGrantSource(table);
		if (grants.length() > 0)
		{
			result.append(grants);
		}
		
		if (this.dbSettings.ddlNeedsCommit())
		{
			result.append(lineEnding);
			result.append("COMMIT;");
			result.append(lineEnding);
		}

		return result.toString();
	}

	private boolean isSystemConstraintName(String name)
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
	
	public StringBuilder getPkSource(String tablename, List pkCols, String pkName)
	{
		String template = metaSqlMgr.getPrimaryKeyTemplate();
		StringBuilder result = new StringBuilder();
		if (StringUtil.isEmptyString(template)) return result;
		
		template = StringUtil.replace(template, MetaDataSqlManager.TABLE_NAME_PLACEHOLDER, tablename);
		template = StringUtil.replace(template, MetaDataSqlManager.COLUMNLIST_PLACEHOLDER, StringUtil.listToString(pkCols, ','));
		
		if (pkName == null)
		{
			if (Settings.getInstance().getAutoGeneratePKName()) pkName = "pk_" + tablename.toLowerCase();
		}
		else if (isSystemConstraintName(pkName))
		{
			pkName = null;
		}
		
		if (isKeyword(pkName)) pkName = "\"" + pkName + "\"";
		if (StringUtil.isEmptyString(pkName)) 
		{
			pkName = ""; // remove placeholder if no name is available
			template = StringUtil.replace(template, " CONSTRAINT ", ""); // remove CONSTRAINT KEYWORD if not name is available
		}

		template = StringUtil.replace(template, MetaDataSqlManager.PK_NAME_PLACEHOLDER, pkName);
		result.append(template);
		result.append(';');
		return result;
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
		if (this.constraintReader != null)
		{
			try
			{
				columnConstraints = this.constraintReader.getColumnConstraints(this.dbConnection.getSqlConnection(), table);
			}
			catch (Exception e)
			{
				if (this.isPostgres) try { this.dbConnection.rollback(); } catch (Throwable th) {}
				columnConstraints = Collections.emptyMap();
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
	public StringBuilder getTableColumnCommentsSql(TableIdentifier table, List<ColumnIdentifier> columns)
	{
		String columnStatement = metaSqlMgr.getColumnCommentSql();
		if (columnStatement == null || columnStatement.trim().length() == 0) return null;
		StringBuilder result = new StringBuilder(columns.size() * 25);
		for (ColumnIdentifier col : columns)
		{
			String column = col.getColumnName();
			String remark = col.getComment();
			if (column == null || remark == null || remark.trim().length() == 0) continue;
			try
			{
				String commentSql = columnStatement.replaceAll(MetaDataSqlManager.COMMENT_TABLE_PLACEHOLDER, table.getTableName());
				commentSql = StringUtil.replace(commentSql, MetaDataSqlManager.COMMENT_COLUMN_PLACEHOLDER, column);//, comment)comment.replaceAll(MetaDataSqlManager.COMMENT_COLUMN_PLACEHOLDER, column);
				remark = StringUtil.replace(remark, "'", "''");
				commentSql = StringUtil.replace(commentSql, MetaDataSqlManager.COMMENT_PLACEHOLDER, remark);
				result.append(commentSql);
				result.append("\n");
			}
			catch (Exception e)
			{
				LogMgr.logError("DbMetadata.getTableColumnCommentsSql()", "Error creating comments SQL for remark=" + remark, e);
			}
		}
		return result;
	}

	/**
	 * Return the SQL that is needed to re-create the comment on the given table.
	 * The syntax to be used, can be configured in the TableCommentStatements.xml file.
	 */
	public String getTableCommentSql(TableIdentifier table)
	{
		String commentStatement = metaSqlMgr.getTableCommentSql();
		if (commentStatement == null || commentStatement.trim().length() == 0) return null;

		String comment = this.getTableComment(table);
		String result = null;
		if (comment != null && comment.trim().length() > 0)
		{
			result = commentStatement.replaceAll(MetaDataSqlManager.COMMENT_TABLE_PLACEHOLDER, table.getTableName());
			result = result.replaceAll(MetaDataSqlManager.COMMENT_PLACEHOLDER, comment);
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

	public StringBuilder getFkSource(TableIdentifier table)
	{
		DataStore fkDef = this.getForeignKeys(table, false);
		return getFkSource(table.getTableName(), fkDef, null);
	}
	
	/**
	 *	Return a SQL script to re-create the Foreign key definition for the given table.
	 *
	 *	@param aTable the tablename for which the foreign keys should be created
	 *  @param aFkDef a DataStore with the FK definition as returned by #getForeignKeys()
	 *
	 *	@return a SQL statement to add the foreign key definitions to the given table
	 */
	public StringBuilder getFkSource(String aTable, DataStore aFkDef, String tableNameToUse)
	{
		if (aFkDef == null) return StringUtil.emptyBuffer();
		int count = aFkDef.getRowCount();
		if (count == 0) return StringUtil.emptyBuffer();

		String template = metaSqlMgr.getForeignKeyTemplate(this.createInlineConstraints);

		// collects all columns from the base table mapped to the
		// defining foreign key constraing.
		// The fk name is the key.
		// to the hashtable. The entry will be a HashSet containing the column names
		// this ensures that each column will only be used once per fk definition
		// (the postgres driver returns some columns twice!)
		HashMap<String, List<String>> fkCols = new HashMap<String, List<String>>();

		// this hashmap contains the columns of the referenced table
		HashMap<String, List<String>> fkTarget = new HashMap<String, List<String>>();

		HashMap<String, String> fks = new HashMap<String, String>();
		HashMap<String, String> updateRules = new HashMap<String, String>();
		HashMap<String, String> deleteRules = new HashMap<String, String>();
		HashMap<String, String> deferrable = new HashMap<String, String>();
		
		String fkname;
		String col;
		String fkCol;
		String updateRule;
		String deleteRule;
		String deferRule;
		
		for (int i=0; i < count; i++)
		{
			//"FK_NAME", "COLUMN_NAME", "REFERENCES"};
			fkname = aFkDef.getValueAsString(i, COLUMN_IDX_FK_DEF_FK_NAME);
			col = aFkDef.getValueAsString(i, COLUMN_IDX_FK_DEF_COLUMN_NAME);
			fkCol = aFkDef.getValueAsString(i, COLUMN_IDX_FK_DEF_REFERENCE_COLUMN_NAME);
			updateRule = aFkDef.getValueAsString(i, COLUMN_IDX_FK_DEF_UPDATE_RULE);
			deleteRule = aFkDef.getValueAsString(i, COLUMN_IDX_FK_DEF_DELETE_RULE);
			deferRule = aFkDef.getValueAsString(i, COLUMN_IDX_FK_DEF_DEFERRABLE);
			
			List<String> colList = fkCols.get(fkname);
			if (colList == null)
			{
				colList = new LinkedList<String>();
				fkCols.put(fkname, colList);
			}
			colList.add(col);
			updateRules.put(fkname, updateRule);
			deleteRules.put(fkname, deleteRule);
			deferrable.put(fkname, deferRule);
			
			colList = fkTarget.get(fkname);
			if (colList == null)
			{
				colList = new LinkedList<String>();
				fkTarget.put(fkname, colList);
			}
			colList.add(fkCol);
		}

		// now put the real statements together
		Iterator<Map.Entry<String, List<String>>> names = fkCols.entrySet().iterator();
		while (names.hasNext())
		{
			Map.Entry<String, List<String>> mapentry = names.next();
			fkname = mapentry.getKey();
			List<String> colList = mapentry.getValue();

			String stmt = fks.get(fkname);
			if (stmt == null)
			{
				// first time we hit this FK definition in this loop
				stmt = template;
			}
			String entry = null;
			stmt = StringUtil.replace(stmt, MetaDataSqlManager.TABLE_NAME_PLACEHOLDER, (tableNameToUse == null ? aTable : tableNameToUse));
			
			if (this.isSystemConstraintName(fkname))
			{
				stmt = StringUtil.replace(stmt, MetaDataSqlManager.FK_NAME_PLACEHOLDER, "");
				stmt = StringUtil.replace(stmt, " CONSTRAINT ", "");
			}
			else
			{
				stmt = StringUtil.replace(stmt, MetaDataSqlManager.FK_NAME_PLACEHOLDER, fkname);
			}
			
			entry = StringUtil.listToString(colList, ',');
			stmt = StringUtil.replace(stmt, MetaDataSqlManager.COLUMNLIST_PLACEHOLDER, entry);
			String rule = updateRules.get(fkname);
			stmt = StringUtil.replace(stmt, MetaDataSqlManager.FK_UPDATE_RULE, " ON UPDATE " + rule);
			rule = deleteRules.get(fkname);
			if (this.isOracle())
			{
				// Oracle does not allow ON DELETE RESTRICT, so we'll have to
				// remove the placeholder completely
				if ("restrict".equalsIgnoreCase(rule))
				{
					stmt = MetaDataSqlManager.removePlaceholder(stmt, MetaDataSqlManager.FK_DELETE_RULE, true);
				}
				else
				{
					stmt = StringUtil.replace(stmt, MetaDataSqlManager.FK_DELETE_RULE, " ON DELETE " + rule);
				}
			}
			else
			{
				stmt = StringUtil.replace(stmt, MetaDataSqlManager.FK_DELETE_RULE, " ON DELETE " + rule);
			}
			
			rule = getDeferrableVerb(deferrable.get(fkname));
			if (StringUtil.isEmptyString(rule))
			{
				stmt = MetaDataSqlManager.removePlaceholder(stmt, MetaDataSqlManager.DEFERRABLE, true);
			}
			else
			{
				stmt = StringUtil.replace(stmt, MetaDataSqlManager.DEFERRABLE, rule.trim());
			}
			
			colList = fkTarget.get(fkname);
			if (colList == null)
			{
				LogMgr.logError("DbMetadata.getFkSource()", "Retrieved a null list for constraing [" + fkname + "] but should contain a list for table [" + aTable + "]",null);
				continue;
			}
			
			Iterator itr = colList.iterator();
			StringBuilder colListBuffer = new StringBuilder(30);
			String targetTable = null;
			boolean first = true;
			//while (tok.hasMoreTokens())
			while (itr.hasNext())
			{
				col = (String)itr.next();//tok.nextToken();
				int pos = col.lastIndexOf('.');
				if (targetTable == null)
				{
					// The last element has to be the column name!
					String t = col.substring(0, pos);
					TableIdentifier tbl = new TableIdentifier(t);
					targetTable = tbl.getTableExpression(this.dbConnection);
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
			stmt = StringUtil.replace(stmt, MetaDataSqlManager.FK_TARGET_TABLE_PLACEHOLDER, targetTable);
			stmt = StringUtil.replace(stmt, MetaDataSqlManager.FK_TARGET_COLUMNS_PLACEHOLDER, colListBuffer.toString());
			fks.put(fkname, stmt.trim());
		}
		StringBuilder fk = new StringBuilder();

		String nl = Settings.getInstance().getInternalEditorLineEnding();
		
		Iterator values = fks.values().iterator();
		while (values.hasNext())
		{
			if (this.createInlineConstraints)
			{
				fk.append("   ,");
				fk.append((String)values.next());
				fk.append(nl);
			}
			else
			{
				fk.append((String)values.next());
				fk.append(';');
				fk.append(nl);fk.append(nl);
			}
		}
		return fk;
	}

	private String getDeferrableVerb(String type)
	{
		if (dbSettings.isNotDeferrable(type)) return StringUtil.EMPTY_STRING;
		return " DEFERRABLE " + type;
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

	/**
	 *	Return the GRANTs for the given table
	 *
	 *	Some JDBC drivers return all GRANT privileges separately even if the original
	 *  GRANT was a GRANT ALL ON object TO user.
	 *
	 *	@return a List with TableGrant objects.
	 */
	public Collection<TableGrant> getTableGrants(TableIdentifier table)
	{
		Collection<TableGrant> result = new HashSet<TableGrant>();
		ResultSet rs = null;
		try
		{
			TableIdentifier tbl = table.createCopy();
			tbl.adjustCase(this.dbConnection);
			rs = this.metaData.getTablePrivileges(tbl.getCatalog(), tbl.getSchema(), tbl.getTableName());
			while (rs.next())
			{
				String from = rs.getString(4);
				String to = rs.getString(5);
				String what = rs.getString(6);
				boolean grantable = StringUtil.stringToBool(rs.getString(7));
				TableGrant grant = new TableGrant(to, what, grantable);
				result.add(grant);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("DbMetadata.getTableGrants()", "Error when retrieving table privileges",e);
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
	public StringBuilder getTableGrantSource(TableIdentifier table)
	{
		Collection<TableGrant> grantList = this.getTableGrants(table);
		StringBuilder result = new StringBuilder(200);
		int count = grantList.size();

		// as several grants to several users can be made, we need to collect them
		// first, in order to be able to build the complete statements
		Map<String, List<String>> grants = new HashMap<String, List<String>>(count);

		for (TableGrant grant : grantList)
		{
			String grantee = grant.getGrantee();
			String priv = grant.getPrivilege();
			if (priv == null) continue;
			List<String> privs = grants.get(grantee);
			if (privs == null)
			{
				privs = new LinkedList<String>();
				grants.put(grantee, privs);
			}
			privs.add(priv.trim());
		}
		Iterator<Entry<String, List<String>>> itr = grants.entrySet().iterator();

		String user = dbConnection.getCurrentUser();
		while (itr.hasNext())
		{
			Entry<String, List<String>> entry = itr.next();
			String grantee = entry.getKey();
			// Ignore grants to ourself
			if (user.equalsIgnoreCase(grantee)) continue;
			
			List<String> privs = entry.getValue();
			result.append("GRANT ");
			result.append(StringUtil.listToString(privs, ','));
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
