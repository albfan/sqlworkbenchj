/*
 * ToggleExtraConnection.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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

import workbench.resource.ResourceMgr;

import workbench.gui.MainWindow;

/**
 *
 * @author Thomas Kellerer
 */
public class ToggleExtraConnection
	extends CheckBoxAction
{
	private MainWindow window;

	public ToggleExtraConnection(MainWindow client)
	{
		super("MnuTxtUseExtraConn", null);
		this.setMenuItemName(ResourceMgr.MNU_TXT_FILE);
		this.setEnabled(false);
		this.window = client;
		checkState();
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		if (this.window == null) return;
		if (window.canUseSeparateConnection())
		{
			if (window.usesSeparateConnection())
			{
				this.window.disconnectCurrentPanel();
				this.setSwitchedOn(false);
			}
			else
			{
				this.window.createNewConnectionForCurrentPanel();
				this.setSwitchedOn(true);
			}
		}
	}

	public final void checkState()
	{
		if (this.window == null)
		{
			this.setEnabled(false);
			this.setSwitchedOn(false);
		}
		else
		{
			this.setEnabled(window.canUseSeparateConnection());
			this.setSwitchedOn(window.usesSeparateConnection());
		}
	}

}
