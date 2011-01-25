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

/**
 *
 * @author Thomas Kellerer
 */
public class OraclePartitionDefinition
{

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
	
	public OraclePartitionDefinition(String partitionName, int partitionPosition)
	{
		name = partitionName;
		position = partitionPosition;
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
	
	
	
}
