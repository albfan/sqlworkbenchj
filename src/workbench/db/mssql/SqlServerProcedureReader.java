/*
 * SqlServerProcedureReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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
package workbench.db.mssql;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import workbench.db.JdbcProcedureReader;
import workbench.db.NoConfigException;
import workbench.db.ProcedureDefinition;
import workbench.db.ProcedureReader;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.StringUtil;

/**
 * A ProcedureReader for Microsoft SQL Server.
 *
 * @author  Thomas Kellerer
 */
public class SqlServerProcedureReader
  extends JdbcProcedureReader
{

  public SqlServerProcedureReader(WbConnection db)
  {
    super(db);
  }

  /**
   * The MS JDBC driver does not return the PROCEDURE_TYPE column correctly so we implement it ourselves by looking at the "group number".
   * procedures and functions we need to return this correctly.
   * <br/>
   * The correct "type" is important because e.g. a DROP from within the DbExplorer
   * relies on the correct type returned by getProcedures()
   * <br/>
   * Functions seem to always have a "procedure group" 0
   * and "real" procedures always have a group number greater than zero
   */
  @Override
  public DataStore getProcedures(String catalog, String owner, String namePattern)
    throws SQLException
  {
    DataStore ds = super.getProcedures(catalog, owner, namePattern);
    updateRemarks(ds, owner);
    return ds;
  }

  @Override
  protected Integer getProcedureType(ResultSet rs)
    throws SQLException
  {
    String name = rs.getString("PROCEDURE_NAME");
    return getProcTypeFromName(name);
  }

  private Integer getProcTypeFromName(String name)
  {
    int groupNumber = getProcGroupNumber(name);

    Integer procType;

    if (groupNumber == 0)
    {
      procType = Integer.valueOf(DatabaseMetaData.procedureReturnsResult);
    }
    else
    {
      procType = Integer.valueOf(DatabaseMetaData.procedureNoResult);
    }
    return procType;
  }

  private int getProcGroupNumber(String procname)
  {
    if (procname == null) return -1;
    int pos = procname.lastIndexOf(';');

    // the jTDS driver does not return a group number at all for the first procedure in a group
    if (pos < 1) return 1;

    return StringUtil.getIntValue(procname.substring(pos + 1), -1);
  }

  protected void updateRemarks(DataStore ds, String owner)
  {
    if (!connection.getDbSettings().getBoolProperty("remarks.procedure.retrieve", false)) return;

    if (ds == null || ds.getRowCount() == 0) return;

    String object = null;
    if (ds.getRowCount() == 1)
    {
      object = ds.getValueAsString(0, ProcedureReader.COLUMN_IDX_PROC_LIST_NAME);
    }

    SqlServerObjectListEnhancer reader = new SqlServerObjectListEnhancer();
    Map<String, String> remarks = reader.readRemarks(connection, owner, object, new String[] { "procedure"});

    for (int row = 0; row < ds.getRowCount(); row ++)
    {
      String schema = ds.getValueAsString(row, ProcedureReader.COLUMN_IDX_PROC_LIST_SCHEMA);
      String name = ds.getValueAsString(row, ProcedureReader.COLUMN_IDX_PROC_LIST_NAME);
      String remark = remarks.get(schema + "." + name);
      if (remark != null)
      {
        ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_REMARKS, remark);
      }
    }
  }

  @Override
  public CharSequence retrieveProcedureSource(ProcedureDefinition def)
    throws NoConfigException
  {
    SpHelpTextRunner runner = new SpHelpTextRunner();
    String procName = stripProcGroupInfo(def.getProcedureName());
    CharSequence sql = runner.getSource(connection, def.getCatalog(), def.getSchema(), procName);
    return sql;
  }
}
