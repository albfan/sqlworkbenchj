/*
 * CopyAction.java
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
import workbench.interfaces.ClipboardSupport;
import workbench.resource.PlatformShortcuts;
import workbench.resource.ResourceMgr;

/**
 *	Action to copy the contents of an entry field into the clipboard
 * 
 *	@author  Thomas Kellerer
 */
public class CopyAction
	extends WbAction
{
	private ClipboardSupport client;

	public CopyAction(ClipboardSupport aClient)
	{
		super();
		this.client = aClient;
		initMenuDefinition("MnuTxtCopy", PlatformShortcuts.getDefaultCopyShortcut());
		this.setIcon("Copy");
		this.setMenuItemName(ResourceMgr.MNU_TXT_EDIT);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.copy();
	}
}
