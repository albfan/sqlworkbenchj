/*
 * NewGroupAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.profiles;

import java.awt.event.ActionEvent;
import workbench.gui.actions.WbAction;
import workbench.interfaces.GroupTree;
import workbench.log.LogMgr;

/**
 * @author Thomas Kellerer
 */
public class NewGroupAction
	extends WbAction
{
	private GroupTree client;
	
	public NewGroupAction(GroupTree panel, String resourceKey)
	{
		super();
		this.client = panel;
		this.setIcon("NewFolder");
		this.initMenuDefinition(resourceKey);
	}

	public void executeAction(ActionEvent e)
	{
		try
		{
			this.client.addGroup();
		}
		catch (Exception ex)
		{
			LogMgr.logError("NewListEntryAction.executeAction()", "Error copying profile", ex);
		}
	}

}
