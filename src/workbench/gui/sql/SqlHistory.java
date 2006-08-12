/*
 * SqlHistory.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
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
import workbench.gui.actions.ClearStatementHistoryAction;
import workbench.gui.actions.WbAction;

import workbench.log.LogMgr;
import workbench.util.StringUtil;
import java.io.UnsupportedEncodingException;
import workbench.gui.actions.FirstStatementAction;
import workbench.gui.actions.LastStatementAction;
import workbench.gui.actions.NextStatementAction;
import workbench.gui.actions.PrevStatementAction;

/**
 *
 * @author  support@sql-workbench.net
 */
public class SqlHistory
{
	private static final String LIST_DELIMITER = "----------- WbStatement -----------";

	private ArrayList history;
	private int currentEntry;
	private int maxSize;
	private boolean changed = false;
	private EditorPanel editor;
	private NextStatementAction nextStmtAction;
	private PrevStatementAction prevStmtAction;
	private FirstStatementAction firstStmtAction;
	private LastStatementAction lastStmtAction;
	private ClearStatementHistoryAction clearAction;

	public SqlHistory(EditorPanel ed, int maxSize)
	{
		this.maxSize = maxSize;
		this.history = new ArrayList(maxSize + 2);
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

	public WbAction getShowFirstStatementAction() { return this.firstStmtAction; }
	public WbAction getShowLastStatementAction() { return this.lastStmtAction; }
	public WbAction getShowNextStatementAction() { return this.nextStmtAction; }
	public WbAction getShowPreviousStatementAction() { return this.prevStmtAction; }
	public WbAction getClearHistoryAction() { return this.clearAction; }
	
	public synchronized void addContent(EditorPanel editor)
	{
		String text = editor.getText();
		if (text == null || text.length() == 0) return;

		try
		{
			SqlHistoryEntry entry = null;
			if (editor.currentSelectionIsTemporary())
			{
				entry = new SqlHistoryEntry(text, editor.getCaretPosition(), 0, 0);
			}
			else
			{
				entry = new SqlHistoryEntry(text, editor.getCaretPosition(), editor.getSelectionStart(), editor.getSelectionEnd());
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

	public void showLastStatement()
	{
		if (this.history.size() == 0) return;
		this.currentEntry = this.history.size() - 1;
		SqlHistoryEntry entry = (SqlHistoryEntry)this.history.get(this.currentEntry);
		entry.applyTo(editor);
		checkActions();
	}

	public void showFirstStatement()
	{
		if (this.history.size() == 0) return;
		this.currentEntry = 0;
		SqlHistoryEntry entry = (SqlHistoryEntry)this.history.get(this.currentEntry);
		entry.applyTo(editor);
		checkActions();
	}

	public void showCurrent()
	{
		if (this.currentEntry >= this.history.size()) return;
		SqlHistoryEntry entry = (SqlHistoryEntry)this.history.get(this.currentEntry);
		entry.applyTo(editor);
		checkActions();
	}

	public void showPreviousStatement()
	{
		if (!this.hasPrevious()) return;
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
		BufferedWriter writer = null;
		try
		{
			writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
		}
		catch (UnsupportedEncodingException e)
		{
			// cannot happen!
		}
		try
		{
			int count = this.history.size();
			for (int i=0; i < count; i++)
			{
				SqlHistoryEntry entry = (SqlHistoryEntry)this.history.get(i);
				writer.write(KEY_POS);
				writer.write(Integer.toString(entry.getCursorPosition()));
				writer.write('\n');

				writer.write(KEY_START);
				writer.write(Integer.toString(entry.getSelectionStart()));
				writer.write('\n');

				writer.write(KEY_END);
				writer.write(Integer.toString(entry.getSelectionEnd()));
				writer.write('\n');

				writer.write(entry.getText());
				writer.write('\n');
				writer.write(LIST_DELIMITER);
				writer.write('\n');
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
		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
		}
		catch (UnsupportedEncodingException e)
		{
			// cannot happen!
		}

		StringBuffer content = new StringBuffer(500);
		int pos = 0;
		int start = -1;
		int end = -1;
		try
		{
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

	private void checkActions()
	{
		this.nextStmtAction.setEnabled(this.hasNext());
		this.lastStmtAction.setEnabled(this.hasNext());
		this.prevStmtAction.setEnabled(this.hasPrevious());
		this.firstStmtAction.setEnabled(this.hasPrevious());
		this.clearAction.setEnabled(this.history.size() > 0);
	}
}
