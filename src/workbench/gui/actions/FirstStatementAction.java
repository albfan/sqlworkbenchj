/*
 * FirstStatementAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
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
import workbench.resource.ResourceMgr;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  support@sql-workbench.net
 */
public class FirstStatementAction extends WbAction
{
	private SqlHistory history;

	public FirstStatementAction(SqlHistory aHistory)
	{
		super();
		this.history = aHistory;
		this.initMenuDefinition("MnuTxtFirstStatement", KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.ALT_MASK | InputEvent.SHIFT_MASK ));
		this.setIcon(ResourceMgr.getImage("First"));
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
		this.setCreateMenuSeparator(true);
		this.setCreateToolbarSeparator(true);
	}

	public void executeAction(ActionEvent e)
	{
		this.history.showFirstStatement();
	}
}
