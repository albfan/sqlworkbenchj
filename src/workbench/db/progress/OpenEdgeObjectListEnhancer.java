/*
 * SqlServerObjectListEnhancer.java
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
package workbench.db.progress;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.TreeMap;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.DbMetadata;
import workbench.db.ObjectListEnhancer;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.CaseInsensitiveComparator;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class OpenEdgeObjectListEnhancer
  implements ObjectListEnhancer
{

  @Override
  public void updateObjectList(WbConnection con, DataStore result, String aCatalog, String aSchema, String objects, String[] requestedTypes)
  {
    if (Settings.getInstance().getBoolProperty("workbench.db." + DbMetadata.DBID_OPENEDGE + ".remarks.object.retrieve", false))
    {
      updateObjectRemarks(con, result, aCatalog, aSchema, objects);
    }
  }

  protected void updateObjectRemarks(WbConnection con, DataStore result, String catalog, String schema, String objects)
  {
    if (result == null) return;
    if (result.getRowCount() == 0) return;

    String object = null;
    if (result.getRowCount() == 1)
    {
      object = result.getValueAsString(0, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
    }

    Map<String, String> remarks = readRemarks(con, schema, object);

    for (int row = 0; row < result.getRowCount(); row++)
    {
      String name = result.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
      String objectSchema = result.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA);

      String remark = remarks.get(objectSchema + "." + name);
      if (remark != null)
      {
        result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_REMARKS, remark);
      }
    }
  }

  public Map<String, String> readRemarks(WbConnection con, String schema, String object)
  {

    PreparedStatement stmt = null;
    ResultSet rs = null;

    if (schema == null)
    {
      schema = con.getMetadata().getCurrentSchema();
    }

    int schemaIndex = -1;
    int tableIndex = -1;

    String sql =
      "select owner, tbl, description \n" +
      "from sysprogress.systables_full \n" +
      "where description is not null \n" +
      "  and description <> '' \n" +
      "  and tbltype = 'T' \n";

    if (schema != null)
    {
      schemaIndex = 1;
      sql += "and owner = ? \n";
    }

    if (object != null)
    {
      tableIndex = (schemaIndex == -1 ? 1 : 2);
      sql += " and tbl = ? ";
    }

    Map<String, String> remarks = new TreeMap<>(CaseInsensitiveComparator.INSTANCE);

    long start = System.currentTimeMillis();
    try
    {
      if (Settings.getInstance().getDebugMetadataSql())
      {
        LogMgr.logInfo("OpenEdgeObjectListEnhancer.updateObjectRemarks()", "Retrieving table remarks using:\n" + SqlUtil.replaceParameters(sql, schema, object));
      }
      stmt = con.getSqlConnection().prepareStatement(sql);
      if (schemaIndex > 0) stmt.setString(schemaIndex, schema);
      if (tableIndex > 0) stmt.setString(tableIndex, object);

      rs = stmt.executeQuery();
      while (rs.next())
      {
        String objectSchema = rs.getString(1);
        String objectName = rs.getString(2);
        String remark = rs.getString(3);
        if (objectName != null && StringUtil.isNonEmpty(remark))
        {
          remarks.put(objectSchema + "." + objectName.trim(), remark);
        }
      }
      SqlUtil.closeResult(rs);
    }
    catch (Exception e)
    {
      LogMgr.logError("OpenEdgeObjectListEnhancer.updateObjectRemarks()", "Error retrieving remarks using:\n " + SqlUtil.replaceParameters(sql, schema, object), e);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
    long duration = System.currentTimeMillis() - start;
    LogMgr.logDebug("OpenEdgeObjectListEnhancer.updateObjectRemarks()", "Retrieving table remarks took: " + duration + "ms");
    return remarks;
  }
}
