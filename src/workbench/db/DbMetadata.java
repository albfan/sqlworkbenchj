/*
 * DbMetadata.java
 *
 * Created on 16. Juli 2002, 13:09
 */

package workbench.db;

import java.io.BufferedInputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import workbench.WbManager;
import workbench.db.oracle.DbmsOutput;
import workbench.exception.WbException;
import workbench.gui.components.DataStoreTableModel;
import workbench.log.LogMgr;
import workbench.storage.DataStore;
import workbench.storage.NullValue;
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
	
	private List serversWhichNeedReconnect = Collections.EMPTY_LIST;
	
	// These Hashmaps contains templates 
	// for object creation
	private HashMap procSourceSql;
	private HashMap viewSourceSql;
	private HashMap triggerSourceSql;
	private HashMap triggerList;
	private HashMap pkStatements;
	private HashMap idxStatements;
	private HashMap fkStatements;
	private HashMap dateLiteralFormatter;

	private DbmsOutput oraOutput;
	
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
		this.dateLiteralFormatter = this.readStatementTemplates("DateLiteralFormats.xml");
		
		this.schemaTerm = this.metaData.getSchemaTerm();
		
		if (this.schemaTerm == null || this.schemaTerm.length() == 0)
			this.schemaTerm = "Schema";
		
		this.catalogTerm = this.metaData.getCatalogTerm();
		if (this.catalogTerm == null || this.catalogTerm.length() == 0)
			this.catalogTerm = "Catalog";
		
		this.productName = this.metaData.getDatabaseProductName();
		if (this.productName.toLowerCase().indexOf("oracle") > -1)
		{
			try
			{
				this.oraOutput = new DbmsOutput(c);
			}
			catch (Throwable th)
			{
				this.oraOutput = null;
			}
		}
		this.serversWhichNeedReconnect = WbManager.getSettings().getCancelWithReconnectServers();
	}

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
	 *	Returns a literal which can be used directly in a SQL statement.
	 *	This method will quote character datatypes and convert
	 *	Date datatypes to the correct format.
	 */
	public String getLiteral(Object aValue)
	{
		return getDefaultLiteral(aValue, getDateLiteralFormatter());
	}
	
	public static String getDefaultLiteral(Object aValue)
	{
		return getDefaultLiteral(aValue, null);
	}
	
	public static String getDefaultLiteral(Object aValue, DbDateFormatter formatter)
	{
		if (aValue == null) return "NULL";

		if (aValue instanceof String)
		{
			// Single quotes in a String must be "quoted"...
			String realValue = StringUtil.replace((String)aValue, "'", "''");
			return "'" + realValue + "'";
		}
		else if (aValue instanceof Date)
		{
			if (formatter == null) formatter = DbDateFormatter.DEFAULT_FORMATTER;
			return formatter.getLiteral((Date)aValue);
		}
		else if (aValue instanceof NullValue)
		{
			return "NULL";
		}
		else
		{
			return aValue.toString();
		}

	}

	public DbDateFormatter getDateLiteralFormatter()
	{
		Object value = this.dateLiteralFormatter.get(this.productName);
		if (value == null)
			value = this.dateLiteralFormatter.get(GENERAL_SQL);
		DbDateFormatter format = (DbDateFormatter)value;
		return format;
	}
	
	//public List getTableListColumns() { return this.tableListColumns; }
	
	public String getViewSource(String aCatalog, String aSchema, String aViewname)
	{
		if (aViewname == null) return null;
		if (aViewname.length() == 0) return null;
		
		StringBuffer source = new StringBuffer(500);
		try
		{
			GetMetaDataSql sql = (GetMetaDataSql)this.viewSourceSql.get(this.productName);
			aViewname = this.adjustObjectname(aViewname);
			sql.setSchema(aSchema);
			sql.setObjectName(aViewname);
			sql.setCatalog(aCatalog);
			Statement stmt = this.dbConnection.getSqlConnection().createStatement();
			ResultSet rs = stmt.executeQuery(sql.getSql());
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
		}
		catch (Exception e)
		{
			source = new StringBuffer(e.getMessage());
		}
		return source.toString();
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
			while (rs.next())
			{
				String line = rs.getString(1);
				if (line != null) source.append(line);
			}
			rs.close();
			stmt.close();
		}
		catch (Exception e)
		{
			source = new StringBuffer(e.getMessage());
		}
		return source.toString();
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

	public boolean cancelNeedsReconnect()
	{
		return this.serversWhichNeedReconnect.contains(this.productName);
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
		String display = null;
		
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
				else if (aTypeName.indexOf('(') == -1)
				{
					display = aTypeName + "(" + size + "," + digits + ")";
				}
				else
				{
					display = aTypeName;
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
		String catalog = this.dbConnection.getSqlConnection().getCatalog();
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
		if (this.oraOutput != null)
		{
			try
			{
				this.oraOutput.enable(aLimit);
			}
			catch (Throwable e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public void disableOutput()
	{
		if (this.oraOutput != null)
		{
			try
			{
				this.oraOutput.disable();
			}
			catch (Throwable e)
			{
				e.printStackTrace();
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
				result = "";
			}
		}
		return result;
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
	
	public DataStore getTableDefinition(String aCatalog, String aSchema, String aTable)
		throws SQLException, WbException
	{
		final String cols[] = {"COLUMN_NAME", "TYPE_NAME", "PK", "NULLABLE", "DEFAULT", "REMARKS"};
		final int types[] =   {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
		final int sizes[] =   {20, 18, 5, 8, 10, 50};
		DataStore ds = new DataStore(cols, types, sizes);
		
		ArrayList keys = new ArrayList();
		try
		{
			ResultSet keysRs = this.metaData.getPrimaryKeys(aCatalog, aSchema, this.adjustObjectname(aTable));
			while (keysRs.next())
			{
				keys.add(keysRs.getString("COLUMN_NAME").toLowerCase());
			}
			keysRs.close();
		}
		catch (Exception e)
		{
			// getPrimaryKeys() probably not supported...
		}
		
		ResultSet rs = this.metaData.getColumns(this.adjustObjectname(aCatalog), this.adjustObjectname(aSchema), this.adjustObjectname(aTable), "%");
		while (rs.next())
		{
			int row = ds.addRow();
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

		try
		{
			// Retrieve the name of the PK index
			String pkName = "";
			try
			{
				ResultSet keysRs = this.metaData.getPrimaryKeys(aCatalog, aSchema, this.adjustObjectname(aTable));
				while (keysRs.next())
				{
					pkName = keysRs.getString("PK_NAME");
				}
				keysRs.close();
			}
			catch (Exception e)
			{
			}

			HashMap idxInfo = new HashMap();

			ResultSet idxRs = this.metaData.getIndexInfo(aCatalog, aSchema, this.adjustObjectname(aTable), false, true);
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
			}
			idxRs.close();
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
		}
		catch (Exception e)
		{
			// clear any entries which might have made into the DataStore
			idxData.reset();
		}
		return idxData; 
	}
	
	public DataStore getCatalogInformation()
	{
		
		String catalogTerm;
		try
		{
			catalogTerm = this.metaData.getCatalogTerm();
		}
		catch (Exception e)
		{
			catalogTerm = "CATALOG";
		}
		
		String[] cols = { catalogTerm };
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
	
	public static final int COLUMN_IDX_TABLE_TRIGGERLIST_TRG_NAME = 0;
	public static final int COLUMN_IDX_TABLE_TRIGGERLIST_TRG_TYPE = 1;
	public static final int COLUMN_IDX_TABLE_TRIGGERLIST_TRG_EVENT = 2;

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
		sql.setSchema(aSchema);
		sql.setCatalog(aCatalog);
		sql.setObjectName(aTable);
		Statement stmt = this.dbConnection.getSqlConnection().createStatement();
		ResultSet rs = stmt.executeQuery(sql.getSql());
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
		ResultSet rs = stmt.executeQuery(sql.getSql());
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

	public DataStore getForeignKeys(String aCatalog, String aSchema, String aTable)
	{
		return this.getKeyList(aCatalog, aSchema, aTable, true);
	}
	
	public DataStore getReferencedBy(String aCatalog, String aSchema, String aTable)
	{
		return this.getKeyList(aCatalog, aSchema, aTable, false);
	}
	
	private DataStore getKeyList(String aCatalog, String aSchema, String aTable, boolean getOwnFk)
	{
		String cols[];
		
		if (getOwnFk) 
			cols = new String[] { "FK_NAME", "COLUMN_NAME", "REFERENCES"};
		else
			cols = new String[] { "FK_NAME", "COLUMN_NAME", "REFERENCED BY"};
			
		int types[] = {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
		int sizes[] = {15, 15, 40};
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
			ResultSet rs;
			if (getOwnFk)
			{
				rs = this.metaData.getImportedKeys(aCatalog, aSchema, aTable);
				tableCol = 3;
				fkNameCol = 12;
				colCol = 8;
				fkColCol = 4;
			}
			else
			{
				rs = this.metaData.getExportedKeys(aCatalog, aSchema, aTable);
				tableCol = 7;
				fkNameCol = 12;
				colCol = 4;
				fkColCol = 8;
			}
			while (rs.next())
			{
				String table = rs.getString(tableCol);
				String fk_col = rs.getString(fkColCol);
				String col = rs.getString(colCol);
				String fk_name = rs.getString(fkNameCol);
				int row = ds.addRow();
				ds.setValue(row, 0, fk_name);
				ds.setValue(row, 1, col);
				ds.setValue(row, 2, table + "." + fk_col);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError(this, "Could not retrieve FK information", e);
			ds.reset();
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
	
	public String getTableSource(String aTablename, DataStore aTableDef)
	{
		return this.getTableSource(aTablename, aTableDef, null, null);
	}
	
	public String getTableSource(String aTablename, DataStore aTableDef, DataStore aIndexDef, DataStore aFkDef)
	{
		if (aTableDef == null) return "";
		
		StringBuffer result = new StringBuffer("CREATE TABLE " + aTablename + "\n(\n");
		int count = aTableDef.getRowCount();
		StringBuffer pkCols = new StringBuffer(1000);
		int maxColLength = 0;
		int maxTypeLength = 0;
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
			//{"COLUMN_NAME", "TYPE_NAME", "PK", "NULLABLE", "DEFAULT", "REMARKS"};
			String colName = aTableDef.getValue(i, COLUMN_IDX_TABLE_DEFINITION_COL_NAME).toString();
			String type = aTableDef.getValue(i, COLUMN_IDX_TABLE_DEFINITION_DATA_TYPE).toString();
			String pk = aTableDef.getValue(i, COLUMN_IDX_TABLE_DEFINITION_PK_FLAG).toString();
			String nul = aTableDef.getValue(i, COLUMN_IDX_TABLE_DEFINITION_NULLABLE).toString();
			String def = aTableDef.getValue(i, COLUMN_IDX_TABLE_DEFINITION_DEFAULT).toString();
			result.append("   ");
			result.append(colName);
			if ("YES".equalsIgnoreCase(pk))
			{
				if (pkCols.length() > 0) pkCols.append(',');
				pkCols.append(colName);
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
				result.append(" DEFAULT");
				result.append(def);
			}
			if (i < count - 1) result.append(',');
			result.append('\n');
		}
		result.append(");");
		result.append('\n');
		if (pkCols.length() > 0)
		{
			String template = this.getSqlTemplate(this.pkStatements);
			template = StringUtil.replace(template, TABLE_NAME_PLACEHOLDER, aTablename);
			template = StringUtil.replace(template, COLUMNLIST_PLACEHOLDER, pkCols.toString());
			String name = this.getPkIndexName(aIndexDef);
			if (name == null) name = "pk_" + aTablename.toLowerCase();
			template = StringUtil.replace(template, PK_NAME_PLACEHOLDER, name);
			result.append(template);
			result.append('\n');
		}
		result.append(this.getIndexSource(aTablename, aIndexDef));
		result.append('\n');
		result.append(this.getFkSource(aTablename, aFkDef));
		return result.toString();
	}
	
	public String getFkSource(String aTable, DataStore aFkDef)
	{
		if (aFkDef == null) return "";
		int count = aFkDef.getRowCount();
		if (count == 0) return "";

		String template = (String)this.fkStatements.get(this.productName);
		if (template == null) 
		{
			template = (String)this.fkStatements.get(this.GENERAL_SQL);
		}

		// collects all columns from the base table mapped to the 
		// defining foreign key constraing.
		// The fk name is the key.
		// to the hashtable. The entry will be a comma
		// separated list of columns
		HashMap fkCols = new HashMap();
		
		// this hashmap contains the columns of the referenced table
		HashMap fkTarget = new HashMap();
		
		// this will contain the 
		HashMap fks = new HashMap();
		
		String name;
		String col;
		String fkCol;
		String entry;
		
		for (int i=0; i < count; i++)
		{
			//"FK_NAME", "COLUMN_NAME", "REFERENCES"};
			name = aFkDef.getValue(i, 0).toString();
			col = aFkDef.getValue(i, 1).toString();
			fkCol = aFkDef.getValue(i, 2).toString();
			entry = (String)fkCols.get(name);
			if (entry == null)
			{
				fkCols.put(name, col);
			}
			else
			{
				entry = entry + "," + col;
				fkCols.put(name, entry);
			}
			entry = (String)fkTarget.get(name);
			if (entry == null)
			{
				fkTarget.put(name, fkCol);
			}
			else
			{
				entry = entry + "," + fkCol;
				fkTarget.put(name, entry);
			}
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
			stmt = StringUtil.replace(stmt, TABLE_NAME_PLACEHOLDER, aTable);
			stmt = StringUtil.replace(stmt, FK_NAME_PLACEHOLDER, name);
			entry = (String)fkCols.get(name);
			stmt = StringUtil.replace(stmt, COLUMNLIST_PLACEHOLDER, entry);
			entry = (String)fkTarget.get(name);
			StringTokenizer tok = new StringTokenizer(entry, ",");
			StringBuffer colList = new StringBuffer();
			String targetTable = null;
			boolean first = true;
			while (tok.hasMoreTokens())
			{
				col = tok.nextToken();
				int pos = col.indexOf('.');
				if (targetTable == null)
				{
					targetTable = col.substring(0, pos);
				}
				if (!first) 
				{
					colList.append(",");
					first = false;
				}
				colList.append(col.substring(pos + 1));
			}
			stmt = StringUtil.replace(stmt, FK_TARGET_TABLE_PLACEHOLDER, targetTable);
			stmt = StringUtil.replace(stmt, FK_TARGET_COLUMNS_PLACEHOLDER, colList.toString());
			fks.put(name, stmt);
		}
		StringBuffer fk = new StringBuffer(500);
		
		Iterator values = fks.values().iterator();
		while (values.hasNext())
		{
			fk.append((String)values.next());
			fk.append(';');
			fk.append(LINE_TERMINATOR);
		}
		return fk.toString();
	}
	
	public String getIndexSource(String aTable, DataStore aIndexDef)
	{
		if (aIndexDef == null) return "";
		int count = aIndexDef.getRowCount();
		if (count == 0) return "";
		
		StringBuffer pk = new StringBuffer(100);
		StringBuffer idx = new StringBuffer(1000);
		String template = this.getSqlTemplate(this.idxStatements);
		String sql;
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
			System.out.println("processing index definition");
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
			// PK's have been created with the table source...
			if ("NO".equalsIgnoreCase(is_pk))
			{
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
				idx.append(LINE_TERMINATOR);
			}
		}
		return idx.toString();
	}

	private String getSqlTemplate(HashMap aMap)
	{
		String template = (String)aMap.get(this.productName);
		if (template == null)
		{
			template = (String)aMap.get(this.GENERAL_SQL);
		}
		return template;
	}
	
	public static void main(String args[])
	{
		HashMap m = new HashMap();
		m.put("Oracle", "ALTER TABLE %tablename% \r\n " + 
		                " ADD CONSTRAINT %constraintname% (%columnlist%) \r\n" +
										" REFERENCES %targettable% (%targetcolumnlist%)");
		WbPersistence.writeObject(m, "FkStatements.xml");
	}
	
}
