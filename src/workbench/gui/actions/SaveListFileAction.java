/*
 * SaveListFileAction.java
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

import workbench.interfaces.FileActions;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

/**
 *	@author  Thomas Kellerer
 */
public class SaveListFileAction extends WbAction
{
	private FileActions client;

	public SaveListFileAction(FileActions aClient)
	{
		this(aClient, "LblSaveProfiles");
	}

	public SaveListFileAction(FileActions aClient, String labelKey)
	{
		super();
		this.client = aClient;
		this.setMenuTextByKey(labelKey);
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
