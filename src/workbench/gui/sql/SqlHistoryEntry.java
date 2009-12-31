/*
 * SqlHistoryEntry.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
			this.cursorPos = len - 1;
		else
			this.cursorPos = pos;

		if (selStart < 0)
			this.selectionStart = 0;
		else
			this.selectionStart = selStart;

		if (selEnd > len)
			this.selectionEnd = len - 1;
		else
			this.selectionEnd = selEnd;
	}

	public SqlHistoryEntry(String sql)
	{
		this.setText(sql);
		this.cursorPos = -1;
		this.selectionStart = -1;
		this.selectionEnd = -1;
	}

	public String getText() { return this.text; }
	public int getCursorPosition() { return this.cursorPos; }
	public int getSelectionStart() { return this.selectionStart; }
	public int getSelectionEnd() { return this.selectionEnd; }

	public void applyTo(EditorPanel editor)
	{
		if (editor == null) return;
		try
		{
			editor.setText(this.text);
			editor.setCaretPosition(0);
			if (this.cursorPos > -1) 
			{
				editor.setCaretPosition(this.cursorPos);
				editor.scrollToCaret();
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

	public String toString()
	{
		return "{" + StringUtil.getMaxSubstring(this.text, 40) + ", Cursor=" + this.cursorPos + ", Selection=[" + this.selectionStart + "," + this.selectionEnd + "]}";
	}


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
