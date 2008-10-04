/*
 * TableDependency.java
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
import java.util.List;
import workbench.log.LogMgr;
import workbench.storage.DataStore;
import workbench.util.StringUtil;

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
	private List<DependencyNode> leafs;
	private boolean directChildrenOnly = false;
	private boolean readAborted = false;
	
	public TableDependency(WbConnection con, TableIdentifier tbl)
	{
		this.connection = con;
		this.wbMetadata = this.connection.getMetadata();
		this.theTable = this.wbMetadata.findTable(tbl);
	}

	public void setRetrieveDirectChildrenOnly(boolean flag)
	{
		this.directChildrenOnly = flag;
	}

	public DependencyNode findLeafNodeForTable(TableIdentifier table)
	{
		return findLeafNodeForTable(table, null);
	}
	
	public DependencyNode findLeafNodeForTable(TableIdentifier table, String fkName)
	{
		String findExpr = table.getRawTableName();//table.getTableExpression(connection);
		for (DependencyNode node : leafs)
		{
			TableIdentifier nodeTable = node.getTable();
			String expr = nodeTable.getRawTableName();//getTableExpression(connection);
			if (expr.equalsIgnoreCase(findExpr))
			{
				if (fkName == null) return node;
				if (StringUtil.equalStringIgnoreCase(node.getFkName(), fkName)) return node;
			}
			
		}
		return null;
	}

	public void readTreeForChildren()
	{
		readDependencyTree(true);
	}
	
	public void readTreeForParents()
	{
		readDependencyTree(false);
	}
	
	public void readDependencyTree(boolean exportedKeys)
	{
		if (this.theTable == null) return;
		if (this.connection == null) return;
		this.readAborted = false;
		this.leafs = new ArrayList<DependencyNode>();
		
		// Make sure we are using the "correct" TableIdentifier
		// if the TableIdentifier passed in the constructor was 
		// created "on the commandline" e.g. by using a user-supplied
		// table name, we might not correctly find or compare all nodes
		// those identifiers will not have the flag "neverAdjustCase" set
		TableIdentifier tableToUse = this.theTable;
		if (!this.theTable.getNeverAdjustCase())
		{
			tableToUse = this.wbMetadata.findTable(theTable);
		}
		if (tableToUse == null) return;
		this.tableRoot = new DependencyNode(tableToUse);
		//this.currentLevel = 0;
		this.readTree(this.tableRoot, exportedKeys, 0);
	}

	/**
	 *	Create the dependency tree.
	 */
	private int readTree(DependencyNode parent, boolean exportedKeys, int level)
	{
		try
		{
			DataStore ds = null;
			int catalogcol;
			int schemacol;
			int tablecol;
			int fknamecol;
			int tablecolumncol;
			int parentcolumncol;

			TableIdentifier ptbl = this.wbMetadata.resolveSynonym(parent.getTable());
			
			if (exportedKeys)
			{
				catalogcol = 4;
				schemacol = 5;
				tablecol = 6;
				fknamecol = 11;
				tablecolumncol = 7;
				parentcolumncol = 3;
				ds = this.wbMetadata.getExportedKeys(ptbl);
			}
			else
			{
				catalogcol = 0;
				schemacol = 1;
				tablecol = 2;
				fknamecol = 11;
				tablecolumncol = 3;
				parentcolumncol = 7;
				ds = this.wbMetadata.getImportedKeys(ptbl);
			}

			int count = ds.getRowCount();

			for (int i=0; i<count; i++)
			{
				String catalog = ds.getValueAsString(i, catalogcol);
				String schema = ds.getValueAsString(i, schemacol);
				String table = ds.getValueAsString(i, tablecol);
        String fkname = ds.getValueAsString(i, fknamecol);

				TableIdentifier tbl = new TableIdentifier(catalog, schema, table);

				tbl.setNeverAdjustCase(true);
				DependencyNode child = parent.addChild(tbl, fkname);
				String tablecolumn = ds.getValueAsString(i, tablecolumncol); // the column in "table" referencing the other table
				String parentcolumn = ds.getValueAsString(i, parentcolumncol); // the column in the parent table

				int update = ds.getValueAsInt(i, 9, -1);
				int delete = ds.getValueAsInt(i, 10, -1);
				child.setUpdateAction(this.wbMetadata.getDbSettings().getRuleDisplay(update));
				child.setDeleteAction(this.wbMetadata.getDbSettings().getRuleDisplay(delete));
				child.addColumnDefinition(tablecolumn, parentcolumn);
			}

			if (level > 15) 
			{
				// this is a bit paranoid, as I am testing for cycles before recursing
				// into the next child. This is a safetey net, just in case the cycle
				// is not detected. Better display the user incorrect data, than 
				// ending up in an endless loop.
				// A circular dependency with more than 10 levels is an ugly design anyway :)
				LogMgr.logError("TableDependency.readDependencyTree()", "Endless reference cycle detected for root=" + this.tableRoot + ", parent=" + parent, null);
				this.readAborted = true;
				return count;
			}

			if (directChildrenOnly && level == 1) return count;
			
			List<DependencyNode> children = parent.getChildren();
			for (DependencyNode child : children)
			{
				if (!isCycle(child, parent))
				{
					this.readTree(child, exportedKeys, level + 1);
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

	private boolean isCycle(DependencyNode child, DependencyNode parent)
	{
		if (child.equals(parent)) return true;
		if (child.getTable().equals(parent.getTable())) return true;
		
		DependencyNode nextParent = parent.getParent();
		while (nextParent != null)
		{
			if (child.equals(nextParent)) return true;		
			nextParent = nextParent.getParent();
		}
		return false;
	}
	
	public boolean wasAborted()
	{
		return this.readAborted;
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
