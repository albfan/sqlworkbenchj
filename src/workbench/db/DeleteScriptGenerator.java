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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import workbench.storage.DmlStatement;
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
	private List<DependencyNode> visitedTables = new ArrayList<DependencyNode>();
	private List<DmlStatement> statements;
	private SqlLiteralFormatter formatter;
	
	public DeleteScriptGenerator(WbConnection aConnection)
		throws SQLException
	{
		this.connection = aConnection;
		this.meta = this.connection.getMetadata();
		this.formatter = new SqlLiteralFormatter(this.connection);
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
	
	private void createStatements()
	{
		ArrayList<DependencyNode> parents = new ArrayList<DependencyNode>();
		this.dependency.readDependencyTree(true);
		List<DependencyNode> leafs = this.dependency.getLeafs();
		
		for (DependencyNode node : leafs)
		{
			if (this.visitedTables.contains(node)) continue;
			statements.add(createDeleteStatement(node));
			this.visitedTables.add(node);
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
			if (this.visitedTables.contains(pnode)) continue;
			statements.add(createDeleteStatement(pnode));
			this.visitedTables.add(pnode);
		}

		DependencyNode root = this.dependency.getRootNode();
		StringBuilder rootSql = new StringBuilder(100);
		rootSql.append("DELETE FROM ");
		rootSql.append(root.getTable().getTableExpression(this.connection));
		rootSql.append("\n WHERE ");
		this.addRootTableWhere(rootSql);
		DmlStatement dml = new DmlStatement(rootSql, this.columnValues);
		statements.add(dml);
	}

	private DmlStatement createDeleteStatement(DependencyNode node)
	{
		if (node == null) return null;
		StringBuilder sql = new StringBuilder(200);
		sql.append("DELETE FROM ");
		sql.append(node.getTable().getTableExpression(this.connection));
		sql.append("\n WHERE ");

		this.addParentWhere(sql, node);
		DmlStatement result = new DmlStatement(sql, this.columnValues);
		return result;
	}
	
//	private void addDeleteStatement(StringBuilder sql, DependencyNode node)
//	{
//		if (node == null) return;
//
//		sql.append("DELETE FROM ");
//		sql.append(node.getTable().getTableExpression(this.connection));
//		sql.append("\n WHERE ");
//
//		this.addParentWhere(sql, node);
//		sql.append(';');
//	}

	private void addParentWhere(StringBuilder sql, DependencyNode node)
	{
		try
		{
			DependencyNode parent = node.getParent();
			sql.append(" (");

			Map columns = node.getColumns();
			Iterator itr = columns.entrySet().iterator();
			int count = 0;
			while (itr.hasNext())
			{
				Map.Entry entry = (Map.Entry)itr.next();
				String column = (String)entry.getKey();
				column = this.meta.adjustObjectnameCase(column);
				String parentColumn = (String)entry.getValue();
				//if (nodeColumn != null && !nodeColumn.equals(column)) continue;
				if (count > 0) sql.append("\n          AND ");
				if (!this.rootTable.equals(parent.getTable()))
				{
					sql.append('(');
					sql.append(column);
					sql.append(" IN ( SELECT ");
					sql.append(parentColumn);
					sql.append(" FROM ");
					sql.append(parent.getTable().getTableExpression(this.connection));
					sql.append("\n WHERE ");
					this.addParentWhere(sql, parent);
					sql.append(")) ");
					count ++;
				}
				else
				{
					this.addRootTableWhere(sql, parentColumn, column);
				}

			}
			sql.append(')');
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
				sql.append("\n   AND ");
			}
			else
			{
				first = false;
			}

			sql.append(col.getIdentifier().getColumnName());
			if (col.isNull())
			{
				sql.append(" IS NULL");
			}
			else
			{
				sql.append(" = ?");
			}
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
		
		parentColumn = this.meta.adjustObjectnameCase(parentColumn);

		sql.append(SqlUtil.quoteObjectname(childColumn));
		if (data.isNull())
		{
			sql.append(" IS NULL");
		}
		else
		{
			sql.append(" = ?");
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
		if (this.statements == null)
		{
			this.generateScript();
		}
		StringBuilder script = new StringBuilder();
		
		for (DmlStatement dml : statements)
		{
			script.append(dml.getExecutableStatement(formatter));
			script.append(";\n\n");
		}
		
		try
		{
			int max = Settings.getInstance().getFormatterMaxSubselectLength();
			SqlFormatter format = new SqlFormatter(script, max);
			return format.getFormattedSql().toString();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return script.toString();
		}
	}

	public CharSequence getScriptForValues(List<ColumnData> values)
		throws SQLException
	{
		this.statements = new ArrayList<DmlStatement>(50);
		this.setValues(values);
		this.createStatements();
		return getScript();
	}
	
	public List<DmlStatement> getStatementsForValues(List<ColumnData> values)
	{
		this.statements = new ArrayList<DmlStatement>(50);
		this.setValues(values);
		this.createStatements();
		return Collections.unmodifiableList(statements);
	}
	
	public void generateScript()
	{
		this.statements = new ArrayList<DmlStatement>(50);
		
		if (this.sourceTable == null) return;

		DataStore ds = this.sourceTable.getDataStore();
		if (ds == null) return;

		int[] rows = this.sourceTable.getSelectedRows();
		if (rows.length == 0)
		{
			return;
		}

		ds.checkUpdateTable();
		TableIdentifier tbl = ds.getUpdateTable();
			
		int numRows = rows.length;
		
		try
		{
			this.setTable(tbl);
			
			for (int i=0; i < numRows; i++)
			{
				List<ColumnData> pkvalues = ds.getPkValues(rows[i]);
				this.setValues(pkvalues);
				if (monitor != null) this.monitor.setCurrentObject(ResourceMgr.getString("MsgGeneratingScriptForRow") + " " + (i + 1));
				
				this.createStatements();
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("SqlPanel.generateDeleteScript", "Error generating delete script", e);
		}
	}
	
}
