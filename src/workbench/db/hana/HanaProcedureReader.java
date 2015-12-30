/*
 * HanaProcedureReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
package workbench.db.hana;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import workbench.log.LogMgr;

import workbench.db.JdbcProcedureReader;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class HanaProcedureReader
  extends JdbcProcedureReader
{

  public HanaProcedureReader(WbConnection conn)
  {
    super(conn);
  }

  @Override
  public DataStore getProcedures(String catalog, String schema, String name)
    throws SQLException
  {
    // The SAP JDBC driver does not return functions, only procedures
    DataStore ds = super.getProcedures(catalog, schema, name);

    if (connection.getDbSettings().getBoolProperty("retrieve.functions", true))
    {
      appendFunctions(ds, schema);
    }

    return ds;
  }

  private void appendFunctions(DataStore ds, String schema)
  {
    ResultSet rs = null;
    Statement stmt = null;
    String sql =
      "select null as PROCEDURE_CAT, \n" +
      "       schema_name as PROCEDURE_SCHEM, \n" +
      "       function_name as PROCEDURE_NAME,\n" +
      "       null as remarks, \n" +
      "       " + DatabaseMetaData.procedureReturnsResult + " as PROCEDURE_TYPE \n" +
      "from sys.functions\n";
    try
    {
      if (StringUtil.isNonEmpty(schema))
      {
        sql += " where schema_name = '" + schema + "'";
      }

      stmt = this.connection.createStatement();
      rs = stmt.executeQuery(sql);
      fillProcedureListDataStore(rs, ds, false);
    }
    catch (Exception ex)
    {
      LogMgr.logError("HanaProcedureReader.appendFunctions()", "Could not read functions using: \n" + sql, ex);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
  }


}
