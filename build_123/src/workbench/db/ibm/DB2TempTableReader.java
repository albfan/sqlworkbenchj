/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016 Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0 (the "License")
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.ibm;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.DbMetadata;
import workbench.db.ObjectListAppender;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.SqlUtil;

/**
 * An ObjectListAppender for DB2/LUW to read temporary tables.
 *
 * The DB2 JDBC driver does not return created global temporary tables. This ObjectListAppender retrieves them
 * from syscat.tables and adds them to the regular list of tables.
 *
 * @author Thomas Kellerer
 */
public class DB2TempTableReader
  implements ObjectListAppender
{
  private static final String TABLE_TYPE = "GLOBAL TEMPORARY";

  @Override
  public boolean extendObjectList(WbConnection con, DataStore result, String catalog, String schema, String objects, String[] requestedTypes)
  {
    if (!DbMetadata.typeIncluded("TABLE", requestedTypes)) return false;

    String sql =
      "select tabschema, \n" +
      "       tabname, \n" +
      "       remarks \n" +
      "from syscat.tables \n" +
      "where type = 'G' ";

    int schemaIndex = 0;
    int tableIndex = 0;
    if (schema != null)
    {
      if (schema.indexOf('%') > -1)
      {
        sql += "\n  and tabschema like ? ";
      }
      else
      {
        sql += "\n  and tabschema = ? ";
      }
      schemaIndex = 1;
    }

    if (objects != null)
    {
      if (objects.indexOf('%') > -1)
      {
        sql += "\n  and tabname like ? ";
      }
      else
      {
        sql += "\n  and tabname = ? ";
      }
      tableIndex = schemaIndex + 1;
    }

    sql += "\nfor read only";

    if (Settings.getInstance().getDebugMetadataSql())
    {
      LogMgr.logInfo("Db2TempTableReader.extendObjectList()", "Reading temp tables using:\n" + SqlUtil.replaceParameters(sql, schema, objects));
    }

    PreparedStatement stmt = null;
    ResultSet rs = null;
    int count = 0;

    try
    {
      stmt = con.getSqlConnection().prepareStatement(sql);
      if (schemaIndex > 0)
      {
        stmt.setString(schemaIndex, schema);
      }
      if (tableIndex > 0)
      {
        stmt.setString(tableIndex, objects);
      }
      rs = stmt.executeQuery();
      while (rs.next())
      {
        int row = result.addRow();
        String tabSchema = rs.getString(1);
        String tabName = rs.getString(2);
        String remarks = rs.getString(3);

        result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA, tabSchema);
        result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME, tabName);
        result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_REMARKS, remarks);
        result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE, "TABLE");

        TableIdentifier tbl = new TableIdentifier(null, tabSchema, tabName, false);
        tbl.setType("TABLE");
        tbl.setNeverAdjustCase(true);
        tbl.setComment(remarks);
        tbl.getSourceOptions().setTypeModifier(TABLE_TYPE);
        tbl.getSourceOptions().setInitialized();
        result.getRow(row).setUserObject(tbl);
        count ++;
      }
    }
    catch (Exception ex)
    {
      LogMgr.logError("Db2TempTableReader.extendObjectList()", "Error reading temp tables using:\n" + SqlUtil.replaceParameters(sql, schema, objects), ex);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }

    return count > 0;
  }

}
