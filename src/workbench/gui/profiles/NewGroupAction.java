/*
 * NewGroupAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.profiles;

import java.awt.event.ActionEvent;
import workbench.gui.actions.WbAction;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

/**
 * @author support@sql-workbench.net
 */
public class NewGroupAction
	extends WbAction
{
	private ProfileTree client;
	public NewGroupAction(ProfileTree panel)
	{
		this.client = panel;
		this.setIcon(ResourceMgr.getImage("NewFolder"));
		this.initMenuDefinition("LblNewProfileGroup");
	}
	
	public void executeAction(ActionEvent e)
	{
		try
		{
			this.client.addProfileGroup();
		}
		catch (Exception ex)
		{
			LogMgr.logError("NewListEntryAction.executeAction()", "Error copying profile", ex);
		}
	}
	
}
