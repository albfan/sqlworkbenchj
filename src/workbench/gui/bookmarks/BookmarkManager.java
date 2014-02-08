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
 *//*
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
 *//*
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
 *//*
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

package workbench.gui.bookmarks;

import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import workbench.interfaces.MainPanel;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.gui.MainWindow;

import workbench.storage.DataStore;

import workbench.util.WbThread;

/**
 *
 * @author Thomas Kellerer
 */
public class BookmarkManager
{

	// Maps the ID of a MainWindow to bookmarks defined for each tab.
	// each BookmarkGroup represents the bookmarks from a single editor tab
	private final Map<String, Map<String, BookmarkGroup>> bookmarks = new HashMap<String, Map<String, BookmarkGroup>>();

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
	}

	public synchronized void clearBookmarksForPanel(String windowId, String panelId)
	{
		Map<String, BookmarkGroup> windowBookmarks = bookmarks.get(windowId);
		if (windowBookmarks != null)
		{
			windowBookmarks.remove(panelId);
		}
	}

	public synchronized void updateBookmarks(MainWindow win)
	{
		if (win == null) return;
		long start = System.currentTimeMillis();
		int count = win.getTabCount();
		for (int i=0; i < count; i++)
		{
			MainPanel panel = win.getSqlPanel(i);
			updateBookmarks(win, panel);
		}
		long end = System.currentTimeMillis();
		LogMgr.logDebug("BookmarkManager.updateBookmarks()", "Parsing bookmarks for all tabs took: " + (end - start) + "ms");
	}

	public synchronized void updateBookmarks(MainWindow win, MainPanel panel)
	{
		Map<String, BookmarkGroup> windowBookmarks = bookmarks.get(win.getWindowId());
		if (windowBookmarks == null)
		{
			windowBookmarks = new HashMap<String, BookmarkGroup>();
			bookmarks.put(win.getWindowId(), windowBookmarks);
		}

		BookmarkGroup group = windowBookmarks.get(panel.getId());

		long modified = 0;

		if (group != null)
		{
			modified = group.creationTime();
		}

		if (group == null || panel.isModifiedAfter(modified))
		{
			List<NamedScriptLocation> panelBookmarks = panel.getBookmarks();
			BookmarkGroup pGroup = new BookmarkGroup(panelBookmarks, panel.getId());
			int index = win.getIndexForPanel(panel);
			pGroup.setName(panel.getTabTitle() + " " + (index + 1));
			windowBookmarks.put(pGroup.getGroupId(), pGroup);
		}
	}

	public DataStore getBookmarks(MainWindow window)
	{
		DataStore result = createDataStore();

		String id = window == null ? "" : window.getWindowId();
		Map<String, BookmarkGroup> bm = bookmarks.get(id);
		if (bm != null)
		{
			for (BookmarkGroup group : bm.values())
			{
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
		}
		return result;
	}

	private DataStore createDataStore()
	{
		String[] columns = new String[] { ResourceMgr.getString("LblBookName"), ResourceMgr.getString("LblBookPanel"), ResourceMgr.getString("LblBookLine") };
		int[] types = new int[] { Types.VARCHAR, Types.VARCHAR, Types.INTEGER };

		DataStore ds =new DataStore(columns, types);
		return ds;
	}

	public void updateInBackground(final MainWindow win, final MainPanel panel)
	{
		if (win == null) return;
		if (panel == null) return;

		WbThread bmThread = new WbThread("Bookmark parser")
		{
			@Override
			public void run()
			{
				long start = System.currentTimeMillis();
				BookmarkManager.getInstance().updateBookmarks(win, panel);
				long duration = System.currentTimeMillis() - start;
				LogMgr.logDebug("BookmarManager.updateTabBookmarks()", "Updating bookmark for panel: " + panel.getTabTitle() + " took "  + duration + "ms");
			}
		};
		bmThread.setPriority(Thread.MIN_PRIORITY);
		bmThread.start();
	}

	public void updateInBackground(final MainWindow win)
	{
		WbThread bmThread = new WbThread("Bookmark parser")
		{
			@Override
			public void run()
			{
				updateBookmarks(win);
			}
		};
		bmThread.setPriority(Thread.MIN_PRIORITY);
		bmThread.start();
	}

	public void reset()
	{
		bookmarks.clear();
	}
}
