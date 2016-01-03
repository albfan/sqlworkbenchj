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

package workbench.gui.editor;

/**
 *
 * @author Thomas Kellerer
 */
public class SearchResult
{
	private final String lineText;
	private final int offset;
  private final int matchLength;
	private String tabId;
	private final int lineNumber;
  private final int startInLine;

	public SearchResult(String line, int textPosition, int length, int lineNr, int posInLine)
	{
		this.lineText = line;
		this.offset = textPosition;
    this.matchLength = length;
    this.lineNumber = lineNr;
    this.startInLine = posInLine;
	}

  public void setTabId(String id)
  {
    tabId = id;
  }

	public int getLineNumber()
	{
		return lineNumber;
	}

	public String getTabId()
	{
		return tabId;
	}

	public String getLineText()
	{
		return lineText;
	}

  public int getStartInLine()
  {
    return startInLine;
  }

	public int getOffset()
	{
		return offset;
	}

  public int getMatchLength()
  {
    return matchLength;
  }

}
