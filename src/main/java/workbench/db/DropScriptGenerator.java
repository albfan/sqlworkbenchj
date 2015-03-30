/*
 * DropScriptGenerator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import workbench.interfaces.ScriptGenerationMonitor;
import workbench.interfaces.Scripter;
import workbench.interfaces.TextOutput;

import workbench.db.sqltemplates.TemplateHandler;

import workbench.storage.RowActionMonitor;

import workbench.sql.parser.ParserType;
import workbench.sql.parser.ScriptParser;

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
	private Map<TableIdentifier, List<String>> dropConstraints = new HashMap<>();
	private Map<TableIdentifier, List<String>> restoreConstraints = new HashMap<>();
	private Map<TableIdentifier, String> dropTableStatements = new HashMap<>();
	private String dropTemplate;
	private boolean includeCreate = true;
	private boolean includeDropTable = true;
	private boolean sortByType;
	private boolean includeTableMarkers = true;

	public DropScriptGenerator(WbConnection aConnection)
	{
		this.connection = aConnection;
		dropTemplate = connection.getDbSettings().getDropFKConstraint("table");
	}

  @Override
  public void setTextOutput(TextOutput output)
  {
  }

	@Override
	public WbConnection getCurrentConnection()
	{
		return connection;
	}

	public void setIncludeComments(boolean flag)
	{
		this.includeTableMarkers = flag;
	}

	public void setSortByType(boolean flag)
	{
		this.sortByType = flag;
	}

	public void setIncludeRecreateStatements(boolean flag)
	{
		this.includeCreate = flag;
	}

	public void setIncludeDropTable(boolean flag)
	{
		this.includeDropTable = flag;
	}

	public void setTable(TableIdentifier table)
	{
		if (table == null) throw new IllegalArgumentException("The table name may not be empty");
		this.tables = Collections.singletonList(this.connection.getMetadata().findTable(table, false));
		reset();
	}

	public void setTables(List<TableIdentifier> tableList)
	{
		this.tables = new ArrayList<>(tableList.size());
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
		List<DependencyNode> visitedTables = new ArrayList<>();
		TableDependency dependency = new TableDependency(connection, table);
		dependency.readDependencyTree(true);
		dependency.setRetrieveDirectChildrenOnly(true);
		List<DependencyNode> leafs = dependency.getRootNode().getChildren();

		List<String> drop = new ArrayList<>();
		List<String> restore = new ArrayList<>();

		if (includeDropTable)
		{
			TableSourceBuilder builder = TableSourceBuilderFactory.getBuilder(connection);
			PkDefinition pk = connection.getMetadata().getIndexReader().getPrimaryKey(table);
			CharSequence pkSource = builder.getPkSource(table, pk, false, false);
			if (pkSource != null)
			{
				restore.add(pkSource.toString().trim());
			}
		}

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
		ScriptParser p = new ScriptParser(ParserType.getTypeFromConnection(connection));
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
		ScriptParser p = new ScriptParser(ParserType.getTypeFromConnection(connection));
		p.setScript(builder.getFkSource(node.getTable()).toString());
		int count = p.getSize();
		for (int i=0; i < count; i++)
		{
			String stmt = p.getCommand(i);
			if (stmt.contains(node.getFkName()))
			{
				restore.add(SqlUtil.addSemicolon(stmt));
			}
		}
	}

	private String createDropFKStatement(DependencyNode node)
	{
		if (node == null) return null;
    String drop = TemplateHandler.replaceTablePlaceholder(dropTemplate, node.getTable(), connection);
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

		if (sortByType)
		{
			appendDropFKs(script);

			if (includeDropTable)
			{
				appendDropTables(script);
			}

			if (includeCreate)
			{
				appendCreateFks(script);
			}
		}
		else
		{
			boolean includeMarkers = includeTableMarkers && tables.size() > 1;
			for (TableIdentifier table : tables)
			{
				appendTable(script, table, includeMarkers);
			}
		}
		return script.toString();
	}

	private void appendCreateFks(StringBuilder script)
	{
		for (List<String> restore : restoreConstraints.values())
		{
			for (String dml : restore)
			{
				script.append(dml);
				script.append('\n');
			}
		}
		script.append('\n');
	}

	private void appendDropTables(StringBuilder script)
	{
		for (String drop : dropTableStatements.values())
		{
			script.append(drop);
			script.append('\n');
		}
		script.append('\n');
	}

	private void appendDropFKs(StringBuilder script)
	{
		int count = 0;
		for (List<String> tableCons : dropConstraints.values())
		{
			for (String drop : tableCons)
			{
				script.append(drop);
				script.append('\n');
				count ++;
			}
		}
		if (count > 0)
		{
			script.append('\n');
		}
	}
	private void appendTable(StringBuilder script, TableIdentifier table, boolean includeMarkers)
	{
		List<String> constraintsDrop = dropConstraints.get(table);
		if (constraintsDrop == null) return;

		boolean markerAdded = false;
		if (includeMarkers && (constraintsDrop.size() > 0 || includeDropTable))
		{
			script.append("--- BEGIN ");
			script.append(table.getTableName());
			script.append(" ---\n");
			markerAdded = true;
		}

		for (String drop : constraintsDrop)
		{
			script.append(drop);
			script.append('\n');
		}
		if (constraintsDrop.size() > 0) script.append('\n');

		if (this.includeDropTable)
		{
			script.append(dropTableStatements.get(table));
			script.append("\n\n");
		}

		if (includeCreate)
		{
			List<String> restore = restoreConstraints.get(table);
			for (String dml : restore)
			{
				script.append(dml);
				script.append('\n');
			}
		}

		if (markerAdded)
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
				scriptMonitor.setCurrentObject(table.getTableName(), currentTable, count);
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
