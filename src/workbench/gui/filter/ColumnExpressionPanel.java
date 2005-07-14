/*
 * ColumnExpressionPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.filter;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import workbench.db.ColumnIdentifier;
import workbench.resource.ResourceMgr;
import workbench.storage.ResultInfo;
import workbench.storage.filter.ColumnComparator;
import workbench.storage.filter.ColumnExpression;
import workbench.storage.filter.ComparatorFactory;
import workbench.storage.filter.FilterExpression;
import workbench.util.StringUtil;

/**
 * @author support@sql-workbench.net
 */
public class ColumnExpressionPanel
	extends JPanel
	implements ActionListener
{
	private static final ComparatorFactory factory = new ComparatorFactory();
	private JComboBox comparatorDropDown;
	private JCheckBox ignoreCase;
	
	private JComboBox columnSelector;
	private ArrayList comparatorItems;
	private ListComboBoxModel activeItems;
	private ResultInfo columnInfo;
	private JTextField valueField;
	
	public ColumnExpressionPanel(ResultInfo info)
	{
		columnInfo = info;
		comparatorDropDown = new JComboBox();
		activeItems = new ListComboBoxModel();
		
		ColumnComparator[] comps = factory.getAvailableComparators();
		comparatorItems = new ArrayList(comps.length);
		for (int i=0; i < comps.length; i++)
		{
			comparatorItems.add(new ComparatorListItem(comps[i]));
		}
		columnSelector = new JComboBox();
		int count = info.getColumnCount();
		ArrayList l = new ArrayList(count);
		for (int i=0; i < count; i++)
		{
			l.add(info.getColumnName(i));
		}
		ListComboBoxModel model = new ListComboBoxModel(l);
		columnSelector.setModel(model);

		// Pre-Fill the comparator dropdown, so that a proper
		// size can be calculated
		ArrayList cl = new ArrayList();
		count = comparatorItems.size();
		for (int i=0; i < count; i++)
		{
			ComparatorListItem item = (ComparatorListItem)comparatorItems.get(i);
			if (item.getComparator().supportsType(String.class))
			{
				cl.add(item);
			}
		}
		activeItems.setData(cl);
		comparatorDropDown.setModel(activeItems);
		
		ignoreCase = new JCheckBox(ResourceMgr.getString("LabelFilterIgnoreCase"));
		ignoreCase.setSelected(false);
		ignoreCase.setEnabled(false);
		this.setLayout(new GridBagLayout());
		
		GridBagConstraints cons = new GridBagConstraints();
		cons.gridx = 0;
		cons.gridy = 0;
		cons.weighty = 1.0;
		cons.weightx = 0;
		cons.fill = GridBagConstraints.HORIZONTAL;
		cons.anchor = GridBagConstraints.WEST;
		this.add(columnSelector, cons);
		
		cons.gridx = 1;
		cons.gridy = 0;
		cons.weighty = 1.0;
		cons.weightx = 0;
		cons.anchor = GridBagConstraints.WEST;		
		this.add(comparatorDropDown, cons);
		
		cons.gridx = 2;
		cons.gridy = 0;
		cons.weighty = 1.0;
		cons.weightx = 0;
		cons.anchor = GridBagConstraints.WEST;		
		this.add(ignoreCase, cons);
		
		valueField = new JTextField(10);
		cons.gridx = 3;
		cons.gridy = 0;
		cons.weighty = 1.0;
		cons.weightx = 1.0;
		cons.fill = GridBagConstraints.HORIZONTAL;
		cons.anchor = GridBagConstraints.WEST;		
		this.add(valueField, cons);
		
		this.doLayout();
		
		Dimension d = comparatorDropDown.getPreferredSize();
		comparatorDropDown.setMinimumSize(d);
		comparatorDropDown.setPreferredSize(d);
		comparatorDropDown.setMaximumSize(d);
		
		columnSelector.addActionListener(this);
		comparatorDropDown.addActionListener(this);
	}

	public void actionPerformed(ActionEvent evt)
	{
		if (evt.getSource() == this.columnSelector)
		{
			int index = this.columnSelector.getSelectedIndex();
			ColumnIdentifier col = this.columnInfo.getColumn(index);
			buildColumnComparatorDropDown(col);
		}
		else if (evt.getSource() == this.comparatorDropDown)
		{
			ComparatorListItem item = (ComparatorListItem)comparatorDropDown.getSelectedItem();
			if (item != null)
			{
				boolean supportsIgnore = item.getComparator().supportsIgnoreCase();
				if (!supportsIgnore)
				{
					ignoreCase.setSelected(false);
				}
				ignoreCase.setEnabled(supportsIgnore);
			}
			else
			{
				ignoreCase.setSelected(false);
				ignoreCase.setEnabled(false);
			}
		}
	}
	
	public FilterExpression getExpression()
	{
		String column = this.getColumnName();
		if (column == null) return null;
		ColumnComparator comp = this.getComparator();
		if (comp == null) return null;
		String value = this.getFilterValue();
		if (StringUtil.isEmptyString(value)) return null;
		boolean ignore = this.ignoreCase.isSelected();
		
		ColumnExpression expr = new ColumnExpression(column, comp, value);
		expr.setIgnoreCase(ignore);
		return expr;
	}
	
	public String getColumnName() 
	{
		return (String)columnSelector.getSelectedItem();
	}
	
	public ColumnComparator getComparator()
	{
		ComparatorListItem item = (ComparatorListItem)this.comparatorDropDown.getSelectedItem();
		if (item != null)
		{
			return item.getComparator();
		}
		return null;
	}
	
	public String getFilterValue()
	{
		return this.valueField.getText();
	}
	
	private void buildColumnComparatorDropDown(ColumnIdentifier col)
	{
		int count = comparatorItems.size();
		ArrayList l = new ArrayList(count);
		Class columnClass = null;
		try
		{
			columnClass = Class.forName(col.getColumnClass());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		for (int i=0; i < count; i++)
		{
			ComparatorListItem item = (ComparatorListItem)comparatorItems.get(i);
			if (item.getComparator().supportsType(columnClass))
			{
				l.add(item);
			}
		}
		activeItems.setData(l);
		comparatorDropDown.setSelectedItem(null);
		comparatorDropDown.updateUI();
	}
}

/**
 * A wrapper class to display the operator for a comparator 
 */
class ComparatorListItem
{
	private ColumnComparator comparator;
	public ComparatorListItem(ColumnComparator comp)
	{
		comparator = comp;
	}
	
	public String toString() { return comparator.getOperator(); }
	public ColumnComparator getComparator() { return comparator; }
}