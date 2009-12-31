/*
 * PrevStatementAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;
import workbench.gui.sql.SqlHistory;

import workbench.resource.PlatformShortcuts;
import workbench.resource.ResourceMgr;

/**
 *	Display the previous statement from the SQL History
 *	@author  Thomas Kellerer
 */
public class PrevStatementAction
	extends WbAction
{
	private SqlHistory history;
	public PrevStatementAction(SqlHistory aHistory)
	{
		super();
		this.history = aHistory;
		this.initMenuDefinition("MnuTxtPrevStatement", PlatformShortcuts.getDefaultPrevStatement());
		this.setIcon("Back");
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
		this.setCreateMenuSeparator(false);
		this.setCreateToolbarSeparator(false);
	}

	public void executeAction(ActionEvent e)
	{
		this.history.showPreviousStatement();
	}
}
