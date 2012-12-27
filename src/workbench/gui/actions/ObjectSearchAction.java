/*
 * ObjectSearchAction.java
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

import workbench.db.ConnectionProfile;
import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.tools.ObjectSourceSearchPanel;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;


/**
 * Action to display the Object Searcher window
 *
 * @author Thomas Kellerer
 */
public class ObjectSearchAction
	extends WbAction
{
	private MainWindow parent;

	public ObjectSearchAction(MainWindow win)
	{
		super();
		this.initMenuDefinition("MnuTxtObjectSearch");
		this.setMenuItemName(ResourceMgr.MNU_TXT_TOOLS);
		this.setIcon("searchsource16.png");
		this.parent = win;
	}

	public void executeAction(ActionEvent e)
	{
		if (parent != null)
		{
			WbSwingUtilities.showWaitCursor(parent);
		}
		try
		{
			ObjectSourceSearchPanel panel = new ObjectSourceSearchPanel();
			panel.showWindow(parent);
		}
		finally
		{
			if (parent != null) WbSwingUtilities.showDefaultCursor(parent);
		}
	}

}
