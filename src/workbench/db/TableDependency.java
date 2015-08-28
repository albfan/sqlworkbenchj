/*
 * TableDependency.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.DatabaseMetaData;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import workbench.interfaces.ScriptGenerationMonitor;
import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.objectcache.DbObjectCache;
import workbench.db.objectcache.DbObjectCacheFactory;

import workbench.storage.DataStore;

import workbench.util.CollectionUtil;
import workbench.util.FileUtil;
import workbench.util.StringUtil;

/**
 * A class to retrieve the FK dependencies of a given table.
 *
 * @author  Thomas Kellerer
 */
public class TableDependency
{
	private final WbConnection connection;
	private TableIdentifier theTable;
	private DependencyNode tableRoot;
	private final DbMetadata metaData;
	private final FKHandler fkHandler;
	private final List<DependencyNode> leafs = new ArrayList<>();
	private boolean directChildrenOnly;
	private boolean readAborted;
	private boolean cancelRetrieve;
	private boolean cancelled;
	private final Map<DependencyNode, DependencyNode> visitedRelations = new HashMap<>();
	private final Set<DependencyNode> visitedParents = new HashSet<>();
	private ScriptGenerationMonitor monitor;
	private final List<TableIdentifier> excludeTables = new ArrayList<>();

	public TableDependency(WbConnection con)
	{
		this.connection = con;
		this.metaData = this.connection.getMetadata();
		this.fkHandler = FKHandlerFactory.createInstance(connection);
	}

	public TableDependency(WbConnection con, TableIdentifier tbl)
	{
		this(con);
		setMainTable(tbl, false);
	}

	public final void setMainTable(TableIdentifier tbl)
	{
		setMainTable(tbl, true);
	}

	public final void setMainTable(TableIdentifier tbl, boolean verifyTable)
	{
		if (verifyTable)
		{
			theTable = metaData.findTable(tbl, false);
		}
		else
		{
			theTable = tbl.createCopy();
		}
		visitedParents.clear();
		visitedRelations.clear();
		leafs.clear();
		tableRoot = null;
	}

	public FKHandler getFKHandler()
	{
		return fkHandler;
	}

	public void setExcludedTables(List<TableIdentifier> toExclude)
	{
		if (CollectionUtil.isEmpty(toExclude))
		{
			this.excludeTables.clear();
		}
		else
		{
			this.excludeTables.clear();
			this.excludeTables.addAll(toExclude);
		}
	}

	public void setScriptMonitor(ScriptGenerationMonitor monitor)
	{
		this.monitor = monitor;
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

	public boolean isCancelled()
	{
		return cancelled;
	}

	@SuppressWarnings("SleepWhileInLoop")
	public void cancel()
	{
		this.cancelled = true;
		this.cancelRetrieve = true;
		LogMgr.logDebug("TableDependency.cancel()", "Cancelling dependency retrieval");
	}

	/**
	 * Read the hierarchy of tables referencing this one (the "incoming" foreign keys).
	 * This is equivalent to calling <tt>readDependencyTree(true)</tt>
	 *
	 * @see #readDependencyTree(boolean)
	 */
	public void readTreeForChildren()
	{
		readDependencyTree(true);
	}

	/**
	 * Read the hierarchy of tables that this table references (the "outgoing" foreign keys).
	 * This is equivalent to calling <tt>readDependencyTree(false)</tt>
	 *
	 * @see #readDependencyTree(boolean)
	 */
	public void readTreeForParents()
	{
		readDependencyTree(false);
	}

	public List<DependencyNode> getIncomingForeignKeys()
	{
		boolean direct = this.directChildrenOnly;
		try
		{
			setRetrieveDirectChildrenOnly(true);
			readTreeForChildren();
		}
		finally
		{
			setRetrieveDirectChildrenOnly(direct);
		}
		return getLeafs();
	}

	public List<DependencyNode> getOutgoingForeignKeys()
	{
		boolean direct = this.directChildrenOnly;
		try
		{
			setRetrieveDirectChildrenOnly(true);
			readTreeForParents();
		}
		finally
		{
			setRetrieveDirectChildrenOnly(direct);
		}
		return getLeafs();
	}

	public void readDependencyTree(boolean exportedKeys)
	{
		if (theTable == null) return;
		if (connection == null) return;

		this.readAborted = false;
		this.cancelRetrieve = false;
		this.cancelled = false;
		this.leafs.clear();

		// Make sure we are using the "correct" TableIdentifier
		// if the TableIdentifier passed in the constructor was
		// created "on the commandline" e.g. by using a user-supplied
		// table name, we might not correctly find or compare all nodes
		// as those identifiers will not have the flag "neverAdjustCase" set
		TableIdentifier tableToUse = this.theTable;
		if (!this.theTable.getNeverAdjustCase())
		{
			tableToUse = this.metaData.findTable(theTable, false);
		}
		if (tableToUse == null) return;

		tableRoot = new DependencyNode(tableToUse);

		visitedRelations.clear();
		visitedParents.clear();

    long start = System.currentTimeMillis();

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
				visitedParents.clear();
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
    long duration = System.currentTimeMillis() - start;
    LogMgr.logDebug("TableDependency.readDependencyTree()", "Retrieving " + (exportedKeys ? "referencing" : "referenced") + " tables for " + tableRoot.getTable().toString() + " took: " + duration + "ms");
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

		if (visitedParents.contains(parent))
		{
			LogMgr.logTrace("TableDependency.readTree()", "Foreign key " + parent.getFkName()+ " has already been processed.");
			return;
		}

		if (excludeTables.contains(parent.getTable()))
		{
			LogMgr.logDebug("TableDependency.readTree()", "Table dependency for " + parent.getTable()+ " will not be analyzed because it has been excluded.");
			return;
		}

		if (this.monitor != null)
		{
			monitor.setCurrentObject(parent.getTable().toString(), -1, -1);
		}

		DbSettings dbSettings = metaData.getDbSettings();

		try
		{
			DataStore ds;
			int catalogcol;
			int schemacol;
			int tablecol;
			int fknamecol;
			int tablecolumncol;
			int parentcolumncol;

			TableIdentifier ptbl = this.metaData.resolveSynonym(parent.getTable());

			if (LogMgr.isTraceEnabled())
			{
				LogMgr.logTrace("TableDependency.readTree()", "level: " + level  + ", retrieving: " + parent.debugString());
			}

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

			// collecting the parents (in addition to collecting parent/child nodes) is necessary because otherwise
			// cycles for parents that do not have children would not be detected
			if (!parent.getTable().equals(theTable))
			{
				visitedParents.add(parent);
			}

			for (int i=0; i < count; i++)
			{
				String catalog = ds.getValueAsString(i, catalogcol);
				String schema = ds.getValueAsString(i, schemacol);
				String table = ds.getValueAsString(i, tablecol);
        String fkname = ds.getValueAsString(i, fknamecol);
				int deferrableCode = ds.getValueAsInt(i, FKHandler.COLUMN_IDX_DEFERRABILITY, DatabaseMetaData.importedKeyNotDeferrable);
				String deferrable = dbSettings.getRuleDisplay(deferrableCode);

				if (fkname == null)
				{
					if (exportedKeys)
					{
						fkname = "WbGenerated_fk_" + parent.getTable().getTableName() + "_referenced_by_" + table;
					}
					else
					{
						fkname = "WbGenerated_fk_" + parent.getTable().getTableName() + "_references_" + table;
					}
					LogMgr.logError("TableDependency.readTree()", "JDBC Driver returned a NULL value for the FK name for table " + parent.getTable().getTableExpression() + "  Using: " + fkname + " instead", null);
				}

				TableIdentifier tbl = new TableIdentifier(catalog, schema, table);

				tbl.setNeverAdjustCase(true);
				DependencyNode child = parent.addChild(tbl, fkname);
				String tablecolumn = ds.getValueAsString(i, tablecolumncol); // the column in "table" referencing the other table
				String parentcolumn = ds.getValueAsString(i, parentcolumncol); // the column in the parent table

				int update = ds.getValueAsInt(i, 9, -1);
				int delete = ds.getValueAsInt(i, 10, -1);
				child.setUpdateActionValue(update);
				child.setDeleteActionValue(delete);
				child.setDeferrableValue(deferrableCode);
				child.setUpdateAction(this.metaData.getDbSettings().getRuleDisplay(update));
				child.setDeleteAction(this.metaData.getDbSettings().getRuleDisplay(delete));
				child.addColumnDefinition(tablecolumn, parentcolumn);
				child.setDeferrableType(deferrable);
				child.setEnabled(true);
				if (fkHandler.supportsStatus())
				{
					String flag = null;
					if (fkHandler.containsStatusColumn())
					{
						flag = ds.getValueAsString(i, "ENABLED");
						if (flag != null)
						{
							child.setEnabled(StringUtil.stringToBool(flag));
						}
						flag = ds.getValueAsString(i, "VALIDATED");
						if (flag != null)
						{
							child.setValidated(StringUtil.stringToBool(flag));
						}
					}
					else
					{
						FKHandler.FkStatusInfo status = fkHandler.getFkEnabledFlag(ptbl, fkname);
						if (status != null)
						{
							child.setEnabled(status.enabled);
							child.setValidated(status.validated);
						}
					}
				}
			}

			if (level > 25)
			{
				// this is a bit paranoid, as I am testing for cycles before recursing
				// into the next child. This is a safetey net, just in case the cycle
				// is not detected. Better display the user incorrect data, than
				// ending up in an endless loop.
				// A circular dependency with more than 25 levels is an ugly design anyway :)
				LogMgr.logError("TableDependency.readTree()", "Endless reference cycle detected for root=" + this.tableRoot + ", parent=" + parent, null);
				this.readAborted = true;
				return;
			}

			if (directChildrenOnly && level == 0)
			{
				List<DependencyNode> children = parent.getChildren();
				for (DependencyNode child : children)
				{
					leafs.add(child);
				}
				return;
			}

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
					break;
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("TableDependencyTree.readTree()", "Error when reading FK definition for " + tableRoot, e);
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

	public static void dumpTree(String fname, DependencyNode root, boolean append)
	{
		if (!Settings.getInstance().getBoolProperty("workbench.debug.dependency", false)) return;

		FileWriter writer = null;
		try
		{
			writer = new FileWriter(new File(fname), append);
			boolean showParents = true;
			if (root.getChildren().isEmpty())
			{
				writer.append("No children for " + root.debugString() + ". Starting from top-level node: \n");
				while (root.getParent() != null)
				{
					root = root.getParent();
				}
				writer.append(root.getTable() + "\n----------------------------------------\n");
				showParents = false;
			}
			else
			{
				writer.append("Tree for: " + root.debugString() + "\n");
			}
			dumpChildren(writer, root, 0);
			if (showParents && root.getParent() != null)
			{
				writer.append("------ Parents for: " + root.debugString() + "\n");
				DependencyNode parent = root.getParent();
				while (parent != null)
				{
					int level = parent.getLevel();
					writer.write(StringUtil.padRight("", level * 4));
					writer.write(parent.getTable().getTableName() + " (" + parent.getFkName() + ")");
					writer.write(", nodeLevel: " + level);
					writer.write("\n");
					parent = parent.getParent();
				}
			}
		}
		catch (IOException io)
		{

		}
		finally
		{
			FileUtil.closeQuietely(writer);
		}
	}

	private static void dumpChildren(FileWriter writer, DependencyNode node, int level)
		throws IOException
	{
		if (node == null) return;

		for (DependencyNode child : node.getChildren())
		{
			writer.write(StringUtil.padRight("", level * 4));
			writer.write(child.getTable().getTableName() + " (" + child.getFkName() + ")");
			writer.write(", nodeLevel: " + node.getLevel());
			writer.write(", iterationLevel: " + level);
			writer.write("\n");
		}
		for (DependencyNode child : node.getChildren())
		{
			dumpChildren(writer, child, level + 1);
		}
	}
	public Set<TableIdentifier> getAllTables()
	{
		Set<TableIdentifier> allTables = getAllTables(this.getRootNode());
		allTables.add(this.getRootNode().getTable());
		return allTables;
	}

	private Set<TableIdentifier> getAllTables(DependencyNode root)
	{
		Set<TableIdentifier> result = new HashSet<>();
		if (root == null) return result;

		List<DependencyNode> children = root.getChildren();
		for (DependencyNode child : children)
		{
			result.add(child.getTable());
			result.addAll(getAllTables(child));
		}
		return result;
	}

	public List<DependencyNode> getAllNodes()
	{
		List<DependencyNode> allNodes = getAllNodes(this.getRootNode());
		allNodes.add(getRootNode());
		return allNodes;
	}

	private List<DependencyNode> getAllNodes(DependencyNode root)
	{
		List<DependencyNode> result = new ArrayList<>();
		if (root == null)
		{
			return result;
		}

		List<DependencyNode> children = root.getChildren();
		for (DependencyNode child : children)
		{
			result.add(child);
			result.addAll(getAllNodes(child));
		}
		return result;
	}

	public DataStore getDisplayDataStore(boolean showImportedKeys)
  {
		setRetrieveDirectChildrenOnly(true);

    if (showImportedKeys)
    {
      readTreeForParents();
    }
    else
    {
      readTreeForChildren();
    }

    DbObjectCache cache = DbObjectCacheFactory.getInstance().getCache(this.connection);
    if (showImportedKeys)
    {
      cache.addReferencedTables(this.theTable, leafs);
    }
    else
    {
      cache.addReferencingTables(this.theTable, leafs);
    }
    return createDisplayDataStore(connection, theTable, leafs, showImportedKeys, fkHandler.supportsStatus());
  }

	public static DataStore createDisplayDataStore(WbConnection connection, TableIdentifier theTable, List<DependencyNode> nodes, boolean showImportedKeys, boolean supportsStatus)
	{
		String[] cols;
		int[] types;
		int[] sizes;

    String refColName = null;
    if (showImportedKeys)
    {
      refColName = "REFERENCES";
    }
    else
    {
      refColName = "REFERENCED BY";
    }

		if (supportsStatus)
		{
			cols = new String[] { "FK_NAME", "COLUMN", refColName , "ENABLED", "UPDATE_RULE", "DELETE_RULE", "DEFERRABLE"};
			types = new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
			sizes = new int[] {25, 10, 30, 10, 12, 12, 15};
		}
		else
		{
			cols = new String[] { "FK_NAME", "COLUMN", refColName , "UPDATE_RULE", "DELETE_RULE", "DEFERRABLE"};
			types = new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
			sizes = new int[] {25, 10, 30, 10, 12, 12, 15};

		}
		DataStore result = new DataStore(cols, types, sizes);

		for (DependencyNode node : nodes)
		{
			int row = result.addRow();
			int col = 0;
			result.setValue(row, col++, node.getFkName());
			result.setValue(row, col++, node.getTargetColumnsList());
			result.setValue(row, col++, node.getTable().getTableExpression(connection) + "(" + node.getSourceColumnsList() + ")");
			if (supportsStatus)
			{
				result.setValue(row, col++, node.isEnabled() ? "YES" : "NO");
			}
			result.setValue(row, col++, node.getUpdateAction());
			result.setValue(row, col++, node.getDeleteAction());
			result.setValue(row, col++, node.getDeferrableType());
			result.getRow(row).setUserObject(node);
		}
		result.resetStatus();
		return result;

	}
}
