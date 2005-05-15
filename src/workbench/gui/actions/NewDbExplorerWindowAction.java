/*
 * ShowDbExplorerAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
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
public class NewDbExplorerWindowAction
	extends WbAction
{
	private MainWindow mainWin;
	public NewDbExplorerWindowAction(MainWindow aWindow)
	{
		super();
		mainWin = aWindow;
		this.initMenuDefinition("MnuTxtNewExplorerWindow");
	}

	public void executeAction(ActionEvent e)
	{
		// don't do this "now" otherwise the toolbar
		// button is not painted correctly
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				mainWin.newDbExplorerWindow();
			}
		});
	}
}
