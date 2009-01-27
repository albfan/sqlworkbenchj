/*
 * ExpandResultAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;

import workbench.gui.sql.SplitPaneExpander;

import workbench.resource.ResourceMgr;

/**
 * Expand the result panel in the editor to the full window size.
 *	@author  support@sql-workbench.net
 */
public class ExpandResultAction extends WbAction
{
	private SplitPaneExpander client;

	public ExpandResultAction(SplitPaneExpander expander)
	{
		super();
		this.client = expander;
		this.initMenuDefinition("MnuTxtExpandResult");
		this.setMenuItemName(ResourceMgr.MNU_TXT_VIEW);
		this.setIcon(null);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.toggleLowerComponentExpand();
	}
}
