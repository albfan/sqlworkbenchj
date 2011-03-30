/*
 * CompletionPopup.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.completion;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
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
import workbench.gui.components.WbTraversalPolicy;
import workbench.gui.editor.JEditTextArea;
import workbench.log.LogMgr;
import workbench.resource.ColumnSortType;
import workbench.resource.GuiSettings;
import workbench.resource.Settings;
import workbench.util.ArgumentValue;
import workbench.util.StringUtil;
import workbench.util.TableAlias;

/**
 * @author  Thomas Kellerer
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
	private boolean dbStoresMixedCase;
	private boolean ignoreSearchChange;
	private boolean partialSearch;
	private boolean filterSearch;

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
		elementList.setCellRenderer(new CompletionListRenderer());

		elementList.addFocusListener(this);
		elementList.addMouseListener(this);

		content = new DummyPanel();

		content.setLayout(new BorderLayout());
		scroll = new JScrollPane(this.elementList);
		scroll.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
		elementList.setVisibleRowCount(10);
		content.add(scroll);
		elementList.addKeyListener(this);
		partialSearch = GuiSettings.getPartialCompletionSearch();
		filterSearch = GuiSettings.getFilterCompletionSearch();
	}

	public void setContext(StatementContext c)
	{
		this.context = c;
	}

	public void showPopup(String valueToSelect)
	{
		if (!editor.isReallyVisible())
		{
			// this can happen if the code completion takes some time to populate the
			// result and the user changes the current tab during this
			return;
		}

		try
		{
			scroll.setColumnHeaderView(headerComponent);
			headerComponent.doLayout();
			final Dimension d = headerComponent.getPreferredSize();
			d.height += 35;
			d.width += 20;
			elementList.setMinimumSize(d);
			scroll.setMinimumSize(d);

			final Point p = editor.getCursorLocation();
			SwingUtilities.convertPointToScreen(p, editor);

			if (selectCurrentWordInEditor)
			{
				// Make sure this is executed on the EDT
				WbSwingUtilities.invoke(new Runnable()
				{
					@Override
					public void run()
					{
						editor.selectWordAtCursor(BaseAnalyzer.SELECT_WORD_DELIM);
					}
				});
			}
			int count = data.getSize();
			elementList.setVisibleRowCount(count < 12 ? count + 1 : 12);

			int index = 0;
			boolean showQuickSearch = false;
			String initialSearchValue = null;

			String s = editor.getSelectedText();
			if (s != null)
			{
				index = findEntry(s);
				initialSearchValue = s;
			}
			else if (StringUtil.isNonBlank(valueToSelect))
			{
				index = findEntry(valueToSelect);
				initialSearchValue = valueToSelect;
			}

			if (index == -1)
			{
				index = 0;
			}
			else
			{
				showQuickSearch = true;
			}

			if (window == null)
			{
				window = new JWindow(SwingUtilities.getWindowAncestor(editor));
			}

			editor.setKeyEventInterceptor(this);

			elementList.validate();

			WbTraversalPolicy pol = new WbTraversalPolicy();
			pol.addComponent(elementList);
			pol.setDefaultComponent(elementList);

			elementList.setFocusable(true);
			elementList.setFocusTraversalKeysEnabled(false);
			window.setFocusCycleRoot(true);
			window.setFocusTraversalPolicy(pol);

			window.setContentPane(content);
			window.addKeyListener(this);
			window.addWindowListener(this);

			final int toSelect = index;

			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					window.setLocation(p);
					window.pack();
					if (window.getWidth() < d.width + 5)
					{
						window.setSize(d.width + 5, window.getHeight());
					}
					window.setVisible(true);
					elementList.requestFocus();
					elementList.setSelectedIndex(toSelect);
					elementList.ensureIndexIsVisible(toSelect);
				}
			});

			if (showQuickSearch)
			{
				showQuickSearchValue(initialSearchValue);
			}
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
				@Override
				public void run()
				{
					elementList.requestFocusInWindow();
				}
			});
		}
	}

	public void setDbStoresMixedCase(boolean flag)
	{
		this.dbStoresMixedCase = flag;
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
		if (value.trim().charAt(0) == '"' || StringUtil.isMixedCase(value) || dbStoresMixedCase)
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
			@Override
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
			else if (o instanceof ArgumentValue)
			{
				ArgumentValue v = (ArgumentValue)o;
				if (value.length() > 0)
				{
					value += ", ";
				}
				value += v.getValue();
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

		if (StringUtil.isNonBlank(value))
		{
			editor.setSelectedText(value);
			if (value.charAt(0) == '<' || value.charAt(0) == '>')
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
		if (ignoreSearchChange) return;

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
		if (count == 0) return -1;

		if (filterSearch && data instanceof CompletionHandler)
		{
			CompletionHandler handler = (CompletionHandler)data;
			handler.filterElements(s);
			return handler.getSize() == 0 ? -1 : 0;
		}
		else
		{
			String search = s.toLowerCase();
			for (int i=0; i < count; i++)
			{
				String entry = StringUtil.trimQuotes(this.data.getElementAt(i).toString());
				if (partialSearch)
				{
					if (entry.toLowerCase().contains(search)) return i;
				}
				else
				{
					if (entry.toLowerCase().startsWith(search)) return i;
				}
			}
		}
		return -1;
	}

	/**
	 * Implementation of the FocusListener interface
	 */
	@Override
	public void focusGained(FocusEvent focusEvent)
	{
	}

	/**
	 * Implementation of the FocusListener interface
	 */
	@Override
	public void focusLost(FocusEvent focusEvent)
	{
		if (this.searchField == null) closePopup(false);
	}

	/**
	 * Implementation of the MouseListener interface
	 */
	@Override
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

	@Override
	public void mouseEntered(MouseEvent mouseEvent)
	{
	}

	@Override
	public void mouseExited(MouseEvent mouseEvent)
	{
	}

	@Override
	public void mousePressed(MouseEvent mouseEvent)
	{
	}

	@Override
	public void mouseReleased(MouseEvent mouseEvent)
	{
	}

	@Override
	public void keyPressed(KeyEvent evt)
	{
		int index = -1;
		boolean syncEntry = false;

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

			case KeyEvent.VK_UP:
				// When the searchfield is displayed the list
				// does not have the focus, und therefor the up and down
				// keys only scroll the list, but do not move the selection
				if (this.searchField != null)
				{
					index = elementList.getSelectedIndex();
					if (index > 0)
					{
						elementList.setSelectedIndex(index - 1);
						elementList.ensureIndexIsVisible(index - 1);
						syncEntry = true;
					}
					evt.consume();
				}
				break;
			case KeyEvent.VK_DOWN:
				if (this.searchField != null)
				{
					index = elementList.getSelectedIndex();
					if (index < data.getSize() - 1)
					{
						elementList.setSelectedIndex(index + 1);
						elementList.ensureIndexIsVisible(index + 1);
						syncEntry = true;
					}
					evt.consume();
				}
				break;
		}
		if (syncEntry)
		{
			try
			{
				ignoreSearchChange = true;
				Object o = elementList.getSelectedValue();
				if (o != null)
				{
					this.searchField.setText(o.toString());
					this.searchField.selectAll();
				}
			}
			finally
			{
				ignoreSearchChange = false;
			}
		}
	}

	protected void showQuickSearchValue(final String text)
	{
		if (StringUtil.isBlank(text)) return;
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				openQuickSearch(text);
				searchField.setText(text.trim());
				searchField.requestFocusInWindow();
			}
		});
		setSearchFieldCursor();
	}

	protected void openQuickSearch(String initialValue)
	{
		if (this.searchField == null)
		{
			this.searchField = new CompletionSearchField(this, initialValue);
			this.scroll.setColumnHeaderView(this.searchField);
			this.scroll.doLayout();
		}
	}

	protected void setSearchFieldCursor()
	{
		EventQueue.invokeLater(new Runnable()
		{
			@Override
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

	@Override
	public void keyTyped(KeyEvent evt)
	{
		if (this.searchField == null)
		{
			String text = String.valueOf(evt.getKeyChar());
			openQuickSearch(text);
		}

		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
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
		setSearchFieldCursor();
	}

	@Override
	public void keyReleased(KeyEvent keyEvent)
	{
	}

	@Override
	public void windowOpened(WindowEvent e)
	{
	}

	@Override
	public void windowClosing(WindowEvent e)
	{
	}

	@Override
	public void windowClosed(WindowEvent e)
	{
		this.cleanup();
	}

	@Override
	public void windowIconified(WindowEvent e)
	{
	}

	@Override
	public void windowDeiconified(WindowEvent e)
	{
	}

	@Override
	public void windowActivated(WindowEvent e)
	{
	}

	@Override
	public void windowDeactivated(WindowEvent e)
	{
	}

	static class DummyPanel
		extends JPanel
	{
		@SuppressWarnings("deprecation")
		@Override
		public boolean isManagingFocus()
		{
			return false;
		}

		@Override
		public boolean getFocusTraversalKeysEnabled()
		{
			return false;
		}
	}

}
