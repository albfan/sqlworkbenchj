/*
 * RecentFileManager.java
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
package workbench.gui.menu;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JMenu;

import workbench.resource.ListItemProperty;
import workbench.resource.Settings;
import workbench.resource.SettingsListener;

import workbench.gui.MainWindow;
import workbench.gui.actions.LoadMacroFileAction;
import workbench.gui.actions.LoadWorkspaceFileAction;

import workbench.util.FixedSizeList;
import workbench.util.WbFile;

/**
 *
 * @author Thomas Kellerer
 */
public class RecentFileManager
	implements SettingsListener
{
	private final String WKSP_PROP_NAME = "workspace.recent";
	private final String MACROS_PROP_NAME = "macros.recent";

	private Map<String, FixedSizeList<WbFile>> recentFiles = new HashMap<>();

	/**
	 * Thread safe singleton-instance
	 */
	protected static class LazyInstanceHolder
	{
		protected static final RecentFileManager instance = new RecentFileManager();
	}

	public static RecentFileManager getInstance()
	{
		return LazyInstanceHolder.instance;
	}

	private RecentFileManager()
	{
		Settings.getInstance().addSaveListener(this);

		FixedSizeList<WbFile> wksp = new FixedSizeList<>(Settings.getInstance().getIntProperty("workbench.workspace.recent.maxcount", 15));
		FixedSizeList<WbFile> macros = new FixedSizeList<>(Settings.getInstance().getIntProperty("workbench.macros.recent.maxcount", 15));
		recentFiles.put(WKSP_PROP_NAME, wksp);
		recentFiles.put(MACROS_PROP_NAME, macros);
		readSettings(WKSP_PROP_NAME);
		readSettings(MACROS_PROP_NAME);
	}

	public void populateRecentWorkspaceMenu(JMenu recentMenu, MainWindow window)
	{
		recentMenu.removeAll();

		FixedSizeList<WbFile> workspaces = recentFiles.get(WKSP_PROP_NAME);

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

	public void populateRecentMacrosMenu(int clientId, JMenu recentMenu)
	{
		recentMenu.removeAll();

		FixedSizeList<WbFile> files = recentFiles.get(MACROS_PROP_NAME);

		if (files.isEmpty())
		{
			recentMenu.setEnabled(false);
			return;
		}

		for (WbFile f : files)
		{
			LoadMacroFileAction load = new LoadMacroFileAction(clientId, f);
			recentMenu.add(load);
		}
		recentMenu.setEnabled(true);
	}

	private void readSettings(String prop)
	{
		FixedSizeList<WbFile> fileList = recentFiles.get(prop);
		ListItemProperty props = new ListItemProperty(prop);
		List<String> fileNames = props.getItems();
		for (String name : fileNames)
		{
			WbFile f = new WbFile(name);
			if (f.exists())
			{
				fileList.append(f);
			}
		}
	}

	public void saveSettings()
	{
		ListItemProperty props = new ListItemProperty(WKSP_PROP_NAME);
		props.storeItems(getRecentWorkspaces());

		props = new ListItemProperty(MACROS_PROP_NAME);
		props.storeItems(getRecentMacroFiles());

	}

	@Override
	public void beforeSettingsSave()
	{
		saveSettings();
	}

	private FixedSizeList<WbFile> getRecentMacroFiles()
	{
		return recentFiles.get(MACROS_PROP_NAME);
	}

	private FixedSizeList<WbFile> getRecentWorkspaces()
	{
		return recentFiles.get(WKSP_PROP_NAME);
	}

	public void workspaceLoaded(WbFile workspace)
	{
		getRecentWorkspaces().addEntry(workspace);
	}

	public void macrosLoaded(WbFile macroFile)
	{
		getRecentMacroFiles().addEntry(macroFile);
	}
}
