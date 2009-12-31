/*
 * CreateNewConnection.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import workbench.gui.MainWindow;
import workbench.resource.ResourceMgr;

/**
 *
 * @author Thomas Kellerer
 */
public class CreateNewConnection 
	extends WbAction
{
	private MainWindow window;
	
	public CreateNewConnection(MainWindow client)
	{
		super();
		this.initMenuDefinition("MnuTxtCreateNewConn");
		this.setMenuItemName(ResourceMgr.MNU_TXT_FILE);
		this.setEnabled(false);
		this.window = client;
		checkState();
	}
	
	@Override
	public void executeAction(ActionEvent e)
	{
		if (this.window == null) return;
		if (!window.canUseSeparateConnection()) return;
		this.window.createNewConnectionForCurrentPanel();
	}

	public void checkState()
	{
		if (this.window == null)
		{
			this.setEnabled(false);
		}
		else
		{
			this.setEnabled(window.canUseSeparateConnection() && !window.usesSeparateConnection());
		}
	}
	
}
