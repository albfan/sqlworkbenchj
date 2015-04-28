/*
 * SqlHistoryEntry.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
package workbench.gui.sql;

import workbench.log.LogMgr;

import workbench.util.StringUtil;

/**
 * A single entry for {@link workbench.gui.sql.SqlHistory}
 *
 * @author Thomas Kellerer
 */
public class SqlHistoryEntry
{
	private String text;
	private int cursorPos;
	private int selectionStart;
	private int selectionEnd;

	public SqlHistoryEntry(String sql, int pos, int selStart, int selEnd)
	{
		this.setText(sql);
		int len = this.text.length();
		if (pos > len)
		{
			this.cursorPos = len - 1;
		}
		else if (pos < 0)
		{
			this.cursorPos = 0;
		}
		else
		{
			this.cursorPos = pos;
		}

		if (selStart < 0)
		{
			this.selectionStart = 0;
		}
		else
		{
			this.selectionStart = selStart;
		}

		if (selEnd > len)
		{
			this.selectionEnd = len - 1;
		}
		else
		{
			this.selectionEnd = selEnd;
		}
	}

	public SqlHistoryEntry(String sql)
	{
		this.setText(sql);
		this.cursorPos = -1;
		this.selectionStart = -1;
		this.selectionEnd = -1;
	}

	public String getText()
	{
		return this.text;
	}

	public int getCursorPosition()
	{
		return this.cursorPos;
	}

	public int getSelectionStart()
	{
		return this.selectionStart;
	}

	public int getSelectionEnd()
	{
		return this.selectionEnd;
	}

	public void applyTo(EditorPanel editor)
	{
		if (editor == null) return;
		try
		{
			editor.setText(this.text);
			if (this.cursorPos > -1)
			{
        editor.setCaretPosition(cursorPos);
        editor.centerLine(editor.getCaretLine());
			}
      else
      {
        editor.setCaretPosition(0);
      }
			if (this.selectionStart > -1 && this.selectionEnd > this.selectionStart && this.selectionEnd < editor.getDocumentLength())
			{
				editor.select(this.selectionStart, this.selectionEnd);
			}
		}
		catch (Exception e)
		{
			LogMgr.logWarning("SqlHistoryEntry.applyTo()", "Error applying " + this.toString(), e);
		}
	}

	@Override
	public String toString()
	{
		return "{" + StringUtil.getMaxSubstring(this.text, 40) + ", Cursor=" + this.cursorPos + ", Selection=[" + this.selectionStart + "," + this.selectionEnd + "]}";
	}

	@Override
	public int hashCode()
	{
		int hash = 3;
		hash = 97 * hash + (this.text != null ? this.text.hashCode() : 0);
		hash = 97 * hash + this.cursorPos;
		hash = 97 * hash + this.selectionStart;
		hash = 97 * hash + this.selectionEnd;
		return hash;
	}

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof SqlHistoryEntry)) return false;
		SqlHistoryEntry other = (SqlHistoryEntry)o;
		if (this.text.equals(other.text))
		{
			return (this.cursorPos == other.cursorPos &&
				      this.selectionEnd == other.selectionEnd &&
						  this.selectionStart == other.selectionStart);
		}
		else
		{
			return false;
		}
	}

	private void setText(String value)
	{
		if (value == null)
		{
			this.text = "";
		}
		else
		{
			this.text = value;
		}
	}
}
