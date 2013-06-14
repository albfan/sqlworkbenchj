/*
 * AbstractOraclePartition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import workbench.db.DbObject;
import workbench.db.JdbcUtils;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
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

	public AbstractOraclePartition(WbConnection conn)
		throws SQLException
	{
		this(conn, true);
	}

	protected AbstractOraclePartition(WbConnection conn, boolean retrieveCompression)
		throws SQLException
	{
		boolean is11r1 = JdbcUtils.hasMinimumServerVersion(conn, "11.1");;
		useCompression = retrieveCompression && is11r1;
		supportsIntervals = is11r1;
	}

	public void retrieve(DbObject object, WbConnection conn)
		throws SQLException
	{
		boolean hasPartitions = object != null ? retrieveDefinition(object, conn) : false;
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

	public String getPartitionType()
	{
		return type;
	}

	public String getSourceForTableDefinition()
	{
		return getSource(true, "");
	}

	public String getSourceForTableDefinition(String indent)
	{
		return getSource(true, indent);
	}

	public String getSourceForIndexDefinition(String indent)
	{
		return getSource(false, indent);
	}

	public String getSourceForIndexDefinition()
	{
		return getSource(false, "");
	}

	private String getSource(boolean forTable, String indent)
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
			if (columns != null)
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
		result.append('\n');
		result.append(indent);
		result.append("(\n");
		int maxLength = forTable ? getMaxPartitionNameLength(): 0;
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
		return result.toString();
	}

	private int getMaxPartitionNameLength()
	{
		int maxLength = 0;
		for (OraclePartitionDefinition def : partitions)
		{
			if (def.getName().length() > maxLength)
			{
				maxLength = def.getName().length();
			}
		}
		return maxLength;
	}

	private boolean retrieveDefinition(DbObject dbObject, WbConnection conn)
		throws SQLException
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		int subKeyCount = 0;
		String retrievePartitionDefinitionSql = getRetrievePartitionDefinitionSql();

		try
		{
			pstmt = conn.getSqlConnection().prepareStatement(retrievePartitionDefinitionSql);
			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logDebug(getClassName() + ".retrieveDefinition()", "Using SQL=\n" +
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
				columns = new ArrayList<String>(colCount);
				int partCount = rs.getInt("PARTITION_COUNT");
				partitions = new ArrayList<OraclePartitionDefinition>(partCount);
			}
		}
		finally
		{
			SqlUtil.closeAll(rs, pstmt);
		}
		if (subKeyCount > 0)
		{
			retrieveSubColumns(dbObject, conn);
		}
		return type != null;
	}


	private void retrieveColumns(DbObject table, WbConnection conn)
		throws SQLException
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String retrieveColumnsSql = getRetrieveColumnsSql();

		try
		{
			pstmt = conn.getSqlConnection().prepareStatement(retrieveColumnsSql);
			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logDebug(getClassName() + ".retrieveColumns()", "Using SQL=\n" +
					SqlUtil.replaceParameters(retrieveColumnsSql, table.getSchema(), table.getObjectName()));
			}

			pstmt.setString(1, SqlUtil.removeObjectQuotes(table.getSchema()));
			pstmt.setString(2, SqlUtil.removeObjectQuotes(table.getObjectName()));
			rs = pstmt.executeQuery();

			columns = new ArrayList<String>();
			while (rs.next())
			{
				columns.add(rs.getString("COLUMN_NAME"));
			}
		}
		finally
		{
			SqlUtil.closeAll(rs, pstmt);
		}
	}

	private void retrieveSubColumns(DbObject dbObject, WbConnection conn)
		throws SQLException
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String retrieveSubColumns = getRetrieveSubColumnsSql();
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

			subColumns = new ArrayList<String>();
			while (rs.next())
			{
				subColumns.add(rs.getString("COLUMN_NAME"));
			}
		}
		finally
		{
			SqlUtil.closeAll(rs, pstmt);
		}
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
	}


	private void retrievePartitions(DbObject object, WbConnection conn)
		throws SQLException
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String retrievePartitionSQL = getRetrievePartitionsSql();

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

			partitions = new ArrayList<OraclePartitionDefinition>();

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
