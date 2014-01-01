/*
 * RecentWorkspaceManager.java
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
package workbench.gui.menu;

import java.util.List;
import javax.swing.JMenu;
import workbench.gui.MainWindow;
import workbench.gui.actions.LoadWorkspaceFileAction;
import workbench.resource.ListItemProperty;
import workbench.resource.Settings;
import workbench.resource.SettingsListener;
import workbench.util.FixedSizeList;
import workbench.util.WbFile;

/**
 *
 * @author Thomas Kellerer
 */
public class RecentWorkspaceManager
	implements SettingsListener
{
	private final String PROP_NAME = "workspace.recent";
	private FixedSizeList<WbFile> workspaces;

	/**
	 * Thread safe singleton-instance
	 */
	protected static class LazyInstanceHolder
	{
		protected static final RecentWorkspaceManager instance = new RecentWorkspaceManager();
	}

	public static RecentWorkspaceManager getInstance()
	{
		return LazyInstanceHolder.instance;
	}

	private RecentWorkspaceManager()
	{
		Settings.getInstance().addSaveListener(this);
		workspaces = new FixedSizeList<WbFile>(Settings.getInstance().getIntProperty("workbench.workspace.recent.maxcount", 15));
		readSettings();
	}

	public void populateMenu(JMenu recentMenu, MainWindow window)
	{
		recentMenu.removeAll();

		if (workspaces.isEmpty())
		{
			recentMenu.setEnabled(false);
			return;
		}

		for (WbFile f : workspaces)
		{
			LoadWorkspaceFileAction load = new LoadWorkspaceFileAction(window, f);
			recentMenu.add(load);
		}
		recentMenu.setEnabled(true);
	}

	public final void readSettings()
	{
		ListItemProperty props = new ListItemProperty(PROP_NAME);
		List<String> fileNames = props.getItems();
		for (String name : fileNames)
		{
			WbFile f = new WbFile(name);
			if (f.exists())
			{
				workspaces.append(f);
			}
		}
	}

	public void saveSettings()
	{
		ListItemProperty props = new ListItemProperty(PROP_NAME);
		props.storeItems(workspaces);
	}

	@Override
	public void beforeSettingsSave()
	{
		saveSettings();
	}

	public void workspaceLoaded(WbFile workspace)
	{
		workspaces.addEntry(workspace);
	}

}
