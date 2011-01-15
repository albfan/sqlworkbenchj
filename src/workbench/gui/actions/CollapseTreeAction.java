/*
 * CollapseTreeAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import workbench.interfaces.ExpandableTree;

/**
 * Action to collapse all nodes in a tree

 * @author Thomas Kellerer
 */
public class CollapseTreeAction
	extends WbAction
{
	private ExpandableTree client;

	public CollapseTreeAction(ExpandableTree tree)
	{
		super();
		this.client = tree;
		this.initMenuDefinition("LblCollapseAll");
		this.setIcon("collapse");
	}

	public void executeAction(ActionEvent e)
	{
		this.client.collapseAll();
	}
}
