/*
 * UnCommentAction.java
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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;
import workbench.gui.editor.TextCommenter;
import workbench.gui.sql.EditorPanel;

import workbench.resource.ResourceMgr;

/**
 *	@author  support@sql-workbench.net
 */
public class UnCommentAction 
	extends WbAction
{
	private EditorPanel client;

	public UnCommentAction(EditorPanel aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtUnCommentSelection",KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.CTRL_MASK + InputEvent.SHIFT_MASK));
		this.setMenuItemName(ResourceMgr.MNU_TXT_EDIT);
	}

	public void executeAction(ActionEvent e)
	{
		TextCommenter commenter = new TextCommenter(client);
		commenter.unCommentSelection();
	}
}
