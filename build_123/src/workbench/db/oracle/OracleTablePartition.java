/*
 * OracleTablePartition.java
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

import workbench.db.WbConnection;

/**
 * A class to read information about a partitioned table in Oracle
 *
 * @author Thomas Kellerer
 */
public class OracleTablePartition
  extends AbstractOraclePartition
{

  public OracleTablePartition(WbConnection conn)
    throws SQLException
  {
    super(conn, true);
    isIndex = false;
  }

  protected OracleTablePartition(WbConnection conn, boolean retrieveCompression)
    throws SQLException
  {
    super(conn, retrieveCompression);
    isIndex = false;
  }

  @Override
  protected String getRetrieveColumnsSql()
  {
    return
      "-- SQL Workbench \n" +
      "select column_name, \n" +
      "       column_position \n" +
      "from all_part_key_columns \n" +
      "where object_type = 'TABLE' \n" +
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
      "       partitioning_key_count, \n" +
      "       subpartitioning_type, \n" +
      "       subpartitioning_key_count, \n" +
      "       def_subpartition_count, " +
      "       " + (supportsIntervals ? "interval" : "null as interval") + ", \n" +
      "       " + (supportsRefPartitions ? "ref_ptn_constraint_name" : "null as ref_ptn_constraint_name") + ", \n" +
      "       def_tablespace_name \n " +
      "from all_part_tables pt \n" +
      "where pt.owner = ? \n" +
      "  and pt.table_name = ? ";
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
        "FROM all_tab_partitions \n" +
        "WHERE table_owner = ?  \n" +
        "  AND table_name = ? \n" +
        "ORDER BY partition_position";
    }
    return
      "-- SQL Workbench \n" +
      "SELECT partition_name,  \n" +
      "       high_value,  \n" +
      "       partition_position, \n" +
      "       subpartition_count \n" +
      "FROM all_tab_partitions \n" +
      "WHERE table_owner = ?  \n" +
      "  AND table_name = ? \n" +
      "ORDER BY partition_position";  }

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
    "where owner = ? \n" +
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
        "from all_tab_subpartitions \n" +
        "where table_owner = ?  \n" +
        "  and table_name = ?  \n" +
        "order by subpartition_position";
    }
    return
      "-- SQL Workbench \n" +
      "select partition_name,  \n" +
      "       subpartition_name,  \n" +
      "       high_value, \n" +
      "       subpartition_position \n" +
      "from all_tab_subpartitions \n" +
      "where table_owner = ?  \n" +
      "  and table_name = ?  \n" +
      "order by subpartition_position";
  }

}
