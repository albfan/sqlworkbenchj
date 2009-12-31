/*
 * NewDbExplorerPanelAction.java
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

/**
 *	@author  Thomas Kellerer
 */
public class NewDbExplorerPanelAction
	extends WbAction
{
	private MainWindow mainWin;

	public NewDbExplorerPanelAction(MainWindow aWindow)
	{
		this(aWindow, "MnuTxtNewExplorerPanel");
		setEnabled(false);
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
