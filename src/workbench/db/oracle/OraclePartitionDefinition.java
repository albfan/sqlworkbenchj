/*
 * OraclePartitionDefinition.java
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class OraclePartitionDefinition
{

	private String type;
	
	/**
	 * The name of the partition
	 */
	private String name;

	/**
	 * Stores the value for LIST partitions
	 */
	private String partitionValue;

	/**
	 * The position of this partition
	 */
	private int position;

	private String compressOption;

	private List<OraclePartitionDefinition> subPartitions;
	
	private boolean isSubpartition;

	public OraclePartitionDefinition(String partitionName, String partitionType, int partitionPosition)
	{
		name = partitionName;
		position = partitionPosition;
		type = partitionType;
	}

	public boolean isIsSubpartition()
	{
		return isSubpartition;
	}

	public void setIsSubpartition(boolean isSubpartition)
	{
		this.isSubpartition = isSubpartition;
	}
	
	public List<OraclePartitionDefinition> getSubPartitions()
	{
		if (subPartitions == null) return Collections.emptyList();
		return Collections.unmodifiableList(subPartitions);
	}

	public void addSubPartition(OraclePartitionDefinition subPartition)
	{
		if (this.subPartitions == null)
		{
			this.subPartitions = new ArrayList<OraclePartitionDefinition>();
		}
		subPartitions.add(subPartition);
	}

	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}
	
	public String getCompressOption()
	{
		return compressOption;
	}

	public void setCompressOption(String compressOption)
	{
		this.compressOption = compressOption;
	}

	public String getName()
	{
		return name;
	}

	public void setPartitionValue(String partitionValue)
	{
		this.partitionValue = partitionValue;
	}

	/**
	 * Return the (high) value of this partition.
	 * Only applicable for LIST partitions
	 */
	public String getPartitionValue()
	{
		return partitionValue;
	}

	public int getPosition()
	{
		return position;
	}

	public CharSequence getSource(boolean forTable, int nameLength)
	{
		StringBuilder result = new StringBuilder((partitionValue == null ? 15 : partitionValue.length()) + 20);
		if (isSubpartition)
		{
			result.append("  SUBPARTITION ");
		}
		else
		{
			result.append("  PARTITION ");
		}
		
		result.append(StringUtil.padRight(name, nameLength));
		if (partitionValue != null && forTable)
		{
			if ("RANGE".equals(type))
			{
				result.append(" VALUES LESS THAN (");
				result.append(partitionValue);
				result.append(')');
			}
			else
			{
				result.append(" VALUES (");
				result.append(partitionValue);
				result.append(')');
			}
		}

		if (compressOption != null && partitionValue != null && partitionValue.indexOf('\'') > -1)
		{
			if ("DISABLED".equals(compressOption))
			{
				result.append(" NOCOMPRESS");
			}
			if ("ENABLED".equals(compressOption))
			{
				result.append(" COMPRESS");
			}
		}
		
		if (subPartitions != null && !subPartitions.isEmpty())
		{
			int maxLength = 0;
			for (OraclePartitionDefinition def : subPartitions)
			{
				if (def.getName().length() > maxLength)
				{
					maxLength = def.getName().length();
				}
			}
			result.append("\n  (\n");
			for (int i=0; i < subPartitions.size(); i++)
			{
				if (i > 0) result.append(",\n");
				result.append("  ");
				result.append(subPartitions.get(i).getSource(forTable, maxLength));
			}
			result.append("\n  )");
		}
		return result;
	}


}
