/*
 * QuickFilterPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.ActionMap;
import javax.swing.ComponentInputMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.border.Border;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.QuickFilterAction;
import workbench.gui.actions.ResetFilterAction;
import workbench.gui.actions.WbAction;
import workbench.interfaces.CriteriaPanel;
import workbench.interfaces.PropertyStorage;
import workbench.interfaces.QuickFilter;
import workbench.resource.ResourceMgr;
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
	implements QuickFilter, CriteriaPanel, ActionListener, MouseListener
{
	private WbTable searchTable;
	private String searchColumn;
	
	private HistoryTextField filterValue;
	private WbToolbar toolbar;
	private JComboBox columnDropDown; 
	private QuickFilterAction filterAction;	
	private ResetFilterAction resetFilterAction;
	private final ColumnComparator comparator = new ContainsComparator();
	private String[] columnList;
	private boolean showColumnDropDown;
	private JCheckBoxMenuItem[] columnItems;
	
	public QuickFilterPanel(WbTable table, String[] columns, boolean showDropDown, String historyProperty)
	{
		this.searchTable = table;
		if (columns.length == 1) 
		{
			showColumnDropDown = false;
		}
		else
		{
			this.columnList = columns;
			showColumnDropDown = showDropDown;
		}
		this.searchColumn = columns[0];
		this.initGui(historyProperty);
	}
	
	public QuickFilterPanel(WbTable table, String column, String historyProperty)
	{
		this.searchTable = table;
		this.searchColumn = column;
		showColumnDropDown = false;
		this.initGui(historyProperty);
	}
	
	private void initGui(String historyProperty)
	{
		GridBagConstraints gridBagConstraints;
		
		this.setLayout(new GridBagLayout());
		this.setBorder(WbSwingUtilities.EMPTY_BORDER);

		this.filterValue = new HistoryTextField(historyProperty);
		this.filterValue.setToolTipText(ResourceMgr.getString("TxtQuickFilterColumnHint"));
		TextComponentMouseListener l = new TextComponentMouseListener();
		if (this.columnList != null)
		{
			JMenu menu = new WbMenu(ResourceMgr.getString("MnuTextFilterOnColumn"));
			menu.setIcon(ResourceMgr.getImage("blank"));
			columnItems = new JCheckBoxMenuItem[columnList.length];
			for (int i=0; i < this.columnList.length; i++)
			{
				columnItems[i] = new JCheckBoxMenuItem(columnList[i]);
				columnItems[i].setSelected(i == 0);
				columnItems[i].putClientProperty("filterColumn", columnList[i]);
				columnItems[i].addActionListener(this);
				menu.add(columnItems[i]);
			}
			l.addMenuItem(menu);
			if (this.showColumnDropDown)
			{
				columnDropDown = new JComboBox(columnList);
				columnDropDown.addActionListener(this);
				columnDropDown.setSelectedIndex(0);
				columnDropDown.setToolTipText(ResourceMgr.getString("TxtQuickFilterColumnSelector"));
			}
		}
		
		filterValue.getEditor().getEditorComponent().addMouseListener(l);
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
		//this.filterValue.setPreferredSize(new Dimension(25,20));
		this.toolbar.setMaximumSize(d);

		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.anchor = GridBagConstraints.WEST;
		gridBagConstraints.gridx = 0;
		this.add(toolbar, gridBagConstraints);

		if (showColumnDropDown)
		{
			gridBagConstraints.gridx ++;
			gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints.weightx = 0.8;
			this.add(filterValue, gridBagConstraints);
			
			gridBagConstraints.gridx ++;
			//gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
			gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints.weightx = 0.2;
			this.add(columnDropDown, gridBagConstraints);
		}
		else
		{
			gridBagConstraints.gridx ++;
			gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
			gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints.weightx = 1.0;
			this.add(filterValue, gridBagConstraints);
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
	
	public void setColumnList(String[] columns, boolean showDropDown)
	{
		this.columnList = columns;
	}
	
	private void setupActionMap(InputMap im, ActionMap am)
	{
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), filterAction.getActionName());
		im.put(filterAction.getAccelerator(), filterAction.getActionName());
		am.put(filterAction.getActionName(), this.filterAction);
//		im.put(resetFilterAction.getAccelerator(), resetFilterAction.getActionName());
//		am.put(resetFilterAction.getActionName(), resetFilterAction);
	}

	public void saveSettings(PropertyStorage props, String prefix)
	{
		filterValue.saveSettings(props, prefix);
	}

	public void restoreSettings(PropertyStorage props, String prefix)
	{
		filterValue.restoreSettings(props, prefix);
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
		String value = this.filterValue.getText();
		if (StringUtil.isEmptyString(value)) 
		{
			this.searchTable.resetFilter();
		}
		else
		{
			ColumnExpression col = new ColumnExpression(this.searchColumn, comparator, value);
			col.setIgnoreCase(true);
			searchTable.applyFilter(col);
			filterValue.addToHistory(value);
		}
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
	}

	public void mouseClicked(MouseEvent e)
	{
		if (e.getButton() == MouseEvent.BUTTON3 && e.getSource() == this.filterValue)
		{
		}
	}

	public void mouseEntered(java.awt.event.MouseEvent e) {}
	public void mouseExited(java.awt.event.MouseEvent e) {}
	public void mousePressed(java.awt.event.MouseEvent e) {}
	public void mouseReleased(java.awt.event.MouseEvent e) {}

}
