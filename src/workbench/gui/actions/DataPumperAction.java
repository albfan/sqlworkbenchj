/*
 * DataPumperAction.java
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

import workbench.db.ConnectionProfile;
import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.tools.DataPumper;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;


/**
 * Action to display the DataPumper window
 * @see workbench.gui.tools.DataPumper
 * @author  Thomas Kellerer
 */
public class DataPumperAction
	extends WbAction
{
	private MainWindow parent;

	public DataPumperAction(MainWindow win)
	{
		super();
		this.initMenuDefinition("MnuTxtDataPumper");
		this.setMenuItemName(ResourceMgr.MNU_TXT_TOOLS);
		this.setIcon("DataPumper");
		this.parent = win;
	}

	public void executeAction(ActionEvent e)
	{
		if (parent != null)
		{
			WbSwingUtilities.showWaitCursor(parent);
		}
		try
		{
			ConnectionProfile profile = null;
			if (parent != null && Settings.getInstance().getAutoConnectDataPumper())
			{
				profile = parent.getCurrentProfile();
			}
			DataPumper p = new DataPumper(profile, null);
			p.showWindow(parent);
		}
		finally
		{
			if (parent != null) WbSwingUtilities.showDefaultCursor(parent);
		}
	}

}
