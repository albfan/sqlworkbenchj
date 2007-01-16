/*
 * CollapseTreeAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import workbench.gui.profiles.ProfileTree;
import workbench.resource.ResourceMgr;

/**
 * Action to collapse all nodes in the connection profile tree
 * @see workbench.gui.profiles.ProfileTree#collapseAll()
 * @author support@sql-workbench.net
 */
public class CollapseTreeAction
	extends WbAction
{
	private ProfileTree client;
	
	public CollapseTreeAction(ProfileTree tree)
	{
		super();
		this.client = tree;
		this.initMenuDefinition("LblCollapseAll");
		this.setIcon(ResourceMgr.getImage("collapse"));	
	}

	public void executeAction(ActionEvent e)
	{
		this.client.collapseAll();
	}	
}
