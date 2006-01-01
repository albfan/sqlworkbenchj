/*
 * FileNewWindowAction.java
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

import java.awt.event.ActionEvent;

import workbench.WbManager;

/**
 *	@author  support@sql-workbench.net
 */
public class FileNewWindowAction extends WbAction
{
	public FileNewWindowAction()
	{
		super();
		this.initMenuDefinition("MnuTxtFileNewWindow");
		//this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK));
	}

	public void executeAction(ActionEvent e)
	{
		WbManager.getInstance().openNewWindow();
	}
}
