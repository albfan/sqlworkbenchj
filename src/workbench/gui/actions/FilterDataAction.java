/*
 * FilterDataAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import workbench.gui.components.WbTable;
import workbench.gui.filter.DefineFilterExpressionPanel;
import workbench.resource.ResourceMgr;

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
