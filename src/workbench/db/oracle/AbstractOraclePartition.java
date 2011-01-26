/*
 * OraclePartitionReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
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
import workbench.db.TableIdentifier;
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
	protected boolean useLocality;
	protected String locality; // only used for indexes

	public AbstractOraclePartition(WbConnection conn)
		throws SQLException
	{
		this(conn, true);
	}

	protected AbstractOraclePartition(WbConnection conn, boolean retrieveCompression)
		throws SQLException
	{
		useCompression = retrieveCompression && JdbcUtils.hasMinimumServerVersion(conn, "11.1");
	}

	public void retrieve(DbObject object, WbConnection conn)
		throws SQLException
	{
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

	public String getPartitionType()
	{
		return type;
	}

	public String getSource()
	{
		if (!this.isPartitioned()) return null;
		StringBuilder result = new StringBuilder(partitions.size() * 15);
		if (locality != null)
		{
			result.append(locality);
		}
		result.append("PARTITION BY ");
		result.append(type);
		result.append(' ');
		if (columns != null)
		{
			result.append('(');
			result.append(StringUtil.listToString(columns, ','));
			result.append(')');
		}
		if (!"NONE".equals(subType))
		{
			result.append("\nSUBPARTITION BY ");
			result.append(subType);
			result.append(" (");
			result.append(StringUtil.listToString(subColumns, ','));
			result.append(')');
			if (defaultSubpartitionCount > 1)
			{
				result.append("\nSUBPARTITIONS ");
				result.append(defaultSubpartitionCount);
			}
		}
		result.append("\n(\n");
		int maxLength = getMaxPartitionNameLength();
		for (int i=0; i < partitions.size(); i++)
		{
			if (i > 0) result.append(",\n");
			result.append(partitions.get(i).getSource(maxLength));
		}
		result.append("\n)");
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
				LogMgr.logDebug("OracleTablePartition.retrieveDefinition()", "Using SQL=\n" +
					SqlUtil.replaceParameters(retrievePartitionDefinitionSql, dbObject.getSchema(), dbObject.getObjectName()));
			}

			pstmt.setString(1, SqlUtil.removeQuoting(dbObject.getSchema()));
			pstmt.setString(2, SqlUtil.removeQuoting(dbObject.getObjectName()));
			rs = pstmt.executeQuery();

			if (rs.next())
			{
				type = rs.getString("PARTITIONING_TYPE");
				subType = rs.getString("SUBPARTITIONING_TYPE");
				if (useLocality)
				{
					locality = rs.getString("LOCALITY");
				}
				defaultSubpartitionCount = rs.getInt("DEF_SUBPARTITION_COUNT");
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
				LogMgr.logDebug("OracleTablePartition.retrieveColumns()", "Using SQL=\n" +
					SqlUtil.replaceParameters(retrieveColumnsSql, table.getSchema(), table.getObjectName()));
			}

			pstmt.setString(1, SqlUtil.removeQuoting(table.getSchema()));
			pstmt.setString(2, SqlUtil.removeQuoting(table.getObjectName()));
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
		return;
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
				LogMgr.logDebug("OracleTablePartition.retrieveColumns()", "Using SQL=\n" +
					SqlUtil.replaceParameters(retrieveSubColumns, dbObject.getSchema(), dbObject.getObjectName()));
			}

			pstmt.setString(1, SqlUtil.removeQuoting(dbObject.getSchema()));
			pstmt.setString(2, SqlUtil.removeQuoting(dbObject.getObjectName()));
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
				LogMgr.logDebug("OracleTablePartition.retrieveColumns()", "Using SQL=\n" +
					SqlUtil.replaceParameters(retrieveSubPartitions, object.getSchema(), object.getObjectName()));
			}

			pstmt.setString(1, SqlUtil.removeQuoting(object.getSchema()));
			pstmt.setString(2, SqlUtil.removeQuoting(object.getObjectName()));
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
				LogMgr.logDebug("OracleTablePartition.retrieveColumns()", "Using SQL=\n" +
					SqlUtil.replaceParameters(retrievePartitionSQL, object.getSchema(), object.getObjectName()));
			}

			pstmt.setString(1, SqlUtil.removeQuoting(object.getSchema()));
			pstmt.setString(2, SqlUtil.removeQuoting(object.getObjectName()));
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

}
