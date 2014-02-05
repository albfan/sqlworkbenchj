/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014 Thomas Kellerer.
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

package workbench.gui;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import workbench.gui.sql.SqlPanel;

import workbench.sql.NamedScriptLocation;

import workbench.util.CollectionUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class BookmarkManager
{

	// Maps the ID of a MainWindow to the IDs of each panel
	private Map<String, Map<String, List<NamedScriptLocation>>> bookmarks = new HashMap<String, Map<String, List<NamedScriptLocation>>>();

	private BookmarkManager()
	{
	}

	public static BookmarkManager getInstance()
	{
		return InstanceHolder.INSTANCE;
	}

	private static class InstanceHolder
	{
		private static final BookmarkManager INSTANCE = new BookmarkManager();
	}

	public void mainWindowClosed(String windowId)
	{
		bookmarks.remove(windowId);
	}

	public void addBookmarks(MainWindow win, SqlPanel panel, List<NamedScriptLocation> items)
	{
		if (win == null) return;
		if (panel == null) return;
		if (CollectionUtil.isEmpty(items)) return;
		addBookmarks(win.getWindowId(), panel.getId(), items);
	}

	public void addBookmarks(String windowId, String tabId, List<NamedScriptLocation> items)
	{
		Map<String, List<NamedScriptLocation>> windowItems = bookmarks.get(windowId);
		if (windowItems == null)
		{
			windowItems = new HashMap<String, List<NamedScriptLocation>>();
		}
		windowItems.put(tabId, items);
	}

	public Map<String, List<NamedScriptLocation>> getBookmarksForWindow(String windowId)
	{
		Map<String, List<NamedScriptLocation>> windowItems = bookmarks.get(windowId);
		if (windowItems == null) return Collections.emptyMap();
		return Collections.unmodifiableMap(windowItems);
	}

	public List<NamedScriptLocation> getBookmarksForTab(String windowId, String tabId)
	{
		Map<String, List<NamedScriptLocation>> windowItems = bookmarks.get(windowId);
		if (windowItems == null) return Collections.emptyList();
		List<NamedScriptLocation> items = windowItems.get(tabId);
		if (items == null) return Collections.emptyList();
		return Collections.unmodifiableList(items);
	}

	public void reset()
	{
		bookmarks.clear();
	}
}
