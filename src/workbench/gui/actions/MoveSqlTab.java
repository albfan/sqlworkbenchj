/*
 * MoveSqlTab.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
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
 *	@author  support@sql-workbench.net
 */
public class MoveSqlTab extends WbAction
{
	private MainWindow client;
	private boolean moveLeft;
	
	public MoveSqlTab(MainWindow aClient, boolean toLeft)
	{
		super();
		this.client = aClient;
		this.moveLeft = toLeft;
		if (moveLeft)
		{
			this.initMenuDefinition("MnuTxtMoveTabLeft");
		}
		else
		{
			this.initMenuDefinition("MnuTxtMoveTabRight");
		}
		this.setIcon(null);
	}

	public void executeAction(ActionEvent e)
	{
		if (moveLeft)
			this.client.moveTabLeft();
		else
			this.client.moveTabRight();
	}
}
