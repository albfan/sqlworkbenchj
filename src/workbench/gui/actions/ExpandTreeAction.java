/*
 * ExpandTreeAction.java
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
import workbench.interfaces.ExpandableTree;

/**
 * Expand all items in a Tree

 * @author Thomas Kellerer
 */
public class ExpandTreeAction
	extends WbAction
{
	private ExpandableTree client;

	public ExpandTreeAction(ExpandableTree tree)
	{
		super();
		this.client = tree;
		this.initMenuDefinition("LblExpandAll");
		this.setIcon("expand");
	}

	public void executeAction(ActionEvent e)
	{
		this.client.expandAll();
	}
}
