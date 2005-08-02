/*
 * FindPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.ActionMap;
import javax.swing.ComponentInputMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.QuickFilterAction;
import workbench.gui.actions.ResetFilterAction;
import workbench.gui.actions.WbAction;
import workbench.interfaces.CriteriaPanel;
import workbench.interfaces.QuickFilter;
import workbench.storage.filter.ColumnComparator;
import workbench.storage.filter.ColumnExpression;
import workbench.storage.filter.ContainsComparator;
import workbench.util.StringUtil;

/**
 * A small panel with a find and find next button, and a criteria field 
*  which provides a quick search facitiliy for a WbTable component
 * @author  support@sql-workbench.net
 */
public class QuickFilterPanel 
	extends JPanel 
	implements QuickFilter, CriteriaPanel, ActionListener
{
	private WbTable searchTable;
	private String searchColumn;
	
	//private JTextField filterValue;
	private HistoryTextField filterValue;
	public WbToolbar toolbar;
	private QuickFilterAction filterAction;	
	private ResetFilterAction resetFilterAction;
	private final ColumnComparator comparator = new ContainsComparator();
	
	public QuickFilterPanel(WbTable table, String column, String historyProperty)
	{
		GridBagConstraints gridBagConstraints;
		this.searchTable = table;
		
		this.searchColumn = column;
		this.setLayout(new GridBagLayout());
		this.setBorder(WbSwingUtilities.EMPTY_BORDER);

		//this.filterValue = new JTextField();
		this.filterValue = new HistoryTextField(historyProperty);
		this.filterValue.addActionListener(this);
		filterValue.getEditor().getEditorComponent().addMouseListener(new TextComponentMouseListener());
		//this.filterValue.addMouseListener(new TextComponentMouseListener());

		this.toolbar = new WbToolbar();
		this.filterAction = new QuickFilterAction(this);
		this.resetFilterAction = this.searchTable.getResetFilterAction();
		
		this.toolbar.add(this.filterAction);
		this.toolbar.add(this.resetFilterAction);
		this.toolbar.setMargin(new Insets(0,0,0,0));
		toolbar.setBorderPainted(true);

		Dimension d = new Dimension(32768, 20);
		this.setMaximumSize(d);
		this.filterValue.setMaximumSize(d);
		this.filterValue.setPreferredSize(new Dimension(50,20));
		this.toolbar.setMaximumSize(d);

		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.anchor = GridBagConstraints.WEST;
		this.add(toolbar, gridBagConstraints);

		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
		gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstraints.weightx = 1.0;

		this.add(filterValue, gridBagConstraints);

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
	}

	private void setupActionMap(InputMap im, ActionMap am)
	{
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), filterAction.getActionName());
		im.put(filterAction.getAccelerator(), filterAction.getActionName());
		am.put(filterAction.getActionName(), this.filterAction);

//		im.put(resetFilterAction.getAccelerator(), resetFilterAction.getActionName());
//		am.put(resetFilterAction.getActionName(), resetFilterAction);
	}
	
	public void saveSettings()
	{
		filterValue.saveSettings();
	}
	
	public void restoreSettings()
	{
		filterValue.restoreSettings();
	}
	
	public void setFocusToEntryField()
	{
		this.filterValue.grabFocus();
	}

	public void applyQuickFilter()
	{
		if (this.searchTable.getRowCount() <= 0) return;
		String value = this.filterValue.getText();
		if (StringUtil.isEmptyString(value)) return;
		
		ColumnExpression col = new ColumnExpression(this.searchColumn, comparator, value);
		col.setIgnoreCase(true);
		searchTable.applyFilter(col);
		filterValue.addToHistory(value);
	}

	public String getText()
	{
		return filterValue.getText();
	}

	public void setSelectedText(String aText)
	{
		setText(aText);
	}

	public void setText(String aText)
	{
		filterValue.setText(aText);
	}

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

	public void actionPerformed(java.awt.event.ActionEvent e)
	{
		if (e.getSource() == filterValue)
		{
			applyQuickFilter();
		}
	}

}
