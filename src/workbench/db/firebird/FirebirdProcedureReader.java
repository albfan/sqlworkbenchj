/*
 * FirebirdProcedureReader.java
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
package workbench.db.firebird;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.DbMetadata;
import workbench.db.JdbcProcedureReader;
import workbench.db.JdbcUtils;
import workbench.db.NoConfigException;
import workbench.db.ProcedureDefinition;
import workbench.db.ProcedureReader;
import workbench.db.WbConnection;

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
	public FirebirdProcedureReader(WbConnection conn)
	{
		super(conn);
	}

	@Override
	public void readProcedureSource(ProcedureDefinition def, String catalogForSource, String schemaForSource)
		throws NoConfigException
  {
    if (supportsPackages() && def.isPackageProcedure())
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

  private boolean supportsPackages()
  {
    return JdbcUtils.hasMinimumServerVersion(connection, "3.0");
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
    StringBuilder sql = new StringBuilder(100);
    sql.append(
      "select trim(rdb$package_name) as procedure_cat,  \n" +
      "       null as procedure_schem, \n" +
      "       trim(rdb$procedure_name) as procedure_name, \n" +
      "       rdb$description as remarks, \n" +
      "       rdb$procedure_outputs as procedure_type \n" +
      "from rdb$procedures");


		schema = DbMetadata.cleanupWildcards(schema);
		name = DbMetadata.cleanupWildcards(name);

    if (StringUtil.isNonEmpty(name))
    {
      SqlUtil.appendAndCondition(sql, "rdb$procedure_name", name, connection);
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
	public StringBuilder getProcedureHeader(String aCatalog, String aSchema, String aProcname, int procType)
	{
		// TODO: handle packages properly (e.g. like in Oracle)
		StringBuilder source = new StringBuilder();
		try
		{
			DataStore ds = this.getProcedureColumns(aCatalog, aSchema, aProcname, null);
      source.append("CREATE OR ALTER PROCEDURE ");

			source.append(aProcname);
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
        source.append(")");
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
        delim.appendTo(result);
        result.append('\n');
        String body = rs.getString(2);
        result.append("RECREATE PACKAGE BODY ");
        result.append(connection.getMetadata().quoteObjectname(packageName));
        result.append("\nAS\n");
        result.append(body);
        delim.appendTo(result);
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



}
