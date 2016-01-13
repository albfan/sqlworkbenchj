/*
 * RunMacroAction.java
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
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.Map;

import javax.swing.Action;
import javax.swing.KeyStroke;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import workbench.resource.ResourceMgr;
import workbench.resource.StoreableKeyStroke;

import workbench.gui.MainWindow;
import workbench.gui.components.WbTable;
import workbench.gui.editor.MacroExpander;
import workbench.gui.macros.MacroRunner;
import workbench.gui.sql.SqlPanel;

import workbench.storage.DataStore;
import workbench.storage.RowData;

import workbench.sql.macros.MacroDefinition;

import workbench.util.NumberStringCache;
import workbench.util.StringUtil;

/**
 *	@author  Thomas Kellerer
 */
public class RunMacroAction
	extends WbAction
	implements ListSelectionListener
{
	private MainWindow client;
	private MacroDefinition macro;
	private WbTable dataTable;
	private Map<String, String> columnMap;

	public RunMacroAction(MainWindow aClient, MacroDefinition def, int index)
	{
		super();
		this.macro = def;
		this.client = aClient;

		if (def == null)
		{
			String title = ResourceMgr.getPlainString("LblRunMacro");
			setMenuText(title);
			String desc = ResourceMgr.getDescription("MnuTxtRunMacro", true);
			desc = desc.replaceAll("[ ]*(%macro%)[ ]*", " ");
			this.putValue(Action.SHORT_DESCRIPTION, desc);
		}
		else
		{
			String menuTitle = def.getName();
			if (index < 10 && index > 0)
			{
				menuTitle = "&" + NumberStringCache.getNumberString(index) + " - " + def.getName();
			}
			setMenuText(menuTitle);
      initTooltip();

			StoreableKeyStroke key = macro.getShortcut();
			if (key != null)
			{
				KeyStroke stroke = key.getKeyStroke();
				setAccelerator(stroke);
			}
		}
		this.setMenuItemName(ResourceMgr.MNU_TXT_MACRO);
		this.setIcon(null);
		setEnabled(macro != null && client != null);
	}

	public void setDataTable(WbTable table, Map<String, String> colMap)
	{
		this.dataTable = table;
		this.columnMap = colMap;
		if (columnMap == null)
		{
			columnMap = Collections.emptyMap();
		}
		if (dataTable != null)
		{
			dataTable.getSelectionModel().addListSelectionListener(this);
			setEnabled(dataTable.getSelectedRowCount() == 1);
		}
	}

	@Override
	public void dispose()
	{
		super.dispose();
		if (dataTable != null)
		{
			dataTable.getSelectionModel().removeListSelectionListener(this);
		}
	}

  private void initTooltip()
  {
    if (macro == null) return;
    String desc = macro.getTooltip();
    if (desc == null)
    {
      desc = ResourceMgr.getDescription("MnuTxtRunMacro", true);
      desc = StringUtil.replace(desc, "%macro%", "'" + macro.getName() + "'");
    }
    putValue(Action.SHORT_DESCRIPTION, desc);
  }

	public void setMacro(MacroDefinition def)
	{
		this.macro = def;
    initTooltip();
	}

	private void executeStandardMacro(ActionEvent e)
	{
		SqlPanel sql = this.client.getCurrentSqlPanel();
		if (sql == null) return;

		if (macro.getExpandWhileTyping())
		{
			MacroExpander expander = sql.getEditor().getMacroExpander();
			if (expander != null)
			{
				expander.insertMacroText(macro.getText());
				sql.selectEditorLater();
			}
		}
		else
		{
			boolean shiftPressed = isShiftPressed(e) && invokedByMouse(e);
			MacroRunner runner = new MacroRunner();
			runner.runMacro(macro, sql, shiftPressed);
		}
	}

	private void executeDataMacro()
	{
		if (dataTable == null) return;

		SqlPanel sql = this.client.getCurrentSqlPanel();
		if (sql == null) return;

		int row = dataTable.getSelectedRow();
		if (row < 0) return;

		DataStore ds = dataTable.getDataStore();
		if (ds == null) return;

		RowData rowData = ds.getRow(row);
		if (rowData == null) return;

		MacroRunner runner = new MacroRunner();
		runner.runDataMacro(macro, ds.getResultInfo(), rowData, sql, columnMap);
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		if (this.client == null || this.macro == null) return;
    
		if (this.dataTable != null)
		{
			executeDataMacro();
		}
		else
		{
			executeStandardMacro(e);
		}
	}

	@Override
	public void valueChanged(ListSelectionEvent e)
	{
		if (e.getValueIsAdjusting()) return;

		if (dataTable == null)
		{
			setEnabled(false);
			return;
		}
		setEnabled(dataTable.getSelectedRowCount() == 1);
	}

}
