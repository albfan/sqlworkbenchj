/*
 * TableRelation.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.report;
import java.util.ArrayList;
import java.util.List;
/**
 * @author support@sql-workbench.net
 */
public class TableRelation 
{
	private int sourceTableId;
	private int targetTableId;
	
	private String fkName;
	private List<String> sourceColumns = new ArrayList<String>();
	private List<String> targetColumns = new ArrayList<String>();
	private int relationId; 
	
	public TableRelation(int id, int sourceId, int destId, String fk)
	{
		relationId = id;
		sourceTableId = sourceId;
		targetTableId = destId;
		fkName = fk;
	}
	
	public void addColumnReference(String sourceCol, String targetCol)
	{
		sourceColumns.add(sourceCol);
		targetColumns.add(targetCol);
	}
	
	public int getRelationId()
	{
		return relationId;
	}
	
	public int getTargetTableId()
	{
		return targetTableId;
	}
	
	public int getSourceTableId()
	{
		return sourceTableId;
	}
	/**
	 * Returns the fk columns as a string suitable to be put 
	 * into a <tt>relation</tt> tag.
	 * @return
	 */
	public String getColumns()
	{
		StringBuilder result = new StringBuilder();
		for (int i=0; i < sourceColumns.size(); i++)
		{
			result.append(sourceColumns.get(0));
			result.append('=');
			result.append(targetColumns.get(0));
			result.append('\n');
		}
		return result.toString();
	}

	public String getRelationName()
	{
		return fkName;
	}
	
}
