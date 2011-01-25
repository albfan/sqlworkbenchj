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
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.SqlUtil;

/**
 * A class to read information about a partitioned tables in Oracle
 * 
 * @author Thomas Kellerer
 */
public class OracleTablePartition
{
	private String type;
	private List<String> columns;
	private List<OraclePartitionDefinition> partitions;

	private final String retrieveColumnsSql = 
			"select pt.owner,  \n" +
			"       pt.table_name, \n" +
			"       pt.partitioning_type,  \n" +
			"       pt.partition_count, \n" +
			"       pt.partitioning_key_count, \n" +
			"       pc.column_name, \n" +
			"       pc.column_position \n" +
			"from all_part_tables pt \n" +
			"  join all_part_key_columns pc on pc.name = pt.table_name and pc.owner = pt.owner \n" +
			"where pt.owner = ? \n" +
			"  and pt.table_name = ? \n" +
			"order by pc.column_position \n";

	private final String retrievePartitionSQL = 
			"SELECT partition_name,  \n" +
			"       high_value,  \n" +
			"       partition_position \n" +
			"FROM all_tab_partitions \n" +
			"WHERE table_owner = ?  \n" +
			"  AND table_name = ? \n" +
			"ORDER BY partition_position";	
	
	public OracleTablePartition(TableIdentifier table, WbConnection conn)
		throws SQLException
	{
		boolean hasPartitions = retrieveColumns(table, conn);
		if (hasPartitions) 
		{
			retrievePartitions(table, conn);
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
	
	public String getSource()
	{
		if (!this.isPartitioned()) return null;
		StringBuilder result = new StringBuilder(partitions.size() * 15);
		result.append("PARTITION BY ");
		result.append(type);
		result.append(' ');
		if (columns != null) 
		{
			result.append('(');
			for (int i=0; i < columns.size(); i++) 
			{
				if (i > 0) result.append(", ");
				result.append(columns.get(i));
			}
			result.append(')');
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
	
	private boolean retrieveColumns(TableIdentifier table, WbConnection conn)
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
					SqlUtil.replaceParameters(retrieveColumnsSql, table.getSchema(), table.getTableName()));
			}
			
			pstmt.setString(1, SqlUtil.removeQuoting(table.getSchema()));
			pstmt.setString(2, SqlUtil.removeQuoting(table.getTableName()));
			rs = pstmt.executeQuery();
			
			if (rs.next())
			{
				type = rs.getString("PARTITIONING_TYPE");
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

	private void retrievePartitions(TableIdentifier table, WbConnection conn)
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
					SqlUtil.replaceParameters(retrievePartitionSQL, table.getSchema(), table.getTableName()));
			}
			
			pstmt.setString(1, SqlUtil.removeQuoting(table.getSchema()));
			pstmt.setString(2, SqlUtil.removeQuoting(table.getTableName()));
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
