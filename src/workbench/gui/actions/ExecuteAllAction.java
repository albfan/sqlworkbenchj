/*
 * ExecuteAllAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
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

import workbench.gui.sql.SqlPanel;
import workbench.resource.ResourceMgr;

/**
 *	Action to paste the contents of the clipboard into the entry field
 *	@author  support@sql-workbench.net
 */
public class ExecuteAllAction extends WbAction
{
	private SqlPanel client;

	public ExecuteAllAction(SqlPanel aPanel)
	{
		super();
		this.client = aPanel;
		this.initMenuDefinition("MnuTxtExecuteAll", KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		this.setIcon(ResourceMgr.getImage("ExecuteAll"));
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.runAll();
	}
}
