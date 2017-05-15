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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresPartitionReader
{
  public static final String OPTION_KEY_STRATEGY = "partition_strategy";
  public static final String OPTION_KEY_EXPRESSION = "partition_expression";

  private final TableIdentifier table;
  private final WbConnection dbConnection;
  private String strategy;
  private String partitionExpression;
  private String partitionDefinition;
  private List<PostgresPartition> partitions;

  public PostgresPartitionReader(TableIdentifier table, WbConnection conn)
  {
    this.table = table;
    this.dbConnection = conn;
  }

  public List<PostgresPartition> getTablePartitions()
  {
    return partitions;
  }

  public String getStrategy()
  {
    return strategy;
  }

  public String getPartitionExpression()
  {
    return partitionExpression;
  }

  public String getPartitionDefinition()
  {
    return partitionDefinition;
  }

  public String getCreatePartitions()
  {
    if (partitions == null) return null;

    String baseTable = table.getTableExpression(dbConnection);

    StringBuilder result = new StringBuilder(partitions.size() * 100);
    for (PostgresPartition part : partitions)
    {
      result.append(generatePartitionDDL(part, baseTable, dbConnection));
      result.append(";\n\n");
    }
    return result.toString();
  }

  public static String generatePartitionDDL(PostgresPartition partition, String baseTable, WbConnection dbConnection)
  {
    if (partition == null) return null;

    TableIdentifier parent = partition.getParentTable();

    String tableOf = parent == null ? baseTable : parent.getTableExpression(dbConnection);

    TableIdentifier name = new TableIdentifier(partition.getSchema(), partition.getName());
    String partSQL =
      "CREATE TABLE " + name.getTableExpression(dbConnection) + "\n" +
      "  PARTITION OF " + tableOf + " " + partition.getDefinition();

    if (partition.getSubPartitionDefinition() != null)
    {
      partSQL += "\n" +
        "  PARTITION BY " + partition.getSubPartitionStrategy() + " " + partition.getSubPartitionDefinition();
    }

    return partSQL;
  }

  public void readPartitionInformation()
  {
    readPartitioningDefinition();
    readPartitions();
  }

  private void readPartitions()
  {
    String sql =
      "with recursive inh as ( \n" +
      "\n" +
      "  select i.inhrelid, null::text as parent  \n" +
      "  from pg_catalog.pg_inherits i  \n" +
      "    join pg_catalog.pg_class cl on i.inhparent = cl.oid \n" +
      "    join pg_catalog.pg_namespace nsp on cl.relnamespace = nsp.oid \n" +
      "  where nsp.nspname = ? \n" +
      "    and cl.relname = ? \n" +
      "  union all \n" +
      "\n" +
      "  select i.inhrelid, (i.inhparent::regclass)::text \n" +
      "  from inh \n" +
      "    join pg_catalog.pg_inherits i on (inh.inhrelid = i.inhparent) \n" +
      ") \n" +
      "select c.relname as partition_name, \n" +
      "       n.nspname as partition_schema,  \n" +
      "       pg_get_expr(c.relpartbound, c.oid, true) as partition_expression, " +
      "       pg_get_expr(p.partexprs, c.oid, true) as sub_partition, \n" +
      "       parent, \n" +
      "       case p.partstrat \n" +
      "         when 'l' then 'LIST' \n" +
      "         when 'r' then 'RANGE' \n" +
      "       end as sub_partition_strategy \n" +
      "from inh \n" +
      "  join pg_catalog.pg_class c on inh.inhrelid = c.oid \n" +
      "  join pg_catalog.pg_namespace n on c.relnamespace = n.oid \n" +
      "  left join pg_partitioned_table p on p.partrelid = c.oid \n" +
      "order by n.nspname, c.relname";

    PreparedStatement pstmt = null;
    ResultSet rs = null;
    Savepoint sp = null;

    partitions = new ArrayList<>();

    try
    {
      sp = dbConnection.setSavepoint();

      pstmt = dbConnection.getSqlConnection().prepareStatement(sql);
      pstmt.setString(1, table.getRawSchema());
      pstmt.setString(2, table.getRawTableName());
      rs = pstmt.executeQuery();

      while (rs.next())
      {
        String partName = rs.getString("partition_name");
        String schema = rs.getString("partition_schema");
        String partExpr = rs.getString("partition_expression");
        String subPartExpr = rs.getString("sub_partition");
        String parent = rs.getString("parent");

        PostgresPartition partition = new PostgresPartition(schema, partName);
        partition.setDefinition(partExpr);
        partition.setSubPartitionDefinition(subPartExpr);
        partition.setSubPartitionStrategy(rs.getString("sub_partition_strategy"));
        if (parent != null)
        {
          TableIdentifier p = new TableIdentifier(parent);
          partition.setParentTable(p);
        }
        partitions.add(partition);
      }

      dbConnection.releaseSavepoint(sp);
    }
    catch (Exception ex)
    {
      dbConnection.rollback(sp);
      LogMgr.logError("PostgresPartitionReader.readPartitions()",
        "Error reading partitions using:\n" + SqlUtil.replaceParameters(sql, table.getSchema(), table.getTableName()), ex);
    }
    finally
    {
      SqlUtil.closeAll(rs, pstmt);
    }
  }

  private void readPartitioningDefinition()
  {
    String sql =
      "select p.partstrat, \n" +
      "       case \n" +
      "         when p.partexprs is null then cols.columns \n" +
      "         else pg_get_expr(p.partexprs, t.oid, true)\n" +
      "       end as partition_expression\n" +
      "from pg_partitioned_table p\n" +
      "  join pg_class t on t.oid = p.partrelid\n" +
      "  join pg_namespace n on n.oid = t.relnamespace\n" +
      "  left join lateral (\n" +
      "    select cols.attrelid, string_agg(cols.attname, ',') as columns\n" +
      "    from pg_attribute cols\n" +
      "    where cols.attrelid = t.oid\n" +
      "      and cols.attnum = any (p.partattrs)\n" +
      "    group by cols.attrelid\n" +
      "  ) as cols on cols.attrelid = t.oid \n" +
      "where n.nspname = ? \n" +
      "  and t.relname = ? ";

    PreparedStatement pstmt = null;
    ResultSet rs = null;
    Savepoint sp = null;

    if (Settings.getInstance().getDebugMetadataSql())
    {
      LogMgr.logInfo("PostgresPartitionReader.readPartitioningDefinition()",
        "Retrieving partitioning information using:\n" + SqlUtil.replaceParameters(sql, table.getSchema(), table.getTableName()));
    }

    try
    {
      sp = dbConnection.setSavepoint();

      pstmt = dbConnection.getSqlConnection().prepareStatement(sql);
      pstmt.setString(1, table.getRawSchema());
      pstmt.setString(2, table.getRawTableName());
      rs = pstmt.executeQuery();

      if (rs.next())
      {
        String strat = rs.getString("partstrat");
        partitionExpression = rs.getString("partition_expression");
        if ("r".equals(strat))
        {
          strategy = "RANGE";
        }
        else if ("l".equals(strat))
        {
          strategy += "LIST";
        }
        partitionDefinition = "PARTITION BY " + strategy + " (" + partitionExpression + ")";
      }

      dbConnection.releaseSavepoint(sp);
    }
    catch (Exception ex)
    {
      dbConnection.rollback(sp);
      LogMgr.logError("PostgresPartitionReader.readPartitioningDefinition()",
        "Error retrieving partitioning information using :\n" + SqlUtil.replaceParameters(sql, table.getSchema(), table.getTableName()), ex);
    }
    finally
    {
      SqlUtil.closeAll(rs, pstmt);
    }
  }

  /**
   * Check if the given table is in fact a partition in Postgres 10.
   *
   * If it is a partition, the definition is returned, otherwise null
   *
   * @param table          the table to check
   * @param dbConnection   the connection to use
   * @return null if the table is not a parition, the definition otherwise
   */
  public static PostgresPartition getPartitionDefinition(TableIdentifier table, WbConnection dbConnection)
  {
    String sql =
      "select bs.nspname as base_table_schema, \n" +
      "       base.relname as base_table, \n" +
      "       pg_get_expr(c.relpartbound, c.oid, true) as partition_expression, \n" +
      "       pg_get_expr(p.partexprs, c.oid, true) as sub_partition, \n" +
      "       case p.partstrat \n" +
      "         when 'l' then 'LIST' \n" +
      "         when 'r' then 'RANGE' \n" +
      "       end as sub_partition_strategy \n" +
      "from pg_catalog.pg_inherits i\n" +
      "  join pg_catalog.pg_class c on i.inhrelid = c.oid \n" +
      "  join pg_catalog.pg_namespace n on c.relnamespace = n.oid \n" +
      "  join pg_partitioned_table p on p.partrelid = i.inhparent\n" +
      "  join pg_class base on base.oid = p.partrelid\n" +
      "  join pg_namespace bs on bs.oid = base.relnamespace\n" +
      "where n.nspname = ? \n" +
      "  and c.relname = ?";

    PreparedStatement pstmt = null;
    ResultSet rs = null;
    Savepoint sp = null;

    if (Settings.getInstance().getDebugMetadataSql())
    {
      LogMgr.logInfo("PostgresPartitionReader.getCreateForPartition()",
        "Retrieving partition information using:\n" + SqlUtil.replaceParameters(sql, table.getSchema(), table.getTableName()));
    }

    PostgresPartition result = null;
    try
    {
      sp = dbConnection.setSavepoint();

      pstmt = dbConnection.getSqlConnection().prepareStatement(sql);
      pstmt.setString(1, table.getRawSchema());
      pstmt.setString(2, table.getRawTableName());
      rs = pstmt.executeQuery();

      if (rs.next())
      {
        String name = rs.getString("base_table");
        String schema = rs.getString("base_table_schema");
        TableIdentifier tbl = new TableIdentifier(schema, name);
        result = new PostgresPartition(table.getRawSchema(), table.getRawTableName());
        result.setParentTable(tbl);
        result.setDefinition(rs.getString("partition_expression"));
        result.setSubPartitionStrategy(rs.getString("sub_partition_strategy"));
        result.setSubPartitionDefinition(rs.getString("sub_partition"));
      }

      dbConnection.releaseSavepoint(sp);
    }
    catch (Exception ex)
    {
      dbConnection.rollback(sp);
      LogMgr.logError("PostgresPartitionReader.getCreateForPartition()",
        "Error retrieving partition information using :\n" + SqlUtil.replaceParameters(sql, table.getSchema(), table.getTableName()), ex);
    }
    finally
    {
      SqlUtil.closeAll(rs, pstmt);
    }
    return result;
  }

}
