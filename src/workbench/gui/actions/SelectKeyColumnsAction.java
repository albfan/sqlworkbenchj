/*
 * SelectKeyColumnsAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;

import workbench.gui.sql.DwPanel;
import workbench.resource.ResourceMgr;

/**
 *	@author  support@sql-workbench.net
 */
public class SelectKeyColumnsAction
	extends WbAction
{
	private DwPanel client;

	public SelectKeyColumnsAction(DwPanel aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtSelectKeyColumns");
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
		this.setIcon("key");
		this.setEnabled(false);
	}

	public void executeAction(ActionEvent e)
	{
		if (this.client != null)
		{
			this.client.checkAndSelectKeyColumns();
		}
	}

}
