/*
 * VersionCheckAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.JFrame;

import workbench.WbManager;
import workbench.gui.WbSwingUtilities;
import workbench.gui.tools.VersionCheckDialog;
import workbench.resource.ResourceMgr;

/**
 *	@author  info@sql-workbench.net
 */
public class VersionCheckAction extends WbAction
{

	public VersionCheckAction()
	{
		super();
		this.initMenuDefinition("MnuTxtVersionCheck");
		this.setMenuItemName(ResourceMgr.MNU_TXT_TOOLS);
		this.setIcon(null);
	}

	public void executeAction(ActionEvent e)
	{
		JFrame parent = WbManager.getInstance().getCurrentWindow();
		VersionCheckDialog dialog = new VersionCheckDialog(parent, true);
		WbSwingUtilities.center(dialog, parent);
		dialog.startRetrieveVersions();
		dialog.show();
	}
}
