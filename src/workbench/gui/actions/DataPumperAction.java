/*
 * DataPumperAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.SwingUtilities;

import workbench.db.ConnectionProfile;
import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.tools.DataPumper;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;


/**
 *	Action to clear the contents of a entry field
 *	@author  info@sql-workbench.net
 */
public class DataPumperAction extends WbAction
{
	private MainWindow parent;

	public DataPumperAction(MainWindow parent)
	{
		super();
		this.parent = parent;
		this.initMenuDefinition("MnuTxtDataPumper");
		this.setMenuItemName(ResourceMgr.MNU_TXT_TOOLS);
		this.setIcon(ResourceMgr.getImage("DataPumper"));
	}

	public void executeAction(ActionEvent e)
	{
		if (parent == null)
		{
			return;
		}
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				WbSwingUtilities.showWaitCursor(parent);
				ConnectionProfile profile = null;
				if (Settings.getInstance().getAutoConnectDataPumper())
				{
					profile = parent.getCurrentProfile();
				}
				DataPumper p = new DataPumper(profile, null);
				p.showWindow(parent);
				WbSwingUtilities.showDefaultCursor(parent);
			}
		});
	}

}
