/*
 * UpdateDatabaseAction.java
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

import workbench.interfaces.DbUpdater;
import workbench.resource.ResourceMgr;

/**
 *	@author  Thomas Kellerer
 */
public class UpdateDatabaseAction
	extends WbAction
{
	private DbUpdater panel;
	
	public UpdateDatabaseAction(DbUpdater aPanel)
	{
		super();
		this.panel = aPanel;
		this.initMenuDefinition("MnuTxtUpdateDatabase");
		this.setIcon(ResourceMgr.IMG_SAVE);
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
		this.setCreateToolbarSeparator(true);
		this.setEnabled(false);
	}

	public void executeAction(ActionEvent e)
	{
		panel.saveChangesToDatabase();
	}

	public void setClient(DbUpdater client)
	{
		this.panel = client;
	}
	
}
