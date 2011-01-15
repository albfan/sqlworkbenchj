/*
 * UndoExpandAction.java
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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;
import workbench.gui.sql.SplitPaneExpander;
import workbench.resource.ResourceMgr;

/**
 *	@author  Thomas Kellerer
 */
public class UndoExpandAction extends WbAction
{
	private SplitPaneExpander client;

	public UndoExpandAction(SplitPaneExpander aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtUndoExpand", KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.ALT_MASK | InputEvent.SHIFT_MASK));
		this.setMenuItemName(ResourceMgr.MNU_TXT_VIEW);
		this.setIcon(null);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.undoExpand();
	}
}
