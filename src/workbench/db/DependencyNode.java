/*
 * DependencyNode.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import workbench.util.StringUtil;

/**
 *	A node in the dependency tree for a cascading delete script.
 *  Used in {@link workbench.db.DeleteScriptGenerator}
 */
public class DependencyNode
{
	private DependencyNode parentNode;
	private TableIdentifier table;

	private String updateAction = "";
	private String deleteAction = "";
	
  private String fkName;

	/**
	 * Maps the columns of the base table to the matching column
	 * of the parent table
	 */
	private HashMap<String, String> columns = new HashMap<String, String>();

	private ArrayList<DependencyNode> childTables = new ArrayList<DependencyNode>();
	

	public DependencyNode(TableIdentifier aTable)
	{
		this.table = aTable.createCopy();
		this.parentNode = null;
	}
	
	public void addColumnDefinition(String aColumn, String aParentColumn)
	{
		Object currentParent = this.columns.get(aColumn);
		if (currentParent == null)
		{
			this.columns.put(aColumn, aParentColumn);
		}
	}
	
	public void setParent(DependencyNode aParent, String aFkName)
	{
		this.parentNode = aParent;
		this.fkName = aFkName;
	}
	
	public String toString() 
	{ 
		if (this.fkName == null)
		{
			return this.table.getTableName();
		}
		else
		{
			return this.table.getTableName() + " (" + this.fkName + ")"; 
		}
	}
  public String getFkName() 
	{ 
		return this.fkName; 
	}
	
	public TableIdentifier getParentTable() 
	{ 
		if (parentNode == null) return null;
		return this.parentNode.getTable();
	}
	
	public TableIdentifier getTable()
	{
		return this.table;
	}
	
	/**
	 * Returns a Map that maps the columns of the base table to the matching column
	 * of the related (parent/child) table.
	 * The keys to the map are columns from this node's table {@link #getTable()}
	 * The values in this map are columns found in this node's "parent" table
	 * 
	 * @see #getTable()
	 * @see #getParentTable()
	 */
	public Map<String, String> getColumns() 
	{ 
		if (this.columns == null)
		{
			return Collections.emptyMap();
		}
		else
		{
			return this.columns; 
		}
	}

	/**
	 *	Checks if this node defines the foreign key constraint name aFkname
	 *	to the given table
	 */
	public boolean isDefinitionFor(TableIdentifier tbl, String aFkname)
	{
		if (aFkname == null) return false;
		
		return (this.table.equals(tbl) && aFkname.equals(this.fkName));
	}
	
	public boolean equals(Object other)
	{
		if (other == null) return false;
		if (other instanceof DependencyNode)
		{
			DependencyNode node = (DependencyNode)other;
			return this.isDefinitionFor(node.getTable(), node.getFkName());
		}
		return false;
	}
	
	public int hashCode()
	{
		StringBuilder sb = new StringBuilder(60);
		sb.append(this.table.getTableExpression() + "-" + this.fkName);
		return StringUtil.hashCode(sb);
	}
	
	public boolean isInParentTree(DependencyNode aNode)
	{
		if (aNode == null) return false;
		DependencyNode currentParent = this;
		while (currentParent != null)
		{
			if (currentParent.equals(aNode)) return true;
			currentParent = currentParent.getParent();
		}
		return false;
	}
	
	public boolean isRoot() { return this.parentNode == null; }

	public DependencyNode getParent() 
	{ 
		return this.parentNode; 
	}
	
	public List<DependencyNode> getChildren()
	{
		return this.childTables;
	}
	
	public DependencyNode addChild(TableIdentifier table, String aFkname)
	{
		int count = this.childTables.size();
		DependencyNode node = null;
		for (int i=0; i < count; i++)
		{
			node = this.getChild(i);
			if (node.isDefinitionFor(table, aFkname))
			{
				return node;
			}
		}
		node = new DependencyNode(table);
		node.setParent(this, aFkname);
		this.childTables.add(node);
		return node;
	}
	
	private DependencyNode getChild(int i)
	{
		return this.childTables.get(i);
	}
	
	public boolean containsChild(DependencyNode aNode)
	{
		if (aNode == null) return false;
		return this.childTables.contains(aNode);
	}
	
	public boolean addChild(DependencyNode aTable)
	{
		if (this.containsChild(aTable)) return false;
		this.childTables.add(aTable);
		return true;
	}

	public String getDeleteAction() { return this.deleteAction; }
	public void setDeleteAction(String anAction)
	{
		this.deleteAction = anAction;
	}
	public String getUpdateAction() { return this.updateAction; }
	public void setUpdateAction(String anAction)
	{
		this.updateAction = anAction;
	}
}
