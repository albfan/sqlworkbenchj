/*
 * ToggleSeparateConnection.java
 * 
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author.
 * 
 * To contact the author please send an email to: support@sql-workbench.net
 */

package workbench.gui.actions;

import java.awt.event.ActionEvent;
import workbench.db.ConnectionProfile;
import workbench.gui.MainWindow;
import workbench.resource.ResourceMgr;

/**
 *
 * @author support@sql-workbench.net
 */
public class CreateNewConnection 
	extends WbAction
{
	private MainWindow window;
	
	public CreateNewConnection(MainWindow client)
	{
		this.initMenuDefinition("MnuTxtCreateNewConn");
		this.setMenuItemName(ResourceMgr.MNU_TXT_FILE);
		this.setEnabled(false);
		this.window = client;
	}
	
	@Override
	public void executeAction(ActionEvent e)
	{
		if (this.window == null) return;
		ConnectionProfile prof = window.getCurrentProfile();
		if (prof.getUseSeparateConnectionPerTab()) return;
		this.window.createNewConnectionForCurrentPanel();
	}
	
	
}
