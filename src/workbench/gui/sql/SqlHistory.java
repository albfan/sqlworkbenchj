/*
 * SqlHistory.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.gui.actions.ClearStatementHistoryAction;
import workbench.gui.actions.FirstStatementAction;
import workbench.gui.actions.LastStatementAction;
import workbench.gui.actions.NextStatementAction;
import workbench.gui.actions.PrevStatementAction;
import workbench.gui.actions.WbAction;

import workbench.util.EncodingUtil;
import workbench.util.StringUtil;

/**
 * Stores the SQL scripts entered in the {@link SqlPanel} and manages
 * a history of statements.
 *
 * @author  Thomas Kellerer
 */
public class SqlHistory
{
	private static final String LIST_DELIMITER = "----------- WbStatement -----------";

	final private List<SqlHistoryEntry> history;
	private int currentEntry;
	private int maxSize;
	private boolean changed;
	private EditorPanel editor;
	private NextStatementAction nextStmtAction;
	private PrevStatementAction prevStmtAction;
	private FirstStatementAction firstStmtAction;
	private LastStatementAction lastStmtAction;
	private ClearStatementHistoryAction clearAction;

	public SqlHistory(EditorPanel ed, int size)
	{
		this.maxSize = size;
		this.history = new ArrayList<>(size + 2);
		this.editor = ed;
		this.firstStmtAction = new FirstStatementAction(this);
		this.firstStmtAction.setEnabled(false);

		this.prevStmtAction = new PrevStatementAction(this);
		this.prevStmtAction.setEnabled(false);

		this.nextStmtAction = new NextStatementAction(this);
		this.nextStmtAction.setEnabled(false);

		this.lastStmtAction = new LastStatementAction(this);
		this.lastStmtAction.setEnabled(false);

		this.clearAction = new ClearStatementHistoryAction(this);
		this.clearAction.setEnabled(false);
	}

	public synchronized void setEnabled(boolean flag)
	{
		nextStmtAction.setEnabled(flag);
		prevStmtAction.setEnabled(flag);
		firstStmtAction.setEnabled(flag);
		lastStmtAction.setEnabled(flag);
	}

	public WbAction getShowFirstStatementAction() { return this.firstStmtAction; }
	public WbAction getShowLastStatementAction() { return this.lastStmtAction; }
	public WbAction getShowNextStatementAction() { return this.nextStmtAction; }
	public WbAction getShowPreviousStatementAction() { return this.prevStmtAction; }
	public WbAction getClearHistoryAction() { return this.clearAction; }

	public synchronized void addContent(EditorPanel edit)
	{
		boolean includeFiles = Settings.getInstance().getStoreFilesInHistory();
		if (!includeFiles && edit.hasFileLoaded()) return;

		int maxLength = Settings.getInstance().getIntProperty("workbench.sql.history.maxtextlength", 1024*1024*10);
		if (edit.getDocumentLength() > maxLength) return;

		String text = edit.getText();
		if (text == null || text.length() == 0) return;

		try
		{
			SqlHistoryEntry entry = null;
			if (edit.currentSelectionIsTemporary())
			{
				entry = new SqlHistoryEntry(text, edit.getCaretPosition(), 0, 0);
			}
			else
			{
				entry = new SqlHistoryEntry(text, edit.getCaretPosition(), edit.getSelectionStart(), edit.getSelectionEnd());
			}

			SqlHistoryEntry top = this.getTopEntry();
			if (top != null && top.equals(entry)) return;
			this.addEntry(entry);
		}
		catch (Exception e)
		{
			LogMgr.logError("SqlHistory.addContent(editor)", "Could not add entry", e);
		}
		checkActions();
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
		this.checkActions();
	}

	public void dispose()
	{
		clear();
		WbAction.dispose(nextStmtAction, prevStmtAction, firstStmtAction, lastStmtAction, clearAction);
	}

	public void showLastStatement()
	{
		if (this.history.isEmpty()) return;
		if (!editor.isEditable()) return;
		this.currentEntry = this.history.size() - 1;
		SqlHistoryEntry entry = this.history.get(this.currentEntry);
		entry.applyTo(editor);
		checkActions();
	}

	public void showFirstStatement()
	{
		if (this.history.isEmpty()) return;
		if (!editor.isEditable()) return;
		this.currentEntry = 0;
		SqlHistoryEntry entry = this.history.get(this.currentEntry);
		entry.applyTo(editor);
		checkActions();
	}

	public void showCurrent()
  {
		if (this.currentEntry >= this.history.size()) return;
		if (!editor.isEditable()) return;
		SqlHistoryEntry entry = this.history.get(this.currentEntry);
		entry.applyTo(editor, true);
		checkActions();
	}

	public void showPreviousStatement()
	{
		if (!this.hasPrevious()) return;
		if (!editor.isEditable()) return;
		SqlHistoryEntry entry = this.getPreviousEntry();
		entry.applyTo(editor);
		checkActions();
	}

	public void showNextStatement()
	{
		if (!this.hasNext()) return;
		SqlHistoryEntry entry = this.getNextEntry();
		entry.applyTo(editor);
		checkActions();
	}

	public SqlHistoryEntry getTopEntry()
	{
		if (this.history.size() < 1) return null;
		SqlHistoryEntry entry = this.history.get(this.history.size() - 1);
		return entry;
	}

	private SqlHistoryEntry getPreviousEntry()
	{
		if (this.currentEntry <= 0) return null;
		this.currentEntry--;
		SqlHistoryEntry entry = this.history.get(this.currentEntry);
		return entry;
	}

	private SqlHistoryEntry getNextEntry()
	{
		if (this.currentEntry >= this.history.size() - 1) return null;
		this.currentEntry++;
		SqlHistoryEntry entry = this.history.get(this.currentEntry);
		return entry;
	}

	private static final String KEY_POS = "##sqlwb.pos=";
	private static final String KEY_START = "##sqlwb.selStart=";
	private static final String KEY_END = "##sqlwb.selEnd=";

	public void writeToStream(OutputStream out)
	{

		String lineEnding = "\n";
		try
		{
			Writer writer = EncodingUtil.createWriter(out, "UTF-8");

			int count = this.history.size();
			for (int i=0; i < count; i++)
			{
				SqlHistoryEntry entry = this.history.get(i);
				writer.write(KEY_POS);
				writer.write(Integer.toString(entry.getCursorPosition()));
				writer.write(lineEnding);

				writer.write(KEY_START);
				writer.write(Integer.toString(entry.getSelectionStart()));
				writer.write(lineEnding);

				writer.write(KEY_END);
				writer.write(Integer.toString(entry.getSelectionEnd()));
				writer.write(lineEnding);

				// Make sure the editor text is converted to the correct line ending
				BufferedReader reader = new BufferedReader(new StringReader(entry.getText()));
				String line = reader.readLine();
				while(line != null)
				{
					int len = StringUtil.getRealLineLength(line);
					if (len > 0)
					{
						writer.write(line.substring(0,len));
					}
					writer.write(lineEnding);
					line = reader.readLine();
				}

				//writer.write(lineEnding);
				writer.write(LIST_DELIMITER);
				writer.write(lineEnding);
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
		StringBuilder content = new StringBuilder(500);
		int pos = 0;
		int start = -1;
		int end = -1;

		String lineEnding = "\n";
		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(EncodingUtil.createReader(in , "UTF-8"));
			String line = reader.readLine();
			while(line != null)
			{
				if (line.equals(LIST_DELIMITER))
				{
					try
					{
						SqlHistoryEntry entry = new SqlHistoryEntry(content.toString(), pos, start, end);
						this.addEntry(entry);
						pos = 0;
						start = -1;
						end = -1;
						content = new StringBuilder(500);
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
					int len = StringUtil.getRealLineLength(line);
					if (len > 0)
					{
						content.append(line, 0, len);
					}
					content.append(lineEnding);
				}
				line = reader.readLine();
			}
			this.changed = false;
		}
		catch (IOException e)
		{
			LogMgr.logError("SqlHistory.readFromStream()", "Could not read history!", e);
		}
		finally
		{
			try { reader.close(); } catch (Throwable th) {}
		}

		if (content.length() > 0)
		{
			SqlHistoryEntry entry = new SqlHistoryEntry(content.toString(), pos, start, end);
			this.addEntry(entry);
		}
	}

	private void checkActions()
	{
		this.nextStmtAction.setEnabled(this.hasNext());
		this.lastStmtAction.setEnabled(this.hasNext());
		this.prevStmtAction.setEnabled(this.hasPrevious());
		this.firstStmtAction.setEnabled(this.hasPrevious());
		this.clearAction.setEnabled(this.history.size() > 0);
	}
}
