/*
 * ColumnAlterAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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
package workbench.gui.actions;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import workbench.interfaces.Reloadable;
import workbench.resource.ResourceMgr;

import workbench.db.ColumnDropper;
import workbench.db.ColumnIdentifier;
import workbench.db.TableColumnsDatastore;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.sqltemplates.ColumnChanger;

import workbench.gui.WbSwingUtilities;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.WbTable;
import workbench.gui.dbobjects.RunScriptPanel;

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
		this.setEnabled(false);
		definition.addTableModelListener(this);
	}

	@Override
	public void dispose()
	{
		if (definition != null)
		{
			definition.removeTableModelListener(this);
		}
		super.dispose();
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

		DataStoreTableModel model = (DataStoreTableModel)e.getSource();
		if (model.getDataStore().isModified())
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
		if (this.definition == null) return;

		this.definition.stopEditing();

		String alterScript = getScript();
		if (alterScript == null)
		{
			String msg = ResourceMgr.getString("MsgNoAlterAvailable");
			WbSwingUtilities.showMessage(definition, msg);
		}

		final RunScriptPanel panel = new RunScriptPanel(dbConnection, alterScript);
		panel.openWindow(definition, ResourceMgr.getString("TxtAlterTable"));

		if (panel.wasRun() && panel.isSuccess() && client != null)
		{
			EventQueue.invokeLater(client::reload);
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

  @Override
  public boolean useInToolbar()
  {
    return false;
  }
}
