/*
 * CompletionPopup.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.completion;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JWindow;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import workbench.db.ColumnIdentifier;
import workbench.gui.WbSwingUtilities;
import workbench.gui.editor.JEditTextArea;
import workbench.log.LogMgr;
import workbench.resource.ColumnSortType;
import workbench.resource.Settings;
import workbench.util.StringUtil;
import workbench.util.TableAlias;
import workbench.util.WbThread;

/**
 * @author  support@sql-workbench.net
 */
public class CompletionPopup
	implements FocusListener, MouseListener, KeyListener, WindowListener
{
	protected JEditTextArea editor;
	private JScrollPane scroll;
	private JWindow window;
	private JPanel content;
	protected JList elementList;
	private ListModel data;
	private JComponent headerComponent;
	
	private StatementContext context;
	private boolean selectCurrentWordInEditor;
	protected CompletionSearchField searchField;
	
	public CompletionPopup(JEditTextArea ed, JComponent header, ListModel listData)
	{
		this.data = listData;
		this.editor = ed;
		this.headerComponent = header;
		
		this.elementList = new JList();
		this.elementList.setModel(this.data);
		this.elementList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		Border b = new CompoundBorder(elementList.getBorder(), new EmptyBorder(0,2,0,2));
		this.elementList.setBorder(b);
		
		elementList.addFocusListener(this);
		elementList.addMouseListener(this);
		
		content = new DummyPanel();
		
		content.setLayout(new BorderLayout());
		scroll = new JScrollPane(this.elementList);
		scroll.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
		elementList.setVisibleRowCount(10);
		content.add(scroll);
		elementList.addKeyListener(this);
	}

	public void setContext(StatementContext c)
	{
		this.context = c;
	}
	
	public void showPopup(String valueToSelect)
	{
		//if (window != null) closePopup(false);
		try
		{
			scroll.setColumnHeaderView(headerComponent);
			headerComponent.doLayout();
			Dimension d = headerComponent.getPreferredSize();
			d.height += 25;
			elementList.setMinimumSize(d);
			scroll.setMinimumSize(d);

			Point p = editor.getCursorLocation();
			SwingUtilities.convertPointToScreen(p, editor);

			if (selectCurrentWordInEditor)
			{
				Thread t = new WbThread("select")
				{
					public void run()
					{
						// Make sure this is executed on the EDT
						WbSwingUtilities.invoke(new Runnable()
						{
							public void run()
							{
								editor.selectWordAtCursor(BaseAnalyzer.SELECT_WORD_DELIM);
							}
						});
					}
				};
				t.start();
			}
			int count = data.getSize();
			elementList.setVisibleRowCount(count < 12 ? count + 1 : 12);
			
			int index = 0;
			String s = editor.getSelectedText();
			if (s != null)
			{
				index = findEntry(s);
			}
			else if (valueToSelect != null)
			{
				index = findEntry(valueToSelect);
			}
			if (index == -1) index = 0;
			
			if (window == null)
			{
				window = new JWindow((Frame)SwingUtilities.getWindowAncestor(editor));
			}
			
			editor.setKeyEventInterceptor(this);
			
			elementList.doLayout();
			scroll.invalidate();
			scroll.doLayout();
			
			window.setLocation(p);
			window.setContentPane(content);
			window.addKeyListener(this);
			window.pack();
			window.addWindowListener(this);
			if (window.getWidth() < d.width + 5)
			{
				window.setSize(d.width + 5, window.getHeight());
			}
			
			elementList.setSelectedIndex(index);
			elementList.ensureIndexIsVisible(index);
			
			WbSwingUtilities.requestFocus(window, elementList);
			window.setVisible(true);
			
		}
		catch (Exception e)
		{
			LogMgr.logWarning("CompletionPopup.showPopup()", "Error displaying popup window",e);
		}
	}
	
	private void cleanup()
	{
		this.searchField = null;
		if (editor != null) editor.removeKeyEventInterceptor();
		if (this.window != null)
		{
			this.window.removeWindowListener(this);
			this.window.setVisible(false);
			this.window.dispose();
		}
		this.scroll.setColumnHeaderView(this.headerComponent);
		this.headerComponent.doLayout();
	}
	
	public void closeQuickSearch()
	{
		this.searchField = null;
		this.scroll.setColumnHeaderView(this.headerComponent);
		this.headerComponent.doLayout();
		
		if (Settings.getInstance().getCloseAutoCompletionWithSearch())
		{
			this.closePopup(false);
		}
		else
		{
			EventQueue.invokeLater(new Runnable()
			{
				public void run()
				{
					elementList.requestFocusInWindow();
				}
			});
		}
	}
	
	/**
	 * Callback from the SearchField when enter has been pressed in the search field
	 */
	public void quickSearchValueSelected()
	{
		this.closePopup(true);
	}
	
	private String getPasteValue(String value)
	{
		if (value == null) return value;
		String result;
		String pasteCase = Settings.getInstance().getAutoCompletionPasteCase();
		if (value.trim().charAt(0) == '"' || StringUtil.isMixedCase(value))
		{
			result = value;
		}
		else if ("lower".equalsIgnoreCase(pasteCase))
		{
			result = value.toLowerCase();
		}
		else if ("upper".equalsIgnoreCase(pasteCase))
		{
			result = value.toUpperCase();
		}
		else 
		{
			result = value;
		}
		if (this.context.getAnalyzer().appendDotToSelection()) result += ".";
		if (this.context.getAnalyzer().isKeywordList()) result += " ";
		if (this.context.getAnalyzer().isWbParam())
		{
			result = "-" + result + "=";
		}
		char c = this.context.getAnalyzer().quoteCharForValue(result);
		if (c != 0)
		{
			result = c + result + c;
		}
		return result;
	}
	
	public void cancelPopup()
	{
		if (this.window == null) return;
		window.setVisible(false);
		window.dispose();
	}
	
	private void selectEditor()
	{
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				editor.requestFocus();
				editor.requestFocusInWindow();
			}
		});
	}
	
	private List<ColumnIdentifier> getColumnsFromData()
	{
		int count = data.getSize();
		List<ColumnIdentifier> result = new ArrayList<ColumnIdentifier>(count);
		
		// The first element is the SelectAllMarker, so we do not 
		// need to include it
		for (int i=1; i < count; i++)
		{
			Object c = this.data.getElementAt(i);
			if (c instanceof ColumnIdentifier) 
			{
				result.add((ColumnIdentifier)c);
			}
			else if (c instanceof String)
			{
				result.add(new ColumnIdentifier((String)c));
			}
		}
		
		if (Settings.getInstance().getAutoCompletionColumnSortType() == ColumnSortType.position)
		{
			ColumnIdentifier.sortByPosition(result);
		}
		return result;
	}
	
	private void closePopup(boolean pasteEntry)
	{
		editor.removeKeyEventInterceptor();
		scroll.setColumnHeaderView(this.headerComponent);

		if (this.window == null)
		{
			return;
		}

		try
		{
			if (pasteEntry)
			{
				doPaste();
			}
		}
		finally
		{
			this.window.removeWindowListener(this);
			this.window.setVisible(false);
			this.window.dispose();
			this.window = null;
			this.searchField = null;
			selectEditor();
		}
	}
	
	private void doPaste()
	{
		Object[] selected = this.elementList.getSelectedValues();
		if (selected == null)
		{
			return;
		}
		String value = "";
		
		for (Object o : selected)
		{
			if (o instanceof TableAlias)
			{
				TableAlias a = (TableAlias) o;
				String table = getPasteValue(a.getNameToUse());
				if (value.length() > 0)
				{
					value += ", ";
				}
				value += table;
			}
			else if (o instanceof SelectAllMarker)
			{
				// The SelectAllMarker is only used when columns are beeing displayed
				List<ColumnIdentifier> columns = getColumnsFromData();

				int count = columns.size();
				StringBuilder cols = new StringBuilder(count * 10);

				for (int i = 0; i < count; i++)
				{
					ColumnIdentifier c = columns.get(i);
					String v = c.getColumnName();
					if (i > 0)
					{
						cols.append(", ");
					}
					if (context.getAnalyzer().getColumnPrefix() != null && i > 0)
					{
						cols.append(context.getAnalyzer().getColumnPrefix());
						cols.append(".");
					}
					cols.append(v);
				}
				value = cols.toString();
				break;
			}
			else
			{
				if (value.length() > 0)
				{
					value += ", ";
				}
				value += getPasteValue(o.toString());
			}
		}

		if (!StringUtil.isEmptyString(value))
		{
			editor.setSelectedText(value);
			if (value.startsWith("<") && value.endsWith(">"))
			{
				editor.selectWordAtCursor(" =-\t\n");
			}
		}
	}
	
	public void selectCurrentWordInEditor(boolean flag)
	{
		this.selectCurrentWordInEditor = flag;
	}
	
	public void selectMatchingEntry(String s)
	{
		int index = this.findEntry(s);
		if (index >= 0)
		{
			elementList.setSelectedIndex(index);
			elementList.ensureIndexIsVisible(index);
		}
		else
		{
			elementList.clearSelection();
		}
	}
	private int findEntry(String s)
	{
		if (s == null) return -1;
		int count = this.data.getSize();
		String search = s.toLowerCase();
		for (int i=0; i < count; i++)
		{
			String entry = StringUtil.trimQuotes(this.data.getElementAt(i).toString());
			if (entry.toLowerCase().startsWith(search)) return i;
		}
		return -1;
	}
	
	protected int findEntry(char c)
	{
		int count = this.data.getSize();
		char sc = Character.toLowerCase(c);
		for (int i=0; i < count; i++)
		{
			String entry = this.data.getElementAt(i).toString();
			if (entry.length() == 0) continue;
			char ec = Character.toLowerCase(entry.charAt(0));
			
			if (ec == sc) return i;
		}
		return -1;
	}
	
	/**
	 * Implementation of the FocusListener interface
	 */
	public void focusGained(FocusEvent focusEvent)
	{
	}
	
	/**
	 * Implementation of the FocusListener interface
	 */
	public void focusLost(FocusEvent focusEvent)
	{
		if (this.searchField == null) closePopup(false);
	}
	
	/**
	 * Implementation of the MouseListener interface
	 */
	public void mouseClicked(java.awt.event.MouseEvent mouseEvent)
	{
		int clicks = mouseEvent.getClickCount();
		if (clicks == 2)
		{
			closePopup(true);
		}
		else if (clicks == 1 && this.searchField != null)
		{
			closeQuickSearch();
		}
	}
	
	public void mouseEntered(MouseEvent mouseEvent) {}
	public void mouseExited(MouseEvent mouseEvent) {}
	public void mousePressed(MouseEvent mouseEvent)	{}
	public void mouseReleased(MouseEvent mouseEvent) {}
	
  public void keyPressed(KeyEvent evt)
  {
    int index = -1;
    switch (evt.getKeyCode())
    {
      case KeyEvent.VK_TAB:
        evt.consume();
        break;
      case KeyEvent.VK_ENTER:
        closePopup(true);
        evt.consume();
        break;
      case KeyEvent.VK_ESCAPE:
        closePopup(false);
        evt.consume();
        break;
//			case KeyEvent.VK_HOME:
//			case KeyEvent.VK_END:
//      case KeyEvent.VK_RIGHT:
//      case KeyEvent.VK_LEFT:
//      case KeyEvent.VK_UP:
//      case KeyEvent.VK_DOWN:
//        forwardKeyToList(evt);
//        break;
      default:
        forwardKeyToList(evt);
    }
  }
	
	private void forwardKeyToList(KeyEvent evt)
	{
		KeyListener[] l = elementList.getKeyListeners();
		for (int i=0; i < l.length; i++)
		{
			if (l[i] != this) l[i].keyPressed(evt);
		}
	}
	
	public void keyTyped(KeyEvent evt)
	{
		if (this.searchField == null)
		{
			String text = String.valueOf(evt.getKeyChar());
			this.searchField = new CompletionSearchField(this, text);
			this.scroll.setColumnHeaderView(this.searchField);
			this.scroll.doLayout();
		}
		WbSwingUtilities.invoke(new Runnable()
		{
			public void run()
			{
				if (searchField != null) 
				{
					searchField.requestFocusInWindow();
				}
			}
		});
		// The JGoodies look and feel automatically selects 
		// the content of the text field when a focusGained event
		// occurs. The moving of the caret has to come later
		// than the focusGained that's why the requestFocus()
		// and the moving of the caret are done in two steps
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				if (searchField != null) 
				{
					int len = searchField.getText().length();
					searchField.setCaretPosition(len);
					searchField.select(len, len);
				}
			}
		});
		
	}

	public void keyReleased(KeyEvent keyEvent)
	{
	}

	public void windowOpened(WindowEvent e)
	{
	}
	
	public void windowClosing(WindowEvent e)
	{
	}
	
	public void windowClosed(WindowEvent e)
	{
		this.cleanup();
	}
	
	public void windowIconified(WindowEvent e)
	{
	}
	
	public void windowDeiconified(WindowEvent e)
	{
	}
	
	public void windowActivated(WindowEvent e)
	{
	}
	
	public void windowDeactivated(WindowEvent e)
	{
	}
	
	class DummyPanel
		extends JPanel
	{
		public boolean isManagingFocus() { return false; }
		public boolean getFocusTraversalKeysEnabled() {	return false;	}
	}

}
