/*
 * DisconnectTabAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
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
public class DisconnectTabAction
	extends WbAction
{
	private MainWindow window;

	public DisconnectTabAction(MainWindow client)
	{
		super();
		this.initMenuDefinition("MnuTxtDisconnectTab");
		this.setMenuItemName(ResourceMgr.MNU_TXT_FILE);
		this.setEnabled(false);
		this.window = client;
		checkState();
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		if (this.window == null) return;
		ConnectionProfile prof = window.getCurrentProfile();
		if (prof.getUseSeparateConnectionPerTab()) return;
		this.window.disconnectCurrentPanel();
	}

	public void checkState()
	{
		if (this.window == null)
		{
			this.setEnabled(false);
		}
		else
		{
			this.setEnabled(window.canUseSeparateConnection() && window.usesSeparateConnection());
		}
	}


}
