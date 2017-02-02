/*
 * AbstractOraclePartition.java
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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.DbObject;
import workbench.db.JdbcUtils;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * An abstract class to read information about a partitioned tables or indexes in Oracle
 *
 * @author Thomas Kellerer
 */
public abstract class AbstractOraclePartition
{
  private String type;
  private List<String> columns;
  private List<OraclePartitionDefinition> partitions;
  private String subType;
  private int defaultSubpartitionCount;
  private List<String> subColumns;
  protected boolean useCompression;
  protected boolean supportsIntervals;
  protected boolean isIndex;
  protected String locality; // only used for indexes
  protected String intervalDefinition;
  protected String tableSpace;
  protected String objectOwner;
  protected String defaultUserTablespace;
  protected String currentUser;
  protected String refPartitionConstraint;
  protected boolean retrievePartitionsForLocalIndex;
  protected boolean supportsRefPartitions;

  public AbstractOraclePartition(WbConnection conn)
    throws SQLException
  {
    this(conn, true);
  }

  protected AbstractOraclePartition(WbConnection conn, boolean retrieveCompression)
    throws SQLException
  {
    boolean is11r1 = JdbcUtils.hasMinimumServerVersion(conn, "11.1");
    useCompression = retrieveCompression && is11r1;
    supportsIntervals = is11r1;
    currentUser = conn.getMetadata().getCurrentSchema();
    supportsRefPartitions = is11r1;
  }

  public void retrieve(DbObject object, WbConnection conn)
    throws SQLException
  {
    if (object == null) return;

    if (OracleUtils.checkDefaultTablespace())
    {
      defaultUserTablespace = OracleUtils.getDefaultTablespace(conn);
    }
    boolean hasPartitions = retrieveDefinition(object, conn);
    if (hasPartitions)
    {
      retrieveColumns(object, conn);
      retrievePartitions(object, conn);
    }
  }

  protected abstract String getRetrievePartitionDefinitionSql();
  protected abstract String getRetrieveColumnsSql();
  protected abstract String getRetrieveSubColumnsSql();
  protected abstract String getRetrieveSubPartitionsSql();
  protected abstract String getRetrievePartitionsSql();

  public List<OraclePartitionDefinition> getPartitions()
  {
    if (!isPartitioned()) return Collections.emptyList();
    return Collections.unmodifiableList(partitions);
  }

  public boolean isPartitioned()
  {
    return columns != null && !columns.isEmpty();
  }

  public List<String> getColumns()
  {
    if (columns == null) return Collections.emptyList();
    return Collections.unmodifiableList(columns);
  }

  public boolean isRefPartition()
  {
    return "REFERENCE".equalsIgnoreCase(type);
  }

  public String getPartitionType()
  {
    return type;
  }

  public String getSourceForTableDefinition()
  {
    return getSource(true, "", true);
  }

  public String getSourceForTableDefinition(boolean includeTablespace)
  {
    return getSource(true, "", includeTablespace);
  }

  public String getSourceForTableDefinition(String indent)
  {
    return getSource(true, indent, true);
  }

  public String getSourceForTableDefinition(String indent, boolean includeTablespace)
  {
    return getSource(true, indent, includeTablespace);
  }

  public String getSourceForIndexDefinition(String indent)
  {
    return getSource(false, indent, true);
  }

  public String getSourceForIndexDefinition()
  {
    return getSource(false, "", true);
  }

  private String getSource(boolean forTable, String indent, boolean includeTablespace)
  {
    if (!this.isPartitioned()) return null;
    StringBuilder result = new StringBuilder(partitions.size() * 15);
    if (locality != null)
    {
      result.append(indent);
      result.append(locality);
    }
    if (locality == null)
    {
      result.append(indent);
      result.append("PARTITION BY ");
      result.append(type);
      result.append(' ');
      if (refPartitionConstraint != null)
      {
        result.append('(');
        result.append(SqlUtil.quoteObjectname(refPartitionConstraint));
        result.append(')');
      }
      else if (columns != null)
      {
        result.append('(');
        result.append(StringUtil.listToString(columns, ','));
        result.append(')');
      }
      if (StringUtil.isNonBlank(intervalDefinition))
      {
        result.append(" INTERVAL (");
        result.append(intervalDefinition);
        result.append(") ");
      }
      if (!"NONE".equals(subType))
      {
        result.append('\n');
        result.append(indent);
        result.append("SUBPARTITION BY ");
        result.append(subType);
        result.append(" (");
        result.append(StringUtil.listToString(subColumns, ','));
        result.append(')');
        if (defaultSubpartitionCount > 1)
        {
          result.append('\n');
          result.append(indent);
          result.append("SUBPARTITIONS ");
          result.append(defaultSubpartitionCount);
        }
      }
    }
    if (partitions.size() > 0)
    {
      result.append('\n');
      result.append(indent);
      result.append("(\n");
      int maxLength = forTable ? OraclePartitionDefinition.getMaxPartitionNameLength(partitions): 0;
      for (int i=0; i < partitions.size(); i++)
      {
        if (i > 0)
        {
          result.append(',');
          result.append(indent);
          result.append('\n');
        }
        result.append(partitions.get(i).getSource(forTable, maxLength, indent));
      }
      result.append("\n");
      result.append(indent);
      result.append(')');
    }
    if (includeTablespace && OracleUtils.shouldAppendTablespace(tableSpace, defaultUserTablespace, objectOwner, currentUser))
    {
      result.append('\n');
      result.append(indent);
      result.append("TABLESPACE ");
      result.append(tableSpace);
    }
    return result.toString();
  }


  protected boolean retrieveDefinition(DbObject dbObject, WbConnection conn)
    throws SQLException
  {
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    int subKeyCount = 0;

    String retrievePartitionDefinitionSql = getRetrievePartitionDefinitionSql();

    long start = System.currentTimeMillis();

    try
    {
      pstmt = conn.getSqlConnection().prepareStatement(retrievePartitionDefinitionSql);
      if (Settings.getInstance().getDebugMetadataSql())
      {
        LogMgr.logDebug(getClassName() + ".retrieveDefinition()", "Retrieving partition information using:\n" +
          SqlUtil.replaceParameters(retrievePartitionDefinitionSql, dbObject.getSchema(), dbObject.getObjectName()));
      }

      pstmt.setString(1, SqlUtil.removeObjectQuotes(dbObject.getSchema()));
      pstmt.setString(2, SqlUtil.removeObjectQuotes(dbObject.getObjectName()));
      rs = pstmt.executeQuery();

      if (rs.next())
      {
        type = rs.getString("PARTITIONING_TYPE");
        subType = rs.getString("SUBPARTITIONING_TYPE");
        if (isIndex)
        {
          locality = rs.getString("LOCALITY");
        }
        defaultSubpartitionCount = rs.getInt("DEF_SUBPARTITION_COUNT");
        intervalDefinition = supportsIntervals ? rs.getString("INTERVAL") : null;
        subKeyCount = rs.getInt("SUBPARTITIONING_KEY_COUNT");
        int colCount = rs.getInt("PARTITIONING_KEY_COUNT");
        columns = new ArrayList<>(colCount);
        int partCount = rs.getInt("PARTITION_COUNT");
        partitions = new ArrayList<>(partCount);
        tableSpace = rs.getString("DEF_TABLESPACE_NAME");
        refPartitionConstraint = rs.getString("REF_PTN_CONSTRAINT_NAME");
        objectOwner = dbObject.getSchema();
      }
    }
    finally
    {
      SqlUtil.closeAll(rs, pstmt);
    }

    if (isRefPartition() && dbObject instanceof TableIdentifier)
    {
      ((TableIdentifier)dbObject).setUseInlineFK(true);
    }

    if (subKeyCount > 0)
    {
      retrieveSubColumns(dbObject, conn);
    }
    long duration = System.currentTimeMillis() - start;
    LogMgr.logDebug("AbstractOraclePartition.retrieveDefinition()", "Retrieving partition definition for " + dbObject.getFullyQualifiedName(conn)+ " took: " + duration + "ms");
    return type != null;
  }


  private void retrieveColumns(DbObject table, WbConnection conn)
    throws SQLException
  {
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    String retrieveColumnsSql = getRetrieveColumnsSql();

    long start = System.currentTimeMillis();
    try
    {
      pstmt = conn.getSqlConnection().prepareStatement(retrieveColumnsSql);
      if (Settings.getInstance().getDebugMetadataSql())
      {
        LogMgr.logDebug(getClassName() + ".retrieveColumns()", "Retrieving partition columns using:\n" +
          SqlUtil.replaceParameters(retrieveColumnsSql, table.getSchema(), table.getObjectName()));
      }

      pstmt.setString(1, SqlUtil.removeObjectQuotes(table.getSchema()));
      pstmt.setString(2, SqlUtil.removeObjectQuotes(table.getObjectName()));
      rs = pstmt.executeQuery();

      columns = new ArrayList<>();
      while (rs.next())
      {
        columns.add(rs.getString("COLUMN_NAME"));
      }
    }
    finally
    {
      SqlUtil.closeAll(rs, pstmt);
    }
    long duration = System.currentTimeMillis() - start;
    LogMgr.logDebug("AbstractOraclePartition.retrieveColumns()", "Retrieving partition columns for " + table.getObjectName() + " took: " + duration + "ms");
  }

  private void retrieveSubColumns(DbObject dbObject, WbConnection conn)
    throws SQLException
  {
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    String retrieveSubColumns = getRetrieveSubColumnsSql();
    long start = System.currentTimeMillis();
    try
    {
      pstmt = conn.getSqlConnection().prepareStatement(retrieveSubColumns);
      if (Settings.getInstance().getDebugMetadataSql())
      {
        LogMgr.logDebug(getClassName() + ".retrieveSubColumns()", "Using SQL=\n" +
          SqlUtil.replaceParameters(retrieveSubColumns, dbObject.getSchema(), dbObject.getObjectName()));
      }

      pstmt.setString(1, SqlUtil.removeObjectQuotes(dbObject.getSchema()));
      pstmt.setString(2, SqlUtil.removeObjectQuotes(dbObject.getObjectName()));
      rs = pstmt.executeQuery();

      subColumns = new ArrayList<>();
      while (rs.next())
      {
        subColumns.add(rs.getString("COLUMN_NAME"));
      }
    }
    finally
    {
      SqlUtil.closeAll(rs, pstmt);
    }
    long duration = System.currentTimeMillis() - start;
    LogMgr.logDebug("AbstractOraclePartition.retrieveSubColumns()", "Retrieving sub partition columns for " + dbObject.getObjectName() + " took: " + duration + "ms");
  }

  private OraclePartitionDefinition findPartition(String name)
  {
    if (partitions == null || partitions.isEmpty()) return null;
    for (OraclePartitionDefinition def : partitions)
    {
      if (def.getName().equals(name))
      {
        return def;
      }
    }
    return null;
  }

  private void retrieveSubPartitions(DbObject object, WbConnection conn)
    throws SQLException
  {
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    String retrieveSubPartitions = getRetrieveSubPartitionsSql();

    long start = System.currentTimeMillis();
    try
    {
      pstmt = conn.getSqlConnection().prepareStatement(retrieveSubPartitions);
      if (Settings.getInstance().getDebugMetadataSql())
      {
        LogMgr.logDebug(getClassName() + ".retrieveSubPartitions()", "Using SQL=\n" +
          SqlUtil.replaceParameters(retrieveSubPartitions, object.getSchema(), object.getObjectName()));
      }

      pstmt.setString(1, SqlUtil.removeObjectQuotes(object.getSchema()));
      pstmt.setString(2, SqlUtil.removeObjectQuotes(object.getObjectName()));
      rs = pstmt.executeQuery();

      while (rs.next())
      {
        String name = rs.getString("PARTITION_NAME");
        String subPart = rs.getString("SUBPARTITION_NAME");
        String value = rs.getString("HIGH_VALUE");
        int position = rs.getInt("SUBPARTITION_POSITION");
        String compress = null;
        if (useCompression)
        {
          compress = rs.getString("COMPRESSION");
        }
        OraclePartitionDefinition subPartition = new OraclePartitionDefinition(subPart, subType, position);
        subPartition.setPartitionValue(value);
        subPartition.setCompressOption(compress);
        subPartition.setIsSubpartition(true);

        OraclePartitionDefinition mainPartition = findPartition(name);
        if (mainPartition != null)
        {
          mainPartition.addSubPartition(subPartition);
        }
      }
    }
    finally
    {
      SqlUtil.closeAll(rs, pstmt);
    }
    long duration = System.currentTimeMillis() - start;
    LogMgr.logDebug("AbstractOraclePartition.retrieveSubPartitions()", "Retrieving sub partitions " + object.getObjectName() + " took: " + duration + "ms");
  }


  protected boolean shouldRetrievePartitions()
  {
    return true;
  }

  private void retrievePartitions(DbObject object, WbConnection conn)
    throws SQLException
  {
    if (!shouldRetrievePartitions()) return;

    PreparedStatement pstmt = null;
    ResultSet rs = null;
    String retrievePartitionSQL = getRetrievePartitionsSql();

    long start = System.currentTimeMillis();
    try
    {
      pstmt = conn.getSqlConnection().prepareStatement(retrievePartitionSQL);
      if (Settings.getInstance().getDebugMetadataSql())
      {
        LogMgr.logDebug(getClassName() + ".retrievePartitions()", "Using SQL=\n" +
          SqlUtil.replaceParameters(retrievePartitionSQL, object.getSchema(), object.getObjectName()));
      }

      pstmt.setString(1, SqlUtil.removeObjectQuotes(object.getSchema()));
      pstmt.setString(2, SqlUtil.removeObjectQuotes(object.getObjectName()));
      rs = pstmt.executeQuery();

      partitions = new ArrayList<>();

      while (rs.next())
      {
        String name = rs.getString("PARTITION_NAME");
        String value = rs.getString("HIGH_VALUE");
        int position = rs.getInt("PARTITION_POSITION");
        String compress = null;
        if (useCompression)
        {
          compress = rs.getString("COMPRESSION");
        }
        OraclePartitionDefinition def = new OraclePartitionDefinition(name, type, position);
        def.setPartitionValue(value);
        def.setCompressOption(compress);
        partitions.add(def);
      }
    }
    finally
    {
      SqlUtil.closeAll(rs, pstmt);
    }

    long duration = System.currentTimeMillis() - start;
    LogMgr.logDebug("AbstractOraclePartition.retrievePartitions()", "Retrieving partitions " + object.getObjectName() + " took: " + duration + "ms");

    if (defaultSubpartitionCount <= 1 && subColumns != null)
    {
      retrieveSubPartitions(object, conn);
    }
  }

  private String getClassName()
  {
    String clsname = getClass().getName();
    return clsname.substring(clsname.lastIndexOf('.') + 1);
  }

}
