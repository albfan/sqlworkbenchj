/*
 * InsertTabAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
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
import workbench.resource.ResourceMgr;

/**
 * Insert a new tab in the MainWindow
 *	@author  support@sql-workbench.net
 */
public class InsertTabAction extends WbAction
{
	private MainWindow client;

	public InsertTabAction(MainWindow aClient)
	{
		super();
		this.client = aClient;
		this.setMenuItemName(ResourceMgr.MNU_TXT_VIEW);
		this.initMenuDefinition("MnuTxtInsTab");
		this.setIcon(null);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.insertTab();
	}
}
