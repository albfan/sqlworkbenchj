/*
 * ColumnAlterAction
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.actions;

import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import workbench.db.ColumnChanger;
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

	public void setReloadableClient(Reloadable reload)
	{
		client = reload;
	}
	
	@Override
	public void tableChanged(TableModelEvent e)
	{
		if (!isTable())
		{
			setEnabled(false);
			return;
		}

		if (e.getType() == TableModelEvent.UPDATE)
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
		return this.sourceTable.getType().equalsIgnoreCase("TABLE");
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

		RunScriptPanel panel = new RunScriptPanel(dbConnection, alterScript);
		panel.openWindow(parent, ResourceMgr.getString("TxtAlterTable"));

		if (panel.isSuccess() && client != null)
		{
			client.reload();
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
		if (dbConnection.getDbSettings().ddlNeedsCommit())
		{
			result.append("\nCOMMIT;\n");
		}
		return result.toString();
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
		col.setIsNullable(StringUtil.stringToBool(nullable));
		col.setComment(comment);
		return col;
	}

	private ColumnIdentifier getOldDefintion(int row)
	{
		DataStore ds = definition.getDataStore();
		String name = (String)ds.getOriginalValue(row, TableColumnsDatastore.COLUMN_IDX_TABLE_DEFINITION_COL_NAME);
		String type = (String)ds.getOriginalValue(row, TableColumnsDatastore.COLUMN_IDX_TABLE_DEFINITION_DATA_TYPE);
		String defaultValue = (String)ds.getOriginalValue(row, TableColumnsDatastore.COLUMN_IDX_TABLE_DEFINITION_DEFAULT);
		String nullable  = (String)ds.getOriginalValue(row, TableColumnsDatastore.COLUMN_IDX_TABLE_DEFINITION_NULLABLE);
		String comment = (String)ds.getOriginalValue(row, TableColumnsDatastore.COLUMN_IDX_TABLE_DEFINITION_REMARKS);
		ColumnIdentifier col = new ColumnIdentifier(name);
		col.setDbmsType(type);
		col.setDefaultValue(defaultValue);
		col.setIsNullable(StringUtil.stringToBool(nullable));
		col.setComment(comment);
		return col;
	}
}
