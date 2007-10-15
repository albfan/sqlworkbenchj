/*
 * CommentAction.java
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

import workbench.interfaces.TextSelectionListener;
import workbench.resource.ResourceMgr;

/**
 * Action to "comment" the currently selected text in the SQL editor. 
 * This is done by addin single line comments to each line
 * @see workbench.gui.sql.EditorPanel#commentSelection()	
 * @author  support@sql-workbench.net
 */
public class CommentAction 
	extends WbAction
	implements TextSelectionListener
{
	private EditorPanel client;

	public CommentAction(EditorPanel aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtCommentSelection",KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK + InputEvent.SHIFT_MASK));
		this.setMenuItemName(ResourceMgr.MNU_TXT_EDIT);
		this.setEnabled(false);
		this.client.addSelectionListener(this);
	}

	public void selectionChanged(int newStart, int newEnd)
	{
		this.setEnabled(newEnd > newStart);
	}
	
	public void executeAction(ActionEvent e)
	{
		TextCommenter commenter = new TextCommenter(client);
		commenter.commentSelection();
	}
}
