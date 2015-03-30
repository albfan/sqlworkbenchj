/*
 * FirebirdProcedureReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.db.firebird;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.DbMetadata;
import workbench.db.JdbcProcedureReader;
import workbench.db.JdbcUtils;
import workbench.db.NoConfigException;
import workbench.db.ProcedureDefinition;
import workbench.db.ProcedureReader;
import workbench.db.WbConnection;
import workbench.db.oracle.OraclePackageParser;

import workbench.storage.DataStore;

import workbench.sql.DelimiterDefinition;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

import static workbench.db.ProcedureReader.*;

/**
 * An implementation of the ProcedureReader interface for the
 * <a href="http://www.firebirdsql.org">Firebird</a> database server.
 *
 * The new packages in Firebird 3.0 are not handled properly yes.
 *
 * @author  Thomas Kellerer
 */
public class FirebirdProcedureReader
	extends JdbcProcedureReader
{
  private boolean is30;

	public FirebirdProcedureReader(WbConnection conn)
	{
		super(conn);
    is30 = JdbcUtils.hasMinimumServerVersion(conn, "3.0");
	}

	@Override
	public void readProcedureSource(ProcedureDefinition def, String catalogForSource, String schemaForSource)
		throws NoConfigException
  {
    if (is30 && def.isPackageProcedure())
    {
      CharSequence src = getPackageSource(null, null, def.getPackageName());
      def.setSource(src);
    }
    else
    {
      super.readProcedureSource(def, catalogForSource, schemaForSource);
    }
  }

	@Override
	public DataStore buildProcedureListDataStore(DbMetadata meta, boolean addSpecificName)
	{
    DataStore ds = super.buildProcedureListDataStore(meta, addSpecificName);
    if (supportsPackages())
    {
      ds.getResultInfo().getColumn(COLUMN_IDX_PROC_LIST_CATALOG).setColumnName("PACKAGE");
    }
    return ds;
	}

  @Override
  public boolean supportsPackages()
  {
    return is30;
  }

	@Override
	public DataStore getProcedures(String catalog, String schema, String name)
		throws SQLException
	{
    if (!supportsPackages())
    {
      return super.getProcedures(catalog, schema, name);
    }
    return getProceduresAndPackages(schema, name);
  }

  private DataStore getProceduresAndPackages(String schema, String name)
    throws SQLException
  {
    StringBuilder sql = new StringBuilder(150);
    sql.append(
      "select * \n" +
      "from (  \n" +
      "  select trim(rdb$package_name) as procedure_cat,   \n" +
      "         null as procedure_schem,  \n" +
      "         trim(rdb$procedure_name) as procedure_name,  \n" +
      "         rdb$description as remarks,  \n" +
      "         rdb$procedure_outputs as procedure_type  \n" +
      "  from rdb$procedures  \n" +
      "  where rdb$private_flag = 0 \n" +
      "     or rdb$package_name is null \n" +
      "  union all  \n" +
      "  select trim(rdb$package_name),   \n" +
      "         null as procedure_schem,  \n" +
      "         trim(rdb$function_name),  \n" +
      "         rdb$description as remarks,  \n" +
      "         2 -- returns result \n" +
      "  from rdb$functions \n" +
      "  where rdb$private_flag = 0 \n" +
      "     or rdb$package_name is null \n" +
      ") t \n");


		schema = DbMetadata.cleanupWildcards(schema);
		name = DbMetadata.cleanupWildcards(name);

    if (StringUtil.isNonEmpty(name))
    {
      SqlUtil.appendAndCondition(sql, "procedure_name", name, connection);
    }

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("FirebirdProcedureReader.getProceduresAndPackages()", "Retrieving procedures using:\n" + sql.toString());
		}

    Statement stmt = null;
    ResultSet rs = null;
		try
		{
      stmt = connection.createStatementForQuery();
      rs = stmt.executeQuery(sql.toString());

			DataStore ds = fillProcedureListDataStore(rs);

      for (int row=0; row < ds.getRowCount(); row++)
      {
        String pkg = ds.getValueAsString(row, ProcedureReader.COLUMN_IDX_PROC_LIST_CATALOG);
        String procName = ds.getValueAsString(row, ProcedureReader.COLUMN_IDX_PROC_LIST_NAME);
        String remarks = ds.getValueAsString(row, ProcedureReader.COLUMN_IDX_PROC_LIST_REMARKS);
        int type = ds.getValueAsInt(row, ProcedureReader.COLUMN_IDX_PROC_LIST_TYPE, DatabaseMetaData.procedureResultUnknown);
        ProcedureDefinition def = new ProcedureDefinition(procName, type);
        def.setComment(remarks);
        def.setPackageName(pkg);
        ds.getRow(row).setUserObject(def);
      }

      // sort the complete combined result according to the JDBC API
      ds.sort(getProcedureListSort());

			ds.resetStatus();
			return ds;
		}
		catch (SQLException ex)
		{
			throw ex;
		}
  }

	@Override
	public StringBuilder getProcedureHeader(ProcedureDefinition def)
	{
		StringBuilder source = new StringBuilder(100);
		try
		{
			DataStore ds = this.getProcedureColumns(def);
      source.append("CREATE OR ALTER ");

      boolean isFunction = false;
      if (is30 && def.isFunction())
      {
        source.append("FUNCTION "); // Firebird 3.0
        isFunction = true;
      }
      else
      {
        source.append("PROCEDURE ");
      }

			source.append(def.getProcedureName());
			String retType = null;
			int count = ds.getRowCount();
			int added = 0;
			for (int i=0; i < count; i++)
			{
				String vartype = ds.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_DATA_TYPE);
				String name = ds.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_COL_NAME);
				String ret = ds.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE);
				if ("OUT".equals(ret))
				{
          if (retType == null)
          {
            retType = "(" + name + " " + vartype;
          }
          else
          {
            retType += ", " + name + " " + vartype;
          }
				}
        else if ("RETURN".equals(ret))
        {
          retType = vartype;
        }
				else
				{
					if (added > 0)
					{
						source.append(", ");
					}
					else
					{
						source.append(" (");
					}
					source.append(name);
					source.append(' ');
					source.append(vartype);
					added ++;
				}
			}
			if (added > 0) source.append(')');
			if (retType != null)
			{
				source.append("\n  RETURNS ");
				source.append(retType);
        if (!isFunction) source.append(")");
			}
			source.append("\nAS\n");
		}
		catch (Exception e)
		{
			source = StringUtil.emptyBuilder();
		}
		return source;
	}

  @Override
  public CharSequence getPackageSource(String catalog, String schema, String packageName)
  {
    String sql =
      "select rdb$package_header_source, rdb$package_body_source \n" +
      "from rdb$packages \n" +
      "where rdb$package_name = ? ";

    PreparedStatement stmt = null;
    ResultSet rs = null;
    StringBuilder result = new StringBuilder(500);
    DelimiterDefinition delim = Settings.getInstance().getAlternateDelimiter(connection, DelimiterDefinition.STANDARD_DELIMITER);

    try
    {
      stmt = connection.getSqlConnection().prepareStatement(sql);
      stmt.setString(1, packageName);
      rs = stmt.executeQuery();
      if (rs.next())
      {
        String header = rs.getString(1);
        result.append("CREATE OR ALTER PACKAGE ");
        result.append(connection.getMetadata().quoteObjectname(packageName));
        result.append("\nAS\n");
        result.append(header);
        result.append(delim.getScriptText());
        result.append('\n');
        String body = rs.getString(2);
        result.append("RECREATE PACKAGE BODY ");
        result.append(connection.getMetadata().quoteObjectname(packageName));
        result.append("\nAS\n");
        result.append(body);
        result.append(delim.getScriptText());
      }
    }
    catch (SQLException ex)
    {
      LogMgr.logError("FirebirdProcedureReader.getPackageSource()", "Could not retrieve package source using: \n" + SqlUtil.replaceParameters(sql, packageName), ex);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
    return result;
  }

	@Override
	public DataStore getProcedureColumns(ProcedureDefinition def)
		throws SQLException
  {
    if (is30)
    {
      return retrieveProcedureColumns(def);
    }
    return super.getProcedureColumns(def);
  }

	public DataStore retrieveProcedureColumns(ProcedureDefinition def)
		throws SQLException
  {
    // for Firebird 30 we need to use our own statement
    // as the JDBC driver does not return any information about packages
    String sql =
      "select * \n" +
      "from ( \n" +
      "  select trim(pp.rdb$parameter_name) as column_name, \n" +
      "         f.rdb$field_type as field_type, \n" +
      "         f.rdb$field_sub_type as field_sub_type, \n" +
      "         f.rdb$field_precision as field_precision, \n" +
      "         f.rdb$field_scale as field_scale, \n" +
      "         f.rdb$field_length as field_length, \n" +
      "         case pp.rdb$parameter_type when 0 then " + DatabaseMetaData.procedureColumnIn + " else " + DatabaseMetaData.procedureColumnOut + " end as parameter_mode, \n" +
      "         trim(pp.rdb$description) as remarks, \n" +
      "         f.rdb$character_length as char_len, \n" +
      "         pp.rdb$parameter_number + 1 as parameter_number,  \n" +
      (is30 ?
        "         trim(pp.rdb$package_name) as package_name,  \n" :
        "         null as package_name, \n"
      ) +
      "         trim(pp.rdb$procedure_name) as procedure_name,  \n" +
      "         'procedure' as proc_type \n" +
      "  from rdb$procedure_parameters pp \n" +
      "    join rdb$fields f on pp.rdb$field_source = f.rdb$field_name \n" +
      "  union all \n" +
      "  select trim(fp.rdb$argument_name), \n" +
      "         f.rdb$field_type, \n" +
      "         f.rdb$field_sub_type, \n" +
      "         f.rdb$field_precision, \n" +
      "         f.rdb$field_scale, \n" +
      "         f.rdb$field_length, \n" +
      "         case when rdb$argument_name is null then " + DatabaseMetaData.procedureColumnReturn + " else " + DatabaseMetaData.procedureColumnIn + " end, \n" +
      "         trim(fp.rdb$description), \n" +
      "         f.rdb$character_length, \n" +
      "         fp.rdb$argument_position + 1,  \n" +
      (is30 ?
        "         trim(fp.rdb$package_name), \n" :
        "         null, \n"
      ) +
      "         trim(fp.rdb$function_name), \n" +
      "         'function' as proc_type \n" +
      "  from rdb$function_arguments fp \n" +
      "    join rdb$fields f on fp.rdb$field_source = f.rdb$field_name \n" +
      ") t \n" +
      "where procedure_name = ? \n " +
      "  and proc_type = ? \n";

    if (def.isPackageProcedure())
    {
      sql += "  and package_name = ? \n";
    }
    sql += "order by parameter_number";


    String type = null;
    if (def.isFunction())
    {
      type =  "function";
    }
    else
    {
      type = "procedure";
    }

		if (Settings.getInstance().getDebugMetadataSql())
		{
      LogMgr.logDebug("FirebirdProcedureReader.retrieveProcedureColumns()", "Retrieving procedure parameters using:\n" + SqlUtil.replaceParameters(sql, def.getProcedureName(), type, def.getPackageName()));
		}

    PreparedStatement pstmt = null;
    ResultSet rs = null;
    DataStore result = createProcColsDataStore();

    try
    {
      pstmt = connection.getSqlConnection().prepareStatement(sql);
      pstmt.setString(1, def.getProcedureName());
      pstmt.setString(2, type);

      if (def.isPackageProcedure())
      {
        pstmt.setString(3, def.getPackageName());
      }
      rs = pstmt.executeQuery();
      while (rs.next())
      {
        String colName = rs.getString("column_name");
        short fbType = rs.getShort("field_type");
        short fbSubType = rs.getShort("field_sub_type");
        int precision = rs.getInt("field_precision");
        short scale = rs.getShort("field_scale");
        int length = rs.getInt("field_length");
        String remarks = rs.getString("remarks");
        int colPos = rs.getInt("parameter_number");
        int mode = rs.getInt("parameter_mode");
        int row = result.addRow();
        result.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_COL_NAME, colName);
        result.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE, convertArgModeToString(mode));
        result.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_COL_NR, colPos);

        int jdbcDataType = getDataType(fbType, fbSubType, scale);
        result.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_JDBC_DATA_TYPE, jdbcDataType);

        String typeName = getDataTypeName(fbType, fbSubType, scale);

        int size = 0;
        int digits = 0;

        if (SqlUtil.isNumberType(jdbcDataType))
        {
          size = precision;
          digits = (scale == -1 ? 0 : scale);
        }
        else
        {
          size = length;
          digits = 0;
        }
        String display = connection.getMetadata().getDataTypeResolver().getSqlTypeDisplay(typeName, jdbcDataType, size, digits);
        result.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_DATA_TYPE, display);
        result.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_REMARKS, remarks);
      }

    }
    catch (Exception ex)
    {
      LogMgr.logError("FirebirdProcedureReader.getFunctionColumns()", "Could not retrieve procedure parameters", ex);
    }
    finally
    {
      SqlUtil.closeAll(rs, pstmt);
    }

    return result;
  }


  @Override
  public CharSequence getPackageProcedureSource(ProcedureDefinition def)
  {
    if (!supportsPackages()) return null;
    if (def == null) return null;
    if (!def.isPackageProcedure()) return null;

    CharSequence procSrc = null;

    try
    {
      if (def.getSource() == null)
      {
        readProcedureSource(def, null, null);
      }
      if (def.getSource() != null)
      {
        // the syntax between Firebird and Oracle is similar enough
        // so that the same "parser" can be used
        // we don't need to supply parameters here, because Firebird doesn't support function overloading
        procSrc = OraclePackageParser.getProcedureSource(def.getSource(), def, null);
      }
    }
    catch (Exception ex)
    {
      LogMgr.logError("OracleProcedureReader.getPackageProcedureSource", "Could not read procedure source", ex);
    }
    return procSrc;
  }


  // -----------------------------------------------------------------------------
  // --- the following code is copied from the Jaybird Source
  // --- as that is all defined as private, it can't be accessed from the outside
  // -----------------------------------------------------------------------------
  private static final short smallint_type = 7;
  private static final short integer_type = 8;
  private static final short quad_type = 9;
  private static final short float_type = 10;
  private static final short d_float_type = 11;
  private static final short date_type = 12;
  private static final short time_type = 13;
  private static final short char_type = 14;
  private static final short int64_type = 16;
  private static final short double_type = 27;
  private static final short timestamp_type = 35;
  private static final short varchar_type = 37;
  private static final short blob_type = 261;
  private static final short boolean_type = 23;

  private static int getDataType(short fieldType, short fieldSubType, short fieldScale)
  {
    switch (fieldType)
    {
      case smallint_type:
        if (fieldSubType == 1 || (fieldSubType == 0 && fieldScale < 0))
          return Types.NUMERIC;
        else if (fieldSubType == 2)
          return Types.DECIMAL;
        else
          return Types.SMALLINT;
      case integer_type:
        if (fieldSubType == 1 || (fieldSubType == 0 && fieldScale < 0))
          return Types.NUMERIC;
        else if (fieldSubType == 2)
          return Types.DECIMAL;
        else
          return Types.INTEGER;
      case double_type:
      case d_float_type:
        return Types.DOUBLE;
      case float_type:
        return Types.FLOAT;
      case char_type:
        return Types.CHAR;
      case varchar_type:
        return Types.VARCHAR;
      case timestamp_type:
        return Types.TIMESTAMP;
      case time_type:
        return Types.TIME;
      case date_type:
        return Types.DATE;
      case int64_type:
        if (fieldSubType == 1 || (fieldSubType == 0 && fieldScale < 0))
          return Types.NUMERIC;
        else if (fieldSubType == 2)
          return Types.DECIMAL;
        else
          return Types.BIGINT;
      case blob_type:
        if (fieldSubType < 0)
          return Types.BLOB;
        else if (fieldSubType == 0)
          return Types.LONGVARBINARY;
        else if (fieldSubType == 1)
          return Types.LONGVARCHAR;
        else
          return Types.OTHER;
      case quad_type:
        return Types.OTHER;
      case boolean_type:
        return Types.BOOLEAN;
      default:
        return Types.NULL;
    }
  }

  private static String getDataTypeName(short sqltype, short sqlsubtype, short sqlscale)
  {
    switch (sqltype)
    {
      case smallint_type:
        if (sqlsubtype == 1 || (sqlsubtype == 0 && sqlscale < 0))
          return "NUMERIC";
        else if (sqlsubtype == 2)
          return "DECIMAL";
        else
          return "SMALLINT";
      case integer_type:
        if (sqlsubtype == 1 || (sqlsubtype == 0 && sqlscale < 0))
          return "NUMERIC";
        else if (sqlsubtype == 2)
          return "DECIMAL";
        else
          return "INTEGER";
      case double_type:
      case d_float_type:
        return "DOUBLE PRECISION";
      case float_type:
        return "FLOAT";
      case char_type:
        return "CHAR";
      case varchar_type:
        return "VARCHAR";
      case timestamp_type:
        return "TIMESTAMP";
      case time_type:
        return "TIME";
      case date_type:
        return "DATE";
      case int64_type:
        if (sqlsubtype == 1 || (sqlsubtype == 0 && sqlscale < 0))
          return "NUMERIC";
        else if (sqlsubtype == 2)
          return "DECIMAL";
        else
          return "BIGINT";
      case blob_type:
        if (sqlsubtype < 0)
          return "BLOB SUB_TYPE <0";
        else if (sqlsubtype == 0)
          return "BLOB SUB_TYPE 0";
        else if (sqlsubtype == 1)
          return "BLOB SUB_TYPE 1";
        else
          return "BLOB SUB_TYPE " + sqlsubtype;
      case quad_type:
        return "ARRAY";
      case boolean_type:
        return "BOOLEAN";
      default:
        return "NULL";
    }
  }
  // ----------- End of Jaybird code ---------
  // -----------------------------------------

}
