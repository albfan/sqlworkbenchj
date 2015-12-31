/*
 * TableDependencySorter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import workbench.interfaces.ScriptGenerationMonitor;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.db.DependencyNode;
import workbench.db.TableDependency;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.util.AggregatingMap;


/**
 * A class to sort tables according to their foreign key constraints,
 * so that data can be imported or deleted without disabling FK constraints.
 *
 * @author Thomas Kellerer
 */
public class TableDependencySorter
{
	private final WbConnection dbConn;
	private final List<TableIdentifier> cycleErrors = new LinkedList<TableIdentifier>();
	private ScriptGenerationMonitor monitor;
	private TableDependency dependencyReader;
	private boolean cancel;
	private boolean validateInputTables = true;

	public TableDependencySorter(WbConnection con)
	{
		this.dbConn = con;
	}

	public void setValidateTables(boolean flag)
	{
		this.validateInputTables = flag;
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
		return cycleErrors.size() > 0;
	}

	public List<TableIdentifier> getErrorTables()
	{
		if (cycleErrors == null) return Collections.emptyList();
		return Collections.unmodifiableList(cycleErrors);
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

		if (validateInputTables)
		{
			// make sure only existing tables are kept in the list of tables that need processing
			tables = validateTables(tables);
		}

		Collection<DependencyNode> allNodes = collectRoots(tables);

		if (cancel)
		{
			return Collections.emptyList();
		}

		if (this.monitor != null)
		{
			this.monitor.setCurrentObject(ResourceMgr.getFormattedString("MsgCalcDelDeps"), -1, -1);
		}

		if (addMissing)
		{
			Set<TableIdentifier> missing = new HashSet<TableIdentifier>();
			for (DependencyNode node : allNodes)
			{
				if (!tables.contains(node.getTable()))
				{
					missing.add(node.getTable());
				}
			}
			tables.addAll(missing);
		}

		List<TableIdentifier> result = sortTables(allNodes, tables, bottomUp);

		return result;
	}

	private List<TableIdentifier> validateTables(List<TableIdentifier> toCheck)
	{
		List<TableIdentifier> result = new ArrayList<TableIdentifier>(toCheck.size());
		for (TableIdentifier tbl : toCheck)
		{
			TableIdentifier realTable = dbConn.getMetadata().findTable(tbl);
			if (realTable != null)
			{
				result.add(realTable);
			}
		}
		return result;
	}

	private DependencyNode findChildTree(Collection<DependencyNode> nodes, TableIdentifier tbl)
	{
		int maxLevel = Integer.MIN_VALUE;
		DependencyNode lastNode = null;

		for (DependencyNode node : nodes)
		{
			DependencyNode child = node.findChildTree(tbl);
			if (child != null)
			{
				int childLevel = child.getLevel();
				if (child.getLevel() > maxLevel)
				{
					maxLevel = childLevel;
					lastNode = child;
				}
			}
		}
		return lastNode;
	}

	public boolean isCancelled()
	{
		return cancel;
	}
	
	public void cancel()
	{
		cancel = true;
		if (this.dependencyReader != null)
		{
			dependencyReader.cancel();
		}
	}

	private List<DependencyNode> collectRoots(List<TableIdentifier> tables)
	{
		List<DependencyNode> allNodes = new ArrayList<DependencyNode>(tables.size() * 2);
		Set<DependencyNode> rootNodes = new HashSet<DependencyNode>(tables.size());
		dependencyReader = new TableDependency(dbConn);

		int num = 1;
		for (TableIdentifier tbl : tables)
		{
			if (cancel) break;

			if (this.monitor != null)
			{
				this.monitor.setCurrentObject(tbl.getTableExpression(), num, tables.size());
			}
			num ++;
			DependencyNode root = findChildTree(allNodes, tbl);
			if (root == null)
			{
				dependencyReader.setMainTable(tbl);
				dependencyReader.readTreeForChildren();
				if (dependencyReader.wasAborted())
				{
					cycleErrors.add(tbl);
				}
				root = dependencyReader.getRootNode();
				rootNodes.add(root);
			}
			else
			{
				LogMgr.logDebug("TableDependencySorter.createLevelMapping()", "Re-using child tree for " + tbl + " with level: " + root.getLevel());
			}
			List<DependencyNode> allChildren = getAllNodes(root);
			allNodes.addAll(allChildren);
		}

		if (cancel) return Collections.emptyList();

		// The "starting" tables have not been added yet.
		// They only need to be added if they did not appear as a child
		// in one of the sub-trees
		for (DependencyNode node : rootNodes)
		{
			DependencyNode check = findNodeForTable(allNodes, node.getTable());
			if (check == null)
			{
				allNodes.add(node);
			}
		}
		return allNodes;
	}

	public static List<TableIdentifier> sortTables(final Collection<DependencyNode> allNodes, final Collection<TableIdentifier> tables, final boolean bottomUp)
	{
		long start = System.currentTimeMillis();

		List<TableIdentifier> sorted = new ArrayList<TableIdentifier>(tables);

		final AggregatingMap<TableIdentifier, DependencyNode> tableNodes = new AggregatingMap<TableIdentifier, DependencyNode>(false);
		final Map<TableIdentifier, Integer> totalLevels = new HashMap<TableIdentifier, Integer>();
		final Map<TableIdentifier, Integer> refCounters = new HashMap<TableIdentifier, Integer>();

		for (DependencyNode node : allNodes)
		{
			tableNodes.addValue(node.getTable(), node);

			int level = 0;
			Integer lvl = totalLevels.get(node.getTable());
			if (lvl != null)
			{
				level = lvl.intValue();
			}
			level += node.getLevel();
			totalLevels.put(node.getTable(), Integer.valueOf(level));
		}

		final Comparator<TableIdentifier> depComp = new Comparator<TableIdentifier>()
		{
			final int factor = bottomUp ? -1 : 1;

			@Override
			public int compare(TableIdentifier o1, TableIdentifier o2)
			{
				if (o1.equals(o2))
				{
					return 0;
				}

				int levelOne = getLevelTotal(o1);
				int levelTwo = getLevelTotal(o2);

				int result = 0;

				if (levelOne == levelTwo)
				{
					int refCountOne = getReferenceCounter(o1);
					int refCountTwo = getReferenceCounter(o2);
					result = factor * (refCountOne - refCountTwo);
				}
				else
				{
					result = factor * (levelOne - levelTwo);
				}

				int sign = (int)Math.signum(result);
				if (result == 0 || sign == factor)
				{
					Set<DependencyNode> o2nodes = tableNodes.get(o2);
					for (DependencyNode n2 : o2nodes)
					{
						if (n2.containsParentTable(o1))
						{
							return -factor;
						}
						if (result == 0 && n2.containsChildTable(o1))
						{
							return factor;
						}
					}
				}

				if (result == 0 || sign == -factor)
				{
					Set<DependencyNode> o1nodes = tableNodes.get(o1);
					for (DependencyNode n1 : o1nodes)
					{
						if (n1.containsParentTable(o2))
						{
							return factor;
						}
						if (result == 0 && n1.containsChildTable(o2))
						{
							return -factor;
						}
					}
				}
				if (result == 0)
				{
					// if all parameters are equal, sort them alphabetically
					result = o1.getTableExpression().compareTo(o2.getTableExpression());
				}
				return result;
			}

			private int getReferenceCounter(TableIdentifier tbl)
			{
				Integer ref = refCounters.get(tbl);
				if (ref != null)
				{
					return ref.intValue();
				}

				int refCount = 0;
				for (DependencyNode node : allNodes)
				{
					if (node.getTable().equals(tbl) || node.containsParentTable(tbl))
					{
						refCount ++;
					}
				}
				refCounters.put(tbl, Integer.valueOf(refCount));
				return refCount;
			}

			private int getLevelTotal(TableIdentifier tbl)
			{
				Integer lvl = totalLevels.get(tbl);
				if (lvl == null)
				{
					return 0;
				}
				return lvl.intValue();
			}
		};

		Collections.sort(sorted, depComp);

		long duration = System.currentTimeMillis() - start;

		LogMgr.logDebug("TableDependencySorter.sortTables()", "Sorting " + sorted.size() + " tables took " + duration + "ms");
		return sorted;
	}

	private DependencyNode findNodeForTable(Collection<DependencyNode> nodes, TableIdentifier tbl)
	{
		int maxLevel = Integer.MIN_VALUE;
		DependencyNode lastNode = null;
		for (DependencyNode node : nodes)
		{
			if (node.getTable().compareNames(tbl))
			{
				int level = node.getLevel();
				if (level > maxLevel)
				{
					lastNode = node;
					maxLevel = level;
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
}


