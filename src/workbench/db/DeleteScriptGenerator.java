/*
 * DeleteScriptGenerator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
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
import workbench.storage.DataStore;
import workbench.util.SqlUtil;

/**
 *	Generates a SQL script to delete a record from the given table and
 *	any dependent tables.
 * @author  support@sql-workbench.net
 */
public class DeleteScriptGenerator
	implements Scripter
{
	private WbConnection connection;
	private Map columnValues;
	private TableDependency dependency;
	private DbMetadata meta;
	private DataStore tableDefinition;
	private TableIdentifier rootTable = null;
	private WbTable sourceTable = null;
	private ScriptGenerationMonitor monitor;
	private List visitedTables = new ArrayList();

	public DeleteScriptGenerator(WbConnection aConnection)
		throws SQLException
	{
		this.connection = aConnection;
		this.meta = this.connection.getMetadata();
		this.dependency = new TableDependency();
		this.dependency.setConnection(this.connection);
	}

	public void setSource(WbTable aTable)
	{
		this.sourceTable = aTable;
	}

	public void setTable(String aCatalog, String aSchema, String aTable)
		throws SQLException
	{
		if (aTable == null || aTable.trim().length() == 0) throw new IllegalArgumentException("The table name may not be empty");
		aTable = this.meta.adjustObjectnameCase(aTable);
		aCatalog = this.meta.adjustObjectnameCase(aCatalog);

		if (aSchema == null)
		{
			try
			{
				aSchema = this.meta.getSchemaForTable(aTable);
			}
			catch (Exception e)
			{
				aSchema = null;
			}
		}
		aSchema = this.meta.adjustObjectnameCase(aSchema);
		this.rootTable = new TableIdentifier(aCatalog, aSchema, aTable);

		this.dependency.setTable(this.rootTable);
		this.tableDefinition = this.meta.getTableDefinition(this.rootTable);
	}

	public void setValues(Map colValues)
	{
		this.columnValues = colValues;
	}

	public String createScript()
	{
		ArrayList parents = new ArrayList();
		this.dependency.readDependencyTree(true);
		List leafs = this.dependency.getLeafs();
		StringBuffer sql = new StringBuffer(2000);
		DependencyNode p, node;
		for (int i=0; i < leafs.size(); i++)
		{
			node = (DependencyNode)leafs.get(i);
			if (this.visitedTables.contains(node)) continue;
			this.addDeleteStatement(sql, node);
			this.visitedTables.add(node);
			p = node.getParent();
			while (p != null)
			{
				if (!isMasterTable(p) && !parents.contains(p) && !leafs.contains(p))
				{
					parents.add(p);
				}
				p = p.getParent();
			}
			sql.append("\n\n");
		}

		for (int i=0; i < parents.size(); i++)
		{
			p = (DependencyNode)parents.get(i);
			if (this.visitedTables.contains(p)) continue;
			this.addDeleteStatement(sql, p);
			this.visitedTables.add(p);
			sql.append("\n");
		}

		DependencyNode root = this.dependency.getRootNode();
		sql.append("DELETE FROM ");
		sql.append(root.getTableId().getTableExpression());
		sql.append("\n WHERE ");
		this.addRootTableWhere(sql);
		sql.append(';');
		try
		{
			int max = Settings.getInstance().getMaxSubselectLength();
			SqlFormatter format = new SqlFormatter(sql.toString(), max);
			return format.getFormattedSql().trim();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return sql.toString();
		}
	}

	private void addDeleteStatement(StringBuffer sql, DependencyNode node)
	{
		if (node == null) return;

		sql.append("DELETE FROM ");
		sql.append(node.getTableId().getTableExpression());
		sql.append("\n WHERE ");

		this.addParentWhere(sql, node);
		sql.append(';');
	}

	private void addParentWhere(StringBuffer sql, DependencyNode node)
	{
		this.addParentWhere(sql, node, null);
	}
	private void addParentWhere(StringBuffer sql, DependencyNode node, String nodeColumn)
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
				if (!this.rootTable.equals(parent.getTableId()))
				{
					sql.append("(");
					sql.append(column);
					sql.append(" IN ( SELECT ");
					sql.append(parentColumn);
					sql.append(" FROM ");
					sql.append(parent.getTableId().getTableExpression());
					sql.append("\n WHERE ");
					this.addParentWhere(sql, parent, parentColumn);
					sql.append(")) ");
					count ++;
				}
				else
				{
					this.addRootTableWhere(sql, parentColumn, column);
				}

			}
			sql.append(")");
		}
		catch (Throwable th)
		{
			LogMgr.logError("DeleteScriptGenerator.addParentWhere()", "Error during script generation", th);
		}
	}

	private boolean isMasterTable(DependencyNode node)
	{
		TableIdentifier table = node.getTableId();
		return (this.rootTable.equals(table));
	}

	private void addRootTableWhere(StringBuffer sql)
	{
		Iterator itr = this.columnValues.entrySet().iterator();
		boolean first = true;
		while (itr.hasNext())
		{
			Map.Entry entry = (Map.Entry)itr.next();
			String column = (String)entry.getKey();
			column = this.meta.adjustObjectnameCase(column);
			Object data = entry.getValue();
			int type = this.getColumnType(tableDefinition, column);
			if (!first)
			{
				sql.append("\n   AND ");
			}
			else
			{
				first = false;
			}

			sql.append(SqlUtil.quoteObjectname(column));
			if (data == null)
			{
				sql.append(" IS NULL");
			}
			else
			{
	      String value = data.toString();
				sql.append(" = ");
				boolean charType = (type == Types.VARCHAR || type == Types.CHAR);
				if (charType)	sql.append('\'');
				sql.append(value);
				if (charType)	sql.append('\'');
			}
		}
	}

	private void addRootTableWhere(StringBuffer sql, String parentColumn, String childColumn)
	{
		Object data = this.columnValues.get(parentColumn);
		parentColumn = this.meta.adjustObjectnameCase(parentColumn);

		int type = this.getColumnType(tableDefinition, parentColumn);
		sql.append(SqlUtil.quoteObjectname(childColumn));
		if (data == null)
		{
			sql.append(" IS NULL");
		}
		else
		{
	     String value = data.toString();
			sql.append(" = ");
			boolean charType = (type == Types.VARCHAR || type == Types.CHAR);
			if (charType)	sql.append('\'');
			sql.append(value);
			if (charType)	sql.append('\'');
		}
	}

	private int getColumnType(DataStore tableDef, String aColname)
	{
		for (int i=0; i < tableDef.getRowCount(); i ++)
		{
			String col = tableDef.getValueAsString(i, DbMetadata.COLUMN_IDX_TABLE_DEFINITION_COL_NAME);
			if (aColname.equals(col))
			{
				String t = tableDef.getValueAsString(i, DbMetadata.COLUMN_IDX_TABLE_DEFINITION_JAVA_SQL_TYPE);
				int type = 0;
				try { type = Integer.parseInt(t); } catch (Exception e) {}
				return type;
			}
		}
		return -1;
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
		if (this.sourceTable == null) return "";

		DataStore ds = this.sourceTable.getDataStore();
		if (ds == null) return "";

		int[] rows = this.sourceTable.getSelectedRows();
		if (rows.length == 0)
		{
			return "";
		}

		ds.checkUpdateTable();
		String updatetable = ds.getUpdateTable();
		String schema = ds.getUpdateTableSchema();

		int numRows = rows.length;
		StringBuffer script = new StringBuffer(numRows * 150);
		int max = Settings.getInstance().getMaxSubselectLength();
		StringBuffer sep = new StringBuffer(max + 2);
		sep.append('\n');
		for (int i=0; i < max; i++) sep.append('=');
		sep.append('\n');

		try
		{
			for (int i=0; i < numRows; i++)
			{
				Map pkvalues = ds.getPkValues(rows[i]);
				this.setTable(null, schema, updatetable);
				this.setValues(pkvalues);
				this.monitor.setCurrentObject(ResourceMgr.getString("MsgGeneratingScriptForRow") + " " + i);
				String rowScript = this.createScript();
				if (i > 0) script.append(sep);
				script.append(rowScript);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("SqlPanel.generateDeleteScript", "Error generating delete script", e);
		}
		return script.toString();
	}
}
