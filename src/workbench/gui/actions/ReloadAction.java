/*
 * ReloadAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;

import workbench.interfaces.Reloadable;
import workbench.resource.ResourceMgr;

/**
 *	@author  info@sql-workbench.net
 */
public class ReloadAction extends WbAction
{
	private Reloadable client;

	public ReloadAction(Reloadable aClient)
	{
		super();
		this.client = aClient;
		this.setMenuTextByKey("TxtReload");
		this.setIcon(ResourceMgr.getImage("Refresh"));
		//this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.reload();
	}
	
}
