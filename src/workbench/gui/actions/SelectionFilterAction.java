/*
 * SelectionFilterAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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

import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import workbench.resource.ResourceMgr;

import workbench.gui.components.WbTable;
import workbench.gui.filter.SelectionFilter;

/**
 *	Filters data from a WbTable based on the currently selected column value.
 *
 *	@author  Thomas Kellerer
 */
public class SelectionFilterAction
		extends WbAction
		implements ListSelectionListener
{
	private WbTable client;

	public SelectionFilterAction()
	{
		super();
		this.initMenuDefinition("MnuTxtColFilter");
		this.setIcon("colfilter");
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
		this.setCreateToolbarSeparator(false);
		this.setEnabled(false);
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		SelectionFilter filter = new SelectionFilter(this.client);
		filter.applyFilter();
	}

	public void setClient(WbTable c)
	{
		if (this.client != null)
		{
			ListSelectionModel m = this.client.getSelectionModel();
			if (m != null)
			{
				m.removeListSelectionListener(this);
			}
		}
		this.client = c;
		this.setEnabled(client != null);
		if (this.client != null)
		{
			ListSelectionModel m = this.client.getSelectionModel();
			if (m != null)
			{
				m.addListSelectionListener(this);
			}
		}
		checkEnabled();
	}

	private void checkEnabled()
	{
		if (client == null)
		{
			this.setEnabled(false);
		}
		else
		{
			int rows = client.getSelectedRowCount();
			int cols = client.getSelectedColumnCount();

			this.setEnabled(rows == 1 || (rows > 1 && cols == 1));
		}
	}

	@Override
	public void valueChanged(ListSelectionEvent e)
	{
		checkEnabled();
	}

}
