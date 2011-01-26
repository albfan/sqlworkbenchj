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
		useLocality = false;
	}

	protected OracleTablePartition(WbConnection conn, boolean retrieveCompression)
		throws SQLException
	{
		super(conn, retrieveCompression);
		useLocality = false;
	}

	@Override
	protected String getRetrieveColumnsSql()
	{
		return
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
			"select pt.owner,  \n" +
			"       pt.table_name, \n" +
			"       pt.partitioning_type,  \n" +
			"       pt.partition_count, \n" +
			"       pt.partitioning_key_count, \n" +
			"       pt.subpartitioning_type, \n" +
			"       pt.subpartitioning_key_count, \n" +
			"       pt.def_subpartition_count \n" +
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
			"SELECT partition_name,  \n" +
			"       high_value,  \n" +
			"       partition_position, \n" +
		  "       subpartition_count \n" +
			"FROM all_tab_partitions \n" +
			"WHERE table_owner = ?  \n" +
			"  AND table_name = ? \n" +
			"ORDER BY partition_position";	}

	@Override
	protected String getRetrieveSubColumnsSql()
	{
		return
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
