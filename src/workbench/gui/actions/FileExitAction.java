/*
 * FileExitAction.java
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

import workbench.WbManager;
import workbench.gui.WbSwingUtilities;
import workbench.resource.ResourceMgr;

/**
 * Exit and close the application.
 *
 * @see workbench.WbManager#exitWorkbench()
 *
 * @author  Thomas Kellerer
 */
public class FileExitAction
	extends WbAction
{
	public FileExitAction()
	{
		super();
		this.initMenuDefinition("MnuTxtExit");
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		boolean forceShutdown = false;
		if (isCtrlPressed(e))
		{
			forceShutdown = WbSwingUtilities.getYesNo(WbManager.getInstance().getCurrentWindow(), ResourceMgr.getString("MsgAbortWarning"));
		}
		WbManager.getInstance().exitWorkbench(forceShutdown);
	}
}
