/*
 * AssignWorkspaceAction.java
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

import workbench.gui.MainWindow;
import workbench.resource.ResourceMgr;

/**
 * Action to assign the currently loaded workspace to the current connection profile
 * @see workbench.gui.MainWindow#assignWorkspace()
 * @see workbench.db.ConnectionProfile
 * @see workbench.util.WbWorkspace
 * 
 * @author  Thomas Kellerer
 */
public class AssignWorkspaceAction 
	extends WbAction
{
	private MainWindow client;

	public AssignWorkspaceAction(MainWindow aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtAssignWorkspace", null);
		this.setMenuItemName(ResourceMgr.MNU_TXT_WORKSPACE);
		this.setIcon(null);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.assignWorkspace();
	}
	
}
