/*
 * DeleteScriptGenerator.java
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
package workbench.db;

import java.io.File;
import java.io.FileWriter;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import workbench.WbManager;
import workbench.interfaces.ScriptGenerationMonitor;
import workbench.interfaces.Scripter;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.gui.components.WbTable;
import workbench.gui.dbobjects.ObjectScripterUI;

import workbench.storage.ColumnData;
import workbench.storage.DataStore;
import workbench.storage.SqlLiteralFormatter;

import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;
import workbench.sql.formatter.SqlFormatter;
import workbench.util.CollectionUtil;
import workbench.util.FileUtil;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * Generates a SQL script to delete a record from the given table and
 * any dependent tables.
 *
 * @author  Thomas Kellerer
 */
public class DeleteScriptGenerator
	implements Scripter
{
	private final WbConnection connection;
	private List<ColumnData> columnValues;
	private TableDependency dependency;
	private final DbMetadata meta;
	private TableIdentifier rootTable;
	private WbTable sourceTable;
	private ScriptGenerationMonitor monitor;
	private final List<String> statements = new LinkedList<String>();
	private final SqlLiteralFormatter formatter;
	private boolean formatSql = true;
	private boolean showFkNames;

	private final Comparator<Integer> descComparator = new Comparator<Integer>()
		{
			@Override
			public int compare(Integer i1, Integer i2)
			{
				int val1 = i1.intValue();
				int val2 = i2.intValue();
				return (val1 < val2 ? 1 : (val1==val2 ? 0 : -1));
			}
		};

	public DeleteScriptGenerator(WbConnection aConnection)
		throws SQLException
	{
		this.connection = aConnection;
		this.meta = this.connection.getMetadata();
		this.formatter = new SqlLiteralFormatter(this.connection);
	}

	@Override
	public WbConnection getCurrentConnection()
	{
		return connection;
	}

	public void setShowConstraintNames(boolean flag)
	{
		this.showFkNames = flag;
	}

	public void setFormatSql(boolean flag)
	{
		this.formatSql = flag;
	}

	public void setSource(WbTable aTable)
	{
		this.sourceTable = aTable;
	}

	public void setTable(TableIdentifier table)
		throws SQLException
	{
		if (table == null) throw new IllegalArgumentException("The table name may not be empty");

		// Make sure we are using a completely filled TableIdentifier
		// otherwise comparisons won't work correctly
		this.rootTable = this.meta.findTable(table, false);
		this.dependency = new TableDependency(this.connection, this.rootTable);
	}

	public void setValues(List<ColumnData> colValues)
	{
		this.columnValues = colValues;
	}

	@Override
	public boolean isCancelled()
	{
		// not implemented yet
		return false;
	}

	@Override
	public void cancel()
	{
		if (dependency != null)
		{
			dependency.cancel();
		}
	}

	private void createStatements(boolean includeRoot)
	{
		this.dependency.setScriptMonitor(monitor);
		this.dependency.readDependencyTree(true);

//		DependencyDuplicateFinder finder = new DependencyDuplicateFinder(dependency.getRootNode());
//		List<String> duplicates = finder.getDuplicates();

		Map<Integer, Set<DependencyNode>> levels = buildLevels(dependency.getRootNode(), 1);
//		Map<Integer, Set<DependencyNode>> levels = buildLevelMap();
		dumpTree(levels, "tree.txt");
//		adjustLevels(levels);

		for (Map.Entry<Integer, Set<DependencyNode>> entry : levels.entrySet())
		{
			// collect all nodes for one table so that we can generate a single delete statement
			// that covers all foreign keys at once
			Map<TableIdentifier, Set<DependencyNode>> tableNodes = new HashMap<TableIdentifier, Set<DependencyNode>>();

			for (DependencyNode node : entry.getValue())
			{
				TableIdentifier tbl = node.getTable();
				Set<DependencyNode> nodes = tableNodes.get(tbl);
				if (nodes == null)
				{
					nodes = new HashSet<DependencyNode>();
					tableNodes.put(tbl, nodes);
				}
				nodes.add(node);
			}

			for (Map.Entry<TableIdentifier, Set<DependencyNode>> tblEntry : tableNodes.entrySet())
			{
				statements.add(createDeleteStatement(tblEntry.getKey(), tblEntry.getValue()));
			}
		}

		if (includeRoot)
		{
			DependencyNode root = this.dependency.getRootNode();
			StringBuilder rootSql = new StringBuilder(100);
			rootSql.append("DELETE FROM ");
			rootSql.append(root.getTable().getTableExpression(this.connection));
			rootSql.append(" WHERE ");
			this.addRootTableWhere(rootSql);
			statements.add(formatSql(rootSql));
		}
	}

	private String formatSql(StringBuilder sql)
	{
		if (!formatSql)
		{
			return sql.toString();
		}

		try
		{
			SqlFormatter f = new SqlFormatter(sql, Settings.getInstance().getFormatterMaxSubselectLength(), connection.getDbId());
			String formatted = f.getFormattedSql() + "\n";
			return formatted;
		}
		catch (Exception e)
		{
			return sql.toString();
		}
	}

	private String createDeleteStatement(TableIdentifier table, Set<DependencyNode> nodes)
	{
		if (table == null) return StringUtil.EMPTY_STRING;
		if (CollectionUtil.isEmpty(nodes)) return StringUtil.EMPTY_STRING;

		Set<DependencyNode> processed = new HashSet<DependencyNode>(nodes.size());
		StringBuilder sql = new StringBuilder(nodes.size() * 200);

		if (showFkNames)
		{
			for (DependencyNode node : nodes)
			{
				sql.append("-- ").append(node.getFkName()).append('\n');
			}
		}
		sql.append("DELETE FROM ");
		sql.append(table.getTableExpression(this.connection));
		sql.append(" WHERE ");

		boolean first = true;
		for (DependencyNode node : nodes)
		{
			if (processed.contains(node)) continue;
			if (first)
			{
				first = false;
			}
			else
			{
				sql.append("\n  OR ");
			}
			this.addParentWhere(sql, node);
			processed.add(node);
		}
		return formatSql(sql);
	}

	private String createDeleteStatement(DependencyNode node)
	{
		if (node == null) return null;
		StringBuilder sql = new StringBuilder(200);
		if (showFkNames)
		{
			sql.append("-- ").append(node.getFkName()).append('\n');
		}
		sql.append("DELETE FROM ");
		sql.append(node.getTable().getTableExpression(this.connection));
		sql.append(" WHERE ");

		this.addParentWhere(sql, node);
		return formatSql(sql);
	}

	private void addParentWhere(StringBuilder sql, DependencyNode node)
	{
		try
		{
			DependencyNode parent = node.getParent();

			Map<String, String> columns = node.getColumns();
			int count = 0;
			for (Entry<String, String> entry : columns.entrySet())
			{
				String column = entry.getKey();
				String parentColumn = entry.getValue();

				boolean addRootWhere = this.rootTable.equals(parent.getTable());

				if (count > 0) sql.append(" AND ");

				if (!addRootWhere)
				{
					sql.append(" (");
					sql.append(column);
					sql.append(" IN ( SELECT ");
					sql.append(parentColumn);
					sql.append(" FROM ");
					sql.append(parent.getTable().getTableExpression(this.connection));
					sql.append(" WHERE ");
					this.addParentWhere(sql, parent);
					sql.append("))");
				}
				else
				{
					this.addRootTableWhere(sql, parentColumn, column);
				}
				count ++;
			}
		}
		catch (Throwable th)
		{
			LogMgr.logError("DeleteScriptGenerator.addParentWhere()", "Error during script generation", th);
		}
	}

	private void addRootTableWhere(StringBuilder sql)
	{
		boolean first = true;
		for (ColumnData data : this.columnValues)
		{
			if (!first)
			{
				sql.append(" AND ");
			}
			else
			{
				first = false;
			}
			ColumnIdentifier col = data.getIdentifier();
			String colname;
			if (col.getDataType() == ColumnIdentifier.NO_TYPE_INFO)
			{
				colname = SqlUtil.quoteObjectname(col.getColumnName(), false);
			}
			else
			{
				colname = connection.getMetadata().quoteObjectname(col.getColumnName());
			}
			appendColumnData(sql, colname, data);
		}
	}

	private ColumnData findColData(String column)
	{
		for (ColumnData col : this.columnValues)
		{
			if (col.getIdentifier().getColumnName().equalsIgnoreCase(column)) return col;
		}
		return null;
	}

	private void addRootTableWhere(StringBuilder sql, String parentColumn, String childColumn)
	{
		ColumnData data = findColData(parentColumn);
		childColumn = connection.getMetadata().quoteObjectname(childColumn);
		appendColumnData(sql, childColumn, data);
	}

	private boolean isExpression(ColumnData data)
	{
		if (data == null) return false;
		if (data.getIdentifier() == null) return false;

		Object value = data.getValue();
		if (value == null)
		{
			return false;
		}

		if (value instanceof String)
		{
			String s = (String)value;
			SQLLexer lexer = new SQLLexer(s);
			SQLToken first = lexer.getNextToken(false, false);
			if (first.isNumberLiteral() || first.isLiteral())
			{
				return false;
			}
			return true;
		}
		return false;
	}

	private void appendColumnData(StringBuilder sql, String column, ColumnData data)
	{
		sql.append(column);
		if (data == null || data.isNull())
		{
			sql.append(" IS NULL");
		}
		else if (isExpression(data))
		{
			sql.append(' ');
			sql.append(data.getValue());
		}
		else
		{
			sql.append(" = ");
			sql.append(formatter.getDefaultLiteral(data));
		}
	}

	public void startGenerate()
	{
		ObjectScripterUI ui = new ObjectScripterUI(this);
		ui.show(WbManager.getInstance().getCurrentWindow());
	}

	@Override
	public void setProgressMonitor(ScriptGenerationMonitor aMonitor)
	{
		this.monitor = aMonitor;
	}

	@Override
	public String getScript()
	{
		if (this.statements.isEmpty())
		{
			this.generateScript();
		}
		StringBuilder script = new StringBuilder();

		for (String dml : statements)
		{
			script.append(dml);
			script.append(";\n\n");
		}

		return script.toString();
	}

	public CharSequence getScriptForValues(List<ColumnData> values)
		throws SQLException
	{
		this.statements.clear();
		this.setValues(values);
		this.createStatements(true);
		return getScript();
	}

	public List<String> getStatementsForValues(List<ColumnData> values, boolean includeRoot)
	{
		this.statements.clear();
		this.setValues(values);
		this.createStatements(includeRoot);
		return Collections.unmodifiableList(statements);
	}

	@Override
	public void generateScript()
	{
		if (this.sourceTable == null) return;

		DataStore ds = this.sourceTable.getDataStore();
		if (ds == null) return;

		int[] rows = this.sourceTable.getSelectedRows();
		if (rows.length == 0)
		{
			return;
		}

		if (this.connection.isBusy())
		{
			Exception e = new Exception("Connection is busy");
			LogMgr.logError("DeleteScriptGenerator.generateScript()", "Connection is busy!", e);
		}

		ds.checkUpdateTable();
		TableIdentifier tbl = ds.getUpdateTable();

		int numRows = rows.length;

		try
		{
			connection.setBusy(true);
			this.setTable(tbl);

			for (int i=0; i < numRows; i++)
			{
				List<ColumnData> pkvalues = ds.getPkValues(rows[i]);
				this.setValues(pkvalues);
				if (monitor != null) this.monitor.setCurrentObject(ResourceMgr.getString("MsgGeneratingScriptForRow"), i+1, numRows);
				this.createStatements(true);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("SqlPanel.generateDeleteScript", "Error generating delete script", e);
		}
		finally
		{
			connection.setBusy(false);
		}
	}

	private void adjustLevels(Map<Integer, Set<DependencyNode>> map)
	{
//		dumpTree(map, "initial_levels.txt");

//		for (Map.Entry<Integer, Set<DependencyNode>> entry : map.entrySet())
//		{
//			int level = entry.getKey();
//			Set<DependencyNode> nodes = entry.getValue();
//			Iterator<DependencyNode> itr = nodes.iterator();
//			while (itr.hasNext())
//			{
//				DependencyNode node = itr.next();
//				int maxLevel = findHighestLevel(node, map);
//				if (maxLevel > level)
//				{
//					itr.remove();
//					map.get(maxLevel).add(node);
//				}
//			}
//		}

//		dumpTree(map, "first_adjust.txt");

		LogMgr.logDebug("DeleteScriptGenerator.adjustLevels()" , "Start shuffling tables around");
		for (Map.Entry<Integer, Set<DependencyNode>> entry : map.entrySet())
		{
			int level = entry.getKey();
			Set<DependencyNode> nodes = entry.getValue();
			Iterator<DependencyNode> itr = nodes.iterator();
			while (itr.hasNext())
			{
				DependencyNode node = itr.next();
				int minLevel = findLowestLevelForTable(node.getTable(), map);
				if (minLevel < level)
				{
					LogMgr.logDebug("DeleteScriptGenerator.adjustLevels()" , "Moving entry for table: " + node.getTable() + " from level " + level + " to level " + minLevel);
					itr.remove();
					map.get(minLevel).add(node);
				}
			}
		}
		dumpTree(map, "final_levels.txt");
	}

	private int findLowestLevelForTable(TableIdentifier table, Map<Integer, Set<DependencyNode>> map)
	{
		int minLevel = Integer.MAX_VALUE;
		for (Map.Entry<Integer, Set<DependencyNode>> entry : map.entrySet())
		{
			int level = entry.getKey();
			for (DependencyNode node : entry.getValue())
			{
				if (node.getTable().equals(table) && level < minLevel)
				{
					minLevel = level;
				}
			}
		}
		return minLevel;
	}

	private int findHighestLevelForTable(TableIdentifier table, Map<Integer, Set<DependencyNode>> map)
	{
		int maxLevel = Integer.MIN_VALUE;
		for (Map.Entry<Integer, Set<DependencyNode>> entry : map.entrySet())
		{
			int level = entry.getKey();
			for (DependencyNode node : entry.getValue())
			{
				if (node.getTable().equals(table) && level > maxLevel)
				{
					maxLevel = level;
				}
			}
		}
		return maxLevel;
	}

	private int findHighestLevel(DependencyNode node, Map<Integer, Set<DependencyNode>> map)
	{
		int maxLevel = Integer.MIN_VALUE;
		for (Map.Entry<Integer, Set<DependencyNode>> entry : map.entrySet())
		{
			int level = entry.getKey();
			if (entry.getValue().contains(node) && level > maxLevel)
			{
				maxLevel = level;
			}
		}
		return maxLevel;
	}

	private Map<Integer, Set<DependencyNode>> buildLevels(DependencyNode root, int level)
	{
		Map<Integer, Set<DependencyNode>> result = new TreeMap<Integer, Set<DependencyNode>>(descComparator);

		List<DependencyNode> children = root.getChildren();

		if (children.isEmpty())
		{
			return result;
		}

		Integer lvl = Integer.valueOf(level);
		for (DependencyNode child : children)
		{
			Set<DependencyNode> nodes = result.get(lvl);
			if (nodes == null)
			{
				nodes = new HashSet<DependencyNode>();
				result.put(lvl, nodes);
			}
			nodes.add(child);
		}

		for (DependencyNode child : children)
		{
			if (child.getChildren().size() > 0)
			{
				mergeMaps(result, buildLevels(child, level + 1));
			}
		}
		return result;
	}

	private void mergeMaps(Map<Integer, Set<DependencyNode>> target, Map<Integer, Set<DependencyNode>> source)
	{
		for (Integer key : source.keySet())
		{
			Set<DependencyNode> main = target.get(key);
			if (main == null)
			{
				main = new HashSet<DependencyNode>();
				target.put(key, main);
			}
			Set<DependencyNode> toAdd = source.get(key);
			main.addAll(toAdd);
		}
	}

	private Map<Integer, Set<DependencyNode>> buildLevelMap()
	{
		Map<Integer, Set<DependencyNode>> result = new HashMap<Integer, Set<DependencyNode>>();
		Set<DependencyNode> visited = new HashSet<DependencyNode>();

		dependency.getLeafs();
		for (DependencyNode leaf : dependency.getLeafs())
		{
			int level = 1;
			if (!visited.contains(leaf))
			{
				Set<DependencyNode> leafNodes = result.get(level);
				if (leafNodes == null)
				{
					leafNodes = new HashSet<DependencyNode>();
					result.put(level, leafNodes);
				}
				leafNodes.add(leaf);
				visited.add(leaf);
			}

			DependencyNode parent = leaf.getParent();
			while (parent != null)
			{
				level++;
				if (!visited.contains(parent))
				{
					Set<DependencyNode> levelNodes = result.get(level);
					if (levelNodes == null)
					{
						levelNodes = new HashSet<DependencyNode>();
						result.put(level, levelNodes);
					}
					levelNodes.add(parent);
					visited.add(parent);
				}
				parent = parent.getParent();
			}
		}
		return result;
	}

	private void dumpTree(Map<Integer, Set<DependencyNode>> levels, String fname)
	{
		FileWriter writer = null;
		try
		{
			writer = new FileWriter(new File("c:/temp", fname));
			writer.append(this.rootTable.getTableExpression() + "\n");

			for (Map.Entry<Integer, Set<DependencyNode>> entry : levels.entrySet())
			{
				Set<DependencyNode> sorted = new TreeSet<DependencyNode>(new Comparator<DependencyNode>()
				{
					@Override
					public int compare(DependencyNode o1, DependencyNode o2)
					{
						return o1.getTable().getTableName().compareTo(o2.getTable().getTableName());
					}
				});
				sorted.addAll(entry.getValue());

				writer.append(entry.getKey() + ":\n");
				for (DependencyNode node : sorted)
				{
					writer.append("  " + node.getTable() + " (" + node.getFkName() + ")\n");
				}
			}
		}
		catch (Exception ex)
		{
			LogMgr.logDebug("dumpTree()", "error writing tree", ex);
		}
		finally
		{
			FileUtil.closeQuietely(writer);
		}
	}

}
