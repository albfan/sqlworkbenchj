/*
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
package workbench.gui.editor;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableColumnModel;

import workbench.interfaces.MainPanel;
import workbench.interfaces.ValidatingComponent;
import workbench.resource.GuiSettings;
import workbench.resource.IconMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.bookmarks.NamedScriptLocation;
import workbench.gui.components.ColumnWidthOptimizer;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.HistoryTextField;
import workbench.gui.components.SelectionHandler;
import workbench.gui.components.ValidatingDialog;
import workbench.gui.components.WbScrollPane;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbTraversalPolicy;
import workbench.gui.renderer.RendererSetup;
import workbench.gui.sql.SqlPanel;

import workbench.storage.DataStore;

import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class GlobalSearch
	extends JPanel
	implements MouseListener, ActionListener, ValidatingComponent, KeyListener
{
  private final String settingsKey = "workbench.gui.global.search.";
	private final String caseProperty = settingsKey + "ignoreCase";
	private final String wordProperty = settingsKey + "wholeWord";
	private final String regexProperty = settingsKey + "useRegEx";
	private final String contextLinesProperty = settingsKey + "contextlines";
  private final String dialogProperty = settingsKey + "dialog";

	private WbTable searchResult;
	private WbScrollPane scroll;
	private final MainWindow window;
	private ValidatingDialog dialog;
	private SearchResult selectedResult;
  private JButton startSearch;
	private JCheckBox ignoreCase;
	private JCheckBox wholeWord;
	private JCheckBox useRegEx;
  private JTextField contextLines;
	private HistoryTextField criteria;
	private SelectionHandler keyHandler;

	public GlobalSearch(MainWindow win)
	{
		super(new GridBagLayout());
		window = win;

		searchResult = new WbTable(false, false, false);
    searchResult.setAutoAdjustColumnWidths(false);
		searchResult.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		searchResult.setReadOnly(true);
		searchResult.setRendererSetup(new RendererSetup(false));
		searchResult.addMouseListener(this);
		searchResult.setColumnSelectionAllowed(false);
    searchResult.setAutoscrolls(false);
		searchResult.setRowSelectionAllowed(true);
		searchResult.setSortIgnoreCase(true);
		searchResult.setShowPopupMenu(false);
		keyHandler = new SelectionHandler(searchResult);

    startSearch = new JButton(ResourceMgr.getString("LblStartSearch"));
    startSearch.addActionListener(this);

		scroll = new WbScrollPane(searchResult);

    JPanel searchPanel = createSearchPanel();

		criteria.getEditor().getEditorComponent().addKeyListener(this);

		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx = 0;
		gc.gridy = 0;
		gc.anchor = GridBagConstraints.FIRST_LINE_START;
		gc.fill = GridBagConstraints.HORIZONTAL;
		gc.weightx = 1.0;
		gc.weighty = 0.0;
		add(searchPanel, gc);

		gc.gridy = 1;
		gc.weightx = 1.0;
		gc.weighty = 1.0;
		gc.fill = GridBagConstraints.BOTH;
		gc.insets = new Insets(5,0,0,0);
		add(scroll, gc);

		WbTraversalPolicy pol = new WbTraversalPolicy();
    pol.addComponent(criteria);
    pol.addComponent(ignoreCase);
    pol.addComponent(wholeWord);
    pol.addComponent(useRegEx);
    pol.addComponent(startSearch);
		setFocusCycleRoot(true);
		searchResult.setFocusCycleRoot(false);
		setFocusTraversalPolicy(pol);
    restoreSettings();
	}

  private JPanel createSearchPanel()
  {
		this.ignoreCase = new JCheckBox(ResourceMgr.getString("LblSearchIgnoreCase"));
		this.ignoreCase.setToolTipText(ResourceMgr.getDescription("LblSearchIgnoreCase"));

		this.wholeWord = new JCheckBox(ResourceMgr.getString("LblSearchWordsOnly"));
		this.wholeWord.setToolTipText(ResourceMgr.getDescription("LblSearchWordsOnly"));

		this.useRegEx = new JCheckBox(ResourceMgr.getString("LblSearchRegEx"));
		this.useRegEx.setToolTipText(ResourceMgr.getDescription("LblSearchRegEx"));

		JLabel criteriaLabel = new JLabel(ResourceMgr.getString("LblSearchCriteria"));
		this.criteria = new HistoryTextField(".search");

    int gap = (int)(IconMgr.getInstance().getSizeForLabel() / 2);

    Border gapBorder = new EmptyBorder(0, 0, 0, gap);

    JPanel input = new JPanel(new BorderLayout());

    criteriaLabel.setBorder(gapBorder);

    input.add(criteriaLabel, BorderLayout.LINE_START);
    input.add(criteria, BorderLayout.CENTER);

    JPanel options = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
    options.add(ignoreCase);
    options.add(wholeWord);
    options.add(useRegEx);
    options.add(Box.createHorizontalStrut(gap*2));
    options.add(new JLabel("Context lines"));
    options.add(Box.createHorizontalStrut(gap));

    contextLines = new JTextField(4);
    options.add(contextLines);

    JPanel result = new JPanel(new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.anchor = GridBagConstraints.LINE_START;
    gc.gridx = 0;
    gc.gridy = 0;
    gc.gridwidth = 2;
    gc.fill = GridBagConstraints.BOTH;
    gc.weightx = 1.0;

    result.add(input, gc);

    gc.insets = new Insets(gap, 0, 0, 0);
    gc.gridy ++;
    gc.gridwidth = 1;

    result.add(options, gc);
    gc.gridx = 1;
    gc.weightx = 0.0;
    gc.anchor = GridBagConstraints.LINE_END;
    result.add(startSearch, gc);

    return result;
  }

  private SortedMap<String, SearchAndReplace> getSearcher()
  {
    int count = window.getTabCount();
    SortedMap<String, SearchAndReplace> replacerList = new TreeMap<>();
    for (int i = 0; i < count; i++)
    {
      MainPanel panel = window.getSqlPanel(i);
      if (panel instanceof SqlPanel)
      {
        SearchAndReplace replacer = ((SqlPanel)panel).getEditor().getReplacer();
        replacerList.put(panel.getId(), replacer);
      }
    }
    return replacerList;
  }

	private int[] getColumnWidths()
	{
		TableColumnModel colMod = searchResult.getColumnModel();
		if (colMod == null) return null;

		int count = colMod.getColumnCount();
    if (count == 0) return null;

		int[] result = new int[count];

		for (int i=0; i<count; i++)
		{
			result[i] = colMod.getColumn(i).getWidth();
		}
		return result;
	}

	private void saveColumnWidths()
	{
    if (GuiSettings.getSaveSearchAllColWidths())
    {
      int[] widths = getColumnWidths();
      if (widths == null) return;
      Settings.getInstance().setProperty(settingsKey + ".search.colwidths", StringUtil.arrayToString(widths));
    }
	}

	private int[] getSavedColumnWidths()
	{
		String widths = Settings.getInstance().getProperty(settingsKey  + ".search.colwidths", null);
		return StringUtil.stringToArray(widths);
	}

  private int getContextLines()
  {
    return StringUtil.getIntValue(contextLines.getText(), 0);
  }

  private void doSearch()
  {
    int[] widths = getColumnWidths();
    if (widths == null)
    {
      widths = getSavedColumnWidths();
    }

    SortedMap<String, SearchAndReplace> searcherList = getSearcher();
    List<SearchResult> result = new ArrayList<>();
    int lines = getContextLines();
    String expr = criteria.getText();
    boolean ignore = ignoreCase.isSelected();
    boolean wordsOnly = wholeWord.isSelected();
    boolean regex = useRegEx.isSelected();

    for (Map.Entry<String, SearchAndReplace> entry : searcherList.entrySet())
    {
      List<SearchResult> hits = entry.getValue().findAll(expr, ignore, wordsOnly, regex, lines);
      for (SearchResult sr : hits)
      {
        sr.setTabId(entry.getKey());
      }
      result.addAll(hits);
    }

    DataStore ds = createDataStore();
    for (SearchResult hit : result)
    {
      int row = ds.addRow();
      ds.setValue(row, 0, window.getTabTitleById(hit.getTabId()));
      ds.setValue(row, 1, hit.getLineNumber() + 1);
      ds.setValue(row, 2, hit.getLineText());
      ds.getRow(row).setUserObject(hit);
    }
    DataStoreTableModel model = new DataStoreTableModel(ds);
    model.setAllowEditing(false);
    searchResult.setModel(model, true);
    searchResult.setMultiLine(2);
    if (widths == null)
    {
      ColumnWidthOptimizer optimizer = new ColumnWidthOptimizer(searchResult);
      optimizer.optimizeAllColWidth(true);
    }
    else
    {
      searchResult.applyColumnWidths(widths);
    }
    searchResult.optimizeRowHeight();
    selectedResult = null;
  }

	private DataStore createDataStore()
	{
		String[] columns = new String[] { ResourceMgr.getPlainString("LblGSTabName"),
                                      ResourceMgr.getPlainString("LblGSLineNr"),
                                      ResourceMgr.getPlainString("LblGSLineContent")};

		int[] types = new int[] { Types.VARCHAR, Types.INTEGER, Types.VARCHAR};
    int[] sizes = new int[] { 20, 5, 50 };
		return new DataStore(columns, types, sizes);
	}

  @Override
  public void keyPressed(KeyEvent e)
  {
    keyHandler.handleKeyPressed(e);
  }

  @Override
  public void keyReleased(KeyEvent e)
  {
  }

  @Override
  public void keyTyped(final KeyEvent e)
  {
    if (e.getSource() == this.criteria.getEditor().getEditorComponent() && e.getKeyChar() == KeyEvent.VK_ENTER)
    {
      if ((searchResult.getRowCount() == 0 || searchResult.getSelectedRowCount() == 0) && StringUtil.isNonBlank(criteria.getText()))
      {
        doSearch();
      }
      else if (searchResult.getSelectedRowCount() == 1)
      {
        selectValueAndClose();
      }
    }
  }

	@Override
	public void actionPerformed(ActionEvent e)
	{
    if (e.getSource() == startSearch)
    {
      doSearch();
    }
	}

	@Override
	public boolean validateInput()
	{
    if (dialog.getSelectedOption() == 2)
    {
      // just close the dialog, don't select anything
      return true;
    }

    if (searchResult.getSelectedRowCount() == 1)
		{
      selectedResult = getSelectedSearchLocation();

      if (dialog.getSelectedOption() == 0) // show line button
      {
        jumpTo(selectedResult);
        return false; // don't close the dialog
      }
      if (dialog.getSelectedOption() == 1) // jump to (select & close) button
      {
        jumpTo(selectedResult);
        return true; // close dialog
      }
		}
		return false;
	}

  @Override
  public void componentWillBeClosed()
  {
    saveSettings();
  }

	@Override
	public void componentDisplayed()
	{
    criteria.requestFocusInWindow();
    criteria.selectAll();
	}

  public SearchResult getSelectedSearchLocation()
  {
    int row = searchResult.getSelectedRow();
    if (row < 0) return null;
    return (SearchResult)searchResult.getDataStore().getRow(row).getUserObject();
  }

	public void selectValueAndClose()
	{
    selectedResult = getSelectedSearchLocation();
    jumpTo(selectedResult);
		dialog.approveAndClose();
	}

  public void restoreSettings()
  {
		criteria.restoreSettings(Settings.getInstance(), settingsKey + ".search");
    ignoreCase.setSelected(Settings.getInstance().getBoolProperty(caseProperty));
    wholeWord.setSelected(Settings.getInstance().getBoolProperty(wordProperty));
    useRegEx.setSelected(Settings.getInstance().getBoolProperty(regexProperty));
    int lines = Settings.getInstance().getIntProperty(contextLinesProperty, 0);
    contextLines.setText(Integer.toString(lines));
  }

	public void saveSettings()
	{
    saveColumnWidths();
		criteria.addToHistory(criteria.getText());
		criteria.saveSettings(Settings.getInstance(), settingsKey + ".search");
		Settings.getInstance().setProperty(caseProperty, ignoreCase.isSelected());
		Settings.getInstance().setProperty(wordProperty, wholeWord.isSelected());
		Settings.getInstance().setProperty(regexProperty, useRegEx.isSelected());
		Settings.getInstance().setProperty(contextLinesProperty, getContextLines());
    Settings.getInstance().storeWindowSize(dialog, dialogProperty);
    Settings.getInstance().storeWindowPosition(dialog, dialogProperty);
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

  private void jumpTo(SearchResult searchLocation)
  {
    final NamedScriptLocation target = new NamedScriptLocation("", searchLocation.getOffset(), searchLocation.getTabId());
    EventQueue.invokeLater(new Runnable()
    {
      @Override
      public void run()
      {
        window.jumpToBookmark(target);
      }
    });
  }

	public void displaySearchDialog()
	{
    String[] options = new String[] { ResourceMgr.getString("LblGSShowLine"), ResourceMgr.getString("LblGSJump"), ResourceMgr.getString("LblClose") };
		dialog = new ValidatingDialog(window, ResourceMgr.getString("TxtWinGlobalSearch"), this, options, false);
		ResourceMgr.setWindowIcons(dialog, "find-all");

		if (!Settings.getInstance().restoreWindowSize(dialog, dialogProperty))
		{
			dialog.setSize(450,350);
		}

		if (!Settings.getInstance().restoreWindowPosition(dialog, dialogProperty))
		{
			WbSwingUtilities.center(dialog, window);
		}
		dialog.setVisible(true);
	}

}
