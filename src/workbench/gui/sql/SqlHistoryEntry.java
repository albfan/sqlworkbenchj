/*
 * SqlHistoryEntry.java
 *
 * Created on June 4, 2003, 6:27 PM
 */

package workbench.gui.sql;

import workbench.log.LogMgr;

/**
 *
 * @author  thomas
 */
public class SqlHistoryEntry
{

	private String text;
	private int cursorPos;
	private int selectionStart;
	private int selectionEnd;
	
	//private static final Pattern PATTERN_EMPTY_LINE = Pattern.compile("$(\r\n|\n\r|\r|\n)");

	public SqlHistoryEntry(String sql, int pos, int selStart, int selEnd)
	{
		this.text = this.trimEmptyLines(sql);
		int len = this.text.length();
		if (pos >= len) 
			this.cursorPos = len - 1;
		else
			this.cursorPos = pos;
		this.selectionStart = selStart;
		
		if (selStart < 0)
			this.selectionStart = 0;
		else
			this.selectionStart = selStart;
			
		if (selEnd > len)
			this.selectionEnd = len;
		else
			this.selectionEnd = selEnd;
	}

	public SqlHistoryEntry(String sql)
	{
		this.text = this.trimEmptyLines(sql);
		this.cursorPos = -1;
		this.selectionStart = -1;
		this.selectionEnd = -1;
	}

	//public String toString() { return this.text; }
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
			if (this.cursorPos > -1) editor.setCaretPosition(this.cursorPos);
			if (this.selectionStart > -1 && this.selectionEnd > this.selectionStart && this.selectionStart < editor.getCaretPosition())
			{
				editor.setSelectionStart(this.selectionStart);
				editor.setSelectionEnd(this.selectionEnd);
			}
		}
		catch (Exception e)
		{
			LogMgr.logWarning("SqlHistoryEntry.applyTo()", "Error applying " + this.toString(), e);
		}
	}
	
	public String toString() 
	{
		return "{" + this.text.substring(0, 10) + "..., Cursor=" + this.cursorPos + ",Selection=[" + this.selectionStart + "," + this.selectionEnd + "] }";
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
	
	private static String trimEmptyLines(String input)
	{
		if (input == null) return null;
		int len = input.length() - 1;
		if (len <= 0) return null;
		
		char c = input.charAt(len);
		while ( (c == '\r' || c == '\n') && len > 0)
		{
			len --;
			c = input.charAt(len);
		}
		return input.substring(0, len + 1);
	}
	
}
