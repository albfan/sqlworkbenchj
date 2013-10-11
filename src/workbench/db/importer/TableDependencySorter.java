/*
 * TableDependencySorter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
package workbench.db.importer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import workbench.interfaces.ScriptGenerationMonitor;
import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.DependencyNode;
import workbench.db.TableDependency;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.util.FileUtil;
import workbench.util.StringUtil;


/**
 * A class to sort tables according to their foreign key constraints,
 * so that data can be imported or deleted without disabling FK constraints.
 *
 * @author Thomas Kellerer
 */
public class TableDependencySorter
{
	private final WbConnection dbConn;
	private List<TableIdentifier> cycleErrors;
	private ScriptGenerationMonitor monitor;
	private TableDependency dependencyReader;
	private boolean cancel;

	public TableDependencySorter(WbConnection con)
	{
		this.dbConn = con;
	}

	public void setProgressMonitor(ScriptGenerationMonitor monitor)
	{
		this.monitor = monitor;
	}

	public List<TableIdentifier> sortForInsert(List<TableIdentifier> tables)
	{
		return getSortedTableList(tables, false, false);
	}

	public List<TableIdentifier> sortForDelete(List<TableIdentifier> tables, boolean addMissing)
	{
		return getSortedTableList(tables, addMissing, true);
	}

	public boolean hasErrors()
	{
		return cycleErrors != null;
	}

	public List<TableIdentifier> getErrorTables()
	{
		if (cycleErrors == null) return Collections.emptyList();
		return Collections.unmodifiableList(cycleErrors);
	}

	private void dumpMapping(List<LevelNode> mapping, String fname)
	{
		if (!Settings.getInstance().getBoolProperty("workbench.debug.dependency", false)) return;

		Comparator<LevelNode> comp = new Comparator<LevelNode>()
		{

			@Override
			public int compare(LevelNode o1, LevelNode o2)
			{
				if (o1.level == o2.level)
				{
					return o1.node.getTable().getTableName().compareTo(o2.node.getTable().getTableName());
				}
				return o1.level - o2.level;
			}
		};

		FileWriter writer = null;
		try
		{
			writer = new FileWriter(new File("c:/temp", fname));

			List<LevelNode> sorted = new ArrayList<LevelNode>(mapping);
			Collections.sort(sorted, comp);
			for (LevelNode node : sorted)
			{
				writer.append(StringUtil.padRight("", (node.level - 1) * 4) + node.node.getTable().getTableName() + " (" + node.level + ") \n");
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

	/**
	 * Determines the FK dependencies for each table in the passed List,
	 * and sorts them so that data can be imported without violating
	 * foreign key constraints
	 *
	 * @param tables the list of tables to be sorted
	 * @returns the tables sorted according to their FK dependencies
	 */
	private List<TableIdentifier> getSortedTableList(List<TableIdentifier> tables, boolean addMissing, boolean bottomUp)
	{
		cancel = false;
		List<LevelNode> levelMapping = createLevelMapping(tables, bottomUp);
//		dumpMapping(levelMapping, "before_cleanup.txt");

		if (!addMissing)
		{
			Iterator<LevelNode> itr = levelMapping.iterator();
			while (itr.hasNext())
			{
				TableIdentifier tbl = itr.next().node.getTable();
				if (!tables.contains(tbl))
				{
					itr.remove();
				}
			}
		}

//		dumpMapping(levelMapping, "after_cleanup.txt");

		ArrayList<TableIdentifier> result = new ArrayList<TableIdentifier>();
		for (LevelNode lvl : levelMapping)
		{
			int index = findTable(lvl.node.getTable(), tables);
			if (index > -1)
			{
				result.add(tables.get(index));
			}
			else if (addMissing)
			{
				result.add(lvl.node.getTable());
			}
		}
		return result;
	}

	private DependencyNode findChildTree(List<LevelNode> levels, TableIdentifier tbl)
	{
		int maxLevel = Integer.MIN_VALUE;
		DependencyNode lastNode = null;

		for (LevelNode nd : levels)
		{
			DependencyNode node = nd.node;
			DependencyNode child = node.findChildTree(tbl);
			if (child != null)
			{
				int childLevel = child.getLevel();
				TableDependency.dumpTree("possible_match_" + tbl.getTableName() + "_" + childLevel + ".txt", child);
				if (child.getLevel() > maxLevel)
				{
					maxLevel = childLevel;
					lastNode = child;
				}
			}
		}
		return lastNode;
	}

	public void cancel()
	{
		cancel = true;
		if (this.dependencyReader != null)
		{
			dependencyReader.cancel();
		}
	}

	private List<LevelNode> createLevelMapping(List<TableIdentifier> tables, boolean bottomUp)
	{
		List<LevelNode> levelMapping = new ArrayList<LevelNode>(tables.size());
		List<DependencyNode> startNodes = new ArrayList<DependencyNode>(tables.size());

		dependencyReader = new TableDependency(dbConn);

		int num = 1;
		for (TableIdentifier tbl : tables)
		{
			if (cancel) break;
			if (!dbConn.getMetadata().tableExists(tbl)) continue;

			if (this.monitor != null)
			{
				this.monitor.setCurrentObject(tbl.getTableExpression(), num, tables.size());
			}
			num ++;
			DependencyNode root = findChildTree(levelMapping, tbl);
			if (root == null)
			{
				dependencyReader.setMainTable(tbl);
				dependencyReader.readTreeForChildren();
				if (dependencyReader.wasAborted())
				{
					if (cycleErrors == null) cycleErrors = new LinkedList<TableIdentifier>();
					cycleErrors.add(tbl);
				}
				root = dependencyReader.getRootNode();
			}
			else
			{
				LogMgr.logDebug("TableDependencySorter.createLevelMapping()", "Re-using child tree for " + tbl + " with level: " + root.getLevel());
			}

			if (root != null)
			{
				TableDependency.dumpTree("tree_" + (num-1) + ".txt", root);
				startNodes.add(root);
				List<DependencyNode> allChildren = getAllNodes(root);
				putNodes(levelMapping, allChildren);
			}
		}

		if (cancel) return Collections.emptyList();

		// The "starting" tables have not been added yet.
		// They only need to be added if they did not appear as a child
		// in one of the sub-trees
		for (DependencyNode node : startNodes)
		{
			LevelNode lvl = findLevelNode(levelMapping, node.getTable());
			if (lvl == null)
			{
				levelMapping.add(new LevelNode(node, node.getLevel()));
			}
		}

		Comparator<LevelNode> comp;

		if (bottomUp)
		{
			comp = new Comparator<LevelNode>()
			{
				@Override
				public int compare(LevelNode o1, LevelNode o2)
				{
					return o2.level - o1.level;
				}
			};
		}
		else
		{
			comp = new Comparator<LevelNode>()
			{
				@Override
				public int compare(LevelNode o1, LevelNode o2)
				{
					return o1.level - o2.level;
				}
			};
		}

		Collections.sort(levelMapping, comp);
		return levelMapping;
	}

	private int findTable(TableIdentifier tofind, List<TableIdentifier> toSearch)
	{
		for (int i=0; i < toSearch.size(); i++)
		{
			TableIdentifier tbl = toSearch.get(i);
			if (tbl.getTableName().equalsIgnoreCase(tofind.getTableName())) return i;
		}
		return -1;
	}

	private void putNodes(List<LevelNode> levelMapping, List<DependencyNode> nodes)
	{
		if (nodes == null || nodes.isEmpty()) return;

		for (DependencyNode node : nodes)
		{
			TableIdentifier tbl = node.getTable();

			int level = node.getLevel();
			LevelNode lvl = findLevelNode(levelMapping, tbl);
			if (lvl == null)
			{
				lvl = new LevelNode(node, level);
				levelMapping.add(lvl);
			}
			else if (level > lvl.level)
			{
				lvl.level = level;
			}
		}
	}

	private LevelNode findLevelNode(List<LevelNode> levelMapping, TableIdentifier tbl)
	{
		int maxLevel = Integer.MIN_VALUE;
		LevelNode lastNode = null;
		for (LevelNode lvl : levelMapping)
		{
			if (lvl.node.getTable().compareNames(tbl))
			{
				if (lvl.node.getLevel() > maxLevel)
				{
					lastNode = lvl;
					maxLevel = lvl.node.getLevel();
				}
			}
		}
		return lastNode;
	}

	/**
	 * Get all nodes of the passed dependency hierarchy as a "flat" list.
	 * This is public mainly to be able to run a unit test agains it.
	 */
	public List<DependencyNode> getAllNodes(DependencyNode startWith)
	{
		if (startWith == null) return Collections.emptyList();

		List<DependencyNode> children = startWith.getChildren();

		if (children.isEmpty()) return Collections.emptyList();

		ArrayList<DependencyNode> result = new ArrayList<DependencyNode>();

		for (DependencyNode node : children)
		{
			if (!(node.getTable().compareNames(startWith.getTable()))) result.add(node);
			result.addAll(getAllNodes(node));
		}
		return result;
	}

	/**
	 * DependencyNode's equals method compares the FK names as well, which is not something
	 * we need in this context.
	 * Additionally we want to manipulate the level of the node according to the max/min
	 * level for that table.
	 */
	static class LevelNode
	{
		int level;
		DependencyNode node;

		LevelNode(DependencyNode nd, int lvl)
		{
			level = lvl;
			node = nd;
		}

		@Override
		public boolean equals(Object other)
		{
			if (other instanceof LevelNode)
			{
				LevelNode n = (LevelNode) other;
				return node.getTable().compareNames(n.node.getTable());
			}
			return false;
		}

		@Override
		public int hashCode()
		{
			return node.getTable().getTableName().hashCode();
		}

		@Override
		public String toString()
		{
			return node.getTable().getTableName() + ", Level=" + level;
		}
	}

}

