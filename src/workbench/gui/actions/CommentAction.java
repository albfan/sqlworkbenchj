/*
 * CommentAction.java
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
import workbench.gui.editor.TextCommenter;
import workbench.gui.sql.EditorPanel;

import workbench.resource.PlatformShortcuts;
import workbench.resource.ResourceMgr;

/**
 * Action to "comment" the currently selected text in the SQL editor.
 * This is done by addin single line comments to each line
 * @see workbench.gui.editor.TextCommenter#commentSelection()
 * @author  Thomas Kellerer
 */
public class CommentAction
	extends WbAction
{
	private EditorPanel client;

	public CommentAction(EditorPanel aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtCommentSelection",KeyStroke.getKeyStroke(KeyEvent.VK_C, PlatformShortcuts.getDefaultModifier() + InputEvent.SHIFT_MASK));
		this.setMenuItemName(ResourceMgr.MNU_TXT_EDIT);
	}

	public void executeAction(ActionEvent e)
	{
		TextCommenter commenter = new TextCommenter(client);
		commenter.commentSelection();
	}
}
