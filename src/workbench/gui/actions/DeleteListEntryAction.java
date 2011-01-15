/*
 * DeleteListEntryAction.java
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
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;

import workbench.interfaces.FileActions;
import workbench.log.LogMgr;

/**
 *	Delete an etry from a List
 *	@author  Thomas Kellerer
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
		super();
		client = aClient;
		isConfigurable = false;
		setMenuTextByKey(aKey);
		setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE,0));
		setIcon("Delete");
	}

	public void executeAction(ActionEvent e)
	{
		try
		{
			client.deleteItem();
		}
		catch (Exception ex)
		{
			LogMgr.logError(this, "Error saving profiles", ex);
		}
	}
}
