/*
 * DeleteListEntryAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
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
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  support@sql-workbench.net
 */
public class DeleteListEntryAction extends WbAction
{
	private FileActions client;

	public DeleteListEntryAction(FileActions aClient)
	{
		this(aClient, "LblDeleteListEntry");
	}

	public DeleteListEntryAction(FileActions aClient, String aKey)
	{
		this.client = aClient;
		this.setMenuTextByKey(aKey);
		this.setIcon(ResourceMgr.getImage(ResourceMgr.IMG_DELETE));
	}

	public void executeAction(ActionEvent e)
	{
		try
		{
			this.client.deleteItem();
		}
		catch (Exception ex)
		{
			LogMgr.logError(this, "Error saving profiles", ex);
		}
	}
}
