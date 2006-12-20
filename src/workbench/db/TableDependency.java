/*
 * TableDependency.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import workbench.log.LogMgr;
import workbench.storage.DataStore;

/**
 *
 * @author  support@sql-workbench.net
 */
public class TableDependency
{
	private WbConnection connection;
	private TableIdentifier theTable;
	private DependencyNode tableRoot;
	private DbMetadata wbMetadata;
	private ArrayList<DependencyNode> leafs;
	private int currentLevel = 0;
	private int maxLevel = Integer.MAX_VALUE;
	
	public TableDependency()
	{
	}

	public void setMaxLevel(int max)
	{
		this.maxLevel = max;
	}
	
	public void setConnection(WbConnection aConn)
	{
		this.connection = aConn;
		this.wbMetadata = this.connection.getMetadata();
	}

	public void setTable(TableIdentifier aTable)
	{
		this.theTable = aTable;
	}
	
	public DependencyNode findLeafNodeForTable(TableIdentifier table)
	{
		String findExpr = table.getTableExpression(connection);
		for (DependencyNode node : leafs)
		{
			String expr = node.getTable().getTableExpression(connection);
			if (expr.equalsIgnoreCase(findExpr)) return node;
		}
		return null;
	}
	
	public List<DependencyNode> retrieveReferencingTables()
	{
		this.readDependencyTree(false);
		return Collections.unmodifiableList(leafs);
	}

	public List<DependencyNode>  retrieveReferencedTables()
	{
		this.readDependencyTree(true);
		return Collections.unmodifiableList(leafs);
	}
	
	protected void readDependencyTree(boolean exportedKeys)
	{
		if (this.theTable == null) return;
		if (this.connection == null) return;
		this.leafs = new ArrayList<DependencyNode>();
		this.tableRoot = new DependencyNode(this.theTable);
		this.currentLevel = 0;
		this.readTree(this.tableRoot, exportedKeys);
	}

	/**
	 *	Create the dependency tree.
	 */
	private int readTree(DependencyNode parent, boolean exportedKeys)
	{
		/* for debugging !
		int indent = 0;
		DependencyNode n = parent.getParent();
		while (n != null)
		{
			indent ++;
			n = n.getParent();
		}
		StringBuilder indentString = new StringBuilder(indent * 2);
		for (int i=0; i < indent; i++) indentString.append("  ");
		*/

		try
		{
			DataStore ds = null;
			int catalogcol;
			int schemacol;
			int tablecol;
			int fknamecol;
			int tablecolumncol;
			int parentcolumncol;

			if (exportedKeys)
			{
				catalogcol = 4;
				schemacol = 5;
				tablecol = 6;
				fknamecol = 11;
				tablecolumncol = 7;
				parentcolumncol = 3;
				ds = this.wbMetadata.getExportedKeys(parent.getTable());
			}
			else
			{
				catalogcol = 0;
				schemacol = 1;
				tablecol = 2;
				fknamecol = 11;
				tablecolumncol = 3;
				parentcolumncol = 7;
				ds = this.wbMetadata.getImportedKeys(parent.getTable());
			}

			DependencyNode child = null;
			String catalog = null;
			String schema = null;
			String table = null;
			String fkname = null;

			int count = ds.getRowCount();
			
			for (int i=0; i<count; i++)
			{
				catalog = ds.getValueAsString(i, catalogcol);
				schema = ds.getValueAsString(i, schemacol);
				table = ds.getValueAsString(i, tablecol);
        fkname = ds.getValueAsString(i, fknamecol);

				TableIdentifier tbl = new TableIdentifier(catalog, schema, table);
				tbl.setNeverAdjustCase(true);
				child = parent.addChild(tbl, fkname);
				String tablecolumn = ds.getValueAsString(i, tablecolumncol); // the column in "table" referencing the other table
				String parentcolumn = ds.getValueAsString(i, parentcolumncol); // the column in the parent table

				int update = ds.getValueAsInt(i, 9, -1);
				int delete = ds.getValueAsInt(i, 10, -1);
				child.setUpdateAction(this.wbMetadata.getRuleTypeDisplay(update));
				child.setDeleteAction(this.wbMetadata.getRuleTypeDisplay(delete));
				child.addColumnDefinition(tablecolumn, parentcolumn);
			}

			this.currentLevel ++;
			
			List<DependencyNode> children = parent.getChildren();
			count = children.size();
			for (int i=0; i < count; i++)
			{
				child = children.get(i);
				if (!child.isInParentTree(parent) && (currentLevel < this.maxLevel))
				{
					this.readTree(child, exportedKeys);
				}
				this.leafs.add(child);
			}
      return count;
		}
		catch (Exception e)
		{
			LogMgr.logError("TableDependencyTree.readTree()", "Error when reading FK definition", e);
		}
    return 0;
	}

	public List<DependencyNode> getLeafs() 
	{ 
		return this.leafs; 
	}
  
	public DependencyNode getRootNode() 
	{ 
		return this.tableRoot; 
	}

}
