/*
 * ToggleTableSourceAction.java
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

import workbench.gui.dbobjects.TableListPanel;

/**
 *	@author  support@sql-workbench.net
 */
public class ToggleTableSourceAction extends WbAction
{
	private TableListPanel client;

	public ToggleTableSourceAction(TableListPanel aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtToggleTableSource", KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		this.setIcon(null);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.toggleExpandSource();
	}
}
