/*
 * PostgresTriggerReader.java
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
package workbench.db.postgres;

import java.sql.Array;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.DefaultTriggerReader;
import workbench.db.JdbcUtils;
import workbench.db.NoConfigException;
import workbench.db.ProcedureDefinition;
import workbench.db.ProcedureReader;
import workbench.db.TableIdentifier;
import workbench.db.TriggerDefinition;
import workbench.db.TriggerReader;
import workbench.db.WbConnection;

import workbench.storage.DataStore;
import workbench.storage.SortDefinition;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A TriggerReader for Postgres that retrieves not only the trigger source but also
 * the source code of the associated function.
 *
 * @author Thomas Kellerer
 */
public class PostgresTriggerReader
  extends DefaultTriggerReader
{
  private boolean is93 = false;
  public PostgresTriggerReader(WbConnection conn)
  {
    super(conn);
    is93 = JdbcUtils.hasMinimumServerVersion(conn, "9.3");
  }

  @Override
  public DataStore getTriggers(String catalog, String schema)
    throws SQLException
  {
    DataStore result = super.getTriggers(catalog, schema);
    if (is93)
    {
      retrieveEventTriggers(result);
    }
    return result;
  }

  public DataStore getEventTriggerse()
  {
    DataStore result = createResultDataStore();
    retrieveEventTriggers(result);
    return result;
  }

  public int retrieveEventTriggers(DataStore triggers)
  {
    String sql =
      "select evtname as trigger,  \n" +
      "       evtevent as event,  \n" +
      "       obj_description(oid, 'pg_event_trigger') as remarks \n" +
      "FROM pg_event_trigger";

    PreparedStatement stmt = null;
    ResultSet rs = null;
    Savepoint sp = null;

    if (Settings.getInstance().getDebugMetadataSql())
    {
      LogMgr.logInfo("PostgresTriggerReader.getDependentSource()", "Retrieving event triggers using:\n" + sql);
    }

    int triggerCount = 0;
    try
    {
      sp = dbConnection.setSavepoint();
      stmt = dbConnection.getSqlConnection().prepareStatement(sql);
      rs = stmt.executeQuery();

      while (rs.next())
      {
        triggerCount ++;
        String name = rs.getString(1);
        String event = rs.getString(2);
        String remarks = rs.getString(3);
        int row = triggers.addRow();
        triggers.setValue(row, TriggerReader.COLUMN_IDX_TABLE_TRIGGERLIST_TRG_NAME, name);
        triggers.setValue(row, TriggerReader.COLUMN_IDX_TABLE_TRIGGERLIST_TRG_TYPE, "EVENT");
        triggers.setValue(row, TriggerReader.COLUMN_IDX_TABLE_TRIGGERLIST_TRG_EVENT, event);
        triggers.setValue(row, TriggerReader.COLUMN_IDX_TABLE_TRIGGERLIST_TRG_COMMENT, remarks);
        TriggerDefinition trg = new TriggerDefinition(null, null, name);
        trg.setComment(remarks);
        trg.setTriggerType("EVENT TRIGGER");
        triggers.getRow(row).setUserObject(trg);
      }
      dbConnection.releaseSavepoint(sp);
    }
    catch (Exception ex)
    {
      dbConnection.rollback(sp);
      LogMgr.logError("PostgresTriggerReader.retrieveEventTriggers()", "Couldn not retrieve event triggers using:\n" + sql, ex);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }

    if (triggerCount > 0)
    {
      // sort the datastore again
      SortDefinition def = new SortDefinition();
      def.addSortColumn(0, true);
      triggers.sort(def);
    }
    triggers.resetStatus();

    return triggerCount;
  }

  @Override
  public String getTriggerSource(String triggerCatalog, String triggerSchema, String triggerName, TableIdentifier triggerTable, String trgComment, boolean includeDependencies)
    throws SQLException
  {
    if (triggerTable == null && is93)
    {
      return getEventTriggerSource(triggerName);
    }
    return super.getTriggerSource(triggerCatalog, triggerSchema, triggerName, triggerTable, trgComment, includeDependencies);
  }

  public String getEventTriggerSource(String triggerName)
    throws SQLException
  {
    StringBuilder result = new StringBuilder(100);
    PreparedStatement stmt = null;
    ResultSet rs = null;
    Savepoint sp = null;


    final String sql =
      "select pr.proname, \n" +
      "       trg.evtevent, \n " +
      "       trg.evttags, \n "+
      "       pg_get_functiondef(pr.oid) as func_source \n" +
      "FROM pg_event_trigger trg \n" +
      " JOIN pg_proc pr on pr.oid = trg.evtfoid \n" +
      " join pg_namespace nsp on nsp.oid = pr.pronamespace \n" +
      "where trg.evtname = ?";

    if (Settings.getInstance().getDebugMetadataSql())
    {
      LogMgr.logInfo("PostgresTriggerReader.getEventTriggerSource()", "Retrieving event trigger source using:\n" + SqlUtil.replaceParameters(sql, triggerName));
    }

    try
    {
      String funcName = null;
      String event = null;
      String funcSource = null;
      String[] tags = null;

      sp = dbConnection.setSavepoint();

      stmt = dbConnection.getSqlConnection().prepareStatement(sql);
      stmt.setString(1, triggerName);
      rs = stmt.executeQuery();

      if (rs.next())
      {
        event = rs.getString("evtevent");
        Array tagArray = rs.getArray("evttags");
        if (tagArray != null)
        {
          tags = (String[])tagArray.getArray();
        }
        funcSource = rs.getString("func_source");
        funcName = rs.getString("proname");
      }

      result.append("DROP EVENT TRIGGER IF EXISTS ");
      result.append(dbConnection.getMetadata().quoteObjectname(funcName));
      result.append(" CASCADE;\n\n");
      result.append("CREATE EVENT TRIGGER ");
      result.append(dbConnection.getMetadata().quoteObjectname(funcName));
      result.append("\n  ON ");
      result.append(event);
      if (tags != null)
      {
        result.append("\n  WHEN TAG IN (");
        for (int i=0; i < tags.length; i++)
        {
          if (i > 0) result.append(", ");
          result.append('\'');
          result.append(tags[i]);
          result.append('\'');
        }
        result.append(')');
      }
      result.append("\n  EXECUTE PROCEDURE ");
      result.append(funcName);
      result.append("();\n\nCOMMIT;\n");

      result.append("\n---[ ");
      result.append(funcName);
      result.append(" ]---\n");
      result.append(funcSource);
      result.append('\n');

      dbConnection.releaseSavepoint(sp);
    }
    catch (SQLException ex)
    {
      LogMgr.logInfo("PostgresTriggerReader.getEventTriggerSource()", "Could not retrieve event trigger source using:\n" + SqlUtil.replaceParameters(sql, triggerName), ex);
      dbConnection.rollback(sp);
      throw ex;
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
    return result.toString();
  }

  @Override
  public CharSequence getDependentSource(String triggerCatalog, String triggerSchema, String triggerName, TableIdentifier triggerTable)
    throws SQLException
  {

    final String sql
      = "SELECT trgsch.nspname as function_schema, proc.proname as function_name \n" +
      "FROM pg_trigger trg  \n" +
      "  JOIN pg_class tbl ON tbl.oid = trg.tgrelid  \n" +
      "  JOIN pg_proc proc ON proc.oid = trg.tgfoid \n" +
      "  JOIN pg_namespace trgsch ON trgsch.oid = proc.pronamespace \n" +
      "  JOIN pg_namespace tblsch ON tblsch.oid = tbl.relnamespace \n" +
      "WHERE trg.tgname = ? \n" +
      "  AND tblsch.nspname = ? ";

    if (Settings.getInstance().getDebugMetadataSql())
    {
      LogMgr.logInfo("PostgresTriggerReader.getDependentSource()", "Retrieving dependent trigger source using:\n" +
        SqlUtil.replaceParameters(sql, triggerName, triggerTable.getSchema()));
    }

    PreparedStatement stmt = null;
    ResultSet rs = null;
    String funcName = null;
    String funcSchema = null;
    StringBuilder result = null;
    Savepoint sp = null;

    try
    {
      sp = dbConnection.setSavepoint();

      stmt = dbConnection.getSqlConnection().prepareStatement(sql);
      stmt.setString(1, triggerName);
      stmt.setString(2, triggerTable.getSchema());
      rs = stmt.executeQuery();
      if (rs.next())
      {
        funcSchema = rs.getString(1);
        funcName = rs.getString(2);
      }
      dbConnection.releaseSavepoint(sp);
    }
    catch (SQLException ex)
    {
      dbConnection.rollback(sp);
      LogMgr.logError("PostgresTriggerReader.getDependentSource()", "Could not retrieve dependent trigger source using:\n" +
        SqlUtil.replaceParameters(sql, triggerName, triggerTable.getSchema()), ex);
      throw ex;
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }

    if (funcName != null && funcSchema != null)
    {
      ProcedureReader reader = dbMeta.getProcedureReader();
      ProcedureDefinition def = new ProcedureDefinition(null, funcSchema, funcName, DatabaseMetaData.procedureNoResult);
      try
      {
        reader.readProcedureSource(def);
        CharSequence src = def.getSource();
        if (src != null)
        {
          result = new StringBuilder(src.length() + 50);
          result.append("\n---[ ");
          result.append(funcName);
          result.append(" ]---\n");
          result.append(src);
          result.append('\n');
        }
      }
      catch (NoConfigException cfg)
      {
        // nothing to do
      }
    }
    return result;
  }

  /**
   * Triggers on views are supported since Version 9.1
   */
  @Override
  public boolean supportsTriggersOnViews()
  {
    if (dbConnection == null) return false;
    return JdbcUtils.hasMinimumServerVersion(dbConnection, "9.1");
  }

}
