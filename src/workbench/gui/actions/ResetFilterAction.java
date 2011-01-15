/*
 * ResetFilterAction.java
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
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import workbench.gui.components.WbTable;
import workbench.resource.ResourceMgr;

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
		this.setIcon("resetFilter");
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
		this.setCreateToolbarSeparator(false);
		this.setEnabled(false);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.resetFilter();
	}

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
