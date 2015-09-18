/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.db.oracle;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import workbench.db.DefaultTriggerReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleTriggerReader
  extends DefaultTriggerReader
{

  public OracleTriggerReader(WbConnection conn)
  {
    super(conn);
  }

  @Override
  public String getTriggerSource(String triggerCatalog, String triggerSchema, String triggerName, TableIdentifier triggerTable, String trgComment, boolean includeDependencies)
    throws SQLException
  {
    if (OracleUtils.useDBMSMetaData(OracleUtils.DbmsMetadataTypes.trigger))
    {
      try
      {
        return retrieveUsingDbmsMetadata(triggerSchema, triggerName);
      }
      catch (SQLException ex)
      {
        // ignore logging already done, but catching the exception we let the ancestor do the job.
      }
    }
    return super.getTriggerSource(triggerCatalog, triggerSchema, triggerName, triggerTable, trgComment, includeDependencies);
  }


  private String retrieveUsingDbmsMetadata(String triggerSchema, String triggerName)
    throws SQLException
  {

    ResultSet rs = null;
    Statement stmt = null;
    String source = null;
    String sql = "select dbms_metadata.get_ddl('TRIGGER', '" + triggerName + "', '" + triggerSchema + "') from dual";

    try
    {
      OracleUtils.initDBMSMetadata(dbConnection);
      if (Settings.getInstance().getDebugMetadataSql())
      {
        LogMgr.logDebug("OracleTriggerReader.retrieveUsingDbmsMetadata()", "Reading trigger source using:\n" + sql);
      }
      stmt = dbConnection.createStatementForQuery();
      rs = stmt.executeQuery(sql);
      if (rs.next())
      {
        source = rs.getString(1);
      }
    }
    catch (SQLException ex)
    {
      LogMgr.logError("OracleTriggerReader.retrieveUsingDbmsMetadata()", "Could not retrieve trigger source using:\n" + sql, ex);
      throw ex;
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
      OracleUtils.resetDBMSMetadata(dbConnection);
    }
    return source;
  }
}
