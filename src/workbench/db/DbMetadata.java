/*
 * DbMetadata.java
 *
 * Created on 16. Juli 2002, 13:09
 */

package workbench.db;

import java.io.BufferedInputStream;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import workbench.WbManager;

import workbench.db.oracle.DbmsOutput;
import workbench.db.oracle.OracleMetaData;
import workbench.db.oracle.SynonymReader;
import workbench.exception.ExceptionUtil;
import workbench.exception.WbException;
import workbench.gui.components.DataStoreTableModel;
import workbench.log.LogMgr;
import workbench.storage.DataStore;
import workbench.storage.DbDateFormatter;
import workbench.storage.SqlSyntaxFormatter;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbPersistence;

/**
 *  @author  workbench@kellerer.org
 */
public class DbMetadata
{
	public static final String PROC_RESULT_UNKNOWN = "";
	public static final String PROC_RESULT_YES = "RESULT";
	public static final String PROC_RESULT_NO = "NO RESULT";

	public static final String TABLE_NAME_PLACEHOLDER = "%tablename%";
	public static final String INDEX_NAME_PLACEHOLDER = "%indexname%";
	public static final String PK_NAME_PLACEHOLDER = "%pk_name%";
	public static final String UNIQUE_PLACEHOLDER = "%unique_key% ";
	public static final String COLUMNLIST_PLACEHOLDER = "%columnlist%";
	public static final String FK_NAME_PLACEHOLDER = "%constraintname%";
	public static final String FK_TARGET_TABLE_PLACEHOLDER = "%targettable%";
	public static final String FK_TARGET_COLUMNS_PLACEHOLDER = "%targetcolumnlist%";

	public static final String GENERAL_SQL = "All";
	private static final String LINE_TERMINATOR = "\r\n";

	private String schemaTerm;
	private String catalogTerm;
	String productName;
	private DatabaseMetaData metaData;
	//private List tableListColumns;
	private WbConnection dbConnection;
	private OracleMetaData oracleMetaData;
	
	private static List serversWhichNeedReconnect = Collections.EMPTY_LIST;
	private static List caseSensitiveServers = Collections.EMPTY_LIST;
	private static List ddlNeedsCommitServers = Collections.EMPTY_LIST;
	private static List serverNeedsJdbcCommit = Collections.EMPTY_LIST;
	// These Hashmaps contains templates
	// for object creation
	private HashMap procSourceSql;
	private HashMap viewSourceSql;
	private HashMap triggerSourceSql;
	private HashMap triggerList;
	private HashMap pkStatements;
	private HashMap idxStatements;
	private HashMap fkStatements;

	//private HashMap dateLiteralFormatter;
	private DbmsOutput oraOutput;
  private boolean needsReconnect;
  private boolean caseSensitive;
	private boolean useJdbcCommit;
	private boolean ddlNeedsCommit;
  private boolean isOracle;
	private boolean isPostgres;
	private boolean isHsql;
	private boolean isFirebird;
	private boolean isSqlServer;

	private List keywords;
	private String quoteCharacter;
	
	/** Creates a new instance of DbMetadata */
	public DbMetadata(WbConnection aConnection)
		throws SQLException
	{
		Connection c = aConnection.getSqlConnection();
		this.dbConnection = aConnection;
		this.metaData = c.getMetaData();
		//String[] cols = {"TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "TABLE_TYPE"};
		//this.tableListColumns = Collections.unmodifiableList(Arrays.asList(cols));

		this.procSourceSql = this.readStatementTemplates("ProcSourceStatements.xml");
		this.viewSourceSql = this.readStatementTemplates("ViewSourceStatements.xml");
		this.fkStatements = this.readStatementTemplates("CreateFkStatements.xml");
		this.pkStatements = this.readStatementTemplates("CreatePkStatements.xml");
		this.idxStatements = this.readStatementTemplates("CreateIndexStatements.xml");
		this.triggerList = this.readStatementTemplates("ListTriggersStatements.xml");
		this.triggerSourceSql = this.readStatementTemplates("TriggerSourceStatements.xml");
		//this.dateLiteralFormatter = this.readStatementTemplates("DateLiteralFormats.xml");

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
		// and don't throw an Exception. This is to ensure that getCatalogTerm() will
		// always return something usable.
		if (this.schemaTerm == null || this.schemaTerm.length() == 0)
			this.schemaTerm = "Schema";

		if (this.catalogTerm == null || this.catalogTerm.length() == 0)
			this.catalogTerm = "Catalog";

		try
		{
			this.productName = this.metaData.getDatabaseProductName();
		}
		catch (SQLException e)
		{
			LogMgr.logWarning("DbMetadata.<init>", "Could not retrieve Database Product name", e);
			this.productName = aConnection.getProfile().getDriverclass();
		}

		// For some functions we need to know which DBMS this is.
		if (this.productName.toLowerCase().indexOf("oracle") > -1)
		{
			this.isOracle = true;
			this.oracleMetaData = new OracleMetaData(this.dbConnection.getSqlConnection());
		}
		else if (this.productName.toLowerCase().indexOf("postgres") > - 1)
		{
			this.isPostgres = true;
		}
		else if (this.productName.toLowerCase().indexOf("hsql") > -1)
		{
			this.isHsql = true;
		}
		else if (this.productName.toLowerCase().indexOf("firebird") > -1)
		{
			this.isFirebird = true;
		}
		else if (this.productName.toLowerCase().indexOf("sql server") > -1)
		{
			this.isSqlServer = true;
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
		
		this.needsReconnect = serversWhichNeedReconnect.contains(this.productName);
		this.caseSensitive = caseSensitiveServers.contains(this.productName);
		this.useJdbcCommit = serverNeedsJdbcCommit.contains(this.productName);
		this.ddlNeedsCommit = ddlNeedsCommitServers.contains(this.productName);
	}

	public boolean getDDLNeedsCommit() { return ddlNeedsCommit; }
	public boolean getUseJdbcCommit() { return this.useJdbcCommit; }
  public boolean isStringComparisonCaseSensitive() { return this.caseSensitive; }
	public boolean cancelNeedsReconnect() { return this.needsReconnect; }

	public boolean isPostgres() { return this.isPostgres; }
  public boolean isOracle() { return this.isOracle; }
	public boolean isHsql() { return this.isHsql; }
	public boolean isFirebird() { return this.isFirebird; }

	private HashMap readStatementTemplates(String aFilename)
	{
		HashMap result = null;

		BufferedInputStream in = new BufferedInputStream(this.getClass().getResourceAsStream(aFilename));
		Object value = WbPersistence.readObject(in);
		if (value != null && value instanceof HashMap)
		{
			result = (HashMap)value;
		}

		// try to read additional definitions from local file
		value = WbPersistence.readObject(aFilename);
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
		return result;
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
		if (!"TABLE".equalsIgnoreCase(aType)) return null;
		
		if (this.isOracle)
		{
			return "CASCADE CONSTRAINTS";
		}
		
		return null;
	}
	
	public String getUserName()
	{
		try
		{
			return this.metaData.getUserName();
		}
		catch (Exception e)
		{
			return "";
		}
	}

	public DbDateFormatter getDateLiteralFormatter()
	{
		return SqlSyntaxFormatter.getDateLiteralFormatter(this.productName);
	}

	public String getExtendedViewSource(String aCatalog, String aSchema, String aView, boolean includeDrop)
		throws SQLException, WbException
	{
		return this.getExtendedViewSource(aCatalog, aSchema, aView, null, includeDrop);
	}
	
	public String getExtendedViewSource(String aCatalog, String aSchema, String aView, DataStore viewTableDefinition, boolean includeDrop)
		throws SQLException, WbException
	{
		if (viewTableDefinition == null)
		{
			viewTableDefinition = this.getTableDefinition(aCatalog, aSchema, aView);
		}
		String source = this.getViewSource(aCatalog, aSchema, aView);
		
		if (source == null || source.length() == 0) return "";

		StringBuffer result = new StringBuffer(source.length() + 100);

		if (this.isOracle())
		{
			result.append("CREATE OR REPLACE VIEW " + aView);
		}
		else
		{
			if (includeDrop) result.append("DROP VIEW " + aView);
			if (this.isHsql()) result.append(" IF EXISTS");
			result.append(";\nCREATE VIEW " + aView);
		}
		
		// currently HSQLDB does not support a column list in the view definition
		if (!isHsql())
		{
			result.append("\n(\n");
			int rows = viewTableDefinition.getRowCount();
			for (int i=0; i < rows; i++)
			{
				String colName = viewTableDefinition.getValueAsString(i, DbMetadata.COLUMN_IDX_TABLE_DEFINITION_COL_NAME);
				if (i == 0)
				{
					result.append("  ");
				}
				else
				{
					result.append(" ,");
				}
				result.append(colName);
				result.append("\n");
			}
		}
		result.append(")\nAS \n");
		result.append(source);
		result.append("\n");
		return result.toString();
	}

	/**
	 *	Return the source of a view definition as it is stored in the database.
	 *	Usually (depending on how the meta data is stored in the database) the DBMS
	 *	only stores the underlying SELECT statement, and that will be returned by this method.
	 *	To create a complete SQL to re-create a view, use #getExtendedViewSource(String, String, String, DataStore, boolean)
	 *
	 *	@return the view source as stored in the database.
	 */
	public String getViewSource(String aCatalog, String aSchema, String aViewname)
	{
		if (aViewname == null) return null;
		if (aViewname.length() == 0) return null;

		StringBuffer source = new StringBuffer(500);
		try
		{
			GetMetaDataSql sql = (GetMetaDataSql)this.viewSourceSql.get(this.productName);
			if (sql == null) return StringUtil.EMPTY_STRING;
			aViewname = this.adjustObjectname(aViewname);
			sql.setSchema(aSchema);
			sql.setObjectName(aViewname);
			sql.setCatalog(aCatalog);
			Statement stmt = this.dbConnection.getSqlConnection().createStatement();
			String query = sql.getSql();
			if ("true".equals(WbManager.getSettings().getProperty("workbench.dbmetadata", "logsql", "false")))
			{
				LogMgr.logInfo("DbMetadata.getViewSource()", "Using query=\n" + query);
			}
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next())
			{
				String line = rs.getString(1);
				if (line != null)
				{
					source.append(line.replaceAll("\r", ""));
				}
			}
			rs.close();
			stmt.close();
			if (source.indexOf(";") < 0) source.append(';');
		}
		catch (Exception e)
		{
			LogMgr.logWarning("DbMetadata.getViewSource()", "Could not retrieve view definition for " + aViewname, e);
			source = new StringBuffer(ExceptionUtil.getDisplay(e));
		}
		return source.toString();
	}

	private void readKeywords()
	{
		try
		{
			String keys = this.metaData.getSQLKeywords();
			this.keywords = StringUtil.stringToList(keys, ",");
		}
		catch (Exception e)
		{
			this.keywords = Collections.EMPTY_LIST;
		}
	}
	
	public String getProcedureSource(String aCatalog, String aSchema, String aProcname)
	{
		if (aProcname == null) return null;
		if (aProcname.length() == 0) return null;

		// this is for MS SQL Server, which appends a ;1 to
		// the end of the procedure name
		int i = aProcname.indexOf(';');
		if (i > -1)
			aProcname = aProcname.substring(0, i);

		StringBuffer source = new StringBuffer(4000);
		try
		{
			GetMetaDataSql sql = (GetMetaDataSql)this.procSourceSql.get(this.productName);
			aProcname = this.adjustObjectname(aProcname);
			sql.setSchema(aSchema);
			sql.setObjectName(aProcname);
			sql.setCatalog(aCatalog);
			Statement stmt = this.dbConnection.getSqlConnection().createStatement();
			ResultSet rs = stmt.executeQuery(sql.getSql());
      int linecount = 0;
			while (rs.next())
			{
				String line = rs.getString(1);
				if (line != null)
        {
          linecount ++;
          source.append(line);
        }
			}
			rs.close();
			stmt.close();

      if (this.isOracle && linecount == 0)
      {
        // this might be a procedure from a package. Then wie need
        // to retrieve the whole package. This will be stored in the catalog
        sql.setSchema(aSchema);
        sql.setObjectName(aCatalog);
        sql.setCatalog(null);
        stmt = this.dbConnection.getSqlConnection().createStatement();
        rs = stmt.executeQuery(sql.getSql());
        while (rs.next())
        {
          String line = rs.getString(1);
          if (line != null) source.append(line);
        }
        rs.close();
        stmt.close();
      }
		}
		catch (Exception e)
		{
			source = new StringBuffer(e.getMessage());
		}
		return source.toString();
	}


	/**
	 *	Encloses the given object name in double quotes if necessary. 
	 *	Quoting of names is necessary if the name is a reserved word in the 
	 *	database. To check if the given name is a keyword, it is compared 
	 *  to the words returned by getSQLKeywords().
	 *
	 *	If the given name is not a keyword, {@link workbench.util.SqlUtil#quoteObjectname(String)}
	 *  will be called to check if the name contains special characters which require
	 *	double quotes around the object name
	 */
	public String quoteObjectname(String aName)
	{
		if (aName == null) return null;
		if (this.keywords == null)
		{
			this.readKeywords();
		}
		try
		{
			boolean isKeyword = false;
			if (this.storesLowerCaseIdentifiers())
			{
				isKeyword = this.keywords.contains(aName.trim().toLowerCase());
			}
			if (!isKeyword && this.storesUpperCaseIdentifiers())
			{
				isKeyword = this.keywords.contains(aName.trim().toUpperCase());
			}
			// The ODBC driver for Access returns false for storesLowerCaseIdentifiers() 
			// AND storesUpperCaseIdentifiers()!
			if (!isKeyword && this.productName.equalsIgnoreCase("ACCESS"))
			{
				isKeyword = this.keywords.contains(aName.trim().toUpperCase());
			}
			
			// if the given name is a keyword, then we need to quote it!
			if (isKeyword)
			{
				return this.quoteCharacter + aName.trim() + this.quoteCharacter;
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
	
	public String adjustObjectname(String aTable)
	{
		if (aTable == null) return null;
		try
		{
			if (this.metaData.storesUpperCaseIdentifiers())
			{
				return aTable.toUpperCase();
			}
			else if (this.metaData.storesLowerCaseIdentifiers())
			{
				return aTable.toLowerCase();
			}
		}
		catch (Exception e)
		{
		}
		return aTable;
	}

	public String getSchemaForTable(String aTablename)
		throws SQLException
	{
		aTablename = this.adjustObjectname(aTablename);

		// First we try the current user as the schema name...
		String schema = this.adjustObjectname(this.getUserName());
		ResultSet rs = this.metaData.getTables(null, schema, aTablename, null);
		String table = null;
		if (rs.next())
		{
			table = rs.getString(2);
		}
		else
		{
			schema = null;
		}
		rs.close();
		if (table == null)
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
			rs.close();
		}
		return schema;
	}

	public DataStore getTables()
		throws SQLException
	{
		return this.getTables(null, null, null);
	}

	public final static int COLUMN_IDX_TABLE_LIST_NAME = 0;
	public final static int COLUMN_IDX_TABLE_LIST_TYPE = 1;
	public final static int COLUMN_IDX_TABLE_LIST_CATALOG = 2;
	public final static int COLUMN_IDX_TABLE_LIST_SCHEMA = 3;
	public final static int COLUMN_IDX_TABLE_LIST_REMARKS = 4;

	public DataStore getTables(String aCatalog, String aSchema, String aType)
		throws SQLException
	{
		String[] types;
		if (aType == null || "*".equals(aType) || "%".equals(aType))
		{
			types = null;
		}
		else
		{
			types = new String[] { aType };
		}
		if ("*".equals(aSchema) || "%".equals(aSchema)) aSchema = null;

		String[] cols = new String[] {"NAME", "TYPE", catalogTerm.toUpperCase(), schemaTerm.toUpperCase(), "REMARKS"};
		int coltypes[] = {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
		int sizes[] = {30,12,10,10,20};

		DataStore result = new DataStore(cols, coltypes, sizes);

		ResultSet tableRs = this.metaData.getTables(aCatalog, aSchema, null, types);
		while (tableRs.next())
		{
			String cat = tableRs.getString(1);
			String schem = tableRs.getString(2);
			String name = tableRs.getString(3);
			if (name == null) continue; 
			// filter out "internal" synonyms for Oracle
			if (this.isOracle && name.startsWith("/")) continue;
			String ttype = tableRs.getString(4);
			String rem = tableRs.getString(5);
			int row = result.addRow();
			result.setValue(row, COLUMN_IDX_TABLE_LIST_NAME, name);
			result.setValue(row, COLUMN_IDX_TABLE_LIST_TYPE, ttype);
			result.setValue(row, COLUMN_IDX_TABLE_LIST_CATALOG, cat);
			result.setValue(row, COLUMN_IDX_TABLE_LIST_SCHEMA, schem);
			result.setValue(row, COLUMN_IDX_TABLE_LIST_REMARKS, rem);
		}
		tableRs.close();
		return result;
	}

	public boolean storesUpperCaseIdentifiers()
	{
		try
		{
			return this.metaData.storesUpperCaseIdentifiers();
		}
		catch (SQLException e)
		{
			return false;
		}
	}

	public DataStoreTableModel getProcedureColumns(String aCatalog, String aSchema, String aProcname)
		throws SQLException, WbException
	{
		return new DataStoreTableModel(this.getProcedureColumnInformation(aCatalog, aSchema, aProcname));
	}

	public final static int COLUMN_IDX_PROC_COLUMNS_COL_NAME = 0;
	public final static int COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE = 2;
	public final static int COLUMN_IDX_PROC_COLUMNS_DATA_TYPE = 2;
	public final static int COLUMN_IDX_PROC_COLUMNS_REMARKS = 3;

	public DataStore getProcedureColumnInformation(String aCatalog, String aSchema, String aProcname)
		throws SQLException, WbException
	{
		final String cols[] = {"COLUMN_NAME", "COL_TYPE", "TYPE_NAME", "REMARKS"};
		final int types[] =   {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
		final int sizes[] =   {20, 10, 18, 30};
		DataStore ds = new DataStore(cols, types, sizes);

		ResultSet rs = this.metaData.getProcedureColumns(aCatalog, aSchema, this.adjustObjectname(aProcname), "%");
		while (rs.next())
		{
			int row = ds.addRow();
			String colName = rs.getString("COLUMN_NAME");
			ds.setValue(row, 0, colName);
			int colType = rs.getInt("COLUMN_TYPE");
			String stype;

			if (colType == DatabaseMetaData.procedureColumnIn)
				stype = "IN";
			else if (colType == DatabaseMetaData.procedureColumnInOut)
				stype = "INOUT";
			else if (colType == DatabaseMetaData.procedureColumnOut)
				stype = "OUT";
			else if (colType == DatabaseMetaData.procedureColumnResult)
				stype = "RESULTSET";
			else if (colType == DatabaseMetaData.procedureColumnReturn)
				stype = "RETURN";
			else
				stype = "";
			ds.setValue(row, 1, stype);

			int sqlType = rs.getInt("DATA_TYPE");
			String typeName = rs.getString("TYPE_NAME");
			int digits = rs.getInt("PRECISION");
			int size = rs.getInt("LENGTH");
			String rem = rs.getString("REMARKS");

			String display = this.getSqlTypeDisplay(typeName, sqlType, size, digits);
			ds.setValue(row, 2, display);
			ds.setValue(row, 3, rem);
		}
		rs.close();
		return ds;
	}

	private String getSqlTypeDisplay(String aTypeName, int sqlType, int size, int digits)
	{
		String display = aTypeName;

		switch (sqlType)
		{
			case Types.VARCHAR:
			case Types.CHAR:
				display = aTypeName + "(" + size + ")";
				break;
			case Types.DECIMAL:
			case Types.DOUBLE:
			case Types.NUMERIC:
			case Types.FLOAT:
				if (aTypeName.equalsIgnoreCase("money"))
				{
					display = aTypeName;
				}
				else if ((aTypeName.indexOf('(') == -1))
				{
					if (digits > 0 && size > 0)
					{
						display = aTypeName + "(" + size + "," + digits + ")";
					}
					else if (size > 0 && "NUMBER".equals(aTypeName)) // Oracle specific
					{
						display = aTypeName + "(" + size + ")";
					}
				}
				break;

			default:
				display = aTypeName;
				break;
		}
		return display;
	}

	public DataStoreTableModel getListOfProcedures()
		throws SQLException
	{
		return this.getListOfProcedures(null, null);
	}

	public DataStoreTableModel getListOfProcedures(String aCatalog, String aSchema)
		throws SQLException
	{
		return new DataStoreTableModel(this.getProcedures(aCatalog, aSchema));
	}

	public static final int COLUMN_IDX_PROC_LIST_NAME = 0;
	public static final int COLUMN_IDX_PROC_LIST_TYPE = 1;
	public static final int COLUMN_IDX_PROC_LIST_CATALOG = 2;
	public static final int COLUMN_IDX_PROC_LIST_SCHEMA = 3;
	public static final int COLUMN_IDX_PROC_LIST_REMARKS = 4;

	public DataStore getProcedures(String aCatalog, String aSchema)
		throws SQLException
	{
		String[] cols = new String[] {"PROCEDURE_NAME", "TYPE", catalogTerm.toUpperCase(), schemaTerm.toUpperCase(), "REMARKS"};
		final int types[] = {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
		final int sizes[] = {30,12,10,10,20};

		DataStore ds = new DataStore(cols, types, sizes);

		if ("*".equals(aSchema) || "%".equals(aSchema))
		{
			aSchema = null;
		}
		try
		{
			ResultSet rs = this.metaData.getProcedures(aCatalog, aSchema, "%");

			String sType;

			while (rs.next())
			{
				String cat = rs.getString("PROCEDURE_CAT");
				String schema = rs.getString("PROCEDURE_SCHEM");
				String name = rs.getString("PROCEDURE_NAME");
				String remark = rs.getString("REMARKS");
				short type = rs.getShort("PROCEDURE_TYPE");
				if (rs.wasNull())
				{
					sType = "N/A";
				}
				else
				{
					if (type == DatabaseMetaData.procedureNoResult)
						sType = PROC_RESULT_NO;
					else if (type == DatabaseMetaData.procedureReturnsResult)
						sType = PROC_RESULT_YES;
					else
						sType = PROC_RESULT_UNKNOWN;
				}
				int row = ds.addRow();

				ds.setValue(row, COLUMN_IDX_PROC_LIST_CATALOG, cat);
				ds.setValue(row, COLUMN_IDX_PROC_LIST_SCHEMA, schema);
				ds.setValue(row, COLUMN_IDX_PROC_LIST_NAME, name);
				ds.setValue(row, COLUMN_IDX_PROC_LIST_TYPE, sType);
				ds.setValue(row, COLUMN_IDX_PROC_LIST_REMARKS, remark);
			}
			rs.close();
		}
		catch (Exception e)
		{
			LogMgr.logError("DbMetadata.getProcedures()", "Error while retrieving procedures", e);
		}
		return ds;
	}

	public void enableOutput()
	{
		this.enableOutput(-1);
	}

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

	public void disableOutput()
	{
    if (!this.isOracle)
		{
			return;
		}
		if (this.oraOutput != null)
		{
			try
			{
				this.oraOutput.disable();
			}
			catch (Throwable e)
			{
				LogMgr.logError("DbMetadata.disableOutput()", "Error when disabling DbmsOutput", e);
			}
		}
	}
	public String getOutputMessages()
	{
		String result = "";

		if (this.oraOutput != null)
		{
			try
			{
				result = this.oraOutput.getResult();
			}
			catch (Throwable th)
			{
				LogMgr.logError("DbMetadata.getOutputMessages()", "Error when retrieving Output Messages", th);
				result = "";
			}
		}
		return result;
	}

	public void close()
	{
		if (this.oraOutput != null) this.oraOutput.close();
	}

	public boolean storesLowerCaseIdentifiers()
	{
		try
		{
			return this.metaData.storesLowerCaseIdentifiers();
		}
		catch (SQLException e)
		{
			return false;
		}
	}

	public DataStoreTableModel getListOfTables()
		throws SQLException, WbException
	{
		return this.getListOfTables(null, null, null);
	}

	public DataStoreTableModel getListOfTables(String aCatalog, String aSchema, String aType)
		throws SQLException, WbException
	{
		return new DataStoreTableModel(this.getTables(aCatalog, aSchema, aType));
	}

	public DataStoreTableModel getTableDefinitionModel(String aCatalog, String aSchema, String aTable)
		throws SQLException, WbException
	{
		return new DataStoreTableModel(this.getTableDefinition(aCatalog, aSchema, aTable));
	}
	public DataStoreTableModel getTableDefinitionModel(String aTable)
		throws SQLException, WbException
	{
		return new DataStoreTableModel(this.getTableDefinition(null, null, aTable));
	}

	public DataStore getTableDefinition(String aTable)
		throws SQLException, WbException
	{
		return this.getTableDefinition(null, null, aTable);
	}

	public final static int COLUMN_IDX_TABLE_DEFINITION_COL_NAME = 0;
	public final static int COLUMN_IDX_TABLE_DEFINITION_DATA_TYPE = 1;
	public final static int COLUMN_IDX_TABLE_DEFINITION_PK_FLAG = 2;
	public final static int COLUMN_IDX_TABLE_DEFINITION_NULLABLE = 3;
	public final static int COLUMN_IDX_TABLE_DEFINITION_DEFAULT = 4;
	public final static int COLUMN_IDX_TABLE_DEFINITION_REMARKS = 5;
	public final static int COLUMN_IDX_TABLE_DEFINITION_TYPE_ID = 6;
	public final static int COLUMN_IDX_TABLE_DEFINITION_SCALE = 7;
	public final static int COLUMN_IDX_TABLE_DEFINITION_PRECISION = 8;

	public DataStore getTableDefinition(String aCatalog, String aSchema, String aTable, boolean adjustNames)
		throws SQLException, WbException
	{
		return this.getTableDefinition(aCatalog, aSchema, aTable, "TABLE", adjustNames);
	}

	public DataStore getTableDefinition(String aCatalog, String aSchema, String aTable)
		throws SQLException, WbException
	{
		return this.getTableDefinition(aCatalog, aSchema, aTable, "TABLE", true);
	}

	public DataStore getTableDefinition(String aCatalog, String aSchema, String aTable, String aType)
		throws SQLException, WbException
	{
		return this.getTableDefinition(aCatalog, aSchema, aTable, aType, true);
	}

	/** Return a DataStore containing the definition of the given table.
	 * @param aCatalog The catalog in which the table is defined. This should be null if the DBMS does not support catalogs
	 * @param aSchema The schema in which the table is defined. This should be null if the DBMS does not support schemas
	 * @param aTable The name of the table
	 * @param aType The type of the table
	 * @param adjustNames If true the object names will be quoted if necessary
	 * @throws SQLException
	 * @throws WbException
	 * @return A DataStore with the table definition.
	 * The individual columns should be accessed using the
	 * COLUMN_IDX_TABLE_DEFINITION_xxx constants.
	 *
	 */	
	public DataStore getTableDefinition(String aCatalog, String aSchema, String aTable, String aType, boolean adjustNames)
		throws SQLException, WbException
	{
		final String cols[] = {"COLUMN_NAME", "DATA_TYPE", "PK", "NULLABLE", "DEFAULT", "REMARKS", "java.sql.Types", "SCALE/SIZE", "PRECISION"};
		final int types[] =   {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.INTEGER};
		final int sizes[] =   {20, 18, 5, 8, 10, 25, 18, 2, 2};
		DataStore ds = new DataStore(cols, types, sizes);

		if (adjustNames)
		{
			aCatalog = this.adjustObjectname(aCatalog);
			aSchema = this.adjustObjectname(aSchema);
			aTable = this.adjustObjectname(aTable);
		}

		if ("SYNONYM".equalsIgnoreCase(aType))
		{
			TableIdentifier id = this.getSynonymTable(aSchema, aTable);
			if (id != null)
			{
				aSchema = id.getSchema();
				aTable = id.getTable();
				aCatalog = null;
			}
		}

		ArrayList keys = new ArrayList();
		try
		{
			ResultSet keysRs = this.metaData.getPrimaryKeys(aCatalog, aSchema, aTable);
			while (keysRs.next())
			{
				keys.add(keysRs.getString("COLUMN_NAME").toLowerCase());
			}
			keysRs.close();
		}
		catch (Exception e)
		{
		}

    int rows = 0;
		ResultSet rs = this.metaData.getColumns(aCatalog, aSchema, aTable, "%");
		while (rs.next())
		{
			int row = ds.addRow();
      rows ++;
			String colName = rs.getString("COLUMN_NAME");
			ds.setValue(row, COLUMN_IDX_TABLE_DEFINITION_COL_NAME, colName);
			int sqlType = rs.getInt("DATA_TYPE");
			String typeName = rs.getString("TYPE_NAME");
			int size = rs.getInt("COLUMN_SIZE");
			int digits = rs.getInt("DECIMAL_DIGITS");
			String rem = rs.getString("REMARKS");
			String def = rs.getString("COLUMN_DEF");
			String nul = rs.getString("IS_NULLABLE");

			String display = this.getSqlTypeDisplay(typeName, sqlType, size, digits);
			ds.setValue(row, COLUMN_IDX_TABLE_DEFINITION_DATA_TYPE, display);
			if (keys.contains(colName.toLowerCase()))
				ds.setValue(row, COLUMN_IDX_TABLE_DEFINITION_PK_FLAG, "YES");
			else
				ds.setValue(row, COLUMN_IDX_TABLE_DEFINITION_PK_FLAG, "NO");

			ds.setValue(row, COLUMN_IDX_TABLE_DEFINITION_NULLABLE, nul);

			ds.setValue(row, COLUMN_IDX_TABLE_DEFINITION_DEFAULT, def);
			ds.setValue(row, COLUMN_IDX_TABLE_DEFINITION_REMARKS, rem);
			ds.setValue(row, COLUMN_IDX_TABLE_DEFINITION_TYPE_ID, new Integer(sqlType));
			ds.setValue(row, COLUMN_IDX_TABLE_DEFINITION_SCALE, new Integer(size));
			ds.setValue(row, COLUMN_IDX_TABLE_DEFINITION_PRECISION, new Integer(digits));
		}
		rs.close();
		return ds;
	}

	public DataStoreTableModel getTableIndexes(String aTable)
	{
		return new DataStoreTableModel(this.getTableIndexInformation(null, null, aTable));
	}

	public DataStoreTableModel getTableIndexes(String aCatalog, String aSchema, String aTable)
	{
		return new DataStoreTableModel(this.getTableIndexInformation(aCatalog, aSchema, aTable));
	}

	public static final int COLUMN_IDX_TABLE_INDEXLIST_INDEX_NAME = 0;
	public static final int COLUMN_IDX_TABLE_INDEXLIST_UNIQUE_FLAG = 1;
	public static final int COLUMN_IDX_TABLE_INDEXLIST_PK_FLAG = 2;
	public static final int COLUMN_IDX_TABLE_INDEXLIST_COL_DEF = 3;

	public DataStore getTableIndexInformation(String aCatalog, String aSchema, String aTable)
	{
		String[] cols = {"INDEX_NAME", "UNIQUE", "PK", "DEFINITION"};
		final int types[] =   {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
		final int sizes[] =   {40, 7, 6, 50};
		DataStore idxData = new DataStore(cols, types, sizes);
		ResultSet idxRs = null;
		
		try
		{
			// Retrieve the name of the PK index
			String pkName = "";
			ResultSet keysRs = null;
			try
			{
				keysRs = this.metaData.getPrimaryKeys(aCatalog, aSchema, this.adjustObjectname(aTable));
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
			HashMap funcIndex = null;

			if (this.isOracle)
			{
				idxRs = this.oracleMetaData.getIndexInfo(aCatalog, aSchema, this.adjustObjectname(aTable), false, true);
				funcIndex = new HashMap();
			}
			else
			{
				idxRs = this.metaData.getIndexInfo(aCatalog, aSchema, this.adjustObjectname(aTable), false, true);
			}
			
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
						if (!funcIndex.containsKey(indexName))
						{
							funcIndex.put(indexName, new ArrayList());
						}
					}
				}
				
			}
			
			if (this.isOracle && funcIndex != null)
			{
				this.oracleMetaData.readFunctionIndexDefinition(aSchema, this.adjustObjectname(aTable), funcIndex);
				Iterator defs = funcIndex.entrySet().iterator();
				while (defs.hasNext())
				{
					Map.Entry entry = (Map.Entry)defs.next();
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
				Map.Entry entry = (Map.Entry)itr.next();
				ArrayList colist = (ArrayList)entry.getValue();
				String index = (String)entry.getKey();
				int row = idxData.addRow();
				if (colist != null && colist.size() > 1)
				{
					idxData.setValue(row, 0, index);

					String unique = (String)colist.get(0);
					idxData.setValue(row, 1, unique);
					StringBuffer def = new StringBuffer(100);
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
			if (this.isOracle)
			{
				this.oracleMetaData.closeStatement();
			}
			
		}
		return idxData;
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
				try { rs.close(); } catch (Throwable th) {}
				try { stmt.close(); } catch (Throwable th) {}
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
				catalog = "";
			}
		}
		if (catalog == null) catalog = "";
		
		return catalog;
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
		try
		{
			ResultSet rs = this.metaData.getCatalogs();
			while (rs.next())
			{
				String cat = rs.getString(1);
				if (cat != null)
				{
					int row = result.addRow();
					result.setValue(row, 0, cat);
				}
			}
			rs.close();
		}
		catch (Exception e)
		{
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

		aCatalog = this.adjustObjectname(aCatalog);
		aSchema = this.adjustObjectname(aSchema);
		aTable = this.adjustObjectname(aTable);

		GetMetaDataSql sql = (GetMetaDataSql)this.triggerList.get(this.productName);
		if (sql == null)
		{
			return result;
		}

		sql.setSchema(aSchema);
		sql.setCatalog(aCatalog);
		sql.setObjectName(aTable);
		Statement stmt = this.dbConnection.getSqlConnection().createStatement();
		String query = sql.getSql();
		if ("true".equals(WbManager.getSettings().getProperty("workbench.dbmetadata", "debugmetasql", "false")))
		{
			LogMgr.logInfo("DbMetadata.getTableTriggers()", "Using query=\n" + query);
		}
		ResultSet rs = stmt.executeQuery(query);
		while (rs.next())
		{
			int row = result.addRow();
			String value = rs.getString(1);
			result.setValue(row, COLUMN_IDX_TABLE_TRIGGERLIST_TRG_NAME, value);

			value = rs.getString(2);
			result.setValue(row, COLUMN_IDX_TABLE_TRIGGERLIST_TRG_TYPE, value);

			value = rs.getString(3);
			result.setValue(row, COLUMN_IDX_TABLE_TRIGGERLIST_TRG_EVENT, value);
		}
		rs.close();
		stmt.close();

		return result;
	}

	/** 	Returns the SQL Source of the given trigger.
	 * @param aCatalog The catalog in which the trigger is defined. This should be null if the DBMS does not support catalogs
	 * @param aSchema The schema in which the trigger is defined. This should be null if the DBMS does not support schemas
	 * @param aTriggername
	 * @throws SQLException
	 * @return
	 */
	public String getTriggerSource(String aCatalog, String aSchema, String aTriggername)
		throws SQLException
	{
		StringBuffer result = new StringBuffer(500);

		if ("*".equals(aCatalog)) aCatalog = null;
		if ("*".equals(aSchema)) aSchema = null;

		aCatalog = this.adjustObjectname(aCatalog);
		aSchema = this.adjustObjectname(aSchema);
		aTriggername = this.adjustObjectname(aTriggername);

		GetMetaDataSql sql = (GetMetaDataSql)this.triggerSourceSql.get(this.productName);
		sql.setSchema(aSchema);
		sql.setCatalog(aCatalog);
		sql.setObjectName(aTriggername);
		Statement stmt = this.dbConnection.getSqlConnection().createStatement();
		String query = sql.getSql();
		if ("true".equals(WbManager.getSettings().getProperty("workbench.dbmetadata", "debugmetasql", "false")))
		{
			LogMgr.logInfo("DbMetadata.getTriggerSource()", "Using query=\n" + query);
		}

		ResultSet rs = stmt.executeQuery(query);
		int colCount = rs.getMetaData().getColumnCount();
		while (rs.next())
		{
			for (int i=1; i <= colCount; i++)
			{
				result.append(rs.getString(i));
			}
		}
		rs.close();
		stmt.close();

		return result.toString();
	}

	public DataStoreTableModel getListOfCatalogs()
	{
		return new DataStoreTableModel(this.getCatalogInformation());
	}

	/** Returns a list of database catalogs as returned by DatabaseMetadata.getCatalogs()
	 * @return ArrayList
	 */
	public List getCatalogs()
	{
		ArrayList result = new ArrayList();
		try
		{
			ResultSet rs = this.metaData.getCatalogs();
			while (rs.next())
			{
				result.add(rs.getString(1));
			}
			rs.close();
		}
		catch (Exception e)
		{
		}
		return result;
	}

	/** Returns the list of schemas as returned by DatabaseMetadata.getSchemas()
	 * @return ArrayList
	 */	
	public List getSchemas()
	{
		ArrayList result = new ArrayList();
		try
		{
			ResultSet rs = this.metaData.getSchemas();
			while (rs.next())
			{
				result.add(rs.getString(1));
			}
			rs.close();
		}
		catch (Exception e)
		{
		}
		if (this.isOracle)
		{
			result.add("PUBLIC");
			Collections.sort(result);
		}
		return result;
	}

	public List getTableTypes()
	{
		ArrayList result = new ArrayList();
		try
		{
			ResultSet rs = this.metaData.getTableTypes();
			while (rs.next())
			{
				String type = rs.getString(1);
				if (!result.contains(type)) result.add(type);
			}
			rs.close();
		}
		catch (Exception e)
		{
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
		aCatalog = this.adjustObjectname(aCatalog);
		aSchema = this.adjustObjectname(aSchema);
		aTable = this.adjustObjectname(aTable);
		ResultSet rs;
		if (exported)
			rs = this.metaData.getExportedKeys(aCatalog, aSchema, aTable);
		else
			rs = this.metaData.getImportedKeys(aCatalog, aSchema, aTable);

		DataStore ds = new DataStore(rs, false);

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
		rs.close();
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
	public DataStore getForeignKeys(String aCatalog, String aSchema, String aTable)
	{
		DataStore ds = this.getKeyList(aCatalog, aSchema, aTable, true);
		return ds;
	}

	public DataStore getReferencedBy(String aCatalog, String aSchema, String aTable)
	{
		DataStore ds = this.getKeyList(aCatalog, aSchema, aTable, false);
		return ds;
	}

	private DataStore getKeyList(String aCatalog, String aSchema, String aTable, boolean getOwnFk)
	{
		String cols[];

		if (getOwnFk)
			cols = new String[] { "FK_NAME", "COLUMN", "REFERENCES", "UPDATE_RULE", "DELETE_RULE"};
		else
			cols = new String[] { "FK_NAME", "COLUMN", "REFERENCED BY", "UPDATE_RULE", "DELETE_RULE"};

		int types[] = {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
		int sizes[] = {25, 10, 30, 12, 12};
		DataStore ds = new DataStore(cols, types, sizes);
		try
		{
			if ("*".equals(aCatalog)) aCatalog = null;
			if ("*".equals(aSchema)) aSchema = null;

			aCatalog = this.adjustObjectname(aCatalog);
			aSchema = this.adjustObjectname(aSchema);
			aTable = this.adjustObjectname(aTable);
			int tableCol;
			int fkNameCol;
			int colCol;
			int fkColCol;
			int deleteActionCol;
			int updateActionCol;
			int schemaCol;

			ResultSet rs;
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
				if (schema != null && !rs.wasNull() && schema.length() > 0 && !schema.equals(this.getUserName()))
				{
					schema = schema + ".";
				}
				else
				{
					schema = "";
				}
				String catalog = rs.getString(1);
				catalog = rs.getString(5);

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
			}
		}
		catch (Exception e)
		{
			ds.reset();
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
				return "";
		}
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

	/**
	 *	Return the source SQL for a PostgreSQL sequence definition.
	 *
	 *	@return The SQL to recreate the sequence if the current DBMS is Postgres. An empty String otherwise
	 */
	public String getSequenceSource(String aSequence)
	{
		if (!this.isPostgres) return "";
		if (aSequence == null) return "";
		
		int pos = aSequence.indexOf('.');
		if (pos > 0)
		{
			aSequence = aSequence.substring(pos);
		}
		Statement stmt = null;
		ResultSet rs = null;
		String result = "";
		try
		{
			String sql = "SELECT sequence_name, max_value, min_value, increment_by, cache_value, is_cycled FROM " + aSequence;
			stmt = this.dbConnection.createStatement();
			rs = stmt.executeQuery(sql);
			if (rs.next())
			{
				String name = rs.getString(1);;
				long maxValue = rs.getLong(2);
				long minValue = rs.getLong(3);
				String max = Long.toString(maxValue);
				String min = Long.toString(minValue);
				String inc = rs.getString(4);
				String cache = rs.getString(5);
				String cycle = rs.getString(6);

				StringBuffer buf = new StringBuffer(250);
				buf.append("CREATE SEQUENCE ");
				buf.append(name);
				buf.append(" INCREMENT ");
				buf.append(inc);
				buf.append(" MINVALUE ");
				buf.append(min);
				buf.append(" MAXVALUE ");
				buf.append(max);
				buf.append(" CACHE ");
				buf.append(cache);
				if ("true".equalsIgnoreCase(cycle))
				{
					buf.append(" CYCLE");
				}
				buf.append(";");
				result = buf.toString();
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("DbMetadata.getSequenceSource()", "Error reading sequence definition", e);
			result = "";
		}
		finally
		{
			try { rs.close(); } catch (Throwable th) {}
			try { stmt.close(); } catch (Throwable th) {}
		}
		return result;
	}

	/**
	 *	Return the underlying table of an Oracle synonym.
	 *
	 *	@return the table to which the synonym points. If the DBMS is not Oracle an empty String
	 */
	public TableIdentifier getSynonymTable(String anOwner, String aSynonym)
	{
		if (!this.isOracle) return null;
		TableIdentifier id = null;
		try
		{
			id = SynonymReader.getSynonymTable(this.dbConnection.getSqlConnection(), anOwner, aSynonym);
		}
		catch (Exception e)
		{
			LogMgr.logError("DbMetadata.getSynonymTable()", "Could not retrieve table for synonym", e);
		}
		return id;
	}
	
	/**
	 *	Return the SQL statement to recreate the given synonym.
	 *
	 *	@return the SQL to create the synonym. If the DBMS is not Oracle an empty String
	 */
	public String getSynonymSource(String anOwner, String aSynonym)
	{
		if (!this.isOracle) return "";
		String result = null;
		try
		{
			result = SynonymReader.getSynonymSource(this.dbConnection.getSqlConnection(), anOwner, aSynonym);
		}
		catch (Exception e)
		{
			result = "";
		}
		return result;
	}


	/** 	Return the SQL statement to re-create the given table. (in the dialect for the
	 *  current DBMS)
	 * @return the SQL statement to create the given table.
	 * @param catalog The catalog in which the table is defined. This should be null if the DBMS does not support catalogs
	 * @param schema The schema in which the table is defined. This should be null if the DBMS does not support schemas
	 * @param table The name of the table
	 * @param includeDrop If true, a DROP TABLE statement will be included in the generated SQL script.
	 * @throws SQLException
	 * @throws WbException
	 */
	public String getTableSource(String catalog, String schema, String table, boolean includeDrop)
		throws SQLException, WbException
	{
		DataStore tableDef = this.getTableDefinition(catalog, schema, table, true);
		DataStore index = this.getTableIndexInformation(catalog, schema, table);
		DataStore fkDef = this.getForeignKeys(catalog, schema, table);
		
		String source = this.getTableSource(catalog, schema, table, tableDef, index, fkDef, includeDrop);
		return source;
	}
	
	/** Return the SQL script to recreate the table defined in the DataStore
	 * @param aTablename The name of the table as it should appear in the CREATE TABLE statement
	 * @param aTableDef The DataStore containing a table definition obtained by #getTableDefinition(String, String, String, String)
	 * @return
	 */	
	public String getTableSource(String aTablename, DataStore aTableDef)
	{
		return this.getTableSource(aTablename, aTableDef, null, null);
	}

	public String getTableSource(String aTablename, DataStore aTableDef, DataStore aIndexDef, DataStore aFkDef)
	{
		return this.getTableSource(null, null, aTablename, aTableDef, aIndexDef, aFkDef, false);
	}
	
	public String getTableSource(String aCatalog, String aSchema, String aTablename, DataStore aTableDef, DataStore aIndexDef, DataStore aFkDef, boolean includeDrop)
	{
		if (aTableDef == null) return "";

		StringBuffer result = new StringBuffer(1000);
		if (includeDrop)
		{
			result.append("DROP TABLE " + aTablename);
			if (this.isHsql)
			{
				result.append(" IF EXISTS");
			}
			else if (this.isOracle)
			{
				result.append(" CASCADE CONSTRAINTS");
			}
			result.append(";\n");
		}
		result.append("CREATE TABLE " + aTablename + "\n(\n");
		int count = aTableDef.getRowCount();
		StringBuffer pkCols = new StringBuffer(1000);
		int maxColLength = 0;
		int maxTypeLength = 0;
		
		// calculate the longest column name, so that the display can be formatted
		for (int i=0; i < count; i++)
		{
			String colName = aTableDef.getValue(i, 0).toString();
			String type = aTableDef.getValue(i, 1).toString();
			maxColLength = Math.max(maxColLength, colName.length());
			maxTypeLength = Math.max(maxTypeLength, type.length());
		}
		maxColLength++;
		maxTypeLength++;
		
		for (int i=0; i < count; i++)
		{
			String colName = (String)aTableDef.getValue(i, COLUMN_IDX_TABLE_DEFINITION_COL_NAME);
			String type = aTableDef.getValueAsString(i, COLUMN_IDX_TABLE_DEFINITION_DATA_TYPE);
			String pk = (String)aTableDef.getValue(i, COLUMN_IDX_TABLE_DEFINITION_PK_FLAG);
			String nul = (String)aTableDef.getValue(i, COLUMN_IDX_TABLE_DEFINITION_NULLABLE);
			String def = aTableDef.getValueAsString(i, COLUMN_IDX_TABLE_DEFINITION_DEFAULT);
			
			result.append("   ");
			result.append(colName);
			if ("YES".equalsIgnoreCase(pk))
			{
				if (pkCols.length() > 0) pkCols.append(',');
				pkCols.append(colName.trim());
			}
			for (int k=0; k < maxColLength - colName.length(); k++) result.append(' ');
			result.append(type);
			for (int k=0; k < maxTypeLength - type.length(); k++) result.append(' ');
			if ("YES".equals(nul))
				result.append(" NULL");
			else
				result.append(" NOT NULL");
			if (def != null && def.length() > 0)
			{
				result.append(" DEFAULT ");
				result.append(def.trim());
			}
			if (i < count - 1) result.append(',');
			result.append('\n');
		}
		result.append(");\n");
		if (pkCols.length() > 0)
		{
			String template = this.getSqlTemplate(this.pkStatements);
			template = StringUtil.replace(template, TABLE_NAME_PLACEHOLDER, aTablename);
			template = StringUtil.replace(template, COLUMNLIST_PLACEHOLDER, pkCols.toString());
			String name = this.getPkIndexName(aIndexDef);
			if (name == null) name = "pk_" + aTablename.toLowerCase();
			template = StringUtil.replace(template, PK_NAME_PLACEHOLDER, name);
			result.append(template);
			result.append(";\n\n");
		}
		result.append(this.getIndexSource(aTablename, aIndexDef));
		result.append(this.getFkSource(aTablename, aFkDef));
		String grants = this.getTableGrantSource(null, null, aTablename);
		if (grants.length() > 0)
		{
			result.append(grants);
		}
		return result.toString();
	}

	/**
	 *	Return a SQL script to re-create the Foreign key definition for the given table.
	 *	
	 *	@param aTable the tablename for which the foreign keys should be created
	 *  @param aFkDef a DataStore with the FK definition as returned by #getForeignKeys()
	 *
	 *	@return a SQL statement to add the foreign key definitions to the given table
	 */
	public StringBuffer getFkSource(String aTable, DataStore aFkDef)
	{
		if (aFkDef == null) return StringUtil.EMPTY_STRINGBUFFER;
		int count = aFkDef.getRowCount();
		if (count == 0) return StringUtil.EMPTY_STRINGBUFFER;

		String template = (String)this.fkStatements.get(this.productName);
		if (template == null)
		{
			template = (String)this.fkStatements.get(GENERAL_SQL);
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

		String name;
		String col;
		String fkCol;
		HashSet colList;
		//String entry;

		for (int i=0; i < count; i++)
		{
			//"FK_NAME", "COLUMN_NAME", "REFERENCES"};
			name = aFkDef.getValue(i, COLUMN_IDX_FK_DEF_FK_NAME).toString();
			col = aFkDef.getValue(i, COLUMN_IDX_FK_DEF_COLUMN_NAME).toString();
			fkCol = aFkDef.getValue(i, COLUMN_IDX_FK_DEF_REFERENCE_COLUMN_NAME).toString();
			colList = (HashSet)fkCols.get(name);
			if (colList == null)
			{
				colList = new HashSet();
				fkCols.put(name, colList);
			}
			colList.add(col);

			colList = (HashSet)fkTarget.get(name);
			if (colList == null)
			{
				colList = new HashSet();
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
			stmt = StringUtil.replace(stmt, TABLE_NAME_PLACEHOLDER, aTable);
			stmt = StringUtil.replace(stmt, FK_NAME_PLACEHOLDER, name);
			colList = (HashSet)fkCols.get(name);
			entry = this.convertSetToList(colList);
			stmt = StringUtil.replace(stmt, COLUMNLIST_PLACEHOLDER, entry);
			colList = (HashSet)fkTarget.get(name);
			entry = this.convertSetToList(colList);

			StringTokenizer tok = new StringTokenizer(entry, ",");
			StringBuffer colListBuffer = new StringBuffer(30);
			String targetTable = null;
			boolean first = true;
			while (tok.hasMoreTokens())
			{
				col = tok.nextToken();
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
			fks.put(name, stmt);
		}
		StringBuffer fk = new StringBuffer(500);

		Iterator values = fks.values().iterator();
		while (values.hasNext())
		{
			fk.append((String)values.next());
			fk.append(";\n\n");
		}
		return fk;
	}

	/**
	 *	Convert a Set to a comma separated list.
	 *	@return the entries of the Set delimited by comma
	 */
	private String convertSetToList(HashSet aSet)
	{
		StringBuffer result = new StringBuffer(aSet.size() * 10);
		Iterator itr = aSet.iterator();
		boolean first = true;
		while (itr.hasNext())
		{
			if (first)
			{
				first = false;
			}
			else
			{
				result.append(", ");
			}
			result.append((String)itr.next());
		}
		return result.toString();
	}
	
	public StringBuffer getIndexSource(String aTable, DataStore aIndexDef)
	{
		if (aIndexDef == null) return StringUtil.EMPTY_STRINGBUFFER;
		int count = aIndexDef.getRowCount();
		if (count == 0) return StringUtil.EMPTY_STRINGBUFFER;

		StringBuffer pk = new StringBuffer(100);
		StringBuffer idx = new StringBuffer(1000);
		String template = this.getSqlTemplate(this.idxStatements);
		String sql;
		int idxCount = 0;
		for (int i = 0; i < count; i++)
		{
			String idx_name = aIndexDef.getValue(i, 0).toString();
			String unique = aIndexDef.getValue(i, 1).toString();
			String is_pk  = aIndexDef.getValue(i, 2).toString();
			String definition = aIndexDef.getValue(i, 3).toString();
			StringBuffer columns = new StringBuffer(100);
			StringTokenizer tok = new StringTokenizer(definition, ",");
			String col;
			int pos;
			while (tok.hasMoreTokens())
			{
				col = tok.nextToken().trim();
				if (col == null || col.length() == 0) continue;
				if (columns.length() > 0) columns.append(',');
				pos = col.indexOf(' ');
				if (pos > -1)
				{
					columns.append(col.substring(0, pos));
				}
				else
				{
					columns.append(col);
				}
			}
			// The PK's have been created with the table source, so 
			// we do not need to add the corresponding index here.
			if ("NO".equalsIgnoreCase(is_pk))
			{
				idxCount ++;
				sql = StringUtil.replace(template, TABLE_NAME_PLACEHOLDER, aTable);
				if ("YES".equalsIgnoreCase(unique))
				{
					sql = StringUtil.replace(sql, UNIQUE_PLACEHOLDER, "UNIQUE ");
				}
				else
				{
					sql = StringUtil.replace(sql, UNIQUE_PLACEHOLDER, "");
				}
				sql = StringUtil.replace(sql, COLUMNLIST_PLACEHOLDER, columns.toString());
				sql = StringUtil.replace(sql, INDEX_NAME_PLACEHOLDER, idx_name);
				idx.append(sql);
				idx.append(";\n");
			}
		}
		if (idxCount > 0) idx.append("\n");
		return idx;
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
	public DataStore getTableGrants(String aCatalog, String aSchema, String aTablename)
	{
		String[] columns = new String[] { "TABLENAME", "GRANTOR", "GRANTEE", "PRIVILEGE", "GRANTABLE" };
		int[] colTypes = new int[] { Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR };
		DataStore result = new DataStore(columns, colTypes);
		ResultSet rs = null;
		try
		{
			rs = this.metaData.getTablePrivileges(aCatalog, aSchema, aTablename);
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
	public String getTableGrantSource(String aCatalog, String aSchema, String aTablename)
	{
		DataStore ds = this.getTableGrants(aCatalog, aSchema, aTablename);
		StringBuffer result = new StringBuffer(200);
		int count = ds.getRowCount();
		
		// as several grants to several users can be made, we need to collect them 
		// first, in order to be able to build the complete statements
		HashMap grants = new HashMap(count);
		for (int i=0; i < count; i++)
		{
			String grantee = ds.getValueAsString(i, COLUMN_IDX_TABLE_GRANTS_GRANTEE); 
			String priv = ds.getValueAsString(i, COLUMN_IDX_TABLE_GRANTS_PRIV); 
			StringBuffer privs;
			if (!grants.containsKey(grantee))
			{
				privs = new StringBuffer(priv);
				grants.put(grantee, privs);
			}
			else
			{
				privs = (StringBuffer)grants.get(grantee);
				if (privs == null) privs = new StringBuffer();
				privs.append(", ");
				privs.append(priv);
			}
		}
		Set entries = grants.entrySet();
		Iterator itr = entries.iterator();
		while (itr.hasNext())
		{
			Map.Entry entry = (Map.Entry)itr.next();
			String grantee = (String)entry.getKey();
			StringBuffer privs = (StringBuffer)entry.getValue();
			result.append("GRANT ");
			result.append(privs);
			result.append(" ON ");
			result.append(aTablename);
			result.append(" TO ");
			result.append(grantee);
			result.append(";\n");
		}
		return result.toString();
	}
	
	public static void setServersWhereDDLNeedsCommit(List aList)
	{
		ddlNeedsCommitServers = aList;
	}
	public static void setServersWhichNeedReconnect(List aList)
	{
		serversWhichNeedReconnect = aList;
	}

	public static void setServersWhichNeedJdbcCommit(List aList)
	{
		serverNeedsJdbcCommit = aList;
	}
	
	public static void setCaseSensitiveServers(List aList)
	{
		caseSensitiveServers = aList;
	}
	
	/**
	 *	Return the errors reported in the all_errors table for Oracle. 
	 *	This method can be used to obtain error information after a CREATE PROCEDURE 
	 *	or CREATE TRIGGER statement has been executed. 
	 *
	 *	@return extended error information if the current DBMS is Oracle. An empty string otherwise.
	 */
  public String getExtendedErrorInfo(String schema, String objectType, String objectName)
  {
    if (!this.isOracle) return "";
		if (objectType == null) return "";
		if (objectName == null) return "";

    StringBuffer sql = new StringBuffer(200);

    sql.append("SELECT line, position, text FROM all_errors WHERE ");
    if (schema == null)
    {
      schema = this.getUserName();
    }

    sql.append("owner='");
    sql.append(this.adjustObjectname(schema));
    sql.append('\'');

    if (objectType != null)
    {
      sql.append(" AND ");
      sql.append(" type='");
      sql.append(this.adjustObjectname(objectType));
      sql.append('\'');
    }

    if (objectName != null)
    {
      sql.append(" AND ");
      sql.append(" name='");
      sql.append(this.adjustObjectname(objectName));
      sql.append('\'');
    }

    Statement stmt = null;
    ResultSet rs = null;
    StringBuffer result = new StringBuffer(250);
    try
    {
      stmt = this.dbConnection.getSqlConnection().createStatement();
      rs = stmt.executeQuery(sql.toString());
			int count = 0;
      while (rs.next())
      {
				if (count > 0) result.append("\r\n");
        int line = rs.getInt("LINE");
        int pos = rs.getInt("POSITION");
        String msg = rs.getString("TEXT");
        result.append("Error at line ");
        result.append(line);
        result.append(", position ");
        result.append(pos);
        result.append(" : ");
        result.append(msg);
        count ++;
      }
    }
    catch (SQLException e)
    {
    }
    finally
    {
      try { rs.close(); } catch (Exception e) {}
      try { stmt.close(); } catch (Exception e) {}
    }
    return result.toString();
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

}