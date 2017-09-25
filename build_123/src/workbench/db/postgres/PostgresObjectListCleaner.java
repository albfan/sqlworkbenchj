/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017 Thomas Kellerer.
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
package workbench.db.postgres;

import java.sql.ResultSet;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.DbMetadata;
import workbench.db.ObjectListCleaner;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresObjectListCleaner
  implements ObjectListCleaner
{

  public static boolean removePartitions()
  {
    return Settings.getInstance().getBoolProperty("workbench.db.postgresql.partitions.tablelist.remove", false);
  }

  @Override
  public void cleanupObjectList(WbConnection con, DataStore result, String catalogPattern, String schemaPattern, String objectNamePattern, String[] requestedTypes)
  {
    if (DbMetadata.typeIncluded("TABLE", requestedTypes))
    {
      removePartitions(con, result);
    }
  }

  private void removePartitions(WbConnection con, DataStore result)
  {
    List<TableIdentifier> partitions = getAllPartitions(con);
    int rowCount = result.getRowCount();
    for (int row = rowCount - 1; row >= 0; row --)
    {
      String schema = result.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA);
      String table = result.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
      TableIdentifier tbl = new TableIdentifier(schema, table);
      if (TableIdentifier.findTableByNameAndSchema(partitions, tbl) != null)
      {
        LogMgr.logDebug("PostgresObjectListCleaner.removePartitions()", "Removing: " + schema + "." + table);
        result.deleteRow(row);
      }
    }
  }

  private List<TableIdentifier> getAllPartitions(WbConnection conn)
  {
    List<TableIdentifier> partitions = new ArrayList<>();
    String sql =
      "with recursive inh as ( \n" +
      "\n" +
      "  select i.inhrelid, i.inhparent\n" +
      "  from pg_catalog.pg_inherits i  \n" +
      "  where i.inhparent in (select partrelid from pg_partitioned_table)\n" +
      "  \n" +
      "  union all \n" +
      "\n" +
      "  select i.inhrelid, i.inhparent\n" +
      "  from inh \n" +
      "    join pg_catalog.pg_inherits i on inh.inhrelid = i.inhparent\n" +
      ") \n" +
      "select n.nspname as partition_schema,\n" +
      "       c.relname as partition_name\n" +
      "from inh \n" +
      "  join pg_catalog.pg_class c on inh.inhrelid = c.oid \n" +
      "  join pg_catalog.pg_namespace n on c.relnamespace = n.oid";

    Statement stmt = null;
    ResultSet rs = null;
    Savepoint sp = null;

    long start = System.currentTimeMillis();
    try
    {
      sp = conn.setSavepoint();

      stmt = conn.createStatementForQuery();
      rs = stmt.executeQuery(sql);

      if (Settings.getInstance().getDebugMetadataSql())
      {
        LogMgr.logInfo("PostgresPartitionReader.getAllPartitions()", "Retrieving all partitions using:\n" + sql);
      }

      while (rs.next())
      {
        String schema = rs.getString(1);
        String name = rs.getString(2);
        partitions.add(new TableIdentifier(schema, name));
      }

      conn.releaseSavepoint(sp);
    }
    catch (Exception ex)
    {
      conn.rollback(sp);
      LogMgr.logError("PostgresPartitionReader.getAllPartitions()", "Error retrieving all partitions using:\n" + sql, ex);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }

    long duration = System.currentTimeMillis() - start;
    LogMgr.logDebug("PostgresPartitionReader.getAllPartitions()", "Reading all partitions took: " + duration + "ms");
    return partitions;
  }
}
