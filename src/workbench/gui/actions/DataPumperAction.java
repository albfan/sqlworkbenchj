/*
 * DataPumperAction.java
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

import java.awt.event.ActionEvent;
import workbench.WbManager;

import workbench.db.ConnectionProfile;
import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.tools.DataPumper;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;


/**
 *	Action to clear the contents of a entry field
 *	@author  support@sql-workbench.net
 */
public class DataPumperAction extends WbAction
{
	private static DataPumperAction instance = new DataPumperAction();
	
	private DataPumperAction()
	{
		super();
		this.initMenuDefinition("MnuTxtDataPumper");
		this.setMenuItemName(ResourceMgr.MNU_TXT_TOOLS);
		this.setIcon(ResourceMgr.getImage("DataPumper"));
	}
	
	public static DataPumperAction getInstance() { return instance; }
	
	public void executeAction(ActionEvent e)
	{
		MainWindow parent = WbManager.getInstance().getCurrentWindow();
		if (parent != null) 
		{
			WbSwingUtilities.showWaitCursor(parent);
		}
		try
		{
			ConnectionProfile profile = null;
			if (Settings.getInstance().getAutoConnectDataPumper())
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
