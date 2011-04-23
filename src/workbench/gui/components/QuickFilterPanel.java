/*
 * QuickFilterPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.QuickFilterAction;
import workbench.gui.actions.ResetFilterAction;
import workbench.gui.actions.WbAction;
import workbench.interfaces.CriteriaPanel;
import workbench.interfaces.PropertyStorage;
import workbench.interfaces.QuickFilter;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
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
	private WbTable searchTable;
	private String searchColumn;

	private HistoryTextField filterValue;
	private WbToolbar toolbar;
	private JComboBox columnDropDown;
	private QuickFilterAction filterAction;
	private final ColumnComparator comparator = new RegExComparator();
	private String[] columnList;
	private boolean showColumnDropDown;
	private JCheckBoxMenuItem[] columnItems;
	private TextComponentMouseListener textListener;

	public QuickFilterPanel(WbTable table, boolean showDropDown, String historyProperty)
	{
		super();
		this.searchTable = table;
		this.searchTable.addPropertyChangeListener("model", this);
		showColumnDropDown = showDropDown;
		this.initGui(historyProperty);
	}

	public QuickFilterPanel(WbTable table, String column, String historyProperty)
	{
		super();
		this.searchTable = table;
		this.searchColumn = column;
		showColumnDropDown = false;
		this.initGui(historyProperty);
	}

	public void setFilterOnType(boolean flag)
	{
		if (flag)
		{
			enableAutoFilter();
		}
		else
		{
			disableAutoFilter();
		}
	}

	private void enableAutoFilter()
	{
		Component ed = filterValue.getEditor().getEditorComponent();
		ed.addKeyListener(this);
	}

	private void disableAutoFilter()
	{
		Component ed = filterValue.getEditor().getEditorComponent();
		ed.removeKeyListener(this);
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

	private void setFilterTooltip()
	{
		String col = "";
		if (searchColumn != null)
		{
			col = ResourceMgr.getFormattedString("TxtQuickFilterCurrCol", searchColumn);
		}
		String msg = ResourceMgr.getFormattedString("TxtQuickFilterColumnHint", col);
		this.filterValue.setToolTipText(msg);
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
		ResetFilterAction resetFilterAction = this.searchTable.getResetFilterAction();

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
		String regex = wildcardToRegex(input);

		// Test the "translated" pattern, if that throws an exception
		// let the caller handle the exception
		Pattern.compile(regex);
		LogMgr.logInfo("QuickFilterPanel.getPattern()", "Using regex pattern [" + regex + "] instead of [" + input + "]");

		return regex;
	}

	/**
	 * Convert a string that is expected to have standard "filename wildcards" to
	 * a matching regular expression
	 *
	 * @param wildcard
	 * @return a pattern that can be used as a regular expression
	 */
	public String wildcardToRegex(String wildcard)
	{
		StringBuilder s = new StringBuilder(wildcard.length() + 5);

		s.append('^');

		for (int i = 0, is = wildcard.length(); i < is; i++)
		{
			char c = wildcard.charAt(i);
			if (c == '*' || c == '%')
			{
				s.append(".*");
			}
			else if (c == '?' )
			{
				s.append(".");
			}
			else
			{
				if (StringUtil.REGEX_SPECIAL_CHARS.indexOf(c) != -1)
				{
					s.append('\\');
				}
				s.append(c);
			}
		}
		s.append('$');
		return s.toString();
	}

	@Override
	public void applyQuickFilter()
	{
		applyFilter(this.filterValue.getText(), true);
	}

	private void applyFilter(String filterExpression, boolean storeInHistory)
	{
		try
		{
			this.filterValue.removeActionListener(this);
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
					this.filterValue.addToHistory(filterExpression);
				}
				catch (Exception e)
				{
					searchTable.resetFilter();
					LogMgr.logError("QuickFilterPanel.applyQuickFilter()", "Cannot apply filter expression", e);
					String msg = ResourceMgr.getFormattedString("ErrBadRegex", filterExpression);
					WbSwingUtilities.showErrorMessage(this, msg);
				}
			}
		}
		finally
		{
			this.filterValue.addActionListener(this);
		}
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
		if (e.getSource() == filterValue)
		{
			if (e.getActionCommand().equals("comboBoxEdited"))
			{
				applyQuickFilter();
			}
		}
		else if (e.getSource() instanceof JMenuItem)
		{
			JMenuItem item = (JMenuItem)e.getSource();
			for (int i=0; i < columnItems.length; i++)
			{
				columnItems[i].setSelected(false);
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
				for (int i=0; i < columnItems.length; i++)
				{
					columnItems[i].setSelected(columnItems[i].getText().equals(searchColumn));
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
		int count = this.searchTable.getColumnCount();
		String[] names = new String[count];
		for (int i = 0; i < count; i++)
		{
			names[i] = this.searchTable.getColumnName(i);
		}
		setColumnList(names);
	}

	private synchronized void filterByEditorValue()
	{
		Component comp = filterValue.getEditor().getEditorComponent();
		if (comp instanceof JTextField)
		{
			JTextField editor = (JTextField)comp;
			String value = editor.getText();
			if (StringUtil.isNonBlank(value))
			{
				filterValue.setText(value);
				applyFilter(value, false);
				// this is necessary to remove the text selection that is automatically done
				// because of setting the text in applayFilter
				editor.setCaretPosition(value.length());
			}
		}
	}

	@Override
	public void keyTyped(KeyEvent e)
	{
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				filterByEditorValue();
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
