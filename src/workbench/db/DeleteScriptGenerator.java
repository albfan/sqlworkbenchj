/*
 * DeleteScriptGenerator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import workbench.WbManager;
import workbench.gui.components.WbTable;
import workbench.gui.dbobjects.ObjectScripterUI;
import workbench.interfaces.ScriptGenerationMonitor;
import workbench.interfaces.Scripter;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.formatter.SqlFormatter;
import workbench.storage.ColumnData;
import workbench.storage.DataStore;
import workbench.storage.SqlLiteralFormatter;
import workbench.util.SqlUtil;

/**
 * Generates a SQL script to delete a record from the given table and
 * any dependent tables.
 * 
 * @author  support@sql-workbench.net
 */
public class DeleteScriptGenerator
	implements Scripter
{
	private WbConnection connection;
	private List<ColumnData> columnValues;
	private TableDependency dependency;
	private DbMetadata meta;
	private DataStore tableDefinition;
	private TableIdentifier rootTable = null;
	private WbTable sourceTable = null;
	private ScriptGenerationMonitor monitor;
	private List<String> statements = new LinkedList<String>();
	private SqlLiteralFormatter formatter;
	
	public DeleteScriptGenerator(WbConnection aConnection)
		throws SQLException
	{
		this.connection = aConnection;
		this.meta = this.connection.getMetadata();
		this.formatter = new SqlLiteralFormatter(this.connection);
	}

	public void useJdbcLiterals()
	{
		this.formatter.setProduct("jdbc");
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
		this.rootTable = this.meta.findTable(table);
		this.dependency = new TableDependency(this.connection, this.rootTable);
		this.tableDefinition = this.meta.getTableDefinition(this.rootTable);
	}

	public void setValues(List<ColumnData> colValues)
	{
		this.columnValues = colValues;
	}

	public boolean isCancelled()
	{
		// not implemented yet
		return false;
	}
	
	public void cancel()
	{
		// not implemented yet
	}
	
	private void createStatements(boolean includeRoot)
	{
		ArrayList<DependencyNode> parents = new ArrayList<DependencyNode>();
		List<DependencyNode> visitedTables = new ArrayList<DependencyNode>();
		this.dependency.readDependencyTree(true);
		List<DependencyNode> leafs = this.dependency.getLeafs();
		
		for (DependencyNode node : leafs)
		{
			if (visitedTables.contains(node)) continue;
			statements.add(createDeleteStatement(node));
			visitedTables.add(node);
			DependencyNode p = node.getParent();
			while (p != null)
			{
				if (!isMasterTable(p) && !parents.contains(p) && !leafs.contains(p))
				{
					parents.add(p);
				}
				p = p.getParent();
			}
		}

		for (DependencyNode pnode : parents)
		{
			if (visitedTables.contains(pnode)) continue;
			statements.add(createDeleteStatement(pnode));
			visitedTables.add(pnode);
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
		try
		{
			SqlFormatter f = new SqlFormatter(sql, Settings.getInstance().getFormatterMaxSubselectLength());
			String formatted = f.getFormattedSql().toString() + "\n";
			return formatted;
		}
		catch (Exception e)
		{
			return sql.toString();
		}
	}
	
	private String createDeleteStatement(DependencyNode node)
	{
		if (node == null) return null;
		StringBuilder sql = new StringBuilder(200);
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

	private boolean isMasterTable(DependencyNode node)
	{
		TableIdentifier table = node.getTable();
		return (this.rootTable.equals(table));
	}

	private void addRootTableWhere(StringBuilder sql)
	{
		boolean first = true;
		for (ColumnData col : this.columnValues)
		{
			if (!first)
			{
				sql.append(" AND ");
			}
			else
			{
				first = false;
			}
			appendColumnData(sql, col.getIdentifier().getColumnName(), col);
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
		appendColumnData(sql, childColumn, data);
	}

	private void appendColumnData(StringBuilder sql, String column, ColumnData data)
	{
		sql.append(SqlUtil.quoteObjectname(column));
		if (data.isNull() || data == null)
		{
			sql.append(" IS NULL");
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

	public void setProgressMonitor(ScriptGenerationMonitor aMonitor)
	{
		this.monitor = aMonitor;
	}

	public String getScript()
	{
		if (this.statements.size() == 0)
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
				if (monitor != null) this.monitor.setCurrentObject(ResourceMgr.getString("MsgGeneratingScriptForRow") + " " + (i + 1));
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
	
}
