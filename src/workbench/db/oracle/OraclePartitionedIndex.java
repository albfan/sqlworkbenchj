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
import workbench.db.IndexDefinition;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.SqlUtil;

/**
 * A class to read information about a partitioned tables in Oracle
 * 
 * @author Thomas Kellerer
 */
public class OraclePartitionedIndex
{
	private String type;
	private List<String> columns;
	private List<OraclePartitionDefinition> partitions;
	private String locality;

	private final String retrieveColumnsSql = 
			"select ind.index_name, \n" +
			"       ind.partitioning_type,  \n" +
			"       ind.locality,  \n" +
			"       ind.partition_count, \n" +
			"       ind.partitioning_key_count, \n" +
			"       col.column_name, \n" +
			"       col.column_position \n" +
			"from all_part_indexes ind \n" +
			"  join all_part_key_columns col ON col.owner = ind.owner and col.name = ind.index_name AND col.object_type = 'INDEX' \n" +
			"where ind.owner = ? \n" +
			"  and ind.index_name = ? \n" +
			"order by col.column_position \n";

	private final String retrievePartitionSQL = 
			"SELECT partition_name,  \n" +
			"       high_value,  \n" +
			"       partition_position \n" +
			"FROM all_ind_partitions \n" +
			"WHERE index_owner = ?  \n" +
			"  AND index_name = ? \n" +
			"ORDER BY partition_position";	
	
	public OraclePartitionedIndex(IndexDefinition index, WbConnection conn)
		throws SQLException
	{
		boolean hasPartitions = retrieveColumns(index, conn);
		if (hasPartitions) 
		{
			retrievePartitions(index, conn);
		}
	}

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

	public String getLocality()
	{
		return locality;
	}
	
	public String getSource()
	{
		if (!this.isPartitioned()) return null;
		StringBuilder result = new StringBuilder(partitions.size() * 15);
		result.append(locality);
		if ("HASH".equals(type) || "RANGE".equals(type))
		{
			result.append(" PARTITION BY ");
			result.append(type);
			if (columns != null)
			{
				result.append(" (");
				for (int i=0; i < columns.size(); i++)
				{
					if (i > 0) result.append(", ");
					result.append(columns.get(i));
				}
				result.append(')');
			}
		}
		result.append("\n(\n");
		for (int i=0; i < partitions.size(); i++)
		{
			if (i > 0) result.append(",\n");
			result.append("  PARTITION ");
			result.append(partitions.get(i).getName());
			
			String value = partitions.get(i).getPartitionValue();
			if (value != null) 
			{
				if ("RANGE".equals(type))
				{
					result.append(" VALUES LESS THAN (");
					result.append(value);
					result.append(')');
				}
				else
				{
					result.append(" VALUES (");
					result.append(value);
					result.append(')');
				}
			}
		}
		result.append("\n)");
		return result.toString();
	}
	
	private boolean retrieveColumns(IndexDefinition index, WbConnection conn)
		throws SQLException
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = conn.getSqlConnection().prepareStatement(retrieveColumnsSql);
			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logDebug("OracleTablePartition.retrieveColumns()", "Using SQL=\n" + 
					SqlUtil.replaceParameters(retrieveColumnsSql, index.getSchema(), index.getObjectName()));
			}
			
			pstmt.setString(1, SqlUtil.removeQuoting(index.getSchema()));
			pstmt.setString(2, SqlUtil.removeQuoting(index.getObjectName()));
			rs = pstmt.executeQuery();
			
			if (rs.next())
			{
				type = rs.getString("PARTITIONING_TYPE");
				locality = rs.getString("LOCALITY");
				int colCount = rs.getInt("PARTITIONING_KEY_COUNT");
				columns = new ArrayList<String>(colCount);
				int partCount = rs.getInt("PARTITION_COUNT");
				partitions = new ArrayList<OraclePartitionDefinition>(partCount);
				columns.add(rs.getString("COLUMN_NAME"));
				while (rs.next())
				{
					columns.add(rs.getString("COLUMN_NAME"));
				}
			}
		}
		finally
		{
			SqlUtil.closeAll(rs, pstmt);
		}
		return columns != null && !columns.isEmpty();
	}

	private void retrievePartitions(IndexDefinition index, WbConnection conn)
		throws SQLException
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = conn.getSqlConnection().prepareStatement(retrievePartitionSQL);
			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logDebug("OracleTablePartition.retrieveColumns()", "Using SQL=\n" + 
					SqlUtil.replaceParameters(retrievePartitionSQL, index.getSchema(), index.getName()));
			}
			
			pstmt.setString(1, SqlUtil.removeQuoting(index.getSchema()));
			pstmt.setString(2, SqlUtil.removeQuoting(index.getName()));
			rs = pstmt.executeQuery();
			
			partitions = new ArrayList<OraclePartitionDefinition>();
			
			while (rs.next())
			{
				String name = rs.getString("PARTITION_NAME");
				String value = rs.getString("HIGH_VALUE");
				int position = rs.getInt("PARTITION_POSITION");
				OraclePartitionDefinition def = new OraclePartitionDefinition(name, position);
				def.setPartitionValue(value);
				partitions.add(def);
			}
		}
		finally
		{
			SqlUtil.closeAll(rs, pstmt);
		}
	}
}
