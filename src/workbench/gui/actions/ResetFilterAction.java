/*
 * ResetFilterAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import workbench.resource.ResourceMgr;

import workbench.gui.components.WbTable;

/**
 *	Reset the filter defined on a WbTable
 *	@author  Thomas Kellerer
 */
public class ResetFilterAction
	extends WbAction
	implements TableModelListener
{
	private WbTable client;

	public ResetFilterAction(WbTable aClient)
	{
		super();
		this.initMenuDefinition("MnuTxtResetFilter");
		this.setClient(aClient);
		this.setPngIcon("resetfilter");
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
		this.setCreateToolbarSeparator(false);
		this.setEnabled(false);
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		this.client.resetFilter();
	}

	@Override
	public void tableChanged(TableModelEvent tableModelEvent)
	{
		this.setEnabled(this.client.isFiltered());
	}

	public void setClient(WbTable c)
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
