/*
 * FindAction.java
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
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.interfaces.Searchable;
import workbench.resource.PlatformShortcuts;
import workbench.resource.ResourceMgr;

/**
 *	@author  support@sql-workbench.net
 */
public class FindAction extends WbAction
{
	private Searchable client;

	public FindAction(Searchable aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtFind", KeyStroke.getKeyStroke(KeyEvent.VK_F, PlatformShortcuts.getDefaultModifier()));
		this.setIcon("Find");
		this.setMenuItemName(ResourceMgr.MNU_TXT_EDIT);
		this.setCreateToolbarSeparator(true);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.find();
	}
}
