/*
 * ToggleExtraConnection.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
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
public class ToggleExtraConnection 
	extends CheckBoxAction
{
	private MainWindow window;
	
	public ToggleExtraConnection(MainWindow client)
	{
		super("MnuTxtUseExtraConn", null);
		this.setMenuItemName(ResourceMgr.MNU_TXT_FILE);
		this.setEnabled(false);
		this.window = client;
		checkState();
	}
	
	@Override
	public void executeAction(ActionEvent e)
	{
		if (this.window == null) return;
		if (window.canUseSeparateConnection())
		{
			if (window.usesSeparateConnection())
			{
				this.window.disconnectCurrentPanel();
				this.setSwitchedOn(false);
			}
			else
			{
				this.window.createNewConnectionForCurrentPanel();
				this.setSwitchedOn(true);
			}
		}
	}
	
	public void checkState()
	{
		if (this.window == null)
		{
			this.setEnabled(false);
			this.setSwitchedOn(false);
		}
		else
		{
			this.setEnabled(window.canUseSeparateConnection());
			this.setSwitchedOn(window.usesSeparateConnection());
		}
	}
	
}
