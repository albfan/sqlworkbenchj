/*
 * LoadWorkspaceAction.java
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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.gui.MainWindow;
import workbench.resource.ResourceMgr;

/**
 *	@author  info@sql-workbench.net
 */
public class LoadWorkspaceAction extends WbAction
{
	private MainWindow client;

	public LoadWorkspaceAction(MainWindow aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtLoadWorkspace", KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		this.setMenuItemName(ResourceMgr.MNU_TXT_WORKSPACE);
		this.setIcon(null);
		
	}

	public void executeAction(ActionEvent e)
	{
		this.client.loadWorkspace();
	}
}
