/*
 * AutoCompletionAction.java
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
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;
import workbench.db.WbConnection;
import workbench.gui.completion.CompletionHandler;
import workbench.gui.editor.JEditTextArea;
import workbench.interfaces.StatusBar;
import workbench.resource.ResourceMgr;

/**
 * Action to display the code-completion for SQL statements.
 * @see workbench.gui.completion.DefaultCompletionHandler
 * 
 * @author  support@sql-workbench.net
 */
public class AutoCompletionAction
	extends WbAction
{
	private CompletionHandler handler;
	private JEditTextArea editor;
	private StatusBar status;
	public AutoCompletionAction(JEditTextArea editor, StatusBar status)
	{
		this.editor = editor;
		this.status = status;
		this.initMenuDefinition("MnuTxtAutoComplete", KeyStroke.getKeyStroke(KeyEvent.VK_Q,KeyEvent.CTRL_MASK));
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
		this.setEnabled(false);
		
		// we have to register this keybinding with the editor
		// otherwise Ctrl-Space will not work properly
		editor.addKeyBinding(this);
	}

	public void closePopup()
	{
		if (handler != null) handler.cancelPopup();
	}
	
	public void setConnection(WbConnection conn)
	{
		if (conn != null && this.handler == null)
		{
			try
			{
				// Use reflection to create the instance so 
				// that the classes are not loaded during startup
				this.handler = (CompletionHandler)Class.forName("workbench.gui.completion.DefaultCompletionHandler").newInstance();
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
