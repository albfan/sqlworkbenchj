/*
 * FilterDataAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
 *	@author  support@sql-workbench.net
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
		this.setIcon(ResourceMgr.getImage("filter"));
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
		this.setCreateToolbarSeparator(false);
		this.setEnabled(false);		
	}

	public void executeAction(ActionEvent e)
	{
		DefineFilterExpressionPanel.showDialog(this.client);
	}

	public void tableChanged(TableModelEvent tableModelEvent)
	{
		this.setEnabled(this.client.getLastFilter() != null || this.client.getRowCount() > 0);
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
