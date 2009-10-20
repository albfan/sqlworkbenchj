/*
 * FileOpenAction.java
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


import workbench.interfaces.TextFileContainer;
import workbench.resource.ResourceMgr;

/**
 *	Open a new file in the SQL Editor
 *	@author  support@sql-workbench.net
 */
public class FileOpenAction
	extends WbAction
{
	private TextFileContainer client;

	public FileOpenAction(TextFileContainer aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtFileOpen");
		this.setIcon("Open");
		this.setMenuItemName(ResourceMgr.MNU_TXT_FILE);
		setCreateMenuSeparator(true);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.openFile();
	}
}
