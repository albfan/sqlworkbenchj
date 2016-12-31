/*
 * AutoCompletionAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import workbench.log.LogMgr;
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
				LogMgr.logError("AutoCompletionAction.setConnection()", "Error setting connection", e);
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

	@Override
	public void setAccelerator(KeyStroke key)
	{
		KeyStroke old = this.getAccelerator();
		editor.removeKeyBinding(old);
		super.setAccelerator(key);
		editor.addKeyBinding(this);
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		handler.showCompletionPopup();
	}
}
