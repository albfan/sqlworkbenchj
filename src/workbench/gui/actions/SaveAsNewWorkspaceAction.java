/*
 * SaveAsNewWorkspaceAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;

import workbench.gui.MainWindow;
import workbench.resource.ResourceMgr;

/**
 *	@author  Thomas Kellerer
 */
public class SaveAsNewWorkspaceAction extends WbAction
{
	private MainWindow client;

	public SaveAsNewWorkspaceAction(MainWindow aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtSaveAsNewWorkspace");
		this.setMenuItemName(ResourceMgr.MNU_TXT_FILE);
		this.setIcon(null);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.saveWorkspace(null, false);
	}
	
}
