/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
 *//*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
package workbench.gui.bookmarks;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.FocusTraversalPolicy;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import workbench.interfaces.MainPanel;
import workbench.interfaces.Reloadable;
import workbench.interfaces.ValidatingComponent;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.ReloadAction;
import workbench.gui.components.ColumnWidthOptimizer;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.FlatButton;
import workbench.gui.components.ValidatingDialog;
import workbench.gui.components.WbLabel;
import workbench.gui.components.WbScrollPane;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbToolbarButton;
import workbench.gui.components.WbTraversalPolicy;
import workbench.gui.renderer.RendererSetup;

import workbench.storage.DataStore;
import workbench.storage.NamedSortDefinition;
import workbench.storage.filter.ContainsComparator;
import workbench.storage.filter.DataRowExpression;

import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class BookmarkSelector
	extends JPanel
implements KeyListener, MouseListener, Reloadable, ActionListener, ValidatingComponent
{
	private static final String PROP_PREFIX = "workbench.gui.bookmarks.";
	private JTextField filterValue;
	private JComboBox tabSelector;
	private JLabel info;
	private WbTable bookmarks;
	private WbScrollPane scroll;
	private MainWindow window;
	private ValidatingDialog dialog;
	private NamedScriptLocation selectedBookmark;
	private long lastKeyTyped;
	private String searchValue;
	private int pauseForIncremental;
	private NamedSortDefinition savedSort;
	private JLabel searchInfo;
	private boolean searchInfoVisible;
	private Timer idleTimer;

	public BookmarkSelector(MainWindow win)
	{
		super(new GridBagLayout());
		window = win;

		pauseForIncremental = Settings.getInstance().getIntProperty(PROP_PREFIX + "incremental.pause", 500);

		filterValue = new JTextField();
		filterValue.addKeyListener(this);
		filterValue.addActionListener(this);
		filterValue.setToolTipText(ResourceMgr.getString("TxtBookmarkFilter"));

		bookmarks = new WbTable(false, false, false);
		bookmarks.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		bookmarks.setReadOnly(true);
		bookmarks.setRendererSetup(new RendererSetup(false));
		bookmarks.addMouseListener(this);
		bookmarks.setColumnSelectionAllowed(false);
		bookmarks.setRowSelectionAllowed(true);
		bookmarks.getHeaderRenderer().setShowPKIcon(true);
		bookmarks.setSortIgnoreCase(true);

		tabSelector = new JComboBox(getTabs());
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
				selectValue();
			}
		};

		bookmarks.configureEnterKeyAction(selectValue);
		bookmarks.getActionMap().put("picker-next-comp", nextComponent);
		bookmarks.getActionMap().put("picker-prev-comp", prevComponent);
		bookmarks.addKeyListener(this);

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

		searchInfo = new JLabel("MMMM");
		searchInfo.setBorder(new CompoundBorder(new BevelBorder(BevelBorder.LOWERED), new EmptyBorder(1,1,1,1)));
		searchInfo.setBackground(filterValue.getBackground());
//		Color clr = UIManager.getDefaults().getColor("TextField.inactiveForeground");
		searchInfo.setForeground(filterValue.getForeground());
		searchInfo.setOpaque(true);

		info = new JLabel(ResourceMgr.getFormattedString("TxtBookmarkHelp", BookmarkAnnotation.ANNOTATION));
		gc.gridy = 3; // leave one row for the "search indicator" panel
		gc.weightx = 0.0;
		gc.weighty = 0.0;
		gc.fill = GridBagConstraints.BOTH;
		gc.insets = new Insets(10,0,10,0);
		add(info, gc);

		WbTraversalPolicy pol = new WbTraversalPolicy();
		pol.addComponent(tabSelector);
		pol.addComponent(filterValue);
		pol.addComponent(bookmarks);
		setFocusCycleRoot(true);
		bookmarks.setFocusCycleRoot(false);
		setFocusTraversalPolicy(pol);

		String sort = Settings.getInstance().getProperty(PROP_PREFIX + "sort", null);
		if (StringUtil.isNonBlank(sort))
		{
			savedSort = NamedSortDefinition.parseDefinitionString(sort);
			if (savedSort != null)
			{
				savedSort.setIgnoreCase(true);
				LogMgr.logDebug("BookmarkSelector.<init>", "Using saved sort definition: " + sort);
			}
			else
			{
				LogMgr.logWarning("BookmarkSelector.<init>", "Invalid sort definition saved: " + sort);
			}
		}
		idleTimer = new Timer(pauseForIncremental, this);
		idleTimer.setInitialDelay(0);
	}

	private void hideSearchPanel()
	{
		remove(searchInfo);
		searchInfoVisible = false;
		invalidate();
		validate();
		if (idleTimer.isRunning())
		{
			idleTimer.stop();
		}
	}

	private void showSearchPanel(String text)
	{
		searchInfo.setText(text);
		if (!searchInfoVisible)
		{
			GridBagConstraints gc = new GridBagConstraints();
			gc.gridx = 0;
			gc.gridy = 2;
			gc.insets = new Insets(5,0,0,2);
			gc.anchor = GridBagConstraints.FIRST_LINE_START;
			gc.fill = GridBagConstraints.HORIZONTAL;
			gc.weightx = 0.0;
			gc.weighty = 0.0;
			add(searchInfo, gc);
			searchInfoVisible = true;
		}
	}

	private JPanel createFilterPanel()
	{
		JPanel filterPanel = new JPanel(new GridBagLayout());
		WbLabel lbl = new WbLabel();
		lbl.setText(ResourceMgr.getString("LblFkFilterValue"));
		lbl.setToolTipText(filterValue.getToolTipText());
		lbl.setLabelFor(filterValue);
		lbl.setBorder(new EmptyBorder(0, 0, 0, 10));

		ReloadAction doReload = new ReloadAction(this);
		doReload.setTooltip(ResourceMgr.getString("TxtBookmarkReload"));
		FlatButton reload = new FlatButton(doReload);
		reload.setText(null);
		reload.setMargin(WbToolbarButton.MARGIN);

		WbLabel tabLbl = new WbLabel();
		tabLbl.setText(ResourceMgr.getString("LblBookPanel"));
		tabLbl.setLabelFor(tabSelector);

		String all = ResourceMgr.getString("LblBookPanelAll");
		WbSwingUtilities.setMinimumSize(tabSelector, all.length() * 2);

		GridBagConstraints gc = new GridBagConstraints();
		Insets empty = new Insets(0,0,0,0);
		Insets topTwo = new Insets(2,0,0,0);

		// first line
		gc.gridx = 0;
		gc.gridy = 0;
		gc.anchor = GridBagConstraints.FIRST_LINE_START;
		gc.fill = GridBagConstraints.HORIZONTAL;
		gc.weightx = 0.0;
		gc.weighty = 0.0;
		gc.insets = topTwo;
		filterPanel.add(tabLbl, gc);
		gc.gridx = 1;
		gc.gridy = 0;
		gc.gridwidth = 2;
		gc.insets = empty;
		gc.fill = GridBagConstraints.NONE;
		filterPanel.add(tabSelector, gc);

		// second line
		gc.gridx = 0;
		gc.gridy = 1;
		gc.insets = new Insets(8,0,0,0);
		gc.gridwidth = 1;
		filterPanel.add(lbl, gc);
		gc.gridx = 1;
		gc.gridwidth = 1;
		gc.weightx = 1.0;
		gc.weighty = 0.0;
		gc.insets = new Insets(4,0,0,0);
		gc.fill = GridBagConstraints.HORIZONTAL;
		filterPanel.add(filterValue, gc);

		gc.gridx = 2;
		gc.gridwidth = 2;
		gc.weightx = 0.0;
		gc.weighty = 1.0;
		gc.insets = topTwo;
		gc.fill = GridBagConstraints.NONE;
		filterPanel.add(reload, gc);

		return filterPanel;
	}

	void done()
	{
		saveSettings();
		if (idleTimer.isRunning())
		{
			idleTimer.stop();
			idleTimer = null;
		}
	}

	private Object[] getTabs()
	{
		List<String> tabIds = BookmarkManager.getInstance().getTabs(window);

		List<TabEntry> entries = new ArrayList<TabEntry>();
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
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == this.filterValue)
		{
			applyFilter();
		}
		else if (e.getSource() == this.tabSelector)
		{
			loadBookmarks();
		}
		else if (e.getSource() == idleTimer)
		{
			long pause = System.currentTimeMillis() - lastKeyTyped;
			if (pause > pauseForIncremental)
			{
				hideSearchPanel();
			}
		}
	}

	@Override
	public void reload()
	{
		BookmarkManager.getInstance().clearBookmarksForWindow(window.getWindowId());
		BookmarkManager.getInstance().updateBookmarks(window);
		loadBookmarks();
	}

	protected void resetFilter()
	{
		bookmarks.resetFilter();
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
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				NamedSortDefinition oldSort;
				if (savedSort != null)
				{
					oldSort = savedSort;
					savedSort = null;
				}
				else
				{
					oldSort	= bookmarks.getCurrentSort();
				}
				DataStoreTableModel model = new DataStoreTableModel(data);
				model.setAllowEditing(false);
				bookmarks.setModel(model, true);

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
					ColumnWidthOptimizer optimizer = new ColumnWidthOptimizer(bookmarks);
					optimizer.optimizeAllColWidth(true);
				}

				// always select at least one row.
				// as the focus is set to the table containing the lookup data,
				// the user can immediately use the cursor keys to select one entry.
				bookmarks.getSelectionModel().setSelectionInterval(0,0);
				bookmarks.scrollToRow(0);

				long duration = System.currentTimeMillis() - start;
				LogMgr.logDebug("BookmarkSelector.loadBookmarks()", "Loading bookmarks took: " + duration + "ms");

				bookmarks.requestFocusInWindow();
				WbSwingUtilities.showDefaultCursorOnWindow(BookmarkSelector.this);
			}
		});

	}

	protected void applyFilter()
	{
		ContainsComparator comp = new ContainsComparator();
		DataRowExpression filter = new DataRowExpression(comp, filterValue.getText());
		filter.setIgnoreCase(true);
		bookmarks.applyFilter(filter);
		bookmarks.getSelectionModel().setSelectionInterval(0,0);
		bookmarks.scrollToRow(0);
	}

	private boolean isAltPressed(KeyEvent e)
	{
		return ((e.getModifiers() & ActionEvent.ALT_MASK) == ActionEvent.ALT_MASK);
	}

	@Override
	public void keyTyped(final KeyEvent e)
	{
		if (e.getSource() == this.filterValue)
		{
			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					if (e.getKeyChar() == KeyEvent.VK_ESCAPE)
					{
						resetFilter();
					}
					else if (e.getKeyChar() == KeyEvent.VK_ENTER)
					{
						selectValue();
					}
					else
					{
						applyFilter();
					}
				}
			});
		}
		else if (e.getSource() == bookmarks && !isAltPressed(e)) // Alt+Something might be needed to transfer the focus to a different component
		{
			long pause = System.currentTimeMillis() - lastKeyTyped;
			lastKeyTyped = System.currentTimeMillis();
			if (searchValue != null && pause < pauseForIncremental)
			{
				searchValue += String.valueOf(e.getKeyChar());
			}
			else
			{
				searchValue = String.valueOf(e.getKeyChar());
			}
			if (idleTimer.isRunning())
			{
				idleTimer.restart();
			}
			else
			{
				idleTimer.start();
			}
			showSearchPanel(searchValue);
			jumpBookmark(searchValue.toLowerCase());
		}
	}

	private void jumpBookmark(String startsWith)
	{
		final int row = findBookmark(startsWith);
		if (row > -1)
		{
			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					bookmarks.getSelectionModel().setSelectionInterval(row, row);
					bookmarks.scrollToRow(row);
				}
			});
		}
	}

	private int findBookmark(String start)
	{
		int currentRow = bookmarks.getSelectedRow();
		int rowCount = bookmarks.getRowCount();
		for (int row=currentRow + 1; row < rowCount; row++)
		{
			String name = bookmarks.getValueAsString(row, 0);
			if (name.toLowerCase().startsWith(start))
			{
				return row;
			}
		}
		for (int row=0; row < currentRow; row++)
		{
			String name = bookmarks.getValueAsString(row, 0);
			if (name.toLowerCase().startsWith(start))
			{
				return row;
			}
		}
		return -1;
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
	}

	@Override
	public boolean validateInput()
	{
		return bookmarks.getSelectedRowCount() == 1;
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
		dialog.approveAndClose();
	}

	public void saveSettings()
	{
		NamedSortDefinition sort = bookmarks.getCurrentSort();
		String sortDef = sort.getDefinitionString();
		Settings.getInstance().setProperty(PROP_PREFIX + "sort", sortDef);
	}

	@Override
	public void mouseClicked(MouseEvent e)
	{
		if (e.getClickCount() == 2)
		{
			selectValue();
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

		final BookmarkSelector picker = new BookmarkSelector(window);

		if (GuiSettings.updateAllBookmarksOnSelect())
		{
			BookmarkManager.getInstance().updateInBackground(window);
		}
		else
		{
			// default behaviour is to update only the current panel, because that should only
			// be the one that is stale. The bookmarks for a tab are refreshed when the tab is left
			// so the bookmarks for all others should be correct
			MainPanel panel = window.getCurrentPanel();
			BookmarkManager.getInstance().updateInBackground(window, panel);
		}

		ValidatingDialog dialog = new ValidatingDialog(window, ResourceMgr.getString("TxtWinTitleBookmark"), picker);
		picker.dialog = dialog;
		String prop = PROP_PREFIX + "select";
		if (!Settings.getInstance().restoreWindowSize(dialog, prop))
		{
			dialog.setSize(450,350);
		}
		WbSwingUtilities.center(dialog, window);
		dialog.setVisible(true);

		Settings.getInstance().storeWindowSize(dialog, prop);
		picker.done();

		if (!dialog.isCancelled() && picker.selectedBookmark != null)
		{
			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					window.jumpToBookmark(picker.selectedBookmark);
				}
			});
		}
	}
}
