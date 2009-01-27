/*
 * ExecuteAllAction.java
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

import workbench.gui.sql.SqlPanel;
import workbench.resource.PlatformShortcuts;
import workbench.resource.ResourceMgr;

/**
 * Run all statements in the current SQL Panel
 * @see workbench.gui.sql.SqlPanel#runAll()
 * @author  support@sql-workbench.net
 */
public class ExecuteAllAction extends WbAction
{
	private SqlPanel client;

	public ExecuteAllAction(SqlPanel aPanel)
	{
		super();
		this.client = aPanel;
		this.initMenuDefinition("MnuTxtExecuteAll", KeyStroke.getKeyStroke(KeyEvent.VK_E, PlatformShortcuts.getDefaultModifier() | InputEvent.SHIFT_MASK));
		this.setIcon("ExecuteAll");
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
		this.setEnabled(false);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.runAll();
	}
}
