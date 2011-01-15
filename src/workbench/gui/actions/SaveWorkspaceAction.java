/*
 * SaveWorkspaceAction.java
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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.gui.MainWindow;
import workbench.resource.PlatformShortcuts;
import workbench.resource.ResourceMgr;

/**
 *	@author  Thomas Kellerer
 */
public class SaveWorkspaceAction extends WbAction
{
	private MainWindow client;

	public SaveWorkspaceAction(MainWindow aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtSaveWorkspace",KeyStroke.getKeyStroke(KeyEvent.VK_S, PlatformShortcuts.getDefaultModifier() | InputEvent.SHIFT_MASK));
		this.setMenuItemName(ResourceMgr.MNU_TXT_FILE);
		this.setIcon(null);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.saveWorkspace(this.client.getCurrentWorkspaceFile(), false);
	}
	
}
