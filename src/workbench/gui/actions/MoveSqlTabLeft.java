/*
 * MoveSqlTabLeft.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
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
public class MoveSqlTabLeft 
	extends WbAction
{
	private MainWindow client;
	
	public MoveSqlTabLeft(MainWindow aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtMoveTabLeft");
		this.setIcon(null);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.moveTabLeft();
	}
}
