/*
 * ShowDbExplorerAction.java
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
import java.util.List;

import javax.swing.KeyStroke;

import workbench.interfaces.ToolWindow;
import workbench.resource.DbExplorerSettings;
import workbench.resource.PlatformShortcuts;

import workbench.gui.MainWindow;

/**
 *	@author  Thomas Kellerer
 */
public class ShowDbExplorerAction
	extends WbAction
{
	private MainWindow mainWin;
	public ShowDbExplorerAction(MainWindow aWindow)
	{
		super();
		mainWin = aWindow;
		this.initMenuDefinition("MnuTxtShowDbExplorer",KeyStroke.getKeyStroke(KeyEvent.VK_D, PlatformShortcuts.getDefaultModifier()));
		this.setIcon("database");
		setEnabled(false);
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		boolean useTab = DbExplorerSettings.getShowDbExplorerInMainWindow();
		if (useTab)
		{
			int index = mainWin.findFirstExplorerTab();
			if (index > -1)
			{
				mainWin.selectTab(index);
			}
			else
			{
				mainWin.newDbExplorerPanel(true);
			}
		}
		else
		{
			List<ToolWindow> windows = mainWin.getExplorerWindows();
			if (windows.size() > 0)
			{
				ToolWindow w = windows.get(0);
				w.activate();
			}
			else
			{
				mainWin.newDbExplorerWindow();
			}
		}
	}

}
