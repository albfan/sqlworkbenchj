/*
 * SelectionFilterAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import workbench.gui.components.WbTable;
import workbench.resource.ResourceMgr;
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

	public void valueChanged(ListSelectionEvent e)
	{
		checkEnabled();
	}

}
