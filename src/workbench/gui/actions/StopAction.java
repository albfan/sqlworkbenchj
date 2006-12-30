/*
 * StopAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;

import workbench.interfaces.Interruptable;
import workbench.resource.ResourceMgr;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  support@sql-workbench.net
 */
public class StopAction extends WbAction
{
	private Interruptable panel;

	public StopAction(Interruptable aPanel)
	{
		super();
		this.panel = aPanel;
		this.initMenuDefinition("MnuTxtStopStmt");
		this.setIcon(ResourceMgr.getImage("Stop"));
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
		this.setCreateMenuSeparator(true);
	}

	public void executeAction(ActionEvent e)
	{
		this.panel.cancelExecution();
	}
}
