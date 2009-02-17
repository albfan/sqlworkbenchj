/*
 * RollbackAction.java
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

import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;


/**
 *	Select the next tab from a tabbed pane
 *	@author  support@sql-workbench.net
 */
public class NextTabAction
	extends WbAction
{
	private JTabbedPane client;

	public NextTabAction(JTabbedPane aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtRollback", KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.CTRL_MASK));
		this.removeIcon();
		this.setEnabled(false);
	}

	@Override
	public boolean isEnabled()
	{
		int index = client.getSelectedIndex();
		int count = client.getTabCount();
		return (index + 1 < count);
	}


	public void executeAction(ActionEvent e)
	{
		int newIndex = client.getSelectedIndex() + 1;
		if (newIndex < client.getTabCount())
		{
			System.out.println("next tab!");
			client.setSelectedIndex(newIndex);
		}
	}

}
