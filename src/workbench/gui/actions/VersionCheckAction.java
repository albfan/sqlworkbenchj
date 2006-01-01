/*
 * VersionCheckAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;

import javax.swing.JFrame;

import workbench.WbManager;
import workbench.gui.WbSwingUtilities;
import workbench.gui.tools.VersionCheckDialog;
import workbench.resource.ResourceMgr;

/**
 *	@author  support@sql-workbench.net
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
		final JFrame parent = WbManager.getInstance().getCurrentWindow();
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				VersionCheckDialog dialog = new VersionCheckDialog(parent, true);
				WbSwingUtilities.center(dialog, parent);
				dialog.show();
			}
		});
	}
}
