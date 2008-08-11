/*
 * TableDependencySorter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.importer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import workbench.db.DependencyNode;
import workbench.db.TableDependency;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

/**
 * A class to sort tables according to their foreign key constraints,
 * so that data can be imported or deleted without disabling FK constraints.
 * 
 * @author support@sql-workbench.net
 */
public class TableDependencySorter 
{
	private WbConnection dbConn;
	private List<TableIdentifier> cycleErrors;
	
	public TableDependencySorter(WbConnection con)
	{
		this.dbConn = con;
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
		if (cycleErrors == null) return null;
		return Collections.unmodifiableList(cycleErrors);
	}
	
	/**
	 * Determines the FK dependencies for each table in the passed List, 
	 * and sorts them so that data can be imported without violating 
	 * foreign key constraints
	 * 
	 * @param tables the list of tables to be sorted
	 * @returns the tables sorted according to their FK dependencies
	 * @throws DependencyCycleException if an endless loop in the dependencies was detected
	 */
	private List<TableIdentifier> getSortedTableList(List<TableIdentifier> tables, boolean addMissing, boolean bottomUp)
	{
		List<LevelNode> levelMapping = createLevelMapping(tables, bottomUp);
		
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

	private List<LevelNode> createLevelMapping(List<TableIdentifier> tables, boolean bottomUp)
	{
		List<LevelNode> levelMapping = new ArrayList<LevelNode>(tables.size());
		
		for (TableIdentifier tbl : tables)
		{
			TableDependency deps = new TableDependency(dbConn, tbl);
			deps.readTreeForChildren();
			if (deps.wasAborted())
			{
				if (cycleErrors == null) cycleErrors = new LinkedList<TableIdentifier>();
				cycleErrors.add(tbl);
			}
			
			DependencyNode root = deps.getRootNode();
			if (root != null)
			{
				List<DependencyNode> allChildren = getAllNodes(root);
				putNodes(levelMapping, allChildren);
			}
		}
		
		Comparator<LevelNode> comp = null;
			
		if (bottomUp)
		{
			comp = new Comparator<LevelNode>()
			{
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
	
	protected void putNodes(List<LevelNode> levelMapping, List<DependencyNode> nodes)
	{
		TableIdentifier root = nodes.get(0).getTable();

		for (DependencyNode node : nodes)
		{
			TableIdentifier tbl = node.getTable();

			// There is no need to include self referencing tables into the level
			// mapping, as this will create a wrong level for them and a potential
			// parent of the self referencing table (e.g. through a different FK)
			if (!node.isRoot() && tbl.equals(root)) continue;
			
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
		for (LevelNode lvl : levelMapping)
		{
			if (lvl.node.getTable().getTableName().equalsIgnoreCase(tbl.getTableName())) return lvl;
		}
		return null;
	}
	
	/**
	 * Get all nodes of the passed dependency hierarchy as a "flat" list.
	 * This is public mainly to be able to run a unit test agains it.
	 */
	public List<DependencyNode> getAllNodes(DependencyNode startWith)
	{
		if (startWith == null) return Collections.emptyList();
		
		ArrayList<DependencyNode> result = new ArrayList<DependencyNode>();
		result.add(startWith);
		
		List<DependencyNode> children = startWith.getChildren();
		
		if (children.size() == 0) 
		{
			return result;
		}
		
		for (DependencyNode node : children)
		{
			result.addAll(getAllNodes(node));
		}
		return result;
	}

	static class LevelNode
	{
		int level;
		DependencyNode node;

		public LevelNode(DependencyNode nd, int lvl)
		{
			level = lvl;
			node = nd;
		}

		public boolean equals(Object other)
		{
			if (other instanceof LevelNode)
			{
				LevelNode n = (LevelNode) other;
				return node.getTable().getTableName().equalsIgnoreCase(n.node.getTable().getTableName());
			}
			return false;
		}

		public int hashCode()
		{
			return node.getTable().getTableName().hashCode();
		}

		public String toString()
		{
			return node.getTable().getTableName() + ", Level=" + level;
		}
	}
	
}

