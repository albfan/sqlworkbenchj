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

import workbench.db.TableIdentifier;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresPartition
{
  private final String name;
  private final String schema;
  private String definition;
  private String subPartitionDefinition;
  private String subPartitionStrategy;

  // for sub-partitions
  private TableIdentifier parentTable;

  public PostgresPartition(String partitionSchema, String partitionName)
  {
    this.name = partitionName;
    this.schema = partitionSchema;
  }

  public String getSubPartitionStrategy()
  {
    return subPartitionStrategy;
  }

  public void setSubPartitionStrategy(String subPartitionStrategy)
  {
    this.subPartitionStrategy = subPartitionStrategy;
  }

  /**
   * Return the partition strategy and definition for a sub-partition.
   */
  public String getSubPartitionDefinition()
  {
    return subPartitionDefinition;
  }

  /**
   * Set the partition strategy and definition for a sub-partition.
   * @param subPartitionDefinition
   */
  public void setSubPartitionDefinition(String subPartitionDefinition)
  {
    this.subPartitionDefinition = subPartitionDefinition;
  }

  public TableIdentifier getParentTable()
  {
    return parentTable;
  }

  public void setParentTable(TableIdentifier parentTable)
  {
    this.parentTable = parentTable;
  }

  public void setDefinition(String partitionDefinition)
  {
    this.definition = partitionDefinition;
  }

  public String getDefinition()
  {
    return definition;
  }

  public String getName()
  {
    return name;
  }

  public String getSchema()
  {
    return schema;
  }



}
