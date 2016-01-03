/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */

package workbench.gui.actions;

import java.awt.event.ActionEvent;

import workbench.db.ConnectionProfile;

import workbench.gui.MainWindow;
import workbench.log.LogMgr;

/**
 * Re-Connect the current window.
 *
 * @author Charles (peacech@gmail.com)
 */
public class FileReconnectAction
	extends WbAction
{
	private MainWindow window;

	public FileReconnectAction(MainWindow aWindow)
	{
		super();
		this.window = aWindow;
		this.initMenuDefinition("MnuTxtReconnect");
		setEnabled(false);
	}

	@Override
	public void executeAction(ActionEvent e)
	{
    LogMgr.logDebug("FileReconnectAction.executeAction()", "Initiating reconnect.");
		ConnectionProfile profile = window.getCurrentProfile();
		boolean reloadWorkspace = false;
		window.disconnect(false, false, true);
		if (invokedByMouse(e) && isCtrlPressed(e))
		{
			reloadWorkspace = true;
		}
		window.connectTo(profile, false, reloadWorkspace);
	}

}
