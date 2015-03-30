/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.gui.bookmarks;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FocusTraversalPolicy;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableColumnModel;

import workbench.interfaces.MainPanel;
import workbench.interfaces.Reloadable;
import workbench.interfaces.ValidatingComponent;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.CheckBoxAction;
import workbench.gui.actions.ReloadAction;
import workbench.gui.components.ColumnWidthOptimizer;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.FlatButton;
import workbench.gui.components.SelectionHandler;
import workbench.gui.components.ValidatingDialog;
import workbench.gui.components.WbCheckBox;
import workbench.gui.components.WbLabel;
import workbench.gui.components.WbScrollPane;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbToolbarButton;
import workbench.gui.components.WbTraversalPolicy;
import workbench.gui.renderer.RendererSetup;

import workbench.storage.DataStore;
import workbench.storage.SortDefinition;
import workbench.storage.filter.ColumnExpression;
import workbench.storage.filter.ContainsComparator;
import workbench.storage.filter.DataRowExpression;
import workbench.storage.filter.ExpressionValue;
import workbench.storage.filter.FilterExpression;

import workbench.sql.ResultNameAnnotation;

import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class BookmarkSelector
	extends JPanel
	implements KeyListener, MouseListener, Reloadable, ActionListener, ValidatingComponent, ItemListener
{
	private static final String PROP_SORT_DEF = GuiSettings.PROP_BOOKMARK_PREFIX + "sort";
	private static final String PROP_USE_CURRENT_TAB = GuiSettings.PROP_BOOKMARK_PREFIX + "current.tab.default";
	private static final String PROP_SEARCH_NAME = GuiSettings.PROP_BOOKMARK_PREFIX + "search.name";

	private final JTextField filterValue;
	private final JComboBox tabSelector;
	private JLabel info;
	private WbTable bookmarks;
	private WbScrollPane scroll;
	private final MainWindow window;
	private ValidatingDialog dialog;
	private NamedScriptLocation selectedBookmark;
	private SortDefinition savedSort;
	private WbCheckBox searchNameCbx;
	private WbCheckBox useCurrentEditorCbx;
	private int[] initialColumnWidths;
	private CheckBoxAction rememberColumnWidths;
	private CheckBoxAction rememberSort;
	private SelectionHandler keyHandler;

	public BookmarkSelector(MainWindow win)
	{
		super(new GridBagLayout());
		window = win;

		filterValue = new JTextField();
		filterValue.addKeyListener(this);
		filterValue.addActionListener(this);
		filterValue.setToolTipText(ResourceMgr.getString("TxtBookmarkFilterTip"));

		rememberColumnWidths = new CheckBoxAction("MnuTxtBookmarksSaveWidths", GuiSettings.PROP_BOOKMARKS_SAVE_WIDTHS);
		rememberSort = new CheckBoxAction("MnuTxtRememberSort", GuiSettings.PROP_BOOKMARKS_SAVE_SORT);

		bookmarks = new WbTable(false, false, false)
		{
			@Override
			protected JPopupMenu getHeaderPopup()
			{
				JPopupMenu menu = createLimitedHeaderPopup();
				menu.addSeparator();
				menu.add(rememberColumnWidths.getMenuItem());
				menu.add(rememberSort.getMenuItem());
				return menu;
			}
		};

		bookmarks.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		bookmarks.setReadOnly(true);
		bookmarks.setRendererSetup(new RendererSetup(false));
		bookmarks.addMouseListener(this);
		bookmarks.setColumnSelectionAllowed(false);
		bookmarks.setRowSelectionAllowed(true);
		bookmarks.getHeaderRenderer().setUnderlinePK(true);
		bookmarks.setSortIgnoreCase(true);
		bookmarks.setShowPopupMenu(false);
		keyHandler = new SelectionHandler(bookmarks);

		searchNameCbx = new WbCheckBox();
		searchNameCbx.setText(ResourceMgr.getString("LblBookFilter"));
		searchNameCbx.setSelected(Settings.getInstance().getBoolProperty(PROP_SEARCH_NAME, true));
		searchNameCbx.setToolTipText(ResourceMgr.getDescription("LblBookFilter"));


		boolean useCurrent = Settings.getInstance().getBoolProperty(PROP_USE_CURRENT_TAB, true);
		useCurrentEditorCbx = new WbCheckBox();
		useCurrentEditorCbx.setText(ResourceMgr.getString("LblBookUseCurrent"));
		useCurrentEditorCbx.setToolTipText(ResourceMgr.getDescription("LblBookUseCurrent"));
		useCurrentEditorCbx.setSelected(useCurrent);

		// adding an ActionListener does not work
		// if the checkbox is toggled by using the mnemonic
		useCurrentEditorCbx.addItemListener(this);

		tabSelector = new JComboBox(getTabs());
		if (useCurrent)
		{
			selectCurrentTab();
		}
		tabSelector.addActionListener(this);

		Action nextComponent = new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				FocusTraversalPolicy policy = getFocusTraversalPolicy();
				Component next = policy.getComponentAfter(BookmarkSelector.this, bookmarks);
				next.requestFocusInWindow();
			}
		};

		Action prevComponent = new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				FocusTraversalPolicy policy = getFocusTraversalPolicy();
				Component next = policy.getComponentBefore(BookmarkSelector.this, bookmarks);
				next.requestFocusInWindow();
			}
		};

		InputMap im = bookmarks.getInputMap(JComponent.WHEN_FOCUSED);
		im.put(WbSwingUtilities.TAB, "picker-next-comp");
		im.put(WbSwingUtilities.SHIFT_TAB, "picker-prev-comp");

		Action selectValue = new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent evt)
			{
				selectValueAndClose();
			}
		};

		bookmarks.configureEnterKeyAction(selectValue);
		bookmarks.getActionMap().put("picker-next-comp", nextComponent);
		bookmarks.getActionMap().put("picker-prev-comp", prevComponent);

		scroll = new WbScrollPane(bookmarks);

		JPanel filterPanel = createFilterPanel();

		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx = 0;
		gc.gridy = 0;
		gc.anchor = GridBagConstraints.FIRST_LINE_START;
		gc.fill = GridBagConstraints.HORIZONTAL;
		gc.weightx = 1.0;
		gc.weighty = 0.0;
		add(filterPanel, gc);

		gc.gridy = 1;
		gc.weightx = 1.0;
		gc.weighty = 1.0;
		gc.fill = GridBagConstraints.BOTH;
		gc.insets = new Insets(5,0,0,0);
		add(scroll, gc);

		String tags = null;
		String prefix = "<tt><b>@";
		String close  = "</b></tt>";
		if (GuiSettings.getUseResultTagForBookmarks())
		{
			tags = prefix + BookmarkAnnotation.ANNOTATION + close + " " + ResourceMgr.getString("TxtOr") + " " + prefix + ResultNameAnnotation.ANNOTATION + close;
		}
		else
		{
			tags = prefix + BookmarkAnnotation.ANNOTATION + close;
		}
		info = new JLabel(ResourceMgr.getFormattedString("TxtBookmarkHelp", tags));
		gc.gridy = 2;
		gc.weightx = 0.0;
		gc.weighty = 0.0;
		gc.fill = GridBagConstraints.BOTH;
		gc.insets = new Insets(10,0,10,0);
		add(info, gc);

		WbTraversalPolicy pol = new WbTraversalPolicy();
		pol.addComponent(tabSelector);
		pol.addComponent(useCurrentEditorCbx);
		pol.addComponent(searchNameCbx);
		pol.addComponent(filterValue);

		setFocusCycleRoot(true);
		bookmarks.setFocusCycleRoot(false);

		setFocusTraversalPolicy(pol);

		if (GuiSettings.getSaveBookmarkSort())
		{
			String sort = Settings.getInstance().getProperty(PROP_SORT_DEF, null);
			if (StringUtil.isNonBlank(sort))
			{
				savedSort = SortDefinition.parseDefinitionString(sort);
				if (savedSort != null)
				{
					savedSort.setIgnoreCase(true);
				}
				else
				{
					LogMgr.logWarning("BookmarkSelector.<init>", "Invalid sort definition saved: " + sort);
					savedSort = null;
				}
			}
		}
		else
		{
			savedSort = null;
		}
	}

	private JPanel createFilterPanel()
	{
		JPanel filterPanel = new JPanel(new GridBagLayout());
		WbLabel lbl = new WbLabel();
		lbl.setText(ResourceMgr.getString("LblFkFilterValue"));
		lbl.setToolTipText(filterValue.getToolTipText());
		lbl.setLabelFor(filterValue);
		lbl.setBorder(new EmptyBorder(0, 0, 0, 5));

		ReloadAction doReload = new ReloadAction(this);
		doReload.setTooltip(ResourceMgr.getString("TxtBookmarkReload"));
		FlatButton reload = new FlatButton(doReload);
		reload.setText(null);
		reload.setMargin(WbToolbarButton.MARGIN);

		WbLabel tabLbl = new WbLabel();
		tabLbl.setText(ResourceMgr.getString("LblBookPanel"));
		tabLbl.setLabelFor(tabSelector);
		tabLbl.setBorder(new EmptyBorder(2, 0, 0, 0));

		GridBagConstraints gc = new GridBagConstraints();
		Insets topTwo = new Insets(2,0,0,0);

		gc.gridx = 0;
		gc.gridy = 0;
		gc.anchor = GridBagConstraints.FIRST_LINE_START;
		gc.fill = GridBagConstraints.NONE;
		gc.weightx = 0.0;
		gc.weighty = 0.0;
		gc.insets = topTwo;
		filterPanel.add(tabLbl, gc);

		Dimension pref = tabSelector.getPreferredSize();
		Dimension max = new Dimension((int)(pref.width * 1.05), pref.height);
		tabSelector.setMaximumSize(max);
		JPanel ddPanel = new JPanel();
		ddPanel.setLayout(new BoxLayout(ddPanel, BoxLayout.LINE_AXIS));
		ddPanel.add(tabSelector);
		ddPanel.add(Box.createHorizontalGlue());
		ddPanel.add(useCurrentEditorCbx);
		ddPanel.add(searchNameCbx);

		gc.gridx = 1;
		gc.gridy = 0;
		gc.gridwidth = 2;
		gc.weightx = 1.0;
		gc.anchor = GridBagConstraints.FIRST_LINE_START;
    gc.insets = WbSwingUtilities.getEmptyInsets();
		gc.fill = GridBagConstraints.HORIZONTAL;
		filterPanel.add(ddPanel, gc);

		// second line
		gc.gridx = 0;
		gc.gridy = 1;
		gc.weightx = 0.0;
		gc.anchor = GridBagConstraints.FIRST_LINE_START;
		gc.insets = new Insets(8,0,0,0);
		gc.gridwidth = 1;
		filterPanel.add(lbl, gc);

		gc.gridx = 1;
		gc.gridwidth = 1;
		gc.weightx = 1.0;
		gc.weighty = 0.0;
		gc.insets = new Insets(4,0,0,5);
		gc.fill = GridBagConstraints.HORIZONTAL;
		filterPanel.add(filterValue, gc);

		gc.gridx = 2;
		gc.gridwidth = 1;
		gc.weightx = 0.0;
		gc.weighty = 1.0;
		gc.insets = topTwo;
		gc.fill = GridBagConstraints.NONE;
		filterPanel.add(reload, gc);

		return filterPanel;
	}

	private Object[] getTabs()
	{
		List<String> tabIds = BookmarkManager.getInstance().getTabs(window);

		List<TabEntry> entries = new ArrayList<>();
		entries.add(new TabEntry(null, ResourceMgr.getString("LblBookPanelAll"), -1));
		for (String tabId : tabIds)
		{
			int index = window.getIndexForPanel(tabId);
			if (index > -1)
			{
				entries.add(new TabEntry(tabId, window.getTabTitle(index), index));
			}
		}
		Collections.sort(entries, TabEntry.INDEX_SORTER);
		return entries.toArray();
	}

	@Override
	public void itemStateChanged(ItemEvent e)
	{
		// the checkbox for "Current tab only" was changed
		if (useCurrentEditorCbx.isSelected())
		{
			selectCurrentTab();
		}
		else
		{
			tabSelector.setSelectedIndex(0);
		}
		filterValue.setText("");
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == this.tabSelector)
		{
			loadBookmarks();
		}
	}

	@Override
	public void reload()
	{
		saveColumnWidths();
		BookmarkManager.getInstance().clearBookmarksForWindow(window.getWindowId());
		BookmarkManager.getInstance().updateBookmarks(window);
		loadBookmarks();
	}

	public void loadBookmarks()
	{
		TabEntry tab = (TabEntry)tabSelector.getSelectedItem();

		final long start = System.currentTimeMillis();
		final DataStore data;
		if (tab.getId() == null)
		{
			data = BookmarkManager.getInstance().getAllBookmarks(window);
		}
		else
		{
			data = BookmarkManager.getInstance().getBookmarksForTab(window, tab.getId());
		}

		WbSwingUtilities.showWaitCursorOnWindow(this);
		EventQueue.invokeLater(() ->
    {
      initialColumnWidths = null;

      SortDefinition oldSort;
      if (savedSort != null)
      {
        oldSort = savedSort;
        savedSort = null;
      }
      else
      {
        oldSort	= bookmarks.getCurrentSortColumns();
      }
      DataStoreTableModel model = new DataStoreTableModel(data);
      model.setAllowEditing(false);
      bookmarks.setModel(model, true);
      bookmarks.getTableHeader().setReorderingAllowed(false);

      if (data.getRowCount() > 0)
      {
        if (oldSort != null)
        {
          model.setSortDefinition(oldSort);
        }
        else
        {
          model.sortByColumn(0, true, false);
        }
        handleColumnWidths();
      }

      initialColumnWidths = getColumnWidths();

      // always select at least one row.
      // as the focus is set to the table containing the lookup data,
      // the user can immediately use the cursor keys to select one entry.
      keyHandler.selectRow(0);

      long duration = System.currentTimeMillis() - start;
      LogMgr.logDebug("BookmarkSelector.loadBookmarks()", "Loading bookmarks took: " + duration + "ms");

      filterValue.requestFocusInWindow();
      WbSwingUtilities.showDefaultCursorOnWindow(BookmarkSelector.this);
    });
	}

	private void handleColumnWidths()
	{
		int[] savedWidths = getSavedColumnWidths();

		if (savedWidths != null && GuiSettings.getSaveBookmarkColWidths())
		{
			bookmarks.applyColumnWidths(savedWidths);
		}
		else
		{
			ColumnWidthOptimizer optimizer = new ColumnWidthOptimizer(bookmarks);
			int maxChars = GuiSettings.getBookmarksMaxColumnWidth();
			int maxWidth = WbSwingUtilities.calculateCharWidth(bookmarks, maxChars);
			optimizer.optimizeAllColWidth(10, maxWidth, true);
		}
	}

	protected void resetFilter()
	{
		EventQueue.invokeLater(() ->
    {
      filterValue.setText("");
      bookmarks.resetFilter();
      keyHandler.selectRow(0);
    });
	}

	protected void applyFilter()
	{
		EventQueue.invokeLater(() ->
    {
      if (GuiSettings.getSaveBookmarkColWidths())
      {
        saveColumnWidths();
      }
      ContainsComparator comp = new ContainsComparator();
      FilterExpression filter;
      if (searchNameCbx.isSelected())
      {
        filter = new ColumnExpression(bookmarks.getDataStore().getColumnName(0), comp, filterValue.getText());
      }
      else
      {
        filter = new DataRowExpression(comp, filterValue.getText());
      }
      ((ExpressionValue)filter).setIgnoreCase(true);
      bookmarks.applyFilter(filter, false);
      handleColumnWidths();
      keyHandler.selectRow(0);
    });
	}

	private void selectCurrentTab()
	{
		int count = tabSelector.getItemCount();
		int currentIndex = window.getCurrentPanelIndex();
		for (int i=0; i < count; i++)
		{
			TabEntry entry = (TabEntry)tabSelector.getItemAt(i);
			if (entry.getIndex() == currentIndex)
			{
				tabSelector.setSelectedItem(entry);
				return;
			}
		}
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		if (e.getSource() == this.filterValue && e.getModifiers() == 0)
		{
			if (e.getKeyCode() == KeyEvent.VK_ESCAPE && StringUtil.isNonBlank(filterValue.getText()))
			{
				e.consume();
				resetFilter();
			}
			else
			{
				keyHandler.handleKeyPressed(e);
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
	}

	private boolean isAltPressed(KeyEvent e)
	{
		// TODO: does this work with MacOS?
		return ((e.getModifiers() & KeyEvent.ALT_MASK) == KeyEvent.ALT_MASK);
	}

	@Override
	public void keyTyped(final KeyEvent e)
	{
		if (e.getSource() == this.filterValue)
		{
			if (e.getKeyChar() == KeyEvent.VK_ENTER)
			{
				selectValueAndClose();
			}
			else if (isAltPressed(e) && e.getKeyChar() == searchNameCbx.getMnemonic())
			{
				searchNameCbx.setSelected(!searchNameCbx.isSelected());
				e.consume();
			}
			else if (isAltPressed(e) && e.getKeyChar() == useCurrentEditorCbx.getMnemonic())
			{
				useCurrentEditorCbx.setSelected(!useCurrentEditorCbx.isSelected());
				e.consume();
			}
			else
			{
				applyFilter();
			}
		}
	}

	@Override
	public boolean validateInput()
	{
		if (bookmarks.getSelectedRowCount() == 1)
		{
			selectValue();
			return true;
		}
		return false;
	}

  @Override
  public void componentWillBeClosed()
  {
  }

	@Override
	public void componentDisplayed()
	{
		selectedBookmark = null;
		loadBookmarks();
	}

	public void selectValue()
	{
		int row = bookmarks.getSelectedRow();
		selectedBookmark = (NamedScriptLocation)bookmarks.getDataStore().getRow(row).getUserObject();
	}

	public void selectValueAndClose()
	{
		selectValue();
		dialog.approveAndClose();
	}


	private int[] getColumnWidths()
	{
		TableColumnModel colMod = bookmarks.getColumnModel();
		if (colMod == null) return null;

		int count = colMod.getColumnCount();
		int[] result = new int[count];

		for (int i=0; i<count; i++)
		{
			result[i] = colMod.getColumn(i).getWidth();
		}
		return result;
	}

	private void saveColumnWidths()
	{
		String widths = StringUtil.arrayToString(getColumnWidths());
		Settings.getInstance().setProperty(GuiSettings.PROP_BOOKMARK_PREFIX + "colwidths", widths);
	}

	private int[] getSavedColumnWidths()
	{
		String widths = Settings.getInstance().getProperty(GuiSettings.PROP_BOOKMARK_PREFIX + "colwidths", null);
		return StringUtil.stringToArray(widths);
	}

	private boolean columnWidthChanged()
	{
		if (initialColumnWidths == null) return true;
		if (getSavedColumnWidths() != null) return true;
		int[] currentWidths = getColumnWidths();
		return !Arrays.equals(currentWidths, initialColumnWidths);
	}

	public void saveSettings()
	{
		if (GuiSettings.getSaveBookmarkSort())
		{
			SortDefinition sort = bookmarks.getCurrentSortColumns();
			String sortDef = sort.getDefinitionString();
			Settings.getInstance().setProperty(PROP_SORT_DEF, sortDef);
		}
		Settings.getInstance().setProperty(PROP_SEARCH_NAME, searchNameCbx.isSelected());
		Settings.getInstance().setProperty(PROP_USE_CURRENT_TAB, useCurrentEditorCbx.isSelected());
		if (columnWidthChanged() && GuiSettings.getSaveBookmarkColWidths())
		{
			saveColumnWidths();
		}
	}

	@Override
	public void mouseClicked(MouseEvent e)
	{
		if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1)
		{
			selectValueAndClose();
		}
	}

	@Override
	public void mousePressed(MouseEvent e)
	{
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
	}

	@Override
	public void mouseEntered(MouseEvent e)
	{
	}

	@Override
	public void mouseExited(MouseEvent e)
	{
	}

	public static void selectBookmark(final MainWindow window)
	{
		if (window == null)
		{
			LogMgr.logError("BookmarkSelector.selectBookmark()", "selectBookmark() called with a NULL window!", new Exception("Invalid window"));
			return;
		}

		if (GuiSettings.updateAllBookmarksOnOpen())
		{
			BookmarkManager.getInstance().updateInBackground(window);
		}
		else
		{
			// default behaviour is to update only the current panel, because that should only
			// be the one that is stale. The bookmarks for a tab are refreshed when the tab is left
			// so the bookmarks for all others should be correct
			MainPanel panel = window.getCurrentPanel();
			BookmarkManager.getInstance().updateInBackground(window, panel, false);
		}

		final BookmarkSelector picker = new BookmarkSelector(window);

		ValidatingDialog dialog = new ValidatingDialog(window, ResourceMgr.getString("TxtWinTitleBookmark"), picker);
		ResourceMgr.setWindowIcons(dialog, "bookmark");
		picker.dialog = dialog;
		String prop = GuiSettings.PROP_BOOKMARK_PREFIX + "select";
		if (!Settings.getInstance().restoreWindowSize(dialog, prop))
		{
			dialog.setSize(450,350);
		}
		WbSwingUtilities.center(dialog, window);
		dialog.setVisible(true);

		Settings.getInstance().storeWindowSize(dialog, prop);
		picker.saveSettings();

		if (!dialog.isCancelled() && picker.selectedBookmark != null)
		{
			EventQueue.invokeLater(() ->
      {
        window.jumpToBookmark(picker.selectedBookmark);
      });
		}
	}
}
