/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.bookmarks;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import workbench.interfaces.MainPanel;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;

import workbench.gui.MainWindow;

import workbench.storage.DataStore;

import workbench.util.NumberStringCache;
import workbench.util.WbThread;

/**
 *
 * @author Thomas Kellerer
 */
public class BookmarkManager
{
	// Maps the ID of a MainWindow to bookmarks defined for each tab.
	// each BookmarkGroup represents the bookmarks from a single editor tab
	private final Map<String, Map<String, BookmarkGroup>> bookmarks = new HashMap<>();

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

	public synchronized void clearBookmarksForWindow(String windowId)
	{
		bookmarks.remove(windowId);
		LogMgr.logDebug("BookmarkManager.clearBookmarksForWindow()", "Removed all bookmarks for window: " + windowId);
	}

	public synchronized void clearBookmarksForPanel(String windowId, String panelId)
	{
		Map<String, BookmarkGroup> windowBookmarks = bookmarks.get(windowId);
		if (windowBookmarks != null)
		{
			windowBookmarks.remove(panelId);
		}
	}

	public void updateBookmarks(MainWindow win)
	{
		long start = System.currentTimeMillis();
		int count = -1;
		synchronized (this)
		{
			count = win.getTabCount();
			for (int i=0; i < count; i++)
			{
				MainPanel panel = win.getSqlPanel(i);
				if (panel.supportsBookmarks())
				{
					updateBookmarks(win, panel);
				}
			}
		}
		long end = System.currentTimeMillis();
		LogMgr.logDebug("BookmarkManager.updateBookmarks()", "Parsing bookmarks for " + count + " tabs took: " + (end - start) + "ms");
	}

	private BookmarkGroup updateBookmarks(MainWindow win, MainPanel panel)
	{
		BookmarkGroup updated = null;

		synchronized (this)
		{
			Map<String, BookmarkGroup> windowBookmarks = bookmarks.get(win.getWindowId());
			if (windowBookmarks == null)
			{
				windowBookmarks = new HashMap<>();
				bookmarks.put(win.getWindowId(), windowBookmarks);
			}

			final BookmarkGroup group = windowBookmarks.get(panel.getId());

			long modified = 0;

			if (group != null)
			{
				modified = group.creationTime();
			}

			if (group == null || panel.isModifiedAfter(modified))
			{
				List<NamedScriptLocation> panelBookmarks = panel.getBookmarks();
				// if getBoomarks() returns null, the panel does not support bookmarks
				// (this is essentially only the DbExplorerPanel)
				if (panelBookmarks != null)
				{
					updated = new BookmarkGroup(panelBookmarks, panel.getId());
					String title = panel.getTabTitle();
					if (GuiSettings.getShowTabIndex() && title.equals(ResourceMgr.getDefaultTabLabel()))
					{
						int index = win.getIndexForPanel(panel);
						title += " " + NumberStringCache.getNumberString(index + 1);
					}
					updated.setName(title);
					windowBookmarks.put(updated.getGroupId(), updated);
				}
			}
		}
		return updated;
	}

	public synchronized List<String> getTabs(MainWindow window)
	{
		String windowId = window == null ? "" : window.getWindowId();
		List<String> result = null;
		Map<String, BookmarkGroup> bm = bookmarks.get(windowId);
		if (bm == null) return Collections.emptyList();

		result = new ArrayList<>();
		for (Map.Entry<String, BookmarkGroup> entry : bm.entrySet())
		{
			String id = entry.getKey();
			BookmarkGroup group = entry.getValue();
			if (group.getBookmarks().size() > 0)
			{
				result.add(id);
			}
		}
		return result;
	}

	public DataStore getAllBookmarks(MainWindow window)
	{
		return getBookmarks(window, null);
	}

	public DataStore getBookmarksForTab(MainWindow window, String tabId)
	{
		return getBookmarks(window, tabId);
	}

	private synchronized DataStore getBookmarks(MainWindow window, String tabId)
	{
		DataStore result = createDataStore();

		String id = window == null ? "" : window.getWindowId();
		Map<String, BookmarkGroup> bm = bookmarks.get(id);

		if (bm == null) return result;

		for (BookmarkGroup group : bm.values())
		{
			if (tabId != null && !tabId.equals(group.getGroupId())) continue;

			List<NamedScriptLocation> locations = group.getBookmarks();
			for (NamedScriptLocation loc : locations)
			{
				int row = result.addRow();
				result.setValue(row, 0, loc.getName());
				result.setValue(row, 1, group.getName());
				result.setValue(row, 2, loc.getLineNumber());
				result.getRow(row).setUserObject(loc);
			}
		}

		return result;
	}

	private DataStore createDataStore()
	{
		String[] columns = new String[] { ResourceMgr.getPlainString("LblBookName"), ResourceMgr.getPlainString("LblBookPanel"), ResourceMgr.getPlainString("LblBookLine")};
		int[] types = new int[] { Types.VARCHAR, Types.VARCHAR, Types.INTEGER};

		DataStore ds =new DataStore(columns, types);
		return ds;
	}

	/**
	 * Updates the list of bookmarks for the passed panel.
	 *
	 * If the text in the panel hasn't changed since the bookmarks were parsed the last time, no parsing will take place.
	 *
	 * To force a re-parsing of the panel's bookmarks use the <tt>clearList</tt> parameter.
	 *
	 * @param win    the window to which the panel belongs
	 * @param panel  the panel
	 * @param clearList  if true any existing bookmarks will be deleted before updating the panel's bookmark
	 *
	 * @see #updateInBackground(workbench.gui.MainWindow, workbench.interfaces.MainPanel, boolean)
	 */
	public void updateInBackground(final MainWindow win, final MainPanel panel, boolean clearList)
	{
		if (win == null) return;
		if (panel == null) return;
		if (!panel.supportsBookmarks()) return;

		if (clearList)
		{
			clearBookmarksForPanel(win.getWindowId(), panel.getId());
		}

		WbThread bmThread = new WbThread("Update bookmarks for " + panel.getId())
		{
			@Override
			public void run()
			{
				long start = System.currentTimeMillis();
				BookmarkGroup updated = updateBookmarks(win, panel);
				long duration = System.currentTimeMillis() - start;
				if (updated == null)
				{
					LogMgr.logDebug("BookmarManager.updateInBackground()", "Panel '" + panel.getTabTitle() + "' was up to date");
				}
				else
				{
					LogMgr.logDebug("BookmarManager.updateInBackground()",
						"Panel '" + panel.getTabTitle() + "' was updated in "  + duration + "ms (" + updated.getBookmarks().size() + " bookmarks)");
				}

			}
		};
		bmThread.start();
	}

	/**
	 * Updates all bookmarks that are "dirty" in the background.
	 *
	 * @param win  the MainWindow for which the bookmarks should be updated.
	 */
	public void updateInBackground(final MainWindow win)
	{
		WbThread bmThread = new WbThread("Update bookmarks for all tabs")
		{
			@Override
			public void run()
			{
				updateBookmarks(win);
			}
		};
		bmThread.start();
	}

	public void reset()
	{
		bookmarks.clear();
	}
}
