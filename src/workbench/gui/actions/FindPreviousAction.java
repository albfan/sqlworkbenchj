/*
 * FindPreviousAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.interfaces.Searchable;
import workbench.resource.PlatformShortcuts;
import workbench.resource.ResourceMgr;

/**
 *	@author Thomas Kellerer
 */
public class FindPreviousAction
	extends WbAction
{
	private Searchable client;

	public FindPreviousAction(Searchable aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtFindPrevious", KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.SHIFT_MASK));
		this.setMenuItemName(ResourceMgr.MNU_TXT_EDIT);
		this.setDescriptiveName(ResourceMgr.getString("TxtEdPrefix") + " " + getMenuLabel());
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		this.client.findPrevious();
	}
}
