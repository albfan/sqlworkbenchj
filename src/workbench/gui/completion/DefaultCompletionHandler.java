/*
 * TableCompletionHandler.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
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
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.ScriptParser;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbThread;

/**
 * Handle the auto completion for tables and columns
 * @author  support@sql-workbench.net
 */
public class DefaultCompletionHandler
	implements ListModel, CompletionHandler
{
	private JEditTextArea editor;
	protected List elements = Collections.EMPTY_LIST;
	protected WbConnection dbConnection;
	private JLabel header;
	private List listeners;
	private int currentCursorLocation;
	private CompletionPopup window;
	private StatusBar statusBar;
	private String currentWord;
	
	public DefaultCompletionHandler()
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
	}
	
	private void showPopup()
	{
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
		parser.allowEmptyLineAsSeparator(Settings.getInstance().getAutoCompletionEmptyLineIsSeparator());
		
		int cursorPos = this.editor.getCaretPosition();

		int index = parser.getCommandIndexAtCursorPos(cursorPos);
		int commandCursorPos = parser.getIndexInCommand(index, cursorPos);
		String sql = parser.getCommand(index);
		if (sql == null) 
		{
			showNoObjectsFoundMessage();
			return false;
		}
		this.currentWord = editor.getWordAtCursor(".");//StringUtil.getWordLeftOfCursor(sql, commandCursorPos);
		
		try
		{
			StatementContext ctx = new StatementContext(this.dbConnection, sql, commandCursorPos);
			
			if (ctx.isStatementSupported())
			{
				boolean selectWord = (ctx.getOverwriteCurrentWord() && currentWord != null);
				window.selectCurrentWordInEditor(selectWord);
				this.elements = ctx.getData();
				this.header.setText(ctx.getTitle());
				this.window.setAppendDot(ctx.appendDotToSelection());
				this.window.setColumnPrefix(ctx.getColumnPrefix());
				
				result = (this.elements != null && this.elements.size() > 0);
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

	private void showNoObjectsFoundMessage()
	{
		WbThread t = new WbThread("Notification")
		{
			public void run()
			{
				String msg = ResourceMgr.getString("MsgCompletionNothingFound");
				statusBar.setStatusMessage(msg);
				try { Thread.sleep(2500); } catch (Throwable th) {}
				String m = statusBar.getText();
				if (msg.equals(m)) statusBar.clearStatusMessage();
			}
		};
		t.start();
	}
	
	private void showFailedMessage(String sql)
	{
		final String verb = SqlUtil.getSqlVerb(sql);
		WbThread t = new WbThread("Notification")
		{
			public void run()
			{
				String msg = "'" + verb + "' " + ResourceMgr.getString("MsgCompletionNotSupported");
				statusBar.setStatusMessage(msg);
				try { Thread.sleep(2500); } catch (Throwable th) {}
				String m = statusBar.getText();
				if (msg.equals(m)) statusBar.clearStatusMessage();
			}
		};
		t.start();
	}
	
	private void fireDataChanged()
	{
		if (this.listeners == null) return;
		ListDataEvent evt = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, this.elements.size() - 1);
		for (int i=0; i < this.listeners.size(); i++)
		{
			ListDataListener l = (ListDataListener)this.listeners.get(i);
			l.contentsChanged(evt);
		}
	}
	
	/**
	 * Implementation of the ListModel interface
	 */
	public Object getElementAt(int index)
	{
		if (this.elements == null) return null;
		return this.elements.get(index);
	}
	
	/**
	 * Implementation of the ListModel interface
	 */
	public int getSize()
	{
		if (this.elements == null) return 0;
		return this.elements.size();
	}
	
	/**
	 * Implementation of the ListModel interface
	 */
	public void addListDataListener(ListDataListener listDataListener)
	{
		if (this.listeners == null) this.listeners = new ArrayList();
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
