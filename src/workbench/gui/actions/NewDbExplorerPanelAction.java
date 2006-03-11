/*
 * NewDbExplorerPanelAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.gui.MainWindow;
import workbench.resource.ResourceMgr;

/**
 *	@author  support@sql-workbench.net
 */
public class NewDbExplorerPanelAction
	extends WbAction
{
	private MainWindow mainWin;

	public NewDbExplorerPanelAction(MainWindow aWindow)
	{
		this(aWindow, "MnuTxtNewExplorerPanel");
	}
	public NewDbExplorerPanelAction(MainWindow aWindow, String key)
	{
		super();
		mainWin = aWindow;
		this.initMenuDefinition(key);
	}

	public void executeAction(ActionEvent e)
	{
		mainWin.newDbExplorerPanel(true);
	}
}
