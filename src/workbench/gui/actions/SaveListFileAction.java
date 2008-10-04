/*
 * SaveListFileAction.java
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

import workbench.interfaces.FileActions;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

/**
 *	@author  support@sql-workbench.net
 */
public class SaveListFileAction extends WbAction
{
	private FileActions client;

	public SaveListFileAction(FileActions aClient)
	{
		super();
		this.client = aClient;
		this.setMenuTextByKey("LblSaveProfiles");
		this.setIcon(ResourceMgr.IMG_SAVE);
	}

	public void executeAction(ActionEvent e)
	{
		try
		{
			this.client.saveItem();
		}
		catch (Exception ex)
		{
			LogMgr.logError(this, "Error saving profiles", ex);
		}
	}
}
