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

package workbench.gui.bookmarks;

import java.util.Comparator;

/**
 *
 * @author Thomas Kellerer
 */
class TabEntry
{
	private final String id;
	private final String label;
	private final int index;

	static final Comparator<TabEntry> INDEX_SORTER = new Comparator<TabEntry>()
	{
		@Override
		public int compare(TabEntry o1, TabEntry o2)
		{
			return o1.index - o2.index;
		}
	};

	TabEntry(String tabId, String tabLabel, int tabIndex)
	{
		this.id = tabId;
		this.label = tabLabel;
		this.index = tabIndex;
	}

	String getId()
	{
		return id;
	}

	String getLabel()
	{
		return label;
	}

	int getIndex()
	{
		return index;
	}

	@Override
	public String toString()
	{
		if (index < 0)
		{
			return label;
		}
		return label + " - " + (index + 1);
	}

}
