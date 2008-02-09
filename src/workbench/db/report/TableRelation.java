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
import workbench.db.ColumnIdentifier;
/**
 * @author support@sql-workbench.net
 */
public class TableRelation 
{
	private int sourceTableId;
	private int targetTableId;
	
	private String fkName;
	private List<ColumnIdentifier> sourceColumns = new ArrayList<ColumnIdentifier>();
	private List<ColumnIdentifier> targetColumns = new ArrayList<ColumnIdentifier>();
	private int relationId; 
	private String deleteAction;
	private String updateAction;
	
	public TableRelation(int id, int sourceId, int destId, String fk)
	{
		relationId = id;
		sourceTableId = sourceId;
		targetTableId = destId;
		fkName = fk;
	}

	public int getColCount()
	{
		return sourceColumns.size();
	}
	
	public void addColumnReference(ColumnIdentifier sourceCol, ColumnIdentifier targetCol)
	{
		sourceColumns.add(sourceCol);
		targetColumns.add(targetCol);
	}
	
	private String wbAction2Designer(String action)
	{
		if (action.equalsIgnoreCase("RESTRICT")) return "0";
		if (action.equalsIgnoreCase("CASCADE")) return "1";
		if (action.equalsIgnoreCase("SET NULL")) return "2";
		if (action.equalsIgnoreCase("NO ACTION")) return "3";
		if (action.equalsIgnoreCase("SET DEFAULT")) return "4";
		return "0";
	}
	
	public void setDeleteAction(String wbAction)
	{
		deleteAction = wbAction2Designer(wbAction);
	}
	
	public void setUpdateAction(String wbAction)
	{
		updateAction = wbAction2Designer(wbAction);
	}
	
	public String getDeleteAction()
	{
		return deleteAction;
	}

	public String getUpdateAction()
	{
		return updateAction;
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
	 * 1 = 1:n (identifying)
	 * 2 = 1:n (non-identifying)
	 * @return
	 */
	public String getRelationKind()
	{
		boolean isPK = true;
		for (ColumnIdentifier col : targetColumns)
		{
			if (!col.isPkColumn()) 
			{
				isPK = false;
				break;
			}
		}
		return isPK ? "1" : "2";
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
			result.append(targetColumns.get(0).getColumnName());
			result.append('=');
			result.append(sourceColumns.get(0).getColumnName());
			result.append("\\n");
		}
		return result.toString();
	}

	public String getRelationName()
	{
		return fkName;
	}
	
}
