/*
 * RenameTabAction.java
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

import workbench.WbManager;
import workbench.gui.WbSwingUtilities;
import workbench.gui.sql.RenameableTab;
import workbench.resource.ResourceMgr;

/**
 *	@author  support@sql-workbench.net
 */
public class RenameTabAction
	extends WbAction
{
	private RenameableTab client;

	public RenameTabAction(RenameableTab aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtRenameTab");
		this.setMenuItemName(ResourceMgr.MNU_TXT_VIEW);
		this.setIcon(null);
	}

	public void executeAction(ActionEvent e)
	{
		String oldName = client.getCurrentTabTitle();
		String newName = WbSwingUtilities.getUserInput(WbManager.getInstance().getCurrentWindow(),
			ResourceMgr.getString("MsgEnterNewTabName"), oldName);
		
		if (newName != null)
		{
			client.setCurrentTabTitle(newName);
		}
	}
}
