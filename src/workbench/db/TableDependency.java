/*
 * TableDependency.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import workbench.log.LogMgr;
import workbench.storage.DataStore;
import workbench.util.StringUtil;

/**
 * A class to retrieve the FK dependencies of a given table.
 *
 * @author  Thomas Kellerer
 */
public class TableDependency
{
	private WbConnection connection;
	private TableIdentifier theTable;
	private DependencyNode tableRoot;
	private DbMetadata wbMetadata;
	private FKHandler fkHandler;
	private List<DependencyNode> leafs;
	private boolean directChildrenOnly;
	private boolean readAborted;
	private boolean cancelRetrieve;
	private boolean cancelled;
	private Map<DependencyNode, DependencyNode> visitedRelations;

	public TableDependency(WbConnection con, TableIdentifier tbl)
	{
		this.connection = con;
		this.wbMetadata = this.connection.getMetadata();
		fkHandler = FKHandlerFactory.createInstance(connection);
		this.theTable = this.wbMetadata.findTable(tbl, false);
	}

	/**
	 * Control the retrieval of grand-children.
	 * If this is set to true only directly linked tables will be retrieved.
	 * If set to false, tables indirectly linked to this table are also retrieved (i.e. the full tree)
	 *
	 * @param flag
	 */
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

	@SuppressWarnings("SleepWhileInLoop")
	public void cancel()
	{
		this.cancelled = false;
		this.cancelRetrieve = true;
		long start = System.currentTimeMillis();
		long maxWait = 1000 * 60; // one minute

		while (!cancelled)
		{
			try
			{
				Thread.sleep(100);
				if (System.currentTimeMillis() - start > maxWait)
				{
					// Prevent an endless loop in case cancelling does not work for some reason
					// e.g because the retrieval from the database hangs
					break;
				}
			}
			catch (InterruptedException ex)
			{
			}
		}
	}

	/**
	 * Read the hierarchy of tables referencing this one.
	 * This is equivalent to calling <tt>readDependencyTree(true)</tt>
	 *
	 * @see #readDependencyTree(boolean)
	 */
	public void readTreeForChildren()
	{
		readDependencyTree(true);
	}

	/**
	 * Read the hierarchy of tables that this table references.
	 * This is equivalent to calling <tt>readDependencyTree(false)</tt>
	 *
	 * @see #readDependencyTree(boolean)
	 */
	public void readTreeForParents()
	{
		readDependencyTree(false);
	}

	public void readDependencyTree(boolean exportedKeys)
	{
		if (theTable == null) return;
		if (connection == null) return;

		this.readAborted = false;
		this.cancelRetrieve = false;
		this.cancelled = false;
		this.leafs = new ArrayList<DependencyNode>();

		// Make sure we are using the "correct" TableIdentifier
		// if the TableIdentifier passed in the constructor was
		// created "on the commandline" e.g. by using a user-supplied
		// table name, we might not correctly find or compare all nodes
		// as those identifiers will not have the flag "neverAdjustCase" set
		TableIdentifier tableToUse = this.theTable;
		if (!this.theTable.getNeverAdjustCase())
		{
			tableToUse = this.wbMetadata.findTable(theTable, false);
		}
		if (tableToUse == null) return;

		tableRoot = new DependencyNode(tableToUse);
		visitedRelations = new HashMap<DependencyNode, DependencyNode>();
		boolean resetBusy = false;
		try
		{
			if (!connection.isBusy())
			{
				connection.setBusy(true);
				resetBusy = true;
			}
			readTree(this.tableRoot, exportedKeys, 0);
			if (cancelRetrieve)
			{
				tableRoot = new DependencyNode(tableToUse); // reset all children
				visitedRelations.clear();
				leafs.clear();
			}
		}
		finally
		{
			if (resetBusy)
			{
				connection.setBusy(false);
			}
		}

	}

	/**
	 *	Create the dependency tree.
	 */
	private void readTree(DependencyNode parent, boolean exportedKeys, int level)
	{
		if (cancelRetrieve)
		{
			return;
		}

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
				ds = fkHandler.getExportedKeys(ptbl);
			}
			else
			{
				catalogcol = 0;
				schemacol = 1;
				tablecol = 2;
				fknamecol = 11;
				tablecolumncol = 3;
				parentcolumncol = 7;
				ds = fkHandler.getImportedKeys(ptbl);
			}

			int count = ds.getRowCount();

//			if (LogMgr.isDebugEnabled())
//			{
//				LogMgr.logDebug("TableDependency.readTree()", "level: " + level  + ", retrieving: " + parent.debugString());
//			}

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

			if (level > 25)
			{
				// this is a bit paranoid, as I am testing for cycles before recursing
				// into the next child. This is a safetey net, just in case the cycle
				// is not detected. Better display the user incorrect data, than
				// ending up in an endless loop.
				// A circular dependency with more than 10 levels is an ugly design anyway :)
				LogMgr.logError("TableDependency.readTree()", "Endless reference cycle detected for root=" + this.tableRoot + ", parent=" + parent, null);
				this.readAborted = true;
				return;
			}

			if (directChildrenOnly && level == 1) return;

			List<DependencyNode> children = parent.getChildren();

			for (DependencyNode child : children)
			{
				if (!isCycle(child, parent))
				{
					this.readTree(child, exportedKeys, level + 1);
				}
				visitedRelations.put(parent, child);
				leafs.add(child);
				if (readAborted || cancelRetrieve)
				{
					if (cancelRetrieve)
					{
						cancelled = true;
					}
					break;
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("TableDependencyTree.readTree()", "Error when reading FK definition", e);
		}
	}

	private boolean isCycle(DependencyNode child, DependencyNode parent)
	{
		if (child.equals(parent)) return true;
		if (child.getTable().equals(parent.getTable())) return true;

		DependencyNode cn = visitedRelations.get(parent);
		if (cn != null && cn.equals(child)) return true;

		DependencyNode nextParent = parent.getParent();
		while (nextParent != null)
		{
			if (child.equals(nextParent)) return true;
			if (nextParent.getChildren().contains(child)) return true;
			nextParent = nextParent.getParent();
		}

		return false;
	}

	public boolean wasCancelled()
	{
		return this.cancelled;
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
