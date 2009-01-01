/*
 * RemoveTabAction.java
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
import workbench.resource.ResourceMgr;

/**
 *	@author  support@sql-workbench.net
 */
public class RemoveTabAction 
	extends WbAction
{
	private MainWindow client;

	public RemoveTabAction(MainWindow aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtRemoveTab", KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.CTRL_MASK));
		this.setMenuItemName(ResourceMgr.MNU_TXT_VIEW);
		this.setIcon(null);
	}

	public void executeAction(ActionEvent e)
	{
		client.removeTab();
	}
}
