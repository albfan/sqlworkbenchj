/*
 * FindAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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

import workbench.interfaces.Searchable;
import workbench.resource.PlatformShortcuts;
import workbench.resource.ResourceMgr;

/**
 *	@author Thomas Kellerer
 */
public class FindAction extends WbAction
{
	private Searchable client;

	public FindAction(Searchable aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtFind", KeyStroke.getKeyStroke(KeyEvent.VK_F, PlatformShortcuts.getDefaultModifier()));
		this.setIcon("find");
		this.setMenuItemName(ResourceMgr.MNU_TXT_EDIT);
		this.setCreateToolbarSeparator(true);
		this.setDescriptiveName(ResourceMgr.getString("TxtEdPrefix") + " " + getMenuLabel());
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		this.client.find();
	}
}
