/*
 * FindDataAgainAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright Thomas Kellerer
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

import workbench.interfaces.Searchable;
import workbench.resource.ResourceMgr;


/**
 *	Search the next occurance in the result set. 
 *	@author  Thomas Kellerer
 */
public class FindDataAgainAction extends WbAction
{
	private Searchable client;

	public FindDataAgainAction(Searchable aClient)
	{
		super();
		this.client = aClient;
		
		this.initMenuDefinition("MnuTxtFindInTableDataAgain", KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.SHIFT_MASK));
		this.setIcon("FindAgain");
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.findNext();
	}
}
