/*
 * ChangeDatabaseAction.java
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

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.KeyStroke;

import workbench.resource.ResourceMgr;

/**
 *	@author  support@sql-workbench.net
 */
public class ChangeDatabaseAction extends WbAction
{
	public ChangeDatabaseAction(String aCatalogName)
	{
		super();
		String name = ResourceMgr.getString("MnuTxtChangeDatabase");
		this.putValue(Action.NAME, name);
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_SQL);
		this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_MASK));
	}

	public void executeAction(ActionEvent e)
	{
	}
}
