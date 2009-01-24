/*
 * LockPanelAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import workbench.interfaces.MainPanel;

/**
 *
 * @author support@sql-workbench.net
 */
public class LockPanelAction 
	extends CheckBoxAction
{
	private MainPanel client;
	
	public LockPanelAction(MainPanel panel)
	{
		super("MnuTxtLockPanel");
		client = panel;
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		super.executeAction(e);
		client.setLocked(this.isSwitchedOn());
	}

}
