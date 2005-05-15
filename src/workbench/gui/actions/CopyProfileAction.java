/*
 * CopyProfileAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
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
public class CopyProfileAction 
	extends WbAction
{
	private FileActions client;

	public CopyProfileAction(FileActions aClient)
	{
		this.client = aClient;
		this.setIcon(ResourceMgr.getImage("CopyProfile"));
		this.initMenuDefinition("LabelCopyProfile");
	}

	public void executeAction(ActionEvent e)
	{
		try
		{
			this.client.newItem(true);
		}
		catch (Exception ex)
		{
			LogMgr.logError("NewListEntryAction.executeAction()", "Error copying profile", ex);
		}
		
	}
}
