/*
 * FileNewWindowAction.java
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

import workbench.WbManager;

/**
 * Open a new SQL MainWindow
 *	@author  Thomas Kellerer
 */
public class FileNewWindowAction
	extends WbAction
{
	public FileNewWindowAction()
	{
		super();
		this.initMenuDefinition("MnuTxtFileNewWindow");
	}

	public void executeAction(ActionEvent e)
	{
		WbManager.getInstance().openNewWindow();
	}
}
