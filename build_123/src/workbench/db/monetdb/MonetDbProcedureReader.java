/*
 * MonetDbProcedureReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.monetdb;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Types;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.JdbcProcedureReader;
import workbench.db.NoConfigException;
import workbench.db.ProcedureDefinition;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.ExceptionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class MonetDbProcedureReader
  extends JdbcProcedureReader
{

  public MonetDbProcedureReader(WbConnection conn)
  {
    super(conn);
  }

  @Override
  public DataStore getProcedures(String catalog, String schema, String name)
    throws SQLException
  {
    StringBuilder sql = new StringBuilder();

    sql.append(
      "select null as procedure_cat,  \n" +
      "       sch.name as procedure_schem,  \n" +
      "       fct.name as procedure_name,  \n" +
      "       null as reserved_1,  \n" +
      "       null as reserved_2,  \n" +
      "       null as reserved_3,  \n" +
      "       null as remarks,  \n" +
      "       case type  \n" +
      "           when 2 then 1  \n" +
      "           else 2 \n" +
      "       end as procedure_type, \n" +
      "       fct.name || ':' || fct.id as specific_name \n" +
      "from sys.functions fct \n" +
      "  left join sys.schemas sch on sch.id = fct.schema_id \n");

    if (StringUtil.isNonBlank(schema))
    {
      sql.append("where ");
      SqlUtil.appendExpression(sql, "sch.name", schema, connection);
    }

    sql.append("\norder by 2,3");

    if (Settings.getInstance().getDebugMetadataSql())
    {
      LogMgr.logInfo("MonetDbProcedureReader.getProcedures()", "Query to retrieve procedures:" + sql);
    }

    Statement stmt = null;
    ResultSet rs = null;
    Savepoint sp = null;
    try
    {
      sp = this.connection.setSavepoint();
      stmt = this.connection.createStatementForQuery();
      rs = stmt.executeQuery(sql.toString());
      DataStore ds = fillProcedureListDataStore(rs);
      ds.resetStatus();
      connection.releaseSavepoint(sp);
      return ds;
    }
    catch (SQLException ex)
    {
      connection.rollback(sp);
      throw ex;
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
  }

  @Override
  public DataStore getProcedureColumns(ProcedureDefinition def)
    throws SQLException
  {
    String name = def.getSpecificName();
    if (StringUtil.isEmptyString(name))
    {
      name = def.getProcedureName();
    }
    return getProcedureColumns(def.getCatalog(), def.getSchema(), name, null);
  }


  @Override
  public DataStore getProcedureColumns(String catalog, String schema, String procName, String specificName)
    throws SQLException
  {
    DataStore ds = createProcColsDataStore();
    ResultSet rs = null;
    Savepoint sp = null;

    StringBuilder sql = new StringBuilder();
    sql.append(
        "select null as procedure_cat,  \n" +
        "       sch.name as procedure_name,  \n" +
        "       arg.name as column_name,  \n" +
        "       1 as column_type,  \n" +
        "       case arg.type  \n" +
        "         when 'double' then " + Types.DOUBLE + " \n" +
        "         when 'int' then " + Types.INTEGER + " \n" +
        "         when 'varchar' then " + Types.VARCHAR + " \n" +
        "         when 'decimal' then " + Types.DECIMAL + " \n" +
        "         when 'date' then " + Types.DATE + " \n" +
        "         when 'timestamp' then " + Types.TIMESTAMP + " \n" +
        "         when 'time' then " + Types.TIME + " \n" +
        "         when 'clob' then " + Types.CLOB + " \n" +
        "         when 'blob' then " + Types.BLOB + " \n" +
        "         when 'boolean' then " + Types.BOOLEAN + " \n" +
        "         when 'real' then " + Types.REAL + " \n" +
        "         when 'tinyint' then " + Types.TINYINT + " \n" +
        "         when 'smallint' then " + Types.SMALLINT + " \n" +
        "         else " + Types.OTHER + "\n " +
        "       end as data_type, \n" +
        "       arg.type as type_name, \n" +
        "       arg.type_digits as precision, \n" +
        "       arg.type_digits as length, \n" +
        "       arg.type_scale as scale, \n" +
        "       null as radix, \n" +
        "       null as remarks, \n" +
        "       null as column_def, \n" +
        "       null as SQL_DATA_TYPE, \n" +
        "       null as SQL_DATETIME_SUB, \n" +
        "       case when arg.type = 'varchar' then arg.type_digits else null end as CHAR_OCTET_LENGTH, \n" +
        "       arg.number + 1 as ORDINAL_POSITION, \n" +
        "       'YES' as IS_NULLABLE \n" +
        "from sys.args arg \n" +
        "  join sys.functions fct on fct.id = arg.func_id \n" +
        "  left join sys.schemas sch on sch.id = fct.schema_id \n");

    appendProcNameCondition(sql, procName);

    if (StringUtil.isNonBlank(schema))
    {
      SqlUtil.appendAndCondition(sql, "sch.name", schema, connection);
    }

    if (Settings.getInstance().getDebugMetadataSql())
    {
      LogMgr.logInfo("MonetDbSequenceReader.getRawSequenceDefinition()", "Using query=" + sql.toString());
    }

    Statement stmt = connection.createStatementForQuery();
    try
    {
      sp = this.connection.setSavepoint();
      rs = stmt.executeQuery(sql.toString());
      while (rs.next())
      {
        processProcedureColumnResultRow(ds, rs);
      }
      this.connection.releaseSavepoint(sp);
    }
    catch (SQLException ex)
    {
      this.connection.rollback(sp);
      throw ex;
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
    return ds;
  }

  private void appendProcNameCondition(StringBuilder sql, ProcedureDefinition def)
  {
    String procName = def.getSpecificName();
    if (procName == null)
    {
      procName = def.getProcedureName();
    }
    appendProcNameCondition(sql, procName);
  }

  private void appendProcNameCondition(StringBuilder sql, String procName)
  {
    int pos = procName.indexOf(':');
    String id = null;
    if (pos > -1)
    {
      id = procName.substring(pos + 1);
      procName = procName.substring(0, pos);
    }

    sql.append(" where fct.name = '" + procName + "'\n");
    if (id != null)
    {
      sql.append("  AND fct.id=");
      sql.append(id);
      sql.append("\n ");
    }
  }

  @Override
  protected CharSequence retrieveProcedureSource(ProcedureDefinition def)
    throws NoConfigException
  {

    StringBuilder sql = new StringBuilder(100);
    sql.append(
      "select fct.func \n" +
      "from sys.functions fct\n" +
      "  left join sys.schemas sch on sch.id = fct.schema_id");

    StringBuilder source = new StringBuilder(500);

    appendProcNameCondition(sql, def);
    if (Settings.getInstance().getDebugMetadataSql())
    {
      LogMgr.logInfo("JdbcProcedureReader.getProcedureSource()", "Using query=\n" + sql.toString());
    }

    Statement stmt = null;
    ResultSet rs = null;
    Savepoint sp = null;

    try
    {
      if (useSavepoint)
      {
        sp = this.connection.setSavepoint();
      }

      stmt = this.connection.createStatementForQuery();
      rs = stmt.executeQuery(sql.toString());
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
      LogMgr.logError("MonetDbProcedureReader.getProcedureSource()", "Error retrieving procedure source", e);
      source = new StringBuilder(ExceptionUtil.getDisplay(e));
      this.connection.rollback(sp);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
    return source;
  }

}
