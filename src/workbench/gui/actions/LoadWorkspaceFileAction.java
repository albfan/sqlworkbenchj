/*
 * LoadWorkspaceFileAction.java
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
import workbench.util.WbFile;

/**
 *	@author  Thomas Kellerer
 */
public class LoadWorkspaceFileAction extends WbAction
{
	private MainWindow client;
	private WbFile workspace;

	public LoadWorkspaceFileAction(MainWindow aClient, WbFile file)
	{
		super();
		client = aClient;
		workspace = file;
		this.setMenuText(workspace.getFileName());
		this.setTooltip(workspace.getFullPath());
		this.setIcon(null);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.loadWorkspace(workspace.getFullPath(), true);
	}
}
