/*
 * SaveListFileAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;

import workbench.interfaces.FileActions;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  info@sql-workbench.net
 */
public class SaveListFileAction extends WbAction
{
	private FileActions client;

	public SaveListFileAction(FileActions aClient)
	{
		this.client = aClient;
		this.setMenuTextByKey("LabelSaveProfiles");
		this.setIcon(	ResourceMgr.getImage(ResourceMgr.IMG_SAVE));
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
