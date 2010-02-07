/*
 * AutoCompletionAction.java
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
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;
import workbench.db.WbConnection;
import workbench.gui.completion.CompletionHandler;
import workbench.gui.editor.JEditTextArea;
import workbench.interfaces.StatusBar;
import workbench.resource.ResourceMgr;

/**
 * Action to display the code-completion for SQL statements.
 * @see workbench.gui.completion.CompletionHandler
 *
 * @author  Thomas Kellerer
 */
public class AutoCompletionAction
	extends WbAction
{
	private CompletionHandler handler;
	private JEditTextArea editor;
	private StatusBar status;

	public AutoCompletionAction(JEditTextArea edit, StatusBar bar)
	{
		super();
		this.editor = edit;
		this.status = bar;
		this.initMenuDefinition("MnuTxtAutoComplete", KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, KeyEvent.CTRL_MASK));
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
		this.setEnabled(false);

		// we have to register this keybinding with the editor
		// otherwise Ctrl-Space will not work properly
		edit.addKeyBinding(this);
	}

	public void closePopup()
	{
		if (handler != null) handler.cancelPopup();
	}

	public void setConnection(WbConnection conn)
	{
		if (conn == null)
		{
			this.handler = null;
		}
		else if (this.handler == null)
		{
			try
			{
				this.handler = new CompletionHandler();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		if (conn != null)
		{
			this.handler.setStatusBar(status);
			this.handler.setEditor(editor);
			this.handler.setConnection(conn);
		}

		this.setEnabled(conn != null);
	}

	public void setAccelerator(KeyStroke key)
	{
		KeyStroke old = this.getAccelerator();
		editor.removeKeyBinding(old);
		super.setAccelerator(key);
		editor.addKeyBinding(this);
	}

	public void executeAction(ActionEvent e)
	{
		handler.showCompletionPopup();
	}
}
