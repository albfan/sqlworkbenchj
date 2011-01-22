/*
 * CloseWorkspaceAction.java
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

import workbench.gui.MainWindow;
import workbench.resource.ResourceMgr;

/**
 * Action to close the current workspace
 * @see workbench.gui.MainWindow#closeWorkspace(boolean) 
 * @author  Thomas Kellerer
 */
public class CloseWorkspaceAction extends WbAction
{
	private MainWindow client;

	public CloseWorkspaceAction(MainWindow aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtCloseWorkspace", null);
		this.setMenuItemName(ResourceMgr.MNU_TXT_WORKSPACE);
		this.setIcon(null);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.closeWorkspace(true);
	}
}
