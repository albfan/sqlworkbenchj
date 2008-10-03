/*
 * DependencyNode.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
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
 * A node in the dependency tree for a table
 * 
 * @see workbench.db.TableDependency
 * @see workbench.db.DeleteScriptGenerator
 * @see workbench.db.importer.TableDependencySorter
 * 
 * @author support@sql-workbench.net  
 */
public class DependencyNode
{
	private DependencyNode parentNode;
	private TableIdentifier table;
	private String updateAction = "";
	private String deleteAction = "";
	private String fkName;
	
	/**
	 * Maps the columns of the base table (this.table) to the matching column
	 * of the parent table (parentNode.getTable())
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

	/**
	 * Returns the level of this node in the dependency hierarchy. 
	 * @return 0 if no parent is available (i.e. the root of the tree)
	 *         -1 if this is a self referencing dependency
	 */
	public int getLevel()
	{
		if (parentNode == null) return 0;
		if (parentNode == this) return -1;
		return 1 + parentNode.getLevel();
	}

	public void setParent(DependencyNode aParent, String aFkName)
	{
		if (aFkName == null) throw new NullPointerException("FK Name may not be null");
		this.parentNode = aParent;
		this.fkName = aFkName;
	}

	public String toString()
	{
		return this.table.getTableName();
	}

	public String debugString()
	{
		StringBuilder result = new StringBuilder(20);
		result.append(this.table.getTableName());
		if (fkName != null)
		{
			result.append(" <" + this.fkName + ">");
		}
		if (columns.size() > 0) result.append(" [");
		boolean first = true;
		for (String col : columns.keySet())
		{
			if (!first)
			{
				result.append(", ");
				first = false;
			}
			result.append(col);
			result.append(" -> ");
			result.append(columns.get(col));
		}
		if (columns.size() > 0) result.append("]");
		return result.toString();
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
	 * 
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
		return this.table.equals(tbl) && aFkname.equals(this.fkName);
	}

	public boolean equals(Object other)
	{
		if (other instanceof DependencyNode)
		{
			DependencyNode node = (DependencyNode) other;
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

	public boolean isRoot()
	{
		return this.parentNode == null;
	}

	public DependencyNode getParent()
	{
		return this.parentNode;
	}

	public List<DependencyNode> getChildren()
	{
		return this.childTables;
	}

	/**
	 * Recursively finds a DependencyNode in the tree of nodes
	 */
	public DependencyNode findNode(DependencyNode toFind)
	{
		if (toFind == null) return null;
		if (toFind.equals(this)) return this;
		for (DependencyNode node : childTables)
		{
			if (toFind.equals(node))
			{
				return node;
			}
			else
			{
				DependencyNode n = node.findNode(toFind);
				if (n != null) return n;
			}
		}
		return null;
	}
	
	public DependencyNode addChild(TableIdentifier table, String aFkname)
	{
		if (aFkname == null) throw new NullPointerException("FK Name may not be null");
		for (DependencyNode node : childTables)
		{
			if (node.isDefinitionFor(table, aFkname))
			{
				return node;
			}
		}
		DependencyNode node = new DependencyNode(table);
		node.setParent(this, aFkname);
		this.childTables.add(node);
		return node;
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

	public String getDeleteAction()
	{
		return this.deleteAction;
	}

	public void setDeleteAction(String anAction)
	{
		this.deleteAction = anAction;
	}

	public String getUpdateAction()
	{
		return this.updateAction;
	}

	public void setUpdateAction(String anAction)
	{
		this.updateAction = anAction;
	}

	public void printAll()
	{
		int level = getLevel();
		StringBuilder indent = new StringBuilder(level * 2);
		for (int i=0; i < level; i++) indent.append("  ");
		
		System.out.println(indent + toString() + " [@" + level + "]");
		for (DependencyNode node : childTables)
		{
			node.printAll();
		}
	}
}
