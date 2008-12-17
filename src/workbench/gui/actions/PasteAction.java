/*
 * PasteAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
 *	Action to paste the contents of the clipboard into the entry field
 * 
 *	@author  support@sql-workbench.net
 */
public class PasteAction extends WbAction
{
	private ClipboardSupport client;

	public PasteAction(ClipboardSupport aClient)
	{
		super();
		this.client = aClient;
		initMenuDefinition("MnuTxtPaste", PlatformShortcuts.getDefaultPasteShortcut());
		this.setIcon("Paste");
		this.setMenuItemName(ResourceMgr.MNU_TXT_EDIT);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.paste();
	}
}
