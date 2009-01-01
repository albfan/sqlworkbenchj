/*
 * ShowDbExplorerAction.java
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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.gui.MainWindow;

/**
 *	@author  support@sql-workbench.net
 */
public class ShowDbExplorerAction
	extends WbAction
{
	private MainWindow mainWin;
	public ShowDbExplorerAction(MainWindow aWindow)
	{
		super();
		mainWin = aWindow;
		this.initMenuDefinition("MnuTxtShowDbExplorer",KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_MASK));
		this.setIcon("Database");
		setEnabled(false);
	}

	public void executeAction(ActionEvent e)
	{
		mainWin.showDbExplorer();
	}
}
