/*
 * InformixTableSourceBuilder.java
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
package workbench.db.ibm;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.ObjectSourceOptions;
import workbench.db.TableIdentifier;
import workbench.db.TableSourceBuilder;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A TableSourceBuilder for Informix that will read the lock mode and extent settings for the table.
 *
 * The options will be stored in the TableSourceOptions of the table.
 *
 * @author Thomas Kellerer
 */
public class InformixTableSourceBuilder
  extends TableSourceBuilder
{

  public InformixTableSourceBuilder(WbConnection con)
  {
    super(con);
  }

  /**
   * Read additional table options.
   *
   * @param table    the table to process
   * @param columns  the columns (not used)
   *
   * @see ObjectSourceOptions#getAdditionalSql()
   */
  @Override
  public void readTableOptions(TableIdentifier table, List<ColumnIdentifier> columns)
  {
    if (Settings.getInstance().getBoolProperty("workbench.db.informix_dynamic_server.tablesource.lockmode", true))
    {
      ObjectSourceOptions option = table.getSourceOptions();
      if (!option.isInitialized())
      {
        readLockMode(table);
        option.setInitialized();
      }
    }
  }

  private void readLockMode(TableIdentifier table)
  {
    boolean showExtents = Settings.getInstance().getBoolProperty("workbench.db.informix_dynamic_server.showextentinfo", true);
    String systemSchema = Settings.getInstance().getProperty("workbench.db.informix_dynamic_server.systemschema", "informix");
    TableIdentifier syst = new TableIdentifier(table.getRawCatalog(), systemSchema, "systables");
    String systables = syst.getFullyQualifiedName(dbConnection);

    String sql = "select locklevel ";

    if (showExtents)
    {
      sql +=
        ",\n" +
        "       fextsize, \n" +
        "       nextsize \n";
    }

    sql +=
      "from " + systables + " \n" +
      "where tabname = ? \n" +
      "  and owner = ? \n";

    if (Settings.getInstance().getDebugMetadataSql())
    {
      LogMgr.logInfo("InformixTableSourceBuilder.readLockMode()",
        "Query to retrieve lock mode:\n" + SqlUtil.replaceParameters(sql, table.getTableName(), table.getSchema()));
    }

    PreparedStatement pstmt = null;
    ResultSet rs = null;

    try
    {
      pstmt = dbConnection.getSqlConnection().prepareStatement(sql);
      pstmt.setString(1, table.getRawTableName());
      pstmt.setString(2, table.getRawSchema());

      rs = pstmt.executeQuery();
      if (rs.next())
      {
        ObjectSourceOptions option = table.getSourceOptions();
        String lvl = rs.getString(1);

        int fext = -1;
        int next = -1;
        if (showExtents)
        {
          fext = rs.getInt(2);
          if (rs.wasNull()) fext = -1;

          next = rs.getInt(3);
          if (rs.wasNull()) next = -1;
        }

        String options = "";
        if (fext > -1 && next > -1)
        {
          table.getSourceOptions().addConfigSetting("extent", Integer.toString(fext));
          table.getSourceOptions().addConfigSetting("next_extent", Integer.toString(next));
          options = "EXTENT SIZE " + Integer.toString(fext) + "\nNEXT SIZE " + Integer.toString(next);
        }

        if (StringUtil.isNonEmpty(lvl))
        {
          switch (lvl.charAt(0))
          {
            case 'B':
            case 'P':
              if (options.length() > 0) options += "\n";
              options += "LOCK MODE PAGE";
              table.getSourceOptions().addConfigSetting("lock_mode", "page");
              break;
            case 'R':
              if (options.length() > 0) options += "\n";
              table.getSourceOptions().addConfigSetting("lock_mode", "row");
              options += "LOCK MODE ROW";
          }
        }
        option.setTableOption(options);
      }
    }
    catch (Exception e)
    {
      LogMgr.logError("InformixTableSourceBuilder.readLockMode()", "Error when retrieving lock mode using:\n" +
        SqlUtil.replaceParameters(sql, table.getTableName(), table.getSchema()), e);
    }
    finally
    {
      SqlUtil.closeAll(rs, pstmt);
    }
  }
}
