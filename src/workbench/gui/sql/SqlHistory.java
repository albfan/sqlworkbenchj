/*
 * SqlHistory.java
 *
 * Created on June 4, 2003, 6:26 PM
 */

package workbench.gui.sql;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import workbench.log.LogMgr;
import workbench.util.StringUtil;

/**
 *
 * @author  thomas
 */
public class SqlHistory
{
	private ArrayList history;
	private int currentEntry;
	private int maxSize;
	private boolean changed = false;

	public SqlHistory(int maxSize)
	{
		this.maxSize = maxSize;
		this.history = new ArrayList(maxSize + 2);
	}

	public void addContent(EditorPanel editor)
	{
		String text = editor.getText();
		if (text == null || text.length() == 0) return;

		try
		{
			SqlHistoryEntry entry = new SqlHistoryEntry(text, editor.getCaretPosition(), editor.getSelectionStart(), editor.getSelectionEnd());
			SqlHistoryEntry top = this.getTopEntry();
			if (top != null && top.equals(entry)) return;
			this.addEntry(entry);
		}
		catch (Exception e)
		{
			LogMgr.logError("SqlHistory.addContent(editor)", "Could not add entry", e);
		}
	}

	public void addEntry(SqlHistoryEntry entry)
	{
		this.history.add(entry);
		if (this.history.size() > this.maxSize)
		{
			this.history.remove(0);
		}
		this.currentEntry = this.history.size() - 1;
		this.changed = true;
	}

	public boolean hasNext()
	{
		return (this.currentEntry < (this.history.size() - 1));
	}

	public boolean hasPrevious()
	{
		return (this.currentEntry > 0);
	}

	public void clear()
	{
		this.currentEntry = 0;
		this.history.clear();
		this.changed = false;
	}

	public void showCurrent(EditorPanel editor)
	{
		if (this.currentEntry >= this.history.size()) return;
		SqlHistoryEntry entry = (SqlHistoryEntry)this.history.get(this.currentEntry);
		entry.applyTo(editor);
	}

	public void showPrevious(EditorPanel editor)
	{
		if (!this.hasPrevious()) return;
		SqlHistoryEntry entry = this.getPreviousEntry();
		entry.applyTo(editor);
	}

	public void showNext(EditorPanel editor)
	{
		if (!this.hasNext()) return;
		SqlHistoryEntry entry = this.getNextEntry();
		entry.applyTo(editor);
	}

	public SqlHistoryEntry getTopEntry()
	{
		if (this.history.size() < 1) return null;
		SqlHistoryEntry entry = (SqlHistoryEntry)this.history.get(this.history.size() - 1);
		return entry;
	}

	private SqlHistoryEntry getPreviousEntry()
	{
		if (this.currentEntry <= 0) return null;
		this.currentEntry--;
		SqlHistoryEntry entry = (SqlHistoryEntry)this.history.get(this.currentEntry);
		return entry;
	}

	private SqlHistoryEntry getNextEntry()
	{
		if (this.currentEntry >= this.history.size() - 1) return null;
		this.currentEntry++;
		SqlHistoryEntry entry = (SqlHistoryEntry)this.history.get(this.currentEntry);
		return entry;
	}

	private static final String KEY_POS = "##sqlwb.pos=";
	private static final String KEY_START = "##sqlwb.selStart=";
	private static final String KEY_END = "##sqlwb.selEnd=";

	public void writeToStream(OutputStream out)
	{
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
		try
		{
			int count = this.history.size();
			for (int i=0; i < count; i++)
			{
				SqlHistoryEntry entry = (SqlHistoryEntry)this.history.get(i);
				writer.write(KEY_POS);
				writer.write(Integer.toString(entry.getCursorPosition()));
				writer.write(StringUtil.LINE_TERMINATOR);

				writer.write(KEY_START);
				writer.write(Integer.toString(entry.getSelectionStart()));
				writer.write(StringUtil.LINE_TERMINATOR);

				writer.write(KEY_END);
				writer.write(Integer.toString(entry.getSelectionEnd()));
				writer.write(StringUtil.LINE_TERMINATOR);

				writer.write(entry.getText());
				writer.write(StringUtil.LINE_TERMINATOR);
				writer.write(StringUtil.LIST_DELIMITER);
				writer.write(StringUtil.LINE_TERMINATOR);
			}
			writer.flush();
			this.changed = false;
		}
		catch (IOException e)
		{
			LogMgr.logError("SqlHistory.writeToStream()", "Could not write history!", e);
		}
	}

	public boolean isChanged()
	{
		return this.changed;
	}
	
	public void readFromStream(InputStream in)
	{
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		StringBuffer content = new StringBuffer(500);
		int pos = 0;
		int start = -1;
		int end = -1;
		try
		{
			String line = reader.readLine();
			while(line != null)
			{
				if (line.equals(StringUtil.LIST_DELIMITER))
				{
					try
					{
						SqlHistoryEntry entry = new SqlHistoryEntry(content.toString(), pos, start, end);
						this.addEntry(entry);
						pos = 0;
						start = -1;
						end = -1;
						content = new StringBuffer(500);
					}
					catch (Exception e)
					{
						LogMgr.logError("SqlHistory.readFromStream()", "Error when creating SqlHistoryEntry", e);
					}
				}
				else if (line.startsWith(KEY_POS))
				{
					pos = StringUtil.getIntValue(line.substring(KEY_POS.length()), -1);
				}
				else if (line.startsWith(KEY_START))
				{
					start = StringUtil.getIntValue(line.substring(KEY_START.length()), -1);
				}
				else if (line.startsWith(KEY_END))
				{
					end = StringUtil.getIntValue(line.substring(KEY_END.length()), -1);
				}
				else
				{
					content.append(line);
					content.append('\n');
				}
				line = reader.readLine();
			}
			this.changed = false;
		}
		catch (IOException e)
		{
			LogMgr.logError("SqlHistory.readFromStream()", "Could not read history!", e);
		}
		if (content.length() > 0)
		{
			SqlHistoryEntry entry = new SqlHistoryEntry(content.toString(), pos, start, end);
			this.addEntry(entry);
		}
	}
}
