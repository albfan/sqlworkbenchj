/*
 * QuickFilterPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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
package workbench.gui.components;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.regex.PatternSyntaxException;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;

import workbench.interfaces.CriteriaPanel;
import workbench.interfaces.PropertyStorage;
import workbench.interfaces.QuickFilter;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.FilterDataAction;
import workbench.gui.actions.FilterPickerAction;
import workbench.gui.actions.QuickFilterAction;
import workbench.gui.actions.ReloadAction;
import workbench.gui.actions.ResetFilterAction;
import workbench.gui.actions.WbAction;
import workbench.gui.dbobjects.QuickFilterExpressionBuilder;
import workbench.gui.filter.DefineFilterExpressionPanel;
import workbench.gui.filter.FilterDefinitionManager;

import workbench.storage.filter.ColumnExpression;
import workbench.storage.filter.FilterExpression;

import workbench.util.StringUtil;

/**
 * A small panel which filters a table. Optionally a dropdown to select the filter
 * column can be displayed. The context menu of the input field will always have
 * the ability to select the filter column
 *
 * The available columns are retrieved from the table that should be filtered
 *
 * @author  Thomas Kellerer
 */
public class QuickFilterPanel
	extends JPanel
	implements QuickFilter, CriteriaPanel, ActionListener, MouseListener,
		PropertyChangeListener, KeyListener
{
	private final WbTable searchTable;
	private String searchColumn;

	private HistoryTextField filterValue;
	private WbToolbar toolbar;
	private JComboBox columnDropDown;
	private QuickFilterAction filterAction;
	private String[] columnList;
	private final boolean showColumnDropDown;
  private FilterDefinitionManager filterMgr;
	private JCheckBoxMenuItem[] columnItems;
	private TextComponentMouseListener textListener;
	private boolean assumeWildcards;
	private boolean autoFilterEnabled;
	private ReloadAction delegateFilterAction;
  private boolean ignoreEvents;

	public QuickFilterPanel(WbTable table, boolean showColumnDropDown, String historyProperty)
  {
    this(table, showColumnDropDown, null, historyProperty);
  }

	public QuickFilterPanel(WbTable table, boolean showColumnDropDown, FilterDefinitionManager filterManager, String historyProperty)
	{
		super();
		this.searchTable = table;
		this.searchTable.addPropertyChangeListener("model", this);
		this.showColumnDropDown = showColumnDropDown;
    this.filterMgr = filterManager;
		this.initGui(historyProperty);
	}

	public void setReloadAction(ReloadAction action)
	{
		this.delegateFilterAction = action;
	}

	public void dispose()
	{
		WbAction.dispose(filterAction);
		delegateFilterAction = null;
		if (filterValue != null) filterValue.dispose();
		if (textListener != null) textListener.dispose();
		if (toolbar != null) toolbar.removeAll();
		if (searchTable != null) searchTable.removePropertyChangeListener(this);
		if (columnDropDown != null) columnDropDown.removeActionListener(this);
	}

	@Override
	public void setEnabled(boolean flag)
	{
		super.setEnabled(flag);
		toolbar.setEnabled(flag);
		filterValue.setEnabled(flag);
		setActionsEnabled(flag);
	}

	public void setActionsEnabled(boolean flag)
	{
		filterAction.setEnabled(flag);
		if (searchTable != null)
		{
			ResetFilterAction action = searchTable.getResetFilterAction();
			if (action != null)
			{
				action.setEnabled(flag);
			}
		}
	}

	public void setFilterOnType(boolean flag)
	{
		autoFilterEnabled = flag;
	}

	public void setAlwaysUseContainsFilter(boolean flag)
	{
		this.assumeWildcards = flag;
	}

	private void initDropDown()
	{
		if (this.columnList == null)
		{
			columnDropDown = new JComboBox(new String[] { "        " });
		}
		else
		{
			columnDropDown = new JComboBox(columnList);
		}
		columnDropDown.addActionListener(this);
		columnDropDown.setSelectedIndex(0);
		columnDropDown.setToolTipText(ResourceMgr.getString("TxtQuickFilterColumnSelector"));

		GridBagConstraints gc = new GridBagConstraints();
		gc.anchor = GridBagConstraints.WEST;
		gc.gridx = 2;
		gc.fill = GridBagConstraints.NONE;
		gc.weightx = 0.0;
    gc.insets = new Insets(0, 5, 0, 2);
		this.add(columnDropDown, gc);
	}

	public void setFilterTooltip()
	{
		String col = "";
		if (searchColumn != null)
		{
			col = ResourceMgr.getFormattedString("TxtQuickFilterCurrCol", searchColumn);
		}
		String msg;
		if (GuiSettings.getUseRegexInQuickFilter())
		{
			msg = ResourceMgr.getFormattedString("TxtQuickFilterRegexHint", col);
		}
		else
		{
			msg = ResourceMgr.getFormattedString("TxtQuickFilterColumnHint", col);
		}
		this.filterValue.setToolTipText(msg);
	}

	@Override
	public void setToolTipText(String tip)
	{
		this.filterValue.setToolTipText(tip);
	}

	private void initGui(String historyProperty)
	{
		GridBagConstraints gridBagConstraints;

		setLayout(new GridBagLayout());
		setBorder(WbSwingUtilities.EMPTY_BORDER);

		filterValue = new HistoryTextField(historyProperty);
		setFilterTooltip();
		filterValue.setColumns(10);

		initPopup();

		toolbar = new WbToolbar();
		filterAction = new QuickFilterAction(this);
    filterAction.setUseLabelIconSize(true);
		ResetFilterAction resetFilterAction = this.searchTable.getResetFilterAction();
    resetFilterAction.setUseLabelIconSize(true);

		toolbar.add(this.filterAction);

    if (filterMgr != null)
    {
      FilterDataAction define = new FilterDataAction(searchTable);
      define.setMenuTextByKey("MnuTxtDefineFilter");
      define.removeIcon();

      WbAction open = new WbAction()
      {
        @Override
        public void executeAction(ActionEvent e)
        {
          FilterExpression filter = DefineFilterExpressionPanel.loadFilter(QuickFilterPanel.this, filterMgr);
          if (filter != null)
          {
            searchTable.applyFilter(filter);
          }
        }
      };
      open.setMenuTextByKey("MnuTxtLoadFilter");
      FilterPickerAction fp = new FilterPickerAction(searchTable, filterMgr, define, open);
      toolbar.add(fp);
    }
    
		toolbar.add(resetFilterAction);
		toolbar.setMargin(WbSwingUtilities.getEmptyInsets());
		toolbar.setBorderPainted(true);

		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.anchor = GridBagConstraints.WEST;
		gridBagConstraints.gridx = 0;
		add(toolbar, gridBagConstraints);

		gridBagConstraints.gridx = 1;
		gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstraints.weightx = 1;
		add(filterValue, gridBagConstraints);

		if (showColumnDropDown)
		{
			initDropDown();
		}

		WbTraversalPolicy pol = new WbTraversalPolicy();
		pol.setDefaultComponent(filterValue);
		pol.addComponent(filterValue);
		pol.addComponent(filterAction.getToolbarButton());
		pol.addComponent(resetFilterAction.getToolbarButton());
		setFocusTraversalPolicy(pol);
		setFocusCycleRoot(false);

		filterValue.addActionListener(this);
		Component ed = filterValue.getEditor().getEditorComponent();
		ed.addKeyListener(this);

		Settings.getInstance().addPropertyChangeListener(this, GuiSettings.PROPERTY_QUICK_FILTER_REGEX);
	}

	public void setToolbarBorder(Border b)
	{
		toolbar.setBorder(b);
	}

	public void addToToolbar(WbAction action, int index)
	{
		toolbar.add(action, index);
	}

	private void initPopup()
	{
		if (columnList == null) return;

		Component ed = filterValue.getEditor().getEditorComponent();
		if (this.textListener != null) ed.removeMouseListener(this.textListener);

		this.textListener = new TextComponentMouseListener();
		JMenu menu = new WbMenu(ResourceMgr.getString("MnuTextFilterOnColumn"));
		columnItems = new JCheckBoxMenuItem[columnList.length];
		for (int i=0; i < this.columnList.length; i++)
		{
			columnItems[i] = new JCheckBoxMenuItem(columnList[i]);
			columnItems[i].setSelected(i == 0);
			columnItems[i].putClientProperty("filterColumn", columnList[i]);
			columnItems[i].addActionListener(this);
			menu.add(columnItems[i]);
		}
		textListener.addMenuItem(menu);
		setFilterTooltip();
		ed.addMouseListener(textListener);
	}

	@Override
	public void setColumnList(String[] columns)
	{
		if (columns == null || columns.length == 0) return;
		if (StringUtil.arraysEqual(columns, columnList)) return;

		columnList = columns;

		this.searchColumn = columns[0];
		initPopup();
		if (showColumnDropDown)
		{
			if (columnDropDown == null)
			{
				initDropDown();
			}
			else
			{
				columnDropDown.setModel(new DefaultComboBoxModel(this.columnList));
			}
		}
	}

	@Override
	public void saveSettings(PropertyStorage props, String prefix)
	{
		filterValue.saveSettings(props, prefix);
	}

	@Override
	public void restoreSettings(PropertyStorage props, String prefix)
	{
    filterValue.removeActionListener(this);
		filterValue.restoreSettings(props, prefix);
		filterValue.addActionListener(this);
	}

	@Override
	public void saveSettings()
	{
		filterValue.saveSettings();
	}

	@Override
	public void restoreSettings()
	{
		filterValue.removeActionListener(this);
		filterValue.restoreSettings();
		filterValue.addActionListener(this);
	}

	@Override
	public void setFocusToEntryField()
	{
		this.filterValue.grabFocus();
	}

	@Override
	public void applyQuickFilter()
	{
		applyFilter(filterValue.getEditorText(), true);
	}

  @Override
	public void resetFilter()
	{
		applyFilter(null, false);
	}

	private void applyFilter(String filterExpression, boolean storeInHistory)
	{
    JTextField editor = (JTextField)filterValue.getEditor().getEditorComponent();
    int currentPos = editor.getCaretPosition();
		try
		{
      ignoreEvents = true;
			if (StringUtil.isEmptyString(filterExpression) || filterExpression.trim().equals("*") || filterExpression.trim().equals("%"))
			{
				searchTable.resetFilter();
			}
			else
			{
        filterValue.setText(filterExpression);
				try
				{
          QuickFilterExpressionBuilder builder = new QuickFilterExpressionBuilder();
          ColumnExpression col = builder.buildExpression(filterExpression, searchColumn, assumeWildcards);
					searchTable.applyFilter(col);
					if (storeInHistory)
					{
						filterValue.addToHistory(filterExpression);
					}
				}
				catch (PatternSyntaxException e)
				{
					searchTable.resetFilter();
					LogMgr.logError("QuickFilterPanel.applyQuickFilter()", "Cannot apply filter expression", e);
					String msg = ResourceMgr.getFormattedString("ErrBadRegex", filterExpression);
					WbSwingUtilities.showErrorMessage(this, msg);
				}
				catch (Throwable ex)
				{
					LogMgr.logError("QuickFilterPanel.applyQuickFilter()", "Cannot apply filter expression", ex);
					WbSwingUtilities.showErrorMessage(this, ex.getLocalizedMessage());
				}
			}
		}
		finally
    {
      // this is necessary to remove the text selection that is automatically done
      // because of calling filterValue.setText()
      editor.setCaretPosition(currentPos);

      // try again a bit later just to make sure.
      // A desperate measure to cope with strange issues on MacOS
      setCaretPosition(editor, currentPos);
      ignoreEvents = false;
    }
	}

  private void setCaretPosition(final JTextField editor, final int pos)
  {
    WbSwingUtilities.invokeLater(() ->
    {
      if (editor.getCaretPosition() != pos)
      {
        editor.setCaretPosition(pos);
      }
      if (editor.getSelectionStart() != editor.getSelectionEnd())
      {
        editor.select(pos, pos);
      }
    });
  }

	@Override
	public String getText()
	{
		return filterValue.getText();
	}

	@Override
	public void setText(String aText)
	{
		filterValue.setText(aText);
	}

	@Override
	public void addToToolbar(WbAction anAction, boolean atFront, boolean withSep)
	{
		JButton button = anAction.getToolbarButton();
		if (atFront)
		{
			this.toolbar.add(button, 0);
			if (withSep) this.toolbar.addSeparator(1);
		}
		else
		{
			this.toolbar.addSeparator();
			if (withSep) this.toolbar.add(button);
		}
	}


	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == filterValue && !ignoreEvents)
		{
			if (delegateFilterAction != null)
			{
				delegateFilterAction.executeAction(e);
			}
			// when typing in the editor, the combobox sends an actionPerformed event with "comboBoxEdited" followed by a "comboBoxChanged"
			// We only want to respond to the actionPerformed event if the user selected an entry from the dropdown
      // everything else related to typing is handled in the keyTyped() method.
			// The only way to distinguish between a dropdown selection and "typing" seems to be to check if the dropdown is visible
			else if (filterValue.isPopupVisible() && "comboBoxChanged".equals(e.getActionCommand()))
			{
				applyQuickFilter();
			}
		}
		else if (e.getSource() instanceof JMenuItem)
		{
			JMenuItem item = (JMenuItem)e.getSource();
			for (JCheckBoxMenuItem columnItem : columnItems)
			{
				columnItem.setSelected(false);
			}
			item.setSelected(true);
			this.searchColumn = (String)item.getClientProperty("filterColumn");
			if (this.columnDropDown != null)
			{
				this.columnDropDown.setSelectedItem(searchColumn);
			}
		}
		else if (e.getSource() == columnDropDown)
		{
			Object item = columnDropDown.getSelectedItem();
			if (item != null)
			{
				this.searchColumn = (String)item;
			}
			if (columnItems != null)
			{
				for (JCheckBoxMenuItem columnItem : columnItems)
				{
					columnItem.setSelected(columnItem.getText().equals(searchColumn));
				}
			}
		}
		setFilterTooltip();
	}

	@Override
	public void mouseClicked(MouseEvent e)
	{
	}

	@Override
	public void mouseEntered(java.awt.event.MouseEvent e)
	{
	}

	@Override
	public void mouseExited(java.awt.event.MouseEvent e)
	{
	}

	@Override
	public void mousePressed(java.awt.event.MouseEvent e)
	{
	}

	@Override
	public void mouseReleased(java.awt.event.MouseEvent e)
	{
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		if (evt.getSource() == searchTable)
		{
			int count = searchTable.getColumnCount();
			String[] names = new String[count];
			for (int i = 0; i < count; i++)
			{
				names[i] = searchTable.getColumnName(i);
			}
			setColumnList(names);
		}
		else if (evt.getPropertyName().equals(GuiSettings.PROPERTY_QUICK_FILTER_REGEX))
		{
			setFilterTooltip();
		}
	}

	private synchronized void filterByEditorValue(boolean storeInHistory)
	{
		Component comp = filterValue.getEditor().getEditorComponent();
		if (comp instanceof JTextField)
		{
			JTextField editor = (JTextField)comp;
			String value = editor.getText();
      applyFilter(value, storeInHistory);
		}
	}

	@Override
	public void keyTyped(final KeyEvent e)
	{
    if (ignoreEvents) return;

    // ignore key events with Alt or Ctrl Modifiers
    if (WbAction.isAltPressed(e.getModifiers()) || WbAction.isCtrlPressed(e.getModifiers())) return;

		EventQueue.invokeLater(() ->
    {
      if (e.getKeyChar() == KeyEvent.VK_ESCAPE)
      {
        // reset filter, do not change the input text
        // resetting the filter does not change the cursor location in the edit field
        // so there is no need to take care of that (as done in filterByEditorValue()
        applyFilter(null, false);
        e.consume();
      }
      else if (e.getKeyChar() == KeyEvent.VK_ENTER)
      {
        filterByEditorValue(true);
        e.consume();
      }
      else if (autoFilterEnabled)
      {
        filterByEditorValue(false);
        e.consume();
      }
    });
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
	}

}
