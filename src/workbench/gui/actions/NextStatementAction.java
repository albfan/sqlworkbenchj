/*
 * NextStatementAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
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
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  info@sql-workbench.net
 */
public class NextStatementAction extends WbAction
{
	private SqlPanel panel;
	public NextStatementAction(SqlPanel aPanel)
	{
		super();
		this.panel = aPanel;
		this.initMenuDefinition("MnuTxtNextStatement", KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.ALT_MASK));
		this.setIcon(ResourceMgr.getImage("Forward"));
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
		this.setCreateMenuSeparator(false);
		this.setCreateToolbarSeparator(false);
		//this.putValue(ALTERNATE_ACCELERATOR, KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK));
	}

	public void executeAction(ActionEvent e)
	{
		this.panel.showNextStatement();
	}
}
