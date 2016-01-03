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

import java.util.List;

/**
 *
 * @author Thomas Kellerer
 */
class BookmarkGroup
{
	private final List<NamedScriptLocation> bookmarks;
	private final String groupId;
	private final long createdAt;
	private String groupName;

	BookmarkGroup(List<NamedScriptLocation> bookmarkList, String panelId)
	{
		this.bookmarks = bookmarkList;
		this.groupId = panelId;
		this.createdAt = System.currentTimeMillis();
	}

	public String getName()
	{
		return groupName;
	}

	public void setName(String name)
	{
		this.groupName = name;
	}

	public List<NamedScriptLocation> getBookmarks()
	{
		return bookmarks;
	}

	public String getGroupId()
	{
		return groupId;
	}

	public long creationTime()
	{
		return this.createdAt;
	}

	@Override
	public int hashCode()
	{
		int hash = 5;
		hash = 79 * hash + (this.groupId != null ? this.groupId.hashCode() : 0);
		return hash;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof BookmarkGroup)
		{
			BookmarkGroup other = (BookmarkGroup) obj;
			return this.groupId.equals(other.groupId);
		}
		return false;
	}

	@Override
	public String toString()
	{
		return "Group: " + this.groupName + ", " + this.bookmarks.size() + " bookmarks";
	}
}
