/*
 * DependencyNode.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *	A node in the dependency tree for a cascading delete script.
 *  Used in {@link workbench.db.DeleteScriptGenerator}
 */
public class DependencyNode
{
	private TableIdentifier table;
	private TableIdentifier parenttable;

	private String updateAction = "";
	private String deleteAction = "";
	
	private DependencyNode parent;
  private String fkName;
  
	private HashMap columns = new HashMap();

	private ArrayList childTables = new ArrayList();
	
	public DependencyNode(String aCatalog, String aSchema, String aTable)
	{
		if (aTable == null) throw new IllegalArgumentException("Table name may not be null");
		if (aTable.trim().length() == 0) throw new IllegalArgumentException("Table name may not be empty");
		this.table = new TableIdentifier(adjustCatalogSchemaName(aCatalog), adjustCatalogSchemaName(aSchema), aTable);
		this.parenttable = null;
	}
	public DependencyNode(TableIdentifier aTable)
	{
		this.table = aTable;
		this.parenttable = null;
	}
	
	public void addColumnDefinition(String aColumn, String aParentColumn)
	{
		Object parent = this.columns.get(aColumn);
		if (parent == null)
		{
			this.columns.put(aColumn, aParentColumn);
		}
	}
	
	public void setParent(DependencyNode aParent, String aFkName)
	{
		this.parent = aParent;
		this.setParentTable(aParent.getCatalog(), aParent.getSchema(), aParent.getTable(), aFkName);
	}
	
	private void setParentTable(String aCatalog, String aSchema, String aTable, String aName)
	{
		if (aTable == null) throw new IllegalArgumentException("Parent table may not be null");
		if (aTable.trim().length() == 0) throw new IllegalArgumentException("Parent table may not be empty");
		
		this.parenttable = new TableIdentifier(adjustCatalogSchemaName(aSchema), adjustCatalogSchemaName(aCatalog), aTable);
    this.fkName = aName;
	}
	
	public String toString() 
	{ 
		if (this.fkName == null)
		{
			return this.table.getTable();
		}
		else
		{
			return this.table.getTable() + " (" + this.fkName + ")"; 
		}
	}
  public String getFkName() { return this.fkName; }
	public String getParentTable() { return this.parenttable.getTable(); }
	public String getParentCatalog() { return this.parenttable.getCatalog(); }
	public String getParentSchema() { return this.parenttable.getSchema(); }
	
	public String getTable() { return this.table.getTable(); }
	public String getSchema() { return this.table.getSchema(); }
	public String getCatalog() { return this.table.getCatalog(); }

	public TableIdentifier getTableId()
	{
		return this.table;
	}
	
	public Map getColumns() 
	{ 
		if (this.columns == null)
		{
			return Collections.EMPTY_MAP;
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
	public boolean isDefinitionFor(String aCatalog, String aSchema, String aTable, String aFkname)
	{
		if (aFkname == null) return false;
		TableIdentifier t = new TableIdentifier(aCatalog, aSchema, aTable);
		
		return (this.table.equals(t) && aFkname.equals(this.fkName));
	}
	
	public boolean equals(Object other)
	{
		if (other == null) return false;
		if (other instanceof DependencyNode)
		{
			DependencyNode node = (DependencyNode)other;
			return this.isDefinitionFor(node.getCatalog(), node.getSchema(), node.getTable(), node.getFkName());
		}
		return false;
	}
	
	public boolean isInParentTree(DependencyNode aNode)
	{
		if (aNode == null) return false;
		DependencyNode parent = this;//.getParent();
		while (parent != null)
		{
			if (parent.equals(aNode)) return true;
			parent = parent.getParent();
		}
		return false;
	}
	
	public boolean isRoot() { return this.parent == null; }

	public DependencyNode getParent() { return this.parent; }
	public List getChildren()
	{
		return this.childTables;
	}
	
	public DependencyNode addChild(String aCatalog, String aSchema, String aTable, String aFkname)
	{
		int count = this.childTables.size();
		DependencyNode node = null;
		for (int i=0; i < count; i++)
		{
			node = this.getChild(i);
			if (node.isDefinitionFor(aCatalog, aSchema, aTable, aFkname))
			{
				return node;
			}
		}
		node = new DependencyNode(aCatalog, aSchema, aTable);
		node.setParent(this, aFkname);
		this.childTables.add(node);
		return node;
	}
	private DependencyNode getChild(int i)
	{
		return (DependencyNode)this.childTables.get(i);
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

	private String adjustCatalogSchemaName(String aName)
	{
		if (aName == null) return null;
		if (aName.length() == 0) return null;
		if ("*".equals(aName)) return null;
		if ("%".equals(aName)) return null;
		return aName;
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
