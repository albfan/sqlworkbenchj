/*
 * FilterDataAction.java
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

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import workbench.resource.ResourceMgr;

import workbench.gui.components.WbTable;
import workbench.gui.filter.DefineFilterExpressionPanel;

/**
 *	Filter data from a WbTable
 *	@author  Thomas Kellerer
 */
public class FilterDataAction
		extends WbAction
		implements TableModelListener
{
	private WbTable client;

	public FilterDataAction(WbTable aClient)
	{
		super();
		this.setClient(aClient);
		this.initMenuDefinition("MnuTxtFilter");
		this.setIcon("filter");
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
		this.setCreateToolbarSeparator(false);
		this.setEnabled(false);
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		DefineFilterExpressionPanel.showDialog(this.client);
	}

	@Override
	public void tableChanged(TableModelEvent tableModelEvent)
	{
		this.setEnabled(this.client.getLastFilter() != null || this.client.getRowCount() > 0);
	}

	public final void setClient(WbTable c)
	{
		if (this.client != null)
		{
			this.client.removeTableModelListener(this);
		}
		this.client = c;
		if (this.client != null)
		{
			this.client.addTableModelListener(this);
		}
	}

}
