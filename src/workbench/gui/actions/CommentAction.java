/*
 * CommentAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;
import workbench.gui.sql.EditorPanel;

import workbench.interfaces.Undoable;
import workbench.resource.ResourceMgr;

/**
 *	@author  info@sql-workbench.net
 */
public class CommentAction extends WbAction
{
	private EditorPanel client;

	public CommentAction(EditorPanel aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtCommentSelection",KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK + InputEvent.SHIFT_MASK));
		this.setMenuItemName(ResourceMgr.MNU_TXT_EDIT);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.commentSelection();
	}
}
