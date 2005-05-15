/*
 * FileExitAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;

import workbench.WbManager;
import workbench.resource.ResourceMgr;

/**
 *	Action to paste the contents of the clipboard into the entry field
 *	@author  support@sql-workbench.net
 */
public class FileExitAction extends WbAction
{
	public FileExitAction()
	{
		super();
		this.initMenuDefinition(ResourceMgr.MNU_TXT_EXIT);
	}

	public void executeAction(ActionEvent e)
	{
		WbManager.getInstance().exitWorkbench();
	}
}
