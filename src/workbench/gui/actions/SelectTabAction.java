/*
 * SelectTabAction.java
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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  support@sql-workbench.net
 */
public class SelectTabAction extends WbAction
{
	private JTabbedPane client;
	private int index;

	public SelectTabAction(JTabbedPane aPane, int anIndex)
	{
		super();
		this.client = aPane;
		this.index = anIndex;
		this.initName();
	}

	private void initName()
	{
		switch (this.index)
		{
			case 0:
				this.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, InputEvent.CTRL_MASK));
				break;
			case 1:
				this.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, InputEvent.CTRL_MASK));
				break;
			case 2:
				this.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_3, InputEvent.CTRL_MASK));
				break;
			case 3:
				this.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_4, InputEvent.CTRL_MASK));
				break;
			case 4:
				this.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_5, InputEvent.CTRL_MASK));
				break;
			case 5:
				this.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_6, InputEvent.CTRL_MASK));
				break;
			case 6:
				this.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_7, InputEvent.CTRL_MASK));
				break;
			case 7:
				this.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_8, InputEvent.CTRL_MASK));
				break;
			case 8:
				this.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_9, InputEvent.CTRL_MASK));
				break;
			default:
				this.setAccelerator(null);
		}
		this.setActionName("SelectTab" + (this.index+1));
		this.setMenuText(ResourceMgr.getDefaultTabLabel());// + " &" + Integer.toString(this.index+1));
		this.setIcon(null);
	}
	public int getIndex() { return this.index; }

	public void setNewIndex(int anIndex)
	{
		this.index = anIndex;
		this.initName();
	}

	public void executeAction(ActionEvent e)
	{
		if (client != null)
		{
			try
			{
				this.client.setSelectedIndex(this.index);
			}
			catch (Exception ex)
			{
				LogMgr.logError("SelectTabAction.executeAction()", "Error when selecting tab " + this.index, ex);
			}
		}
	}
}
