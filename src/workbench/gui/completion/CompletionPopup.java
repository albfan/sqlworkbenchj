/*
 * CompletionPopup.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
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
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.gui.WbSwingUtilities;
import workbench.gui.editor.JEditTextArea;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.StringUtil;
import workbench.util.TableAlias;
import workbench.util.WbThread;


/**
 * @author  support@sql-workbench.net
 */
public class CompletionPopup
	implements FocusListener, MouseListener, KeyListener
{
	protected JEditTextArea editor;
	private JScrollPane scroll;
	private JWindow window;
	private JPanel content;
	protected JList elementList;
	private ListModel data;
	private JComponent headerComponent;
	
	private boolean appendDot;
	private boolean appendSpace;
	private boolean selectCurrentWordInEditor;
	private String columnPrefix;
	protected CompletionSearchField searchField;
	
	public CompletionPopup(JEditTextArea ed, JComponent header, ListModel listData)
	{
		this.data = listData;
		this.editor = ed;
		this.headerComponent = header;
		
		this.elementList = new JList();
		this.elementList.setModel(this.data);
		this.elementList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
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
						editor.selectWordAtCursor(BaseAnalyzer.SELECT_WORD_DELIM);
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
	
	public void closeQuickSearch()
	{
		this.searchField = null;
		this.scroll.setColumnHeaderView(this.headerComponent);
		this.headerComponent.doLayout();
		if (Settings.getInstance().getCloseAutoCompletionWithSearch())
		{
			this.closePopup(false);
		}
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				elementList.requestFocusInWindow();
			}
		});
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
		if (this.appendDot) result += ".";
		if (this.appendSpace) result += " ";
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
	
	private void closePopup(boolean pasteEntry)
	{
		editor.removeKeyEventInterceptor();
		this.scroll.setColumnHeaderView(this.headerComponent);
		
		if (this.window == null) return;
		
		try
		{
			this.window.setVisible(false);
			if (pasteEntry)
			{
				Object o = this.elementList.getSelectedValue();
				String value = null;
				if (o != null)
				{
					if (o instanceof TableAlias)
					{
						TableAlias a = (TableAlias)o;
						value = getPasteValue(a.getNameToUse());
					}
					else if (o instanceof SelectAllMarker)
					{
						int count = this.data.getSize();
						StringBuffer cols = new StringBuffer(count * 10);
						int col = 0;
						
						// The first element is the SelectAllMarker, so we do not 
						// need to include it
						for (int i=1; i < count; i++)
						{
							Object c = this.data.getElementAt(i);
							if (c == null) continue;
							String v = c.toString();
							if (c instanceof ColumnIdentifier) 
							{
								v = getPasteValue(c.toString());
								if (columnPrefix != null)
								{
									cols.append(columnPrefix);
									cols.append(".");
								}
							}
							else if (c instanceof String)
							{
								v = (String)c;
							}
							if (col > 0) cols.append(", ");
							cols.append(v);
							col ++;
						}
						value = cols.toString();
					}
					else
					{
						value = getPasteValue(o.toString());
					}
				}
				if (value != null)
				{
					editor.setSelectedText(value);
				}
			}
		}
		finally
		{
			this.window.dispose();
			this.window = null;
			this.searchField = null;
			selectEditor();
		}
	}

	public void setColumnPrefix(String prefix)
	{
		this.columnPrefix = prefix;
	}

	public void selectCurrentWordInEditor(boolean flag)
	{
		this.selectCurrentWordInEditor = flag;
	}
	
	public void setAppendSpace(boolean flag)
	{
		this.appendSpace = flag;
	}
	public void setAppendDot(boolean flag)
	{
		this.appendDot = flag;
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
		switch(evt.getKeyCode())
		{
			case KeyEvent.VK_HOME:
				elementList.setSelectedIndex(0);
				elementList.ensureIndexIsVisible(0);
				evt.consume();
				break;
			case KeyEvent.VK_END:
				index = data.getSize() - 1;
				elementList.setSelectedIndex(index);
				elementList.ensureIndexIsVisible(index);
				evt.consume();
				break;
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
			case KeyEvent.VK_RIGHT:
			case KeyEvent.VK_LEFT:
				forwardKeyToList(evt);
				break;
			case KeyEvent.VK_UP:
				index = elementList.getSelectedIndex();
				if (index > 0)
				{
					elementList.setSelectedIndex(index - 1);
					elementList.ensureIndexIsVisible(index - 1);
				}
				evt.consume();
				break;
			case KeyEvent.VK_DOWN:
				index = elementList.getSelectedIndex();
				if (index < data.getSize() - 1)
				{
					elementList.setSelectedIndex(index + 1);
					elementList.ensureIndexIsVisible(index + 1);
				}
				evt.consume();
				break;
			default:
				evt.consume();
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
			this.searchField = new CompletionSearchField(this);
			String text = "" + evt.getKeyChar();
			this.searchField.setText(text);
			this.scroll.setColumnHeaderView(this.searchField);
			this.scroll.doLayout();
		}
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				if (searchField != null) searchField.requestFocusInWindow();
			}
		});
	}

	public void keyReleased(KeyEvent keyEvent)
	{
	}

	class DummyPanel
		extends JPanel
	{
		public boolean isManagingFocus() { return false; }
		public boolean getFocusTraversalKeysEnabled() {	return false;	}
	}
}
