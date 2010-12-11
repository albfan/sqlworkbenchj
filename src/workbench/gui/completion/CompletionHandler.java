/*
 * CompletionHandler.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import workbench.db.WbConnection;
import workbench.gui.editor.JEditTextArea;
import workbench.interfaces.StatusBar;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.ScriptParser;
import workbench.util.SqlUtil;
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

	public CompletionHandler()
	{
		header = new JLabel("Tables");
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
		if (this.window != null)
		{
			this.window.setDbStoresMixedCase(dbConnection != null ? dbConnection.getMetadata().storesMixedCaseIdentifiers() : false);
		}
	}

	protected void showPopup()
	{
		filteredElements = null;
		try
		{
			statusBar.setStatusMessage(ResourceMgr.getString("MsgCompletionRetrievingObjects"));
			if (this.updateSelectionList())
			{
				this.window.showPopup(currentWord);
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
			this.window.setDbStoresMixedCase(dbConnection != null ? dbConnection.getMetadata().storesMixedCaseIdentifiers() : false);
		}

		// if this is not done in a separate thread
		// the status bar will not be updated...
		WbThread t = new WbThread("Completion")
		{
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
		String script = this.editor.getText();
		ScriptParser parser = new ScriptParser(script);
		parser.setCheckEscapedQuotes(Settings.getInstance().getCheckEscapedQuotes());
		parser.setEmptyLineIsDelimiter(Settings.getInstance().getAutoCompletionEmptyLineIsSeparator());
		parser.setAlternateLineComment(dbConnection == null ? null : dbConnection.getDbSettings().getLineComment());
		parser.setAlternateDelimiter(Settings.getInstance().getAlternateDelimiter(dbConnection));
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

		this.currentWord = editor.getWordAtCursor(BaseAnalyzer.SELECT_WORD_DELIM);

		try
		{
			StatementContext ctx = new StatementContext(this.dbConnection, sql, commandCursorPos);
			if (ctx.isStatementSupported())
			{
				boolean selectWord = (ctx.getAnalyzer().getOverwriteCurrentWord() && currentWord != null);
				BaseAnalyzer analyzer = ctx.getAnalyzer();
				if (analyzer != null && currentWord != null && analyzer.isWbParam() && currentWord.charAt(0) == '-')
				{
					currentWord = currentWord.substring(1);
				}
				window.selectCurrentWordInEditor(selectWord);
				this.elements = ctx.getData();
				this.header.setText(ctx.getTitle());
				this.window.setContext(ctx);

				result = getSize() > 0;
				if (result)
				{
					statusBar.clearStatusMessage();
					fireDataChanged();
				}
				else
				{
					if (Settings.getInstance().getDebugCompletionSearch())
					{
						LogMgr.logDebug("CompletionHandler.updateSelectionList()", "Analyzer did not return any data");
					}
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
		String verb = SqlUtil.getSqlVerb(sql);
		String msg = "'" + verb + "' " + ResourceMgr.getString("MsgCompletionNotSupported");
		statusBar.setStatusMessage(msg, 2500);
	}

	private void fireDataChanged()
	{
		if (this.listeners == null) return;
		ListDataEvent evt = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, getSize() - 1);
		for (int i=0; i < this.listeners.size(); i++)
		{
			ListDataListener l = this.listeners.get(i);
			l.contentsChanged(evt);
		}
	}

	/**
	 * Implementation of the ListModel interface
	 */
	public Object getElementAt(int index)
	{
		return getElementList().get(index);
	}

	/**
	 * Implementation of the ListModel interface
	 */
	public int getSize()
	{
		return getElementList().size();
	}

	/**
	 * Implementation of the ListModel interface
	 */
	public void addListDataListener(ListDataListener listDataListener)
	{
		if (this.listeners == null) this.listeners = new ArrayList<ListDataListener>();
		this.listeners.add(listDataListener);
	}

	/**
	 * Implementation of the ListModel interface
	 */
	public void removeListDataListener(ListDataListener listDataListener)
	{
		if (this.listeners == null) return;
		this.listeners.remove(listDataListener);
	}

}
