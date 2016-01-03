/*
 * HelpContactAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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

import workbench.db.WbConnection;
import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.interfaces.MainPanel;
import workbench.util.BrowserLauncher;
import workbench.util.ExceptionUtil;

/**
 * @author Thomas Kellerer
 */
public class HelpContactAction
	extends WbAction
{
	private MainWindow mainWindow;

	public HelpContactAction(MainWindow parent)
	{
		super();
		mainWindow = parent;
		initMenuDefinition("MnuTxtHelpContact");
		removeIcon();
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		sendEmail();
	}

	private void sendEmail()
	{
		sendEmail(mainWindow);
	}

	public static void sendEmail(MainWindow mainWin)
	{
		WbConnection currentConnection = null;

		if (mainWin != null)
		{
			MainPanel panel = mainWin.getCurrentPanel();
			if (panel != null)
			{
				currentConnection = panel.getConnection();
			}
		}

		try
		{
			BrowserLauncher.openEmail("support@sql-workbench.net", currentConnection);
		}
		catch (Exception ex)
		{
			WbSwingUtilities.showErrorMessage(ExceptionUtil.getDisplay(ex));
		}
	}

}
