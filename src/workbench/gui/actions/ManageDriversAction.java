/*
 * ManageDriversAction.java
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

import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.profiles.DriverEditorDialog;
import workbench.resource.ResourceMgr;

/**
 *	@author  support@sql-workbench.net
 */
public class ManageDriversAction extends WbAction
{
	private MainWindow client;

	public ManageDriversAction(MainWindow aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtEditDrivers");
		this.setMenuItemName(ResourceMgr.MNU_TXT_FILE);
	}

	public void executeAction(ActionEvent e)
	{
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				DriverEditorDialog d = new DriverEditorDialog(client, true);
				WbSwingUtilities.center(d, client);
				d.show();
			}
		});
	}
	
}
