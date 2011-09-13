/*
 * DeleteScriptGenerator.java
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

import java.util.*;
import workbench.interfaces.ScriptGenerationMonitor;
import workbench.interfaces.Scripter;
import workbench.sql.ScriptParser;
import workbench.storage.RowActionMonitor;
import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;

/**
 * Generates a SQL script to drop a table including all foreign keys referencing this table.
 *
 * The foreign keys are detected recursively.
 *
 * @author  Thomas Kellerer
 */
public class DropScriptGenerator
	implements Scripter
{
	private WbConnection connection;
	private List<TableIdentifier> tables;
	private ScriptGenerationMonitor scriptMonitor;
	private RowActionMonitor rowMonitor;
	private Map<TableIdentifier, List<String>> dropConstraints = new HashMap<TableIdentifier, List<String>>();
	private Map<TableIdentifier, List<String>> restoreConstraints = new HashMap<TableIdentifier, List<String>>();
	private Map<TableIdentifier, String> dropTableStatements = new HashMap<TableIdentifier, String>();
	private String dropTemplate;

	public DropScriptGenerator(WbConnection aConnection)
	{
		this.connection = aConnection;
		dropTemplate = connection.getDbSettings().getDropConstraint("table");
	}

	public void setTable(TableIdentifier table)
	{
		if (table == null) throw new IllegalArgumentException("The table name may not be empty");
		this.tables = Collections.singletonList(this.connection.getMetadata().findTable(table, false));
		reset();
	}

	public void setTables(List<TableIdentifier> tableList)
	{
		this.tables = new ArrayList<TableIdentifier>(tableList.size());
		for (TableIdentifier tbl : tableList)
		{
			TableIdentifier toUse = this.connection.getMetadata().findTable(tbl, false);
			if (toUse != null)
			{
				tables.add(toUse);
			}
		}
		reset();
	}

	private void reset()
	{
		dropConstraints.clear();
		restoreConstraints.clear();
		dropTableStatements.clear();
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
		// not implemented yet
	}

	private void createStatementsForTable(TableIdentifier table)
	{
		List<DependencyNode> visitedTables = new ArrayList<DependencyNode>();
		TableDependency dependency = new TableDependency(connection, table);
		dependency.readDependencyTree(true);
		dependency.setRetrieveDirectChildrenOnly(true);
		List<DependencyNode> leafs = dependency.getRootNode().getChildren();

		List<String> drop = new ArrayList<String>();
		List<String> restore = new ArrayList<String>();

		for (DependencyNode node : leafs)
		{
			if (visitedTables.contains(node)) continue;
			drop.add(createDropFKStatement(node));
			createRestoreStatements(restore, node);
			visitedTables.add(node);
		}

		createRootRestoreStatements(restore, table);
		dropConstraints.put(table, drop);
		restoreConstraints.put(table, restore);

		GenericObjectDropper dropper = new GenericObjectDropper();
		dropper.setConnection(connection);
		dropper.setCascade(false);
		dropper.setUseTransaction(false);
		dropper.setObjects(Collections.singletonList(table));
		dropTableStatements.put(table, dropper.getScript().toString().trim());
	}

	private void createRootRestoreStatements(List<String> restore, TableIdentifier root)
	{
		TableSourceBuilder builder = TableSourceBuilderFactory.getBuilder(connection);
		ScriptParser p = new ScriptParser();
		StringBuilder fkSource = builder.getFkSource(root);
		if (fkSource.length() > 0)
		{
			p.setScript(fkSource.toString());
			int count = p.getSize();
			for (int i=0; i < count; i++)
			{
				String stmt = p.getCommand(i);
				restore.add(SqlUtil.addSemicolon(stmt));
			}
		}
	}
	private void createRestoreStatements(List<String> restore, DependencyNode node)
	{
		TableSourceBuilder builder = TableSourceBuilderFactory.getBuilder(connection);
		ScriptParser p = new ScriptParser();
		p.setScript(builder.getFkSource(node.getTable()).toString());
		int count = p.getSize();
		for (int i=0; i < count; i++)
		{
			String stmt = p.getCommand(i);
			if (stmt.indexOf(node.getFkName()) > -1)
			{
				restore.add(SqlUtil.addSemicolon(stmt));
			}
		}
	}

	private String createDropFKStatement(DependencyNode node)
	{
		if (node == null) return null;
		String drop = dropTemplate.replace(MetaDataSqlManager.TABLE_NAME_PLACEHOLDER, node.getTable().getTableExpression(this.connection));
		drop = drop.replace(MetaDataSqlManager.CONSTRAINT_NAME_PLACEHOLDER, node.getFkName());

		return SqlUtil.addSemicolon(drop);
	}

	public void setRowMonitor(RowActionMonitor monitor)
	{
		this.rowMonitor = monitor;
	}

	@Override
	public void setProgressMonitor(ScriptGenerationMonitor monitor)
	{
		this.scriptMonitor = monitor;
	}

	@Override
	public String getScript()
	{
		if (CollectionUtil.isEmpty(dropConstraints))
		{
			generateScript();
		}
		StringBuilder script = new StringBuilder(dropConstraints.size() * 30);

		boolean includeMarkers = tables.size() > 1;
		for (TableIdentifier table : tables)
		{
			appendTable(script, table, includeMarkers);
		}
		return script.toString();
	}

	private void appendTable(StringBuilder script, TableIdentifier table, boolean includeMarkers)
	{
		List<String> drop = dropConstraints.get(table);
		if (drop == null) return;

		if (includeMarkers)
		{
			script.append("--- BEGIN ");
			script.append(table.getTableName());
			script.append(" ---\n");
		}

		for (String dml : drop)
		{
			script.append(dml);
			script.append('\n');
		}
		if (drop.size() > 0) script.append('\n');
		script.append(dropTableStatements.get(table));
		script.append("\n\n");

		List<String> restore = restoreConstraints.get(table);
		for (String dml : restore)
		{
			script.append(dml);
			script.append('\n');
		}
		if (includeMarkers)
		{
			script.append("--- END ");
			script.append(table.getTableName());
			script.append(" ---\n\n");
		}
	}

	@Override
	public void generateScript()
	{
		reset();
		if (rowMonitor != null)
		{
			rowMonitor.setMonitorType(RowActionMonitor.MONITOR_PROCESS);
		}
		int count = tables.size();
		int currentTable = 1;
		for (TableIdentifier table : tables)
		{
			if (rowMonitor != null)
			{
				rowMonitor.setCurrentObject(table.getTableName(), currentTable, count);
			}
			if (scriptMonitor != null)
			{
				scriptMonitor.setCurrentObject(table.getTableName());
			}
			createStatementsForTable(table);
			currentTable ++;
		}
		if (rowMonitor != null)
		{
			rowMonitor.jobFinished();
		}
	}

	public List<TableIdentifier> getTables()
	{
		return Collections.unmodifiableList(this.tables);
	}

	public String getScriptFor(TableIdentifier table)
	{
		if (CollectionUtil.isEmpty(dropConstraints))
		{
			generateScript();
		}
		StringBuilder result = new StringBuilder(250);
		appendTable(result, table, false);
		return result.toString();
	}

	public List<String> getDropConstraintStatements(TableIdentifier table)
	{
		if (CollectionUtil.isEmpty(dropConstraints))
		{
			generateScript();
		}
		return Collections.unmodifiableList(this.dropConstraints.get(table));
	}

	public List<String> getRestoreStatements(TableIdentifier table)
	{
		if (CollectionUtil.isEmpty(dropConstraints))
		{
			generateScript();
		}
		return Collections.unmodifiableList(this.restoreConstraints.get(table));
	}
}
