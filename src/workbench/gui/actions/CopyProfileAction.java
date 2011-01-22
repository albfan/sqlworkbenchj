/*
 * CopyProfileAction.java
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

/**
 * Action to copy a connection profile in the connect dialog.
 *
 * @author  Thomas Kellerer
 */
public class CopyProfileAction
	extends WbAction
{
	private FileActions client;

	public CopyProfileAction(FileActions aClient)
	{
		super();
		this.client = aClient;
		this.setIcon("CopyProfile");
		this.initMenuDefinition("LblCopyProfile");
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
