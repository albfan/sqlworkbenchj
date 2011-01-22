/*
 * AssignWorkspaceAction.java
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
 * Action to assign the currently loaded workspace to the current connection profile
 * @see workbench.gui.MainWindow#assignWorkspace()
 * @see workbench.db.ConnectionProfile
 * @see workbench.util.WbWorkspace
 * 
 * @author  Thomas Kellerer
 */
public class AssignWorkspaceAction 
	extends WbAction
{
	private MainWindow client;

	public AssignWorkspaceAction(MainWindow aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtAssignWorkspace", null);
		this.setMenuItemName(ResourceMgr.MNU_TXT_WORKSPACE);
		this.setIcon(null);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.assignWorkspace();
	}
	
}
