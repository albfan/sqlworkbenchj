/*
 * ExecuteAllAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.gui.sql.SqlPanel;
import workbench.resource.PlatformShortcuts;
import workbench.resource.ResourceMgr;

/**
 * Run all statements in the current SQL Panel
 * @see workbench.gui.sql.SqlPanel#runAll()
 * @author  Thomas Kellerer
 */
public class ExecuteAllAction extends WbAction
{
	private SqlPanel client;

	public ExecuteAllAction(SqlPanel aPanel)
	{
		super();
		this.client = aPanel;
		this.initMenuDefinition("MnuTxtExecuteAll", KeyStroke.getKeyStroke(KeyEvent.VK_E, PlatformShortcuts.getDefaultModifier() | InputEvent.SHIFT_MASK));
		this.setIcon("ExecuteAll");
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
		this.setEnabled(false);
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		this.client.runAll();
	}
}
