/*
 * RedoAction.java
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
import workbench.interfaces.Undoable;
import workbench.resource.ResourceMgr;

/**
 *	@author  Thomas Kellerer
 */
public class RedoAction extends WbAction
{
	private Undoable client;

	public RedoAction(Undoable aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtRedo");
		this.setIcon("Redo");
		this.setMenuItemName(ResourceMgr.MNU_TXT_EDIT);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.redo();
	}
}
