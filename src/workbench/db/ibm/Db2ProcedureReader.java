/*
 * Db2ProcedureReader.java
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
package workbench.db.ibm;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.DbMetadata;
import workbench.db.JdbcProcedureReader;
import workbench.db.JdbcUtils;
import workbench.db.ProcedureDefinition;
import workbench.db.ProcedureReader;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * DB2's JDBC driver only returns procedures, not functions.
 * <br/>
 * This class uses its own SQL Statement to retrieve both objects from the database.
 *
 * @author Thomas Kellerer
 */
public class Db2ProcedureReader
	extends JdbcProcedureReader
{
	private boolean useJDBC;
	private boolean retrieveFunctionParameters;

	public Db2ProcedureReader(WbConnection conn, String dbID)
	{
		super(conn);
		useJDBC = dbID.equals("db2i");
		retrieveFunctionParameters = Settings.getInstance().getBoolProperty("workbench.db." + dbID + ".functionparams.fixretrieval", true);
	}

	@Override
	public DataStore getProcedures(String catalog, String schemaPattern, String namePattern)
		throws SQLException
	{
		if (useJDBC)
		{
			return super.getProcedures(catalog, schemaPattern, namePattern);
		}

		catalog = DbMetadata.cleanupWildcards(catalog);
		schemaPattern = DbMetadata.cleanupWildcards(schemaPattern);
		namePattern = DbMetadata.cleanupWildcards(namePattern);

		Statement stmt = null;
		ResultSet rs = null;
		String sql = getSQL(schemaPattern, namePattern);
		try
		{
			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logDebug("Db2ProcedureReader.getProcedures()", "Query to retrieve procedurelist:\n" + sql);
			}
			stmt = connection.createStatementForQuery();
			rs = stmt.executeQuery(sql);
			DataStore ds = fillProcedureListDataStore(rs);
      if (connection.getDbSettings().showProcedureParameters())
      {
        updateDisplayName(ds);
      }
			return ds;
		}
		catch (Exception e)
		{
			LogMgr.logError("Db2ProcedureReader.getProcedures()", "Error retrieving procedures using query:\n" + sql, e);
			useJDBC = true;
			return super.getProcedures(catalog, schemaPattern, namePattern);
		}
		finally
		{
			// The resultSet is already closed by fillProcedureListDataStore
			SqlUtil.closeStatement(stmt);
		}
	}

  private void updateDisplayName(DataStore procs)
  {
    for (int row=0; row < procs.getRowCount(); row ++)
    {
      ProcedureDefinition def  = (ProcedureDefinition)procs.getRow(row).getUserObject();
      def.readParameters(connection);
      updateDisplayName(def);
      if (def.getDisplayName() != null)
      {
        procs.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_NAME, def.getDisplayName());
      }
    }
  }

  private void updateDisplayName(ProcedureDefinition def)
  {
    List<String> types = def.getParameterTypes();
    if (types.size() > 0)
    {
      String display = def.getProcedureName() + "(" + StringUtil.listToString(types, ',') + ")";
      def.setDisplayName(display);
    }
  }

	private String getSQL(String schemaPattern, String namePattern)
	{
		StringBuilder sql = new StringBuilder(100);
//		if (this.connection.getMetadata().getDbId().equals("db2i"))
//		{
//			sql.append("SELECT '' as PROCEDURE_CAT,  \n" +
//             "       ROUTINE_SCHEMA  as PROCEDURE_SCHEM, \n" +
//             "       ROUTINE_NAME as PROCEDURE_NAME, \n" +
//             "       LONG_COMMENT AS REMARKS, \n" +
//             "       CASE  \n" +
//             "         WHEN RESULT_SETS > 0 THEN " + DatabaseMetaData.procedureReturnsResult + "  \n" +
//             "         ELSE " + DatabaseMetaData.procedureNoResult + "  \n" +
//             "       END as PROCEDURE_TYPE \n" +
//             "FROM qsys2.sysprocs ");
//
//			SqlUtil.appendAndCondition(sql, "ROUTINE_SCHEMA", schemaPattern);
//			SqlUtil.appendAndCondition(sql, "ROUTINE_NAME", namePattern);
//		}

		if (this.connection.getMetadata().getDbId().equals("db2h"))
		{
			// DB Host
			sql.append("SELECT '' as PROCEDURE_CAT,  \n" +
             "       schema  as PROCEDURE_SCHEM, \n" +
             "       name as PROCEDURE_NAME, \n" +
             "       remarks, \n" +
             "       CASE  \n" +
             "         WHEN routinetype = 'F' THEN " + DatabaseMetaData.procedureReturnsResult + "  \n" +
             "         ELSE " + DatabaseMetaData.procedureNoResult + "  \n" +
             "       END as PROCEDURE_TYPE, \n" +
						 "       NULL as SPECIFIC_NAME \n" +
             "FROM SYSIBM.SYSROUTINES \n" +
             "WHERE routinetype in ('F', 'P') \n" +
             "AND origin in ('Q', 'U') \n");

			SqlUtil.appendAndCondition(sql, "schema", schemaPattern, this.connection);
			SqlUtil.appendAndCondition(sql, "name", namePattern, this.connection);
		}
		else
		{
			// DB LUW
			sql.append("SELECT '' as PROCEDURE_CAT,  \n" +
					 "       routineschema as PROCEDURE_SCHEM, \n" +
					 "       routinename as PROCEDURE_NAME, \n" +
					 "       remarks, \n" +
					 "       CASE  \n" +
					 "         WHEN routinetype = 'F' THEN " + DatabaseMetaData.procedureReturnsResult + "  \n" +
					 "         ELSE " + DatabaseMetaData.procedureNoResult + "  \n" +
					 "       END as PROCEDURE_TYPE, \n" +
					 "       SPECIFICNAME as SPECIFIC_NAME \n" +
					 "FROM syscat.routines \n" +
					 "WHERE routinetype in ('F', 'P') \n" +
					 "AND origin in ('Q', 'U') \n");

			SqlUtil.appendAndCondition(sql, "routineschema", schemaPattern, this.connection);
			SqlUtil.appendAndCondition(sql, "routinename", namePattern, this.connection);
		}
		return sql.toString();
	}

  @Override
  public void readProcedureParameters(ProcedureDefinition def)
    throws SQLException
  {
    super.readProcedureParameters(def);
    updateDisplayName(def);
  }

  @Override
  public DataStore getProcedureColumns(ProcedureDefinition def)
    throws SQLException
  {
    if (def.isFunction() && retrieveFunctionParameters)
    {
      return getFunctionParameters(def);
    }
    return super.getProcedureColumns(def);
  }

	/**
	 * Retrieve parameters for a stored function.
	 *
	 * The DB2 JDBC driver does not return parameters for functions, only for stored procedures.
	 * This method uses SYSIBM.SQLFUNCTIONCOLS() to retrieve the parameters for the given stored function
	 *
	 * @param def  the stored function
	 * @return the parameters defined
	 *
	 * @throws SQLException
	 */
	public DataStore getFunctionParameters(ProcedureDefinition def)
		throws SQLException
	{
		DataStore ds = createProcColsDataStore();

		PreparedStatement stmt = null;
		ResultSet rs = null;

		String procSchema = Settings.getInstance().getProperty("workbench.db." + connection.getDbId() + ".functionparams.procschema", "SYSIBM");
		String options = Settings.getInstance().getProperty("workbench.db." + connection.getDbId() + ".functionparams.options", "UNDERSCORE=0");

		String sql = "call " + procSchema + ".SQLFUNCTIONCOLS(?, ?, ?, '%', '" +  options + "')";

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("Db2ProcedureReader.getFunctionParameters()", "Query to retrieve function parameters: " + sql);
		}

		try
		{
			stmt = connection.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, def.getCatalog());
			stmt.setString(2, def.getSchema());
			stmt.setString(3, def.getProcedureName());
			rs = stmt.executeQuery();

			int specIndex = JdbcUtils.getColumnIndex(rs, "SPECIFIC_NAME");
			if (specIndex < 0)
			{
				specIndex = JdbcUtils.getColumnIndex(rs, "SPECIFICNAME");
			}
			String specificName = def.getSpecificName();

			while (rs.next())
			{
				String procSpecName = specIndex  > -1 ? rs.getString(specIndex) : null;
				if (!StringUtil.equalString(procSpecName, specificName)) continue;
				processProcedureColumnResultRow(ds, rs, true);
			}
		}
		catch (SQLException ex)
		{
			LogMgr.logWarning("Db2ProcedureReader.getFunctionParams()", "Could not retrieve function parameters using: " + sql, ex);
			throw ex;
		}
		finally
		{
			SqlUtil.closeResult(rs);
		}

		return ds;

	}

}
