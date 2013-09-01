/*
 * FileSaveAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import workbench.gui.sql.EditorPanel;

import workbench.resource.PlatformShortcuts;
import workbench.resource.ResourceMgr;

/**
 * Save the current file in the SQL Editor.
 *	@author  Thomas Kellerer
 */
public class FileSaveAction extends WbAction
{
	private EditorPanel client;

	public FileSaveAction(EditorPanel aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtFileSave", KeyStroke.getKeyStroke(KeyEvent.VK_S, PlatformShortcuts.getDefaultModifier()));
		this.setMenuItemName(ResourceMgr.MNU_TXT_FILE);
		this.setEnabled(this.client.hasFileLoaded());
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		this.client.saveCurrentFile();
	}
}
