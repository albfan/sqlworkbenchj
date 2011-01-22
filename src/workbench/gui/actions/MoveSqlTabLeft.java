/*
 * MoveSqlTabLeft.java
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
import workbench.gui.MainWindow;

/**
 *	@author  Thomas Kellerer
 */
public class MoveSqlTabLeft 
	extends WbAction
{
	private MainWindow client;
	
	public MoveSqlTabLeft(MainWindow aClient)
	{
		super();
		client = aClient;
		isConfigurable = false;
		initMenuDefinition("MnuTxtMoveTabLeft");
		setIcon(null);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.moveTabLeft();
	}
}
