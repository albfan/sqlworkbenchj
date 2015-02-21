/*
 * QuickFilterPanel.java
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
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.ActionMap;
import javax.swing.ComponentInputMap;
import javax.swing.DefaultComboBoxModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.Border;

import workbench.interfaces.CriteriaPanel;
import workbench.interfaces.PropertyStorage;
import workbench.interfaces.QuickFilter;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.QuickFilterAction;
import workbench.gui.actions.ReloadAction;
import workbench.gui.actions.ResetFilterAction;
import workbench.gui.actions.WbAction;

import workbench.storage.filter.ColumnComparator;
import workbench.storage.filter.ColumnExpression;
import workbench.storage.filter.RegExComparator;

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
	private final ColumnComparator comparator = new RegExComparator();
	private String[] columnList;
	private final boolean showColumnDropDown;
	private JCheckBoxMenuItem[] columnItems;
	private TextComponentMouseListener textListener;
	private boolean assumeWildcards;
	private boolean autoFilterEnabled;
	private boolean enableMultiValue = true;
	private ReloadAction reload;

	public QuickFilterPanel(WbTable table, boolean showDropDown, String historyProperty)
	{
		super();
		this.searchTable = table;
		this.searchTable.addPropertyChangeListener("model", this);
		showColumnDropDown = showDropDown;
		this.initGui(historyProperty);
	}

	public void setReloadAction(ReloadAction action)
	{
		this.reload = action;
	}

	public void dispose()
	{
		WbAction.dispose(filterAction);
		reload = null;
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

	public void setEnableMultipleValues(boolean flag)
	{
		this.enableMultiValue = flag;
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

		GridBagConstraints gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.anchor = GridBagConstraints.WEST;
		gridBagConstraints.gridx = 2;
		gridBagConstraints.fill = GridBagConstraints.NONE;
		gridBagConstraints.weightx = 0.0;
		this.add(columnDropDown, gridBagConstraints);
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

		this.setLayout(new GridBagLayout());
		this.setBorder(WbSwingUtilities.EMPTY_BORDER);

		this.filterValue = new HistoryTextField(historyProperty);
		setFilterTooltip();
		filterValue.setColumns(10);

		initPopup();

		this.toolbar = new WbToolbar();
		this.filterAction = new QuickFilterAction(this);
    filterAction.setUseLabelIconSize(true);
		ResetFilterAction resetFilterAction = this.searchTable.getResetFilterAction();
    resetFilterAction.setUseLabelIconSize(true);

		this.toolbar.add(this.filterAction);
		this.toolbar.add(resetFilterAction);
		this.toolbar.setMargin(new Insets(0,0,0,0));
		toolbar.setBorderPainted(true);

		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.anchor = GridBagConstraints.WEST;
		gridBagConstraints.gridx = 0;
		this.add(toolbar, gridBagConstraints);

		gridBagConstraints.gridx = 1;
		gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstraints.weightx = 1;
		this.add(filterValue, gridBagConstraints);

		if (showColumnDropDown)
		{
			initDropDown();
		}

		InputMap im = new ComponentInputMap(this);
		ActionMap am = new ActionMap();
		setupActionMap(im, am);
		this.setInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, im);
		this.setActionMap(am);

		this.filterValue.setInputMap(JComponent.WHEN_FOCUSED, im);
		this.filterValue.setActionMap(am);

		WbTraversalPolicy pol = new WbTraversalPolicy();
		pol.setDefaultComponent(filterValue);
		pol.addComponent(filterValue);
		pol.addComponent(filterAction.getToolbarButton());
		pol.addComponent(resetFilterAction.getToolbarButton());
		this.setFocusTraversalPolicy(pol);
		this.setFocusCycleRoot(false);
		this.filterValue.addActionListener(this);
		Component ed = filterValue.getEditor().getEditorComponent();
		ed.addKeyListener(this);
		Settings.getInstance().addPropertyChangeListener(this, GuiSettings.PROPERTY_QUICK_FILTER_REGEX);
	}

	public void setToolbarBorder(Border b)
	{
		this.toolbar.setBorder(b);
	}

	public void addToToolbar(WbAction action, int index)
	{
		this.toolbar.add(action, index);
	}

	private void initPopup()
	{
		if (this.columnList == null) return;

		Component ed = filterValue.getEditor().getEditorComponent();
		if (this.textListener != null) ed.removeMouseListener(this.textListener);

		this.textListener = new TextComponentMouseListener();
		JMenu menu = new WbMenu(ResourceMgr.getString("MnuTextFilterOnColumn"));
		//menu.setIcon(null);
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

	private void setupActionMap(InputMap im, ActionMap am)
	{
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), filterAction.getActionName());
		im.put(filterAction.getAccelerator(), filterAction.getActionName());
		am.put(filterAction.getActionName(), this.filterAction);
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

	private String getPattern(String input)
		throws PatternSyntaxException
	{
		if (GuiSettings.getUseRegexInQuickFilter())
		{
			Pattern.compile(input);
			// no exception, so everything is OK
			return input;
		}

		String regex;

		if (enableMultiValue)
		{
			List<String> elements = StringUtil.stringToList(input,",", true, true, false, false);

			for (int i=0; i < elements.size(); i++)
			{
				String element = elements.get(i);
				if (assumeWildcards && !containsWildcards(element))
				{
					element = "*" + element + "*";
				}
				String regexElement = StringUtil.wildcardToRegex(element, true);
				elements.set(i, regexElement);
			}
			regex = StringUtil.listToString(elements, "|",false, '"');
		}
		else
		{
			if (assumeWildcards && !containsWildcards(input))
			{
				input = "*" + input + "*";
			}
			regex = StringUtil.wildcardToRegex(input, true);
		}

		// Test the "translated" pattern, if that throws an exception let the caller handle it
		Pattern.compile(regex);

		return regex;
	}

	@Override
	public void applyQuickFilter()
	{
		applyFilter(this.filterValue.getText(), true);
	}

	public void resetFilter()
	{
		applyFilter("", false);
	}

	private void applyFilter(String filterExpression, boolean storeInHistory)
	{
		try
		{
			filterValue.removeActionListener(this);
			if (StringUtil.isEmptyString(filterExpression) || filterExpression.trim().equals("*") || filterExpression.trim().equals("%"))
			{
				this.searchTable.resetFilter();
			}
			else
			{
				try
				{
					String pattern = getPattern(filterExpression);
					ColumnExpression col = new ColumnExpression(this.searchColumn, comparator, pattern);
					col.setIgnoreCase(true);
					searchTable.applyFilter(col);
					if (storeInHistory)
					{
						this.filterValue.addToHistory(filterExpression);
					}
				}
				catch (PatternSyntaxException e)
				{
					searchTable.resetFilter();
					LogMgr.logError("QuickFilterPanel.applyQuickFilter()", "Cannot apply filter expression", e);
					String msg = ResourceMgr.getFormattedString("ErrBadRegex", filterExpression);
					WbSwingUtilities.showErrorMessage(this, msg);
				}
				catch (Exception ex)
				{
					LogMgr.logError("QuickFilterPanel.applyQuickFilter()", "Cannot apply filter expression", ex);
					WbSwingUtilities.showErrorMessage(this, ex.getLocalizedMessage());
				}
			}
		}
		finally
		{
			this.filterValue.addActionListener(this);
		}
	}

	private boolean containsWildcards(String filter)
	{
		if (filter == null) return false;
		return filter.indexOf('%') > -1 || filter.indexOf('*') > -1;
	}

	@Override
	public String getText()
	{
		return filterValue.getText();
	}

	public void setSelectedText(String aText)
	{
		setText(aText);
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
		if (e.getSource() == filterValue  && !autoFilterEnabled)
		{
			if (reload != null)
			{
				reload.executeAction(e);
			}
			// when typing in the editor, the combobox sends a comboBoxEdited followed by a comboBoxChanged event
			// so this would also do a "filter as you type"
			// the only way to distinguish the "comboBoxChanged" event when selecting a dropdown entry
			// from the typing event is the fact that the dropDown selection has a modifier (usuall Button1)
			else if (e.getModifiers() != 0 && "comboBoxChanged".equals(e.getActionCommand()))
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
			int count = this.searchTable.getColumnCount();
			String[] names = new String[count];
			for (int i = 0; i < count; i++)
			{
				names[i] = this.searchTable.getColumnName(i);
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
			if (StringUtil.isNonBlank(value))
			{
				int currentPos = editor.getCaretPosition();
				filterValue.setText(value);
				applyFilter(value, storeInHistory);
				// this is necessary to remove the text selection that is automatically done
				// because of setting the text in applyFilter
				editor.setCaretPosition(currentPos);
			}
		}
	}

	@Override
	public void keyTyped(final KeyEvent e)
	{
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				if (e.getKeyChar() == KeyEvent.VK_ESCAPE)
				{
					// reset filter, do not change the input text
					// resetting the filter does not change the cursor location in the edit field
					// so there is no need to take care of that (as done in filterByEditorValue()
					applyFilter(null, false);
				}
				else if (e.getKeyChar() == KeyEvent.VK_ENTER)
				{
					filterByEditorValue(true);
				}
				else if (autoFilterEnabled)
				{
					filterByEditorValue(false);
				}
				// make sure the input field keeps the focus
				filterValue.requestFocusInWindow();
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
