/*
 * NewListEntryAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
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
 *	@author  Thomas Kellerer
 */
public class NewListEntryAction
	extends WbAction
{
	private FileActions client;

	public NewListEntryAction(FileActions aClient, String aKey)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition(aKey);
		this.setIcon("New");
	}

	public NewListEntryAction(FileActions aClient)
	{
		super();
		this.client = aClient;
		this.setIcon("New");
		String tip = ResourceMgr.getDescription("LblNewListEntry", true);
		this.initMenuDefinition(ResourceMgr.getString("LblNewListEntry"), tip, null);
	}

	public void executeAction(ActionEvent e)
	{
		try
		{
			this.client.newItem(isShiftPressed(e));
		}
		catch (Exception ex)
		{
			LogMgr.logError("NewListEntryAction.executeAction()", "Error creating new list entry", ex);
		}

	}
}
