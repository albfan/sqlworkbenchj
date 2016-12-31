/*
 * Db2TableSourceBuilder.java
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
import workbench.db.JdbcUtils;
import workbench.db.ObjectSourceOptions;
import workbench.db.TableIdentifier;
import workbench.db.TableSourceBuilder;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class Db2TableSourceBuilder
  extends TableSourceBuilder
{
  private boolean checkHistoryTable;

  public Db2TableSourceBuilder(WbConnection con)
  {
    super(con);
    checkHistoryTable = JdbcUtils.hasMinimumServerVersion(con, "10.1");
  }

  @Override
  public void readTableOptions(TableIdentifier table, List<ColumnIdentifier> columns)
  {
    if (!checkHistoryTable) return;
    if (table == null) return;
    if (table.getSourceOptions().isInitialized()) return;

    String sql =
      "select periodname, \n" +
      "       begincolname, \n" +
      "       endcolname, \n" +
      "       historytabschema, \n" +
      "       historytabname \n " +
      "from syscat.periods \n" +
      "where tabschema = ? \n" +
      "  and tabname = ? ";
    PreparedStatement stmt = null;
    ResultSet rs = null;

    String tablename = table.getTableName();
    String schema = table.getSchema();


    if (Settings.getInstance().getDebugMetadataSql())
    {
      LogMgr.logInfo("Db2TableSourceBuilder.readTableOptions()", "Query to retrieve table options:\n" + SqlUtil.replaceParameters(sql, schema, tablename));
    }

    try
    {
      stmt = this.dbConnection.getSqlConnection().prepareStatement(sql);
      stmt.setString(1, schema);
      stmt.setString(2, tablename);
      rs = stmt.executeQuery();

      if (rs.next())
      {
        String period = rs.getString(1);
        String begin = rs.getString(2);
        String end = rs.getString(3);
        String histSchema = rs.getString(4);
        String histTab = rs.getString(5);
        TableIdentifier histTable = new TableIdentifier(histSchema, histTab);

        ObjectSourceOptions options = table.getSourceOptions();

        String inline = "PERIOD " + period + " (" + begin + ", " + end + ")";
        options.setInlineOption(inline);

        String addSql =
          "ALTER TABLE " + table.getTableExpression(dbConnection) + "\n" +
          "  ADD VERSIONING USE HISTORY TABLE " + histTable.getTableExpression(dbConnection)  + ";\n";
        options.setAdditionalSql(addSql);
      }
    }
    catch (Exception e)
    {
      LogMgr.logWarning("Db2TableSourceBuilder.readTableOptions()", "Could not retrieve history table", e);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
    table.getSourceOptions().setInitialized();
  }

}
