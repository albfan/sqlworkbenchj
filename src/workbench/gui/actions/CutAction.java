/*
 * CutAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
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
 *	Action to cut  the contents of an entry field
 * 
 *	@author  Thomas Kellerer
 */
public class CutAction extends WbAction
{
	private ClipboardSupport client;

	public CutAction(ClipboardSupport aClient)
	{
		super();
		this.client = aClient;
		initMenuDefinition("MnuTxtCut", PlatformShortcuts.getDefaultCutShortcut());
		this.setIcon("Cut");
		this.setMenuItemName(ResourceMgr.MNU_TXT_EDIT);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.cut();
	}
}
