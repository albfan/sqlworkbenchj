/*
 * OracleIndexPartition.java
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
package workbench.db.oracle;

import java.sql.SQLException;

import workbench.resource.Settings;

import workbench.db.IndexDefinition;
import workbench.db.WbConnection;

/**
 * A class to read information about a partitioned index in Oracle
 *
 * @author Thomas Kellerer
 */
public class OracleIndexPartition
  extends AbstractOraclePartition
{
  private boolean showLocalIndexPartitions;

  public OracleIndexPartition(WbConnection conn)
    throws SQLException
  {
    this(conn, false);
  }

  protected OracleIndexPartition(WbConnection conn, boolean retrieveCompression)
    throws SQLException
  {
    super(conn, retrieveCompression);
    isIndex = true;
    supportsIntervals = false; // indexes do not support the INTERVAL option
    showLocalIndexPartitions = Settings.getInstance().getBoolProperty("workbench.db.oracle.partition.index.local.retrieve", false);
  }

  public boolean hasPartitions(IndexDefinition idx, WbConnection conn)
  {
    if (getPartitionType() != null) return true;

    try
    {
      retrieveDefinition(idx, conn);
      boolean isPartitioned = getPartitionType() != null;
      idx.setPartitioned(isPartitioned);
      return isPartitioned;
    }
    catch (SQLException sql)
    {
      return false;
    }
  }

  public String getLocality()
  {
    return locality;
  }

  @Override
  protected String getRetrieveColumnsSql()
  {
    return
      "-- SQL Workbench \n" +
      "select column_name, \n" +
      "       column_position \n" +
      "from all_part_key_columns \n" +
      "where object_type = 'INDEX' \n" +
      "  and owner = ? \n" +
      "  and name = ? \n" +
      "order by column_position \n";
  }

  @Override
  protected String getRetrievePartitionDefinitionSql()
  {
    return
      "-- SQL Workbench \n" +
      "select owner,  \n" +
      "       table_name, \n" +
      "       partitioning_type,  \n" +
      "       partition_count, \n" +
      "       locality, \n " +
      "       partitioning_key_count, \n" +
      "       subpartitioning_type, \n" +
      "       subpartitioning_key_count, \n" +
      "       def_subpartition_count, \n" +
      "       def_tablespace_name \n" +
      "from all_part_indexes \n" +
      "where owner = ? \n" +
      "  and index_name = ? ";
  }

  @Override
  protected String getRetrievePartitionsSql()
  {
    if (useCompression)
    {
      return
        "-- SQL Workbench \n" +
        "SELECT partition_name,  \n" +
        "       high_value,  \n" +
        "       partition_position, \n" +
        "       subpartition_count, \n" +
        "       compression \n" +
        "FROM all_ind_partitions \n" +
        "WHERE index_owner = ?  \n" +
        "  AND index_name = ? \n" +
        "ORDER BY partition_position";
    }
    return
      "-- SQL Workbench \n" +
      "SELECT partition_name,  \n" +
      "       high_value,  \n" +
      "       partition_position, \n" +
      "       subpartition_count \n" +
      "FROM all_ind_partitions \n" +
      "WHERE index_owner = ?  \n" +
      "  AND index_name = ? \n" +
      "ORDER BY partition_position";
  }

  @Override
  protected String getRetrieveSubColumnsSql()
  {
    return
    "-- SQL Workbench \n" +
    "select name, \n" +
    "       object_type, \n" +
    "       column_name, \n" +
    "       column_position \n" +
    "from all_subpart_key_columns \n" +
    "where object_type = 'INDEX' \n " +
    "  and owner = ? \n" +
    "  and name = ? \n" +
    "order by column_position";
  }

  @Override
  protected String getRetrieveSubPartitionsSql()
  {
    if (useCompression)
    {
    return
      "-- SQL Workbench \n" +
      "select partition_name,  \n" +
      "       subpartition_name,  \n" +
      "       high_value, \n" +
      "       subpartition_position, \n" +
      "       compression \n" +
      "from all_ind_subpartitions \n" +
      "where index_owner = ?  \n" +
      "  and index_name = ?  \n" +
      "order by subpartition_position";
    }
    return
      "-- SQL Workbench \n" +
      "select partition_name,  \n" +
      "       subpartition_name,  \n" +
      "       high_value, \n" +
      "       subpartition_position \n" +
      "from all_ind_subpartitions \n" +
      "where index_owner = ?  \n" +
      "  and index_name = ?  \n" +
      "order by subpartition_position";
  }

  @Override
  protected boolean shouldRetrievePartitions()
  {
    if ("LOCAL".equalsIgnoreCase(locality) && !showLocalIndexPartitions)
    {
      return false;
    }
    return true;
  }
}
