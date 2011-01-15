/*
 * AddTabAction.java
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
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.gui.MainWindow;
import workbench.resource.PlatformShortcuts;
import workbench.resource.ResourceMgr;

/**
 * Action to add a new tab to the MainWindow's interface
 * 
 * @see workbench.gui.MainWindow
 * @see workbench.gui.MainWindow#addTab()
 * 
 * @author  Thomas Kellerer
 */
public class AddTabAction 
	extends WbAction
{
	private MainWindow client;

	public AddTabAction(MainWindow aClient)
	{
		super();
		this.client = aClient;
		this.setMenuItemName(ResourceMgr.MNU_TXT_VIEW);
		this.initMenuDefinition("MnuTxtAddTab", KeyStroke.getKeyStroke(KeyEvent.VK_T, PlatformShortcuts.getDefaultModifier()));
		this.setIcon(null);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.addTab();
	}
}
