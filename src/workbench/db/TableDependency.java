/*
 * TableDependency.java
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import workbench.log.LogMgr;
import workbench.storage.DataStore;

/**
 *
 * @author  info@sql-workbench.net
 */
public class TableDependency
{
	private WbConnection connection;
	private TableIdentifier theTable;
	private DependencyNode tableRoot;
	private DbMetadata wbMetadata;
	private ArrayList leafs;

	public TableDependency()
	{
	}

	public void setConnection(WbConnection aConn)
		throws SQLException
	{
		this.connection = aConn;
		this.wbMetadata = this.connection.getMetadata();
	}

	private void setTableName(String aCatalog, String aSchema, String aTable)
	{
		aTable = this.wbMetadata.adjustObjectname(aTable);
		aCatalog = this.wbMetadata.adjustObjectname(aCatalog);
		aSchema = this.wbMetadata.adjustObjectname(aSchema);
		this.theTable = new TableIdentifier(aCatalog, aSchema, aTable);
	}

	public void setTable(TableIdentifier aTable)
	{
		this.theTable = aTable;
	}
	public void readDependencyTree()
	{
		this.readDependencyTree(true);
	}
	public void readDependencyTree(boolean exportedKeys)
	{
		if (this.theTable == null) return;
		if (this.connection == null) return;
		this.leafs = new ArrayList();
		this.tableRoot = new DependencyNode(this.theTable);
		this.readTree(this.tableRoot, exportedKeys);
	}

	/**
	 *	Create the dependency tree.
	 *	If treeParent is passed as null, the TreeNode for a display in a JTree
	 *	are not created.
	 */
	private int readTree(DependencyNode parent, boolean exportedKeys)
	{
		String parentcatalog = parent.getCatalog();
		String parentschema = parent.getSchema();
		String parenttable = parent.getTable();

		/* for debugging !
		int indent = 0;
		DependencyNode n = parent.getParent();
		while (n != null)
		{
			indent ++;
			n = n.getParent();
		}
		StringBuffer indentString = new StringBuffer(indent * 2);
		for (int i=0; i < indent; i++) indentString.append("  ");
		*/

		try
		{
			ResultSet rs = null;
			DataStore ds = null;
			int catalogcol;
			int schemacol;
			int tablecol;
			int fknamecol;
			int tablecolumncol;
			int parentcolumncol;
			int parenttablecol;

			if (exportedKeys)
			{
				catalogcol = 4;
				schemacol = 5;
				tablecol = 6;
				fknamecol = 11;
				tablecolumncol = 7;
				parentcolumncol = 3;
				parenttablecol = 2;
				ds = this.wbMetadata.getExportedKeys(parentcatalog, parentschema, parenttable);
			}
			else
			{
				catalogcol = 0;
				schemacol = 1;
				tablecol = 2;
				fknamecol = 11;
				tablecolumncol = 3;
				parentcolumncol = 7;
				parenttablecol = 6;
				ds = this.wbMetadata.getImportedKeys(parentcatalog, parentschema, parenttable);
			}

			DependencyNode child = null;
			String currentfk = null;
			String currenttable = null;
			DefaultMutableTreeNode treeNode = null;
			String catalog = null;
			String schema = null;
			String table = null;
			String fkname = null;

			boolean created = false;
			int count = ds.getRowCount();
			//System.out.print(indentString);
			//System.out.println("processing " + count + " entries for " + parent);
			for (int i=0; i<count; i++)
			{
				catalog = ds.getValueAsString(i, catalogcol);
				schema = ds.getValueAsString(i, schemacol);
				table = ds.getValueAsString(i, tablecol);
        fkname = ds.getValueAsString(i, fknamecol);

				child = parent.addChild(catalog, schema, table, fkname);
				String tablecolumn = ds.getValueAsString(i, tablecolumncol); // the column in "table" referencing the other table
				String parentcolumn = ds.getValueAsString(i, parentcolumncol); // the column in the parent table
				String parenttable2 = ds.getValueAsString(i, parenttablecol);

				int update = ds.getValueAsInt(i, 9, -1);
				int delete = ds.getValueAsInt(i, 10, -1);
				child.setUpdateAction(this.wbMetadata.getRuleTypeDisplay(update));
				child.setDeleteAction(this.wbMetadata.getRuleTypeDisplay(delete));
				child.addColumnDefinition(tablecolumn, parentcolumn);
				//System.out.print(indentString);
				//System.out.println("processed catalog=" + catalog + ",schema=" + schema + ",table=" + table + ",fk=" + fkname + ",column=" + tablecolumn + ",parentcol=" + parentcolumn);
			}

			List children = parent.getChildren();
			count = children.size();
			for (int i=0; i < count; i++)
			{
				child = (DependencyNode)children.get(i);
				int childrenCount = 0;
				if (!child.isInParentTree(parent))
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

	public List getLeafs() { return this.leafs; }
  public DependencyNode getRootNode() { return this.tableRoot; }

}
