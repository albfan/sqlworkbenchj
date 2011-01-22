/*
 * SelectAllAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.interfaces.ClipboardSupport;
import workbench.resource.PlatformShortcuts;
import workbench.resource.ResourceMgr;

/**
 *	@author  Thomas Kellerer
 */
public class SelectAllAction extends WbAction
{
	private ClipboardSupport client;

	public SelectAllAction(ClipboardSupport aClient)
	{
		super();
		this.client = aClient;
		this.setMenuTextByKey("MnuTxtSelectAll");
		this.setMenuItemName(ResourceMgr.MNU_TXT_EDIT);
		this.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, PlatformShortcuts.getDefaultModifier()));
	}

	public void executeAction(ActionEvent e)
	{
		this.client.selectAll();
	}
}
