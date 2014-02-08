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

package workbench.gui.bookmarks;

/**
 *
 * @author Thomas Kellerer
 */
public class NamedScriptLocation
{
	private final String name;
	private final int offset;
	private final String tabId;
	private int lineNumber;

	public NamedScriptLocation(String bookMark, int position, String panelId)
	{
		this.name = bookMark;
		this.offset = position;
		this.tabId = panelId;
	}

	public int getLineNumber()
	{
		return lineNumber;
	}

	public void setLineNumber(int line)
	{
		this.lineNumber = line;
	}

	public String getTabId()
	{
		return tabId;
	}

	public String getName()
	{
		return name;
	}

	public int getOffset()
	{
		return offset;
	}

	@Override
	public String toString()
	{
		return "{Bookmark: " + name + ", offset=" + offset + '}';
	}
}
