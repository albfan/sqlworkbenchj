/*
 * ExpandTreeAction.java
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
 * Expand all items in the ProfileTree
 * @see workbench.gui.profiles.ProfileTree#expandAll()
 * @author support@sql-workbench.net
 */
public class ExpandTreeAction
	extends WbAction
{
	private ProfileTree client;
	
	public ExpandTreeAction(ProfileTree tree)
	{
		super();
		this.client = tree;
		this.initMenuDefinition("LblExpandAll");
		this.setIcon(ResourceMgr.getImage("expand"));	
	}

	public void executeAction(ActionEvent e)
	{
		this.client.expandAll();
	}	
}
