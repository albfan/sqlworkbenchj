/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.db.oracle;

import java.sql.SQLException;

import workbench.db.DefaultTriggerReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

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
    if (OracleUtils.getUseOracleDBMSMeta(OracleUtils.DbmsMetadataTypes.trigger))
    {
      try
      {
        return OracleUtils.getDDL(dbConnection, "TRIGGER", triggerName, triggerSchema);
      }
      catch (SQLException ex)
      {
        // logging was already done
        // by catching the exception we let the ancestor do the job.
      }
    }
    return super.getTriggerSource(triggerCatalog, triggerSchema, triggerName, triggerTable, trgComment, includeDependencies);
  }

}
