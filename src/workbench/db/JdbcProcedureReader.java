/*
 * JdbcProcedureReader.java
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.storage.DataStore;
import workbench.storage.SortDefinition;

import workbench.sql.DelimiterDefinition;
import workbench.sql.lexer.SQLLexer;
import workbench.sql.lexer.SQLLexerFactory;
import workbench.sql.lexer.SQLToken;

import workbench.util.CollectionUtil;
import workbench.util.ExceptionUtil;
import workbench.util.NumberStringCache;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * Retrieve information about stored procedures from the database.
 * To retrieve the source of the Stored procedure, SQL statements need
 * to be defined in the ProcSourceStatements.xml
 *
 * @see workbench.db.MetaDataSqlManager
 *
 * @author  Thomas Kellerer
 */
public class JdbcProcedureReader
	implements ProcedureReader
{
	final protected WbConnection connection;
	protected boolean useSavepoint;

	public JdbcProcedureReader(WbConnection conn)
	{
		this.connection = conn;
	}

  @Override
  public void clearCache()
  {
  }

	@Override
	public StringBuilder getProcedureHeader(String catalog, String schema, String procName, int procType)
	{
		return StringUtil.emptyBuilder();
	}

	@Override
	public DataStore getProcedures(String catalog, String schema, String name)
		throws SQLException
	{
		catalog = DbMetadata.cleanupWildcards(catalog);
		schema = DbMetadata.cleanupWildcards(schema);
		name = DbMetadata.cleanupWildcards(name);

		Savepoint sp = null;
		try
		{
			if (useSavepoint)
			{
				sp = this.connection.setSavepoint();
			}
			ResultSet rs = this.connection.getSqlConnection().getMetaData().getProcedures(catalog, schema, name);
			if (Settings.getInstance().getBoolProperty("workbench.db.procreader.debug", false))
			{
				SqlUtil.dumpResultSetInfo("getProcedures()", rs.getMetaData());
			}
			DataStore ds = fillProcedureListDataStore(rs);
			this.connection.releaseSavepoint(sp);
			ds.resetStatus();
			return ds;
		}
		catch (SQLException sql)
		{
			this.connection.rollback(sp);
			throw sql;
		}
	}

	public DataStore buildProcedureListDataStore(DbMetadata meta, boolean addSpecificName)
	{
		String[] cols  = null;
		int[] types = null;
		int[] sizes = null;

		if (addSpecificName)
		{
			cols = new String[] {"PROCEDURE_NAME", "TYPE", meta.getCatalogTerm().toUpperCase(), meta.getSchemaTerm().toUpperCase(), "REMARKS", "SPECIFIC_NAME"};
			types = new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
			sizes = new int[] {30,12,10,10,20,50};
		}
		else
		{
			cols = new String[] {"PROCEDURE_NAME", "TYPE", meta.getCatalogTerm().toUpperCase(), meta.getSchemaTerm().toUpperCase(), "REMARKS"};
			types = new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
			sizes = new int[] {30,12,10,10,20};
		}

		DataStore ds = new DataStore(cols, types, sizes);
		return ds;
	}

	public static SortDefinition getProcedureListSort()
	{
		SortDefinition def = new SortDefinition();
		def.addSortColumn(COLUMN_IDX_PROC_LIST_CATALOG, true);
		def.addSortColumn(COLUMN_IDX_PROC_LIST_SCHEMA, true);
		def.addSortColumn(COLUMN_IDX_PROC_LIST_NAME, true);
		return def;
	}

	public DataStore fillProcedureListDataStore(ResultSet rs)
    throws SQLException
  {
		int specIndex = JdbcUtils.getColumnIndex(rs, "SPECIFIC_NAME");
		boolean useSpecificName = specIndex > -1;

    DataStore ds = buildProcedureListDataStore(this.connection.getMetadata(), useSpecificName);
    fillProcedureListDataStore(rs, ds, useSpecificName);
    return ds;
  }

	public void fillProcedureListDataStore(ResultSet rs, DataStore ds, boolean useSpecificName)
		throws SQLException
	{
		try
		{
			while (rs.next())
			{
				String cat = rs.getString("PROCEDURE_CAT");
				String schema = rs.getString("PROCEDURE_SCHEM");
				String name = rs.getString("PROCEDURE_NAME");
				String remark = rs.getString("REMARKS");
				Integer procType = getProcedureType(rs);
				int row = ds.addRow();

				ProcedureDefinition def = new ProcedureDefinition(cat, schema, name, procType);
				def.setComment(remark);

				if (useSpecificName)
				{
					String specname = rs.getString("SPECIFIC_NAME");
					ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_SPECIFIC_NAME, specname);
					def.setSpecificName(specname);
				}

        String displayName = stripProcGroupInfo(name);
        
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_CATALOG, cat);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_SCHEMA, schema);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_NAME, displayName);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_TYPE, procType);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_REMARKS, remark);
				ds.getRow(row).setUserObject(def);
			}
			ds.resetStatus();
		}
		catch (Exception e)
		{
			LogMgr.logError("JdbcProcedureReader.getProcedures()", "Error while retrieving procedures", e);
		}
		finally
		{
			SqlUtil.closeResult(rs);
		}
	}

  protected Integer getProcedureType(ResultSet rs)
    throws SQLException
  {
    int type = rs.getInt("PROCEDURE_TYPE");
    Integer procType;
    if (rs.wasNull() || type == DatabaseMetaData.procedureResultUnknown)
    {
      // we can't really handle procedureResultUnknown, so it is treated as "no result"
      procType = Integer.valueOf(DatabaseMetaData.procedureNoResult);
    }
    else
    {
      procType = Integer.valueOf(type);
    }
    return procType;
  }

	/**
	 * Convert the JDBC result type to either <tt>PROCEDURE</tt> or <tt>FUNCTION</tt>.
	 *
	 * @param type the result type as obtained from the JDBC driver
	 * @return the SQL keyword for this type
	 */
	public static String convertProcTypeToSQL(int type)
	{
		switch (type)
		{
			case DatabaseMetaData.procedureNoResult:
				return ProcedureReader.PROC_RESULT_NO;
			case DatabaseMetaData.procedureReturnsResult:
				return ProcedureReader.PROC_RESULT_YES;
			default:
				return ProcedureReader.PROC_RESULT_UNKNOWN;
		}
	}

	protected DataStore createProcColsDataStore()
	{
		final String[] cols = {"COLUMN_NAME", "TYPE", "TYPE_NAME", TableColumnsDatastore.JAVA_SQL_TYPE_COL_NAME, "REMARKS", "POSITION"};
		final int[] types =   {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.VARCHAR, Types.INTEGER};
		final int[] sizes =   {5, 20, 10, 18, 5, 30};
		DataStore ds = new DataStore(cols, types, sizes);
		return ds;
	}

  /**
   * Remove the procedure group information from a procedure name.
   *
   * This is mainly used for SQL Server (and sibling-DBs like Sybase) to remove the ;0 or ;1
   * at the end of a procedure or function name.
   *
   * The "procedure group" will only be removed if a delimiter is defined AND the removal is configured.
   *
   * @see DbSettings#getProcGroupDelimiter()
   * @see DbSettings#getStripProcGroupNumber()
   */
	protected String stripProcGroupInfo(String procname)
	{
		if (procname == null) return null;
		DbSettings dbs = this.connection.getDbSettings();

    if (dbs.getStripProcGroupNumber() == false) return procname;

		String versionDelimiter = dbs.getProcGroupDelimiter();
		if (StringUtil.isEmptyString(versionDelimiter)) return procname;

		int pos = procname.lastIndexOf(versionDelimiter);
		if (pos < 0) return procname;

		return procname.substring(0,pos);
	}

  @Override
  public void readProcedureParameters(ProcedureDefinition def)
    throws SQLException
  {
    DataStore ds = getProcedureColumns(def);
    updateProcedureParameters(def, ds);
  }

  protected void updateProcedureParameters(ProcedureDefinition def, DataStore ds)
  {
    List<ColumnIdentifier> parameters = new ArrayList<>(ds.getRowCount());

    for (int i = 0; i < ds.getRowCount(); i++)
    {
      String type = ds.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE);
      String colName = ds.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_COL_NAME);
      String typeName = ds.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_DATA_TYPE);
      int jdbcType = ds.getValueAsInt(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_JDBC_DATA_TYPE, Types.OTHER);
      ColumnIdentifier col = new ColumnIdentifier(colName, jdbcType);
      col.setArgumentMode(type);
      col.setDbmsType(typeName);
      parameters.add(col);
    }
    def.setParameters(parameters);
  }


	@Override
	public DataStore getProcedureColumns(ProcedureDefinition def)
		throws SQLException
	{
		DataStore ds = getProcedureColumns(def.getCatalog(), def.getSchema(), def.getProcedureName(), def.getSpecificName());
    updateProcedureParameters(def, ds);
    return ds;
	}

	public DataStore getProcedureColumns(String aCatalog, String aSchema, String aProcname, String specificName)
		throws SQLException
	{
		DataStore ds = createProcColsDataStore();
		ResultSet rs = null;
		Savepoint sp = null;
		try
		{
			if (useSavepoint)
			{
				sp = this.connection.setSavepoint();
			}

			rs = this.connection.getSqlConnection().getMetaData().getProcedureColumns(aCatalog, aSchema, aProcname, "%");

			if (Settings.getInstance().getBoolProperty("workbench.db.procreader.debug", false))
			{
				SqlUtil.dumpResultSetInfo("getProcedureColumns()", rs.getMetaData());
			}

			int specIndex = -1;
			boolean useSpecificName = false;
			if (connection.getDbSettings().useSpecificNameForProcedureColumns())
			{
				String colname = connection.getDbSettings().getSpecificNameColumn();
				specIndex = JdbcUtils.getColumnIndex(rs, colname);
				useSpecificName = specIndex > -1 && StringUtil.isNonEmpty(specificName);
			}

			while (rs.next())
			{
				if (useSpecificName)
				{
					String procSpecName = rs.getString(specIndex);

					// if the specific name is relevant, only process columns for the matching specific name
					if (StringUtil.stringsAreNotEqual(procSpecName, specificName)) continue;
				}
				processProcedureColumnResultRow(ds, rs);
			}
			this.connection.releaseSavepoint(sp);
		}
		catch (SQLException sql)
		{
			this.connection.rollback(sp);
			throw sql;
		}
		finally
		{
			SqlUtil.closeResult(rs);
		}

		return ds;
	}

	protected void processProcedureColumnResultRow(DataStore ds, ResultSet rs)
		throws SQLException
	{
		processProcedureColumnResultRow(ds, rs, false);
	}

	protected void processProcedureColumnResultRow(DataStore ds, ResultSet rs, boolean useColIndex)
		throws SQLException
	{

		int row = ds.addRow();

		String colName = useColIndex ? rs.getString(4) : rs.getString("COLUMN_NAME");
		ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_COL_NAME, colName);
		int colType = useColIndex ? rs.getInt(5) : rs.getInt("COLUMN_TYPE");
		String stype;

		switch (colType)
		{
			case DatabaseMetaData.procedureColumnUnknown:
				stype = "UNKNOWN";
				break;
			case DatabaseMetaData.procedureColumnInOut:
				stype = "INOUT";
				break;
			case DatabaseMetaData.procedureColumnIn:
				stype = "IN";
				break;
			case DatabaseMetaData.procedureColumnOut:
				stype = "OUT";
				break;
			case DatabaseMetaData.procedureColumnResult:
				stype = "RESULTSET";
				break;
			case DatabaseMetaData.procedureColumnReturn:
				stype = "RETURN";
				break;
			default:
				stype = NumberStringCache.getNumberString(colType);
		}
		ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE, stype);

		int sqlType = useColIndex ? rs.getInt(6) : rs.getInt("DATA_TYPE");
		String typeName = useColIndex ? rs.getString(7) : rs.getString("TYPE_NAME");
		int precision = useColIndex ? rs.getInt(8) : rs.getInt("PRECISION");
		int length = useColIndex ? rs.getInt(9) : rs.getInt("LENGTH");
		int scale = useColIndex ? rs.getInt(10) : rs.getInt("SCALE");
		if (rs.wasNull())
		{
			scale = -1;
		}

		if (sqlType == Types.OTHER && typeName.equals("BINARY_INTEGER"))
		{
			// workaround for Oracle
			sqlType = Types.INTEGER;
		}

		int size = 0;
		int digits = 0;

		if (SqlUtil.isNumberType(sqlType))
		{
			size = precision;
			digits = (scale == -1 ? 0 : scale);
		}
		else
		{
			size = length;
			digits = 0;
		}

		String comments = useColIndex ? rs.getString(13) : rs.getString("REMARKS");
		int ordinal = -1;

		try
		{
			ordinal = useColIndex ? rs.getInt(18) : rs.getInt("ORDINAL_POSITION");
		}
		catch (Exception e)
		{
			// LogMgr.logDebug("JdbcProcedureReader.processProcedureColumnResultRow()", "Error retrieving ordinal_position", e);
			// Some Oracle driver versions do not seem to return the correct column list...
			ordinal = row;
		}

		String display = connection.getMetadata().getDataTypeResolver().getSqlTypeDisplay(typeName, sqlType, size, digits);
		ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_COL_NR, ordinal);
		ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_DATA_TYPE, display);
		ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_JDBC_DATA_TYPE, sqlType);
		ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_REMARKS, comments);
	}

	public boolean needsHeader(CharSequence procedureBody)
	{
		return true;
	}

	@Override
	public void readProcedureSource(ProcedureDefinition def)
		throws NoConfigException
  {
    readProcedureSource(def, null, null);
  }

	@Override
	public void readProcedureSource(ProcedureDefinition def, String catalogForSource, String schemaForSource)
		throws NoConfigException
	{
		if (def == null) return;

		String procName = stripProcGroupInfo(def.getProcedureName());

		StringBuilder source = new StringBuilder(500);

		CharSequence body = retrieveProcedureSource(def);
		StringBuilder header = getProcedureHeader(def.getCatalog(), def.getSchema(), procName, def.getResultType());

		if (needsHeader(body))
		{
			source.append(header);
		}
		source.append(body);

		DelimiterDefinition delimiter = connection.getAlternateDelimiter();

		if (delimiter != null && !StringUtil.endsWith(source, delimiter.getDelimiter()))
		{
			if (delimiter.isSingleLine()) source.append('\n');
			source.append(delimiter.getDelimiter());
			if (delimiter.isSingleLine()) source.append('\n');
		}

		String comment = def.getComment();
		if (StringUtil.isNonBlank(comment))
		{
			CommentSqlManager mgr = new CommentSqlManager(connection.getDbSettings().getDbId());
			String template = mgr.getCommentSqlTemplate(def.getObjectType(), CommentSqlManager.COMMENT_ACTION_SET);
			if (StringUtil.isNonBlank(template))
			{
				template = template.replace(CommentSqlManager.COMMENT_OBJECT_NAME_PLACEHOLDER, def.getProcedureName());
				template = template.replace("%specific_name%", def.getSpecificName());

        if (schemaForSource != null)
        {
          template = template.replace(TableSourceBuilder.SCHEMA_PLACEHOLDER, schemaForSource);
        }
				else
        {
          template = template.replace(TableSourceBuilder.SCHEMA_PLACEHOLDER, def.getSchema());
        }

        if (catalogForSource != null)
        {
          template = template.replace(TableSourceBuilder.CATALOG_PLACEHOLDER, catalogForSource);
        }
				else
        {
          template = template.replace(TableSourceBuilder.CATALOG_PLACEHOLDER, def.getCatalog());
        }

				template = template.replace(CommentSqlManager.COMMENT_PLACEHOLDER, comment.replace("'", "''"));
				source.append('\n');
				source.append(template);
				source.append('\n');
				if (!template.endsWith(";"))
				{
					source.append(delimiter.getDelimiter());
					if (delimiter.isSingleLine()) source.append('\n');
				}
			}
		}

		String result = source.toString();

		String dbId = this.connection.getMetadata().getDbId();
		boolean replaceNL = Settings.getInstance().getBoolProperty("workbench.db." + dbId + ".replacenl.proceduresource", false);

		if (replaceNL)
		{
			String nl = Settings.getInstance().getInternalEditorLineEnding();
			result = StringUtil.replace(source.toString(), "\\n", nl);
		}

		def.setSource(result);
	}

	protected CharSequence retrieveProcedureSource(ProcedureDefinition def)
		throws NoConfigException
	{
		GetMetaDataSql sql = this.connection.getMetadata().getMetaDataSQLMgr().getProcedureSourceSql();
		if (sql == null)
		{
			throw new NoConfigException("No sql configured to retrieve procedure source");
		}

		String procName = stripProcGroupInfo(def.getProcedureName());

		StringBuilder source = new StringBuilder(500);

		Statement stmt = null;
		ResultSet rs = null;
		Savepoint sp = null;

		try
		{
			if (useSavepoint)
			{
				sp = this.connection.setSavepoint();
			}
			sql.setSchema(def.getSchema());
			sql.setObjectName(procName);
			sql.setCatalog(def.getCatalog());
			sql.setSpecificName(def.getSpecificName());
			sql.setInternalId(def.getInternalIdentifier());

			String query = sql.getSql();

			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logInfo("JdbcProcedureReader.getProcedureSource()", "Retrieving procedure source using query:\n" + query);
			}

			stmt = this.connection.createStatementForQuery();
			rs = stmt.executeQuery(query);
			while (rs.next())
			{
				String line = rs.getString(1);
				if (line != null)
				{
					source.append(line);
				}
			}
			this.connection.releaseSavepoint(sp);
		}
		catch (SQLException e)
		{
			if (sp != null) this.connection.rollback(sp);
			LogMgr.logError("JdbcProcedureReader.getProcedureSource()", "Error retrieving procedure source", e);
			source = new StringBuilder(ExceptionUtil.getDisplay(e));
			this.connection.rollback(sp);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return source;
	}

	/**
	 * Return a List of {@link workbench.db.ProcedureDefinition} objects.
	 */
	@Override
	public List<ProcedureDefinition> getProcedureList(String catalogPattern, String schemaPattern, String namePattern)
		throws SQLException
	{
		List<ProcedureDefinition> result = new LinkedList<>();

		catalogPattern = DbMetadata.cleanupWildcards(catalogPattern);
		schemaPattern = DbMetadata.cleanupWildcards(schemaPattern);
		namePattern = DbMetadata.cleanupWildcards(namePattern);

		DataStore procs = getProcedures(catalogPattern, schemaPattern, namePattern);

		if (procs == null || procs.getRowCount() == 0) return result;
		procs.sortByColumn(ProcedureReader.COLUMN_IDX_PROC_LIST_NAME, true);
		int count = procs.getRowCount();

		for (int i = 0; i < count; i++)
		{
			String schema  = procs.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_LIST_SCHEMA);
			String cat = procs.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_LIST_CATALOG);
			String procName = procs.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_LIST_NAME);
			String remarks = procs.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_LIST_REMARKS);
			int type = procs.getValueAsInt(i, ProcedureReader.COLUMN_IDX_PROC_LIST_TYPE, DatabaseMetaData.procedureNoResult);
			ProcedureDefinition def = (ProcedureDefinition)procs.getRow(i).getUserObject();
			if (def == null)
			{
				if (this.connection.getMetadata().isOracle() && cat != null)
				{
					def = ProcedureDefinition.createOracleDefinition(schema, procName, cat, type, remarks);
				}
				else
				{
					def = new ProcedureDefinition(cat, schema, procName, type);
					def.setComment(remarks);
				}
			}
			if (def != null) result.add(def);
		}
		return result;
	}

	@Override
	public ProcedureDefinition findProcedureByName(DbObject toFind)
		throws SQLException
	{
		if (toFind == null) return null;
		String objSchema = toFind.getSchema();
		String objCat = toFind.getCatalog();

		if (StringUtil.isNonEmpty(objCat) || StringUtil.isNonBlank(objSchema))
		{
			if (StringUtil.isEmptyString(objCat))
			{
				objCat = connection.getCurrentCatalog();
			}
			if (StringUtil.isEmptyString(objSchema))
			{
				objSchema = connection.getCurrentSchema();
			}
			List<ProcedureDefinition> procs = getProcedureList(objCat, objSchema, toFind.getObjectName());
			if (procs.size() == 1)
			{
				return procs.get(0);
			}
			return null;
		}

		if (StringUtil.isEmptyString(objCat))
		{
			objCat = connection.getCurrentCatalog();
		}

		List<String> searchPath = DbSearchPath.Factory.getSearchPathHandler(connection).getSearchPath(connection, null);
		if (CollectionUtil.isEmpty(searchPath) && StringUtil.isEmptyString(objSchema))
		{
			searchPath = CollectionUtil.arrayList(connection.getCurrentSchema());
		}

		for (String schema : searchPath)
		{
			List<ProcedureDefinition> procs = getProcedureList(objCat, schema, toFind.getObjectName());
			if (procs.size() == 1)
			{
				return procs.get(0);
			}
		}
		return null;
	}

  @Override
  public ProcedureDefinition findProcedureDefinition(ProcedureDefinition toFind)
  {
    List<ProcedureDefinition> procList = null;
    try
    {
      procList = getProcedureList(toFind.getCatalog(), toFind.getSchema(), toFind.getProcedureName());
    }
    catch (SQLException ex)
    {
      LogMgr.logWarning("JdbcProcedureReader.findProcedureDefinition()", "Could not read procedures", ex);
      return null;
    }

    if (CollectionUtil.isEmpty(procList))
    {
      return null;
    }

    List<String> typeSignature = toFind.getParameterTypes();

    if (!connection.getDbSettings().supportsFunctionOverloading())
    {
      if (procList.size() == 1)
      {
        return procList.get(0);
      }
      return null;
    }

    for (ProcedureDefinition def : procList)
    {
      def.readParameters(connection);
      List<String> parameterTypes = def.getParameterTypes();
      if (parameterTypes.equals(typeSignature))
      {
        return def;
      }
    }
    return null;
  }

  @Override
  public boolean isRecreateStatement(CharSequence sql)
  {
    if (sql == null) return false;
    if (sql.length() == 0) return false;

    SQLLexer lexer = SQLLexerFactory.createLexer(connection);
    lexer.setInput(sql);
    SQLToken token = lexer.getNextToken(false, false);
    if (token == null) return false;
    if (token.getText().equalsIgnoreCase("create or replace")) return true;
    if (token.getText().equalsIgnoreCase("recreate")) return true;
    if (token.getText().equalsIgnoreCase("create"))
    {
      token = lexer.getNextToken(false, false);
      if (token.getText().equalsIgnoreCase("or"))
      {
        token = lexer.getNextToken(false, false);
        return token != null && token.getText().equalsIgnoreCase("replace");
      }
    }
    return false;
  }


}
