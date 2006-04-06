/*
 * NewListEntryAction.java
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
import java.awt.event.KeyEvent;

import workbench.interfaces.FileActions;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.util.StringUtil;

/**
 *	@author  support@sql-workbench.net
 */
public class NewListEntryAction 
	extends WbAction
{
	private FileActions client;
	private boolean checkShift = true;
	
	public NewListEntryAction(FileActions aClient, String aKey)
	{
		this.client = aClient;
		this.initMenuDefinition(aKey);
		this.setIcon(ResourceMgr.getImage(ResourceMgr.IMG_NEW));
	}
	
	public NewListEntryAction(FileActions aClient)
	{
		this.client = aClient;
		this.setIcon(ResourceMgr.getImage(ResourceMgr.IMG_NEW));
		this.checkShift = true;
		String tip = ResourceMgr.getDescription("LblNewListEntry", true);
		this.initMenuDefinition(ResourceMgr.getString("LblNewListEntry"), tip, null);
	}

	public void executeAction(ActionEvent e)
	{
		boolean shiftPressed = checkShift && ((e.getModifiers() & ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK);
		try
		{
			this.client.newItem(shiftPressed);
		}
		catch (Exception ex)
		{
			LogMgr.logError("NewListEntryAction.executeAction()", "Error creating new list entry", ex);
		}
		
	}
}
