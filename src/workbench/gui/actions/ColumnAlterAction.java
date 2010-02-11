/*
 * ColumnAlterAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import workbench.db.ColumnChanger;
import workbench.db.ColumnDropper;
import workbench.db.ColumnIdentifier;
import workbench.db.TableColumnsDatastore;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.WbTable;
import workbench.gui.dbobjects.RunScriptPanel;
import workbench.interfaces.Reloadable;
import workbench.resource.ResourceMgr;
import workbench.storage.DataStore;
import workbench.storage.RowData;
import workbench.util.CollectionUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class ColumnAlterAction
	extends WbAction
	implements TableModelListener
{
	private WbTable definition;
	private TableIdentifier sourceTable;
	private WbConnection dbConnection;
	private Reloadable client;

	public ColumnAlterAction(WbTable defTable)
	{
		super();
		this.definition = defTable;
		this.initMenuDefinition("MnuTxtRunAlter");
		this.setIcon("runAlter");
		this.setEnabled(false);
		definition.addTableModelListener(this);
	}

	public void setReloader(Reloadable reload)
	{
		client = reload;
	}

	@Override
	public void tableChanged(TableModelEvent e)
	{
		if (definition.isReadOnly()) return;

		if (!isTable())
		{
			setEnabled(false);
			return;
		}

		if (e.getType() == TableModelEvent.UPDATE || e.getType() == TableModelEvent.DELETE)
		{
			setEnabled(sourceTable != null);
		}
	}

	public void setSourceTable(WbConnection con, TableIdentifier tbl)
	{
		dbConnection = con;
		sourceTable = tbl;
	}

	private boolean isTable()
	{
		if (dbConnection == null) return false;
		if (sourceTable == null) return false;
		return dbConnection.getDbSettings().columnCommentAllowed(this.sourceTable.getType());
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		Window w = SwingUtilities.getWindowAncestor(definition);
		Frame parent = null;
		if (w instanceof Frame)
		{
			parent = (Frame)w;
		}
		String alterScript = getScript();
		if (alterScript == null)
		{
			String msg = ResourceMgr.getString("MsgNoAlterAvailable");
			WbSwingUtilities.showMessage(parent, msg);
		}

		final RunScriptPanel panel = new RunScriptPanel(dbConnection, alterScript);
		panel.openWindow(parent, ResourceMgr.getString("TxtAlterTable"));

		if (panel.wasRun() && panel.isSuccess() && client != null)
		{
			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					client.reload();
				}
			});
		}
	}

	private String getScript()
	{
		DataStore ds = definition.getDataStore();
		ColumnChanger changer = new ColumnChanger(dbConnection);
		StringBuilder result = new StringBuilder(200);

		for (int row = 0; row < ds.getRowCount(); row ++)
		{
			ColumnIdentifier oldCol = getOldDefintion(row);
			ColumnIdentifier newCol = getCurrentDefinition(row);

			String script = changer.getAlterScript(sourceTable, oldCol, newCol);
			if (script != null)
			{
				result.append(script);
			}
		}
		int count = ds.getDeletedRowCount();
		if (count > 0)
		{
			List<ColumnIdentifier> deleted = CollectionUtil.arrayList();
			for (int row = 0;  row < count; row ++)
			{
				deleted.add(getDeletedColumn(row));
			}
			List<String> statements = ColumnDropper.getSql(sourceTable, deleted, dbConnection);
			for (String sql : statements)
			{
				result.append(sql);
				if (!sql.endsWith(";"))
				{
					result.append(';');
				}
				result.append('\n');
			}
		}
		if (dbConnection.getDbSettings().ddlNeedsCommit())
		{
			result.append("\nCOMMIT;\n");
		}
		return result.toString();
	}

	private ColumnIdentifier getDeletedColumn(int row)
	{
		if (definition == null) return null;
		DataStore ds = definition.getDataStore();

		String name = (String)ds.getDeletedValue(row, TableColumnsDatastore.COLUMN_IDX_TABLE_DEFINITION_COL_NAME);
		String type = (String)ds.getDeletedValue(row, TableColumnsDatastore.COLUMN_IDX_TABLE_DEFINITION_DATA_TYPE);
		String defaultValue = (String)ds.getDeletedValue(row, TableColumnsDatastore.COLUMN_IDX_TABLE_DEFINITION_DEFAULT);
		String nullable = (String)ds.getDeletedValue(row, TableColumnsDatastore.COLUMN_IDX_TABLE_DEFINITION_NULLABLE);
		String comment = (String)ds.getDeletedValue(row, TableColumnsDatastore.COLUMN_IDX_TABLE_DEFINITION_REMARKS);
		ColumnIdentifier col = new ColumnIdentifier(name);
		col.setDbmsType(type);
		col.setDefaultValue(defaultValue);
		if (StringUtil.isNonBlank(nullable))
		{
			col.setIsNullable(StringUtil.stringToBool(nullable));
		}
		col.setComment(comment);
		return col;
	}

	private ColumnIdentifier getCurrentDefinition(int row)
	{
		if (definition == null) return null;
		DataStore ds = definition.getDataStore();

		String name = ds.getValueAsString(row, TableColumnsDatastore.COLUMN_IDX_TABLE_DEFINITION_COL_NAME);
		String type = ds.getValueAsString(row, TableColumnsDatastore.COLUMN_IDX_TABLE_DEFINITION_DATA_TYPE);
		String defaultValue = ds.getValueAsString(row, TableColumnsDatastore.COLUMN_IDX_TABLE_DEFINITION_DEFAULT);
		String nullable = ds.getValueAsString(row, TableColumnsDatastore.COLUMN_IDX_TABLE_DEFINITION_NULLABLE);
		String comment = ds.getValueAsString(row, TableColumnsDatastore.COLUMN_IDX_TABLE_DEFINITION_REMARKS);
		ColumnIdentifier col = new ColumnIdentifier(name);
		col.setDbmsType(type);
		col.setDefaultValue(defaultValue);
		if (StringUtil.isNonBlank(nullable))
		{
			col.setIsNullable(StringUtil.stringToBool(nullable));
		}
		col.setComment(comment);
		return col;
	}

	private ColumnIdentifier getOldDefintion(int row)
	{
		DataStore ds = definition.getDataStore();
		if (ds.getRowStatus(row) == RowData.NEW) return null;
		String name = (String)ds.getOriginalValue(row, TableColumnsDatastore.COLUMN_IDX_TABLE_DEFINITION_COL_NAME);
		String type = (String)ds.getOriginalValue(row, TableColumnsDatastore.COLUMN_IDX_TABLE_DEFINITION_DATA_TYPE);
		String defaultValue = (String)ds.getOriginalValue(row, TableColumnsDatastore.COLUMN_IDX_TABLE_DEFINITION_DEFAULT);
		String nullable  = (String)ds.getOriginalValue(row, TableColumnsDatastore.COLUMN_IDX_TABLE_DEFINITION_NULLABLE);
		String comment = (String)ds.getOriginalValue(row, TableColumnsDatastore.COLUMN_IDX_TABLE_DEFINITION_REMARKS);
		ColumnIdentifier col = new ColumnIdentifier(name);
		col.setDbmsType(type);
		col.setDefaultValue(defaultValue);
		if (StringUtil.isNonBlank(nullable))
		{
			col.setIsNullable(StringUtil.stringToBool(nullable));
		}
		col.setComment(comment);
		return col;
	}
}
