/*
 * CompletionHandler.java
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
package workbench.gui.completion;
import java.awt.Color;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import workbench.interfaces.StatusBar;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.WbConnection;

import workbench.gui.editor.JEditTextArea;

import workbench.sql.parser.ParserType;
import workbench.sql.parser.ScriptParser;

import workbench.util.CollectionUtil;
import workbench.util.SqlParsingUtil;
import workbench.util.StringUtil;
import workbench.util.WbThread;

/**
 * Handle the auto completion for tables and columns
 * @author  Thomas Kellerer
 */
public class CompletionHandler
	implements ListModel
{
	private JEditTextArea editor;
	protected List elements = Collections.EMPTY_LIST;
	protected List filteredElements;

	protected WbConnection dbConnection;
	private JLabel header;
	private List<ListDataListener> listeners;
	private CompletionPopup window;
	protected StatusBar statusBar;
	private String currentWord;
	private boolean highlightNotNulls;

	public CompletionHandler()
	{
		header = new JLabel(ResourceMgr.getString("LblCompletionListTables"));
		header.setForeground(Color.BLUE);
		header.setBorder(BorderFactory.createEmptyBorder(0,2,0,2));
	}

	public void setStatusBar(StatusBar bar)
	{
		this.statusBar = bar;
	}

	public void setEditor(JEditTextArea ed)
	{
		this.editor = ed;
	}

	public void setConnection(WbConnection conn)
	{
		this.dbConnection = conn;
	}

	protected void showPopup()
	{
		filteredElements = null;
		try
		{
			statusBar.setStatusMessage(ResourceMgr.getString("MsgCompletionRetrievingObjects"));
			if (this.updateSelectionList())
			{
				this.window.showPopup(currentWord, highlightNotNulls);
			}
		}
		catch (Throwable th)
		{
			LogMgr.logError("CompletionHandler.showPopup()", "Error retrieving completion objects", th);
			statusBar.clearStatusMessage();
		}
	}

	public void cancelPopup()
	{
		if (this.window != null) this.window.cancelPopup();
	}

	public void showCompletionPopup()
	{
		if (this.window == null)
		{
			this.window = new CompletionPopup(editor, header, this);
		}

		// if this is not done in a separate thread
		// the status bar will not be updated...
		WbThread t = new WbThread("Completion")
		{
			@Override
			public void run()
			{
				showPopup();
			}
		};
		t.start();
	}

	private boolean updateSelectionList()
	{
		boolean result = false;
		highlightNotNulls = false;
		String script = this.editor.getText();
		ScriptParser parser = new ScriptParser(script, ParserType.getTypeFromConnection(dbConnection));
		parser.setCheckEscapedQuotes(Settings.getInstance().getCheckEscapedQuotes());
		parser.setEmptyLineIsDelimiter(Settings.getInstance().getEmptyLineIsDelimiter());
		parser.setAlternateDelimiter(dbConnection.getAlternateDelimiter());
		int cursorPos = this.editor.getCaretPosition();

		int index = parser.getCommandIndexAtCursorPos(cursorPos);
		int commandCursorPos = parser.getIndexInCommand(index, cursorPos);
		String sql = parser.getCommand(index, false);

		if (sql == null)
		{
			LogMgr.logWarning("CompletionHandler.updateSelectionList()", "No SQL found!");
			showNoObjectsFoundMessage();
			return false;
		}

		try
		{
			StatementContext ctx = new StatementContext(this.dbConnection, sql, commandCursorPos);

			if (ctx.isStatementSupported())
			{
				currentWord = editor.getWordLeftOfCursor(ctx.getAnalyzer().getWordDelimiters());
				boolean selectWord = (ctx.getAnalyzer().getOverwriteCurrentWord() && StringUtil.isNonBlank(currentWord));
				BaseAnalyzer analyzer = ctx.getAnalyzer();
				if (analyzer != null && StringUtil.isNonBlank(currentWord) && analyzer.isWbParam() && currentWord.charAt(0) == '-')
				{
					currentWord = currentWord.substring(1);
				}
				window.selectCurrentWordInEditor(selectWord);

				this.elements = ctx.getData();

				LogMgr.logDebug("CompletionHandler.updateSelectionList()",
					"Auto-completion invoked for " + analyzer.getSqlVerb() +
						", analyzer: " + analyzer.getClass().getSimpleName() +
						", context: " + analyzer.contextToString() +
						", currentSchema: " + analyzer.getSchemaForTableList() +
						", element count: " + elements.size());

				this.header.setText(ctx.getTitle());
				this.window.setContext(ctx);

				Set<String> dml = CollectionUtil.caseInsensitiveSet("insert", "update", "merge");
				highlightNotNulls = dml.contains(analyzer.getSqlVerb());

				result = getSize() > 0;
				if (result)
				{
					statusBar.clearStatusMessage();
					fireDataChanged();
				}
				else
				{
					showNoObjectsFoundMessage();
				}
			}
			else
			{
				Toolkit.getDefaultToolkit().beep();
				showFailedMessage(sql);
				result = false;
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("CompletionHandler.updateSelectionList()", "Error retrieving objects", e);
			result = false;
			showNoObjectsFoundMessage();
		}
		return result;
	}

	public void resetFilter()
	{
		filteredElements =null;
		fireDataChanged();
	}

	public synchronized int filterElements(String value)
	{
		if (StringUtil.isBlank(value)) return 0;
		filteredElements = null;
		if (getSize() == 0) return 0;

		try
		{
			boolean partialMatch = GuiSettings.getPartialCompletionSearch();
			List filter = new ArrayList(getSize());
			value = value.toLowerCase();
			for (int i=0; i < getSize(); i++)
			{
				Object o = elements.get(i);
				if (o == null) continue;
				String element = o.toString().toLowerCase();
				if (partialMatch)
				{
					if (element.contains(value)) filter.add(o);
				}
				else
				{
					if (element.startsWith(value)) filter.add(o);
				}
			}
			if (filter.size() > 0)
			{
				filteredElements = filter;
			}
			fireDataChanged();
			return getSize();
		}
		catch (Exception e)
		{
			LogMgr.logError("CompletionHandler.filterElements()", "Error when applying filter", e);
			return -1;
		}
	}

	private synchronized List getElementList()
	{
		if (filteredElements != null)
		{
			return filteredElements;
		}
		return elements == null ? Collections.emptyList() : elements;
	}

	private void showNoObjectsFoundMessage()
	{
		String msg = ResourceMgr.getString("MsgCompletionNothingFound");
		statusBar.setStatusMessage(msg, 2500);
	}

	private void showFailedMessage(String sql)
	{
		String verb = SqlParsingUtil.getInstance(dbConnection).getSqlVerb(sql);
		String msg = "'" + verb + "' " + ResourceMgr.getString("MsgCompletionNotSupported");
		statusBar.setStatusMessage(msg, 2500);
	}

	private void fireDataChanged()
	{
		if (this.listeners == null) return;
		ListDataEvent evt = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, getSize() - 1);
		for (ListDataListener l : this.listeners)
		{
			l.contentsChanged(evt);
		}
	}

	/**
	 * Implementation of the ListModel interface
	 */
	@Override
	public Object getElementAt(int index)
	{
		return getElementList().get(index);
	}

	/**
	 * Implementation of the ListModel interface
	 */
	@Override
	public int getSize()
	{
		return getElementList().size();
	}

	/**
	 * Implementation of the ListModel interface
	 */
	@Override
	public void addListDataListener(ListDataListener listDataListener)
	{
		if (this.listeners == null) this.listeners = new ArrayList<>();
		this.listeners.add(listDataListener);
	}

	/**
	 * Implementation of the ListModel interface
	 */
	@Override
	public void removeListDataListener(ListDataListener listDataListener)
	{
		if (this.listeners == null) return;
		this.listeners.remove(listDataListener);
	}

}
