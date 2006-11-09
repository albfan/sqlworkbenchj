/*
 * ColumnExpressionPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.filter;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.ComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import workbench.db.ColumnIdentifier;
import workbench.gui.components.TextComponentMouseListener;
import workbench.gui.components.WbTraversalPolicy;
import workbench.resource.ResourceMgr;
import workbench.storage.ResultInfo;
import workbench.storage.filter.ColumnComparator;
import workbench.storage.filter.ColumnExpression;
import workbench.storage.filter.ComparatorFactory;
import workbench.storage.filter.DataRowExpression;
import workbench.storage.filter.ExpressionValue;
import workbench.storage.filter.FilterExpression;
import workbench.util.ValueConverter;
import java.awt.EventQueue;

/**
 * @author support@sql-workbench.net
 */
public class ColumnExpressionPanel
	extends JPanel
	implements ActionListener
{
	private final ComparatorFactory factory = new ComparatorFactory();
	private JComboBox comparatorDropDown;
	private JCheckBox ignoreCase;
	protected JComboBox columnSelector;
	private ArrayList comparatorItems;
	private ListComboBoxModel activeItems;
	private ResultInfo columnInfo;
	protected JTextField valueField;
	private ValueConverter converter = new ValueConverter();
	private Class lastColumnClass;

	public ColumnExpressionPanel(ResultInfo info, ExpressionValue filter)
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

		// pre-fill dropdown to calculate space
		buildColumnComparatorDropDown(String.class);
		comparatorDropDown.setModel(activeItems);

		Dimension d = comparatorDropDown.getPreferredSize();
		comparatorDropDown.setPreferredSize(d);
		comparatorDropDown.setMinimumSize(d);

		columnSelector = new JComboBox();
		int count = info.getColumnCount();
		ArrayList l = new ArrayList(count);
		l.add("*");
		for (int i=0; i < count; i++)
		{
			l.add(info.getColumnName(i));
		}

		ListComboBoxModel model = new ListComboBoxModel(l);
		columnSelector.setModel(model);

		this.setLayout(new GridBagLayout());
		ignoreCase = new JCheckBox(ResourceMgr.getString("LblFilterIgnoreCase"));
		ignoreCase.setSelected(false);
		ignoreCase.setEnabled(false);
		valueField = new JTextField(10);
		TextComponentMouseListener ml = new TextComponentMouseListener();
		valueField.addMouseListener(ml);
		
		valueField.setMinimumSize(new Dimension(15,24));

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		c.weighty = 0;
		c.weightx = 0;
		c.insets = new Insets(1,0,0,0);
		this.add(columnSelector, c);

		c.gridx ++;
		this.add(comparatorDropDown, c);

		c.gridx ++;
		this.add(ignoreCase, c);

		c.gridx ++;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		this.add(valueField,c);

		columnSelector.addActionListener(this);
		comparatorDropDown.addActionListener(this);
		
		if (filter == null)
		{
			columnSelector.setSelectedIndex(0);
			columnSelector.repaint();
		}
		else
		{
			setExpressionValue(filter);
		}
		WbTraversalPolicy pol = new WbTraversalPolicy();
		pol.addComponent(columnSelector);
		pol.addComponent(comparatorDropDown);
		pol.addComponent(ignoreCase);
		pol.addComponent(valueField);
		pol.setDefaultComponent(valueField);
		this.setFocusTraversalPolicy(pol);
	}


	public void actionPerformed(ActionEvent evt)
	{
		if (evt.getSource() == this.columnSelector)
		{
			int index = this.columnSelector.getSelectedIndex();
			if (index == 0)
			{
				buildColumnComparatorDropDown(String.class);
			}
			else
			{
				ColumnIdentifier col = this.columnInfo.getColumn(index-1);
				buildColumnComparatorDropDown(col);
			}
		}
		else if (evt.getSource() == this.comparatorDropDown)
		{
			ComparatorListItem item = (ComparatorListItem)comparatorDropDown.getSelectedItem();
			if (item != null)
			{
				ignoreCase.setSelected(item.getComparator().supportsIgnoreCase());
				ignoreCase.setEnabled(item.getComparator().supportsIgnoreCase());
				boolean needsValue = item.getComparator().needsValue();
				valueField.setEnabled(needsValue);
				if (needsValue)
				{
					valueField.setBackground(Color.WHITE);
				}
				else
				{
					valueField.setBackground(this.getBackground());
				}
			}
			else
			{
				ignoreCase.setSelected(false);
				ignoreCase.setEnabled(false);
			}
		}
	}

	private int findColumnInDropDown(String col)
	{
		ListComboBoxModel model = (ListComboBoxModel)this.columnSelector.getModel();
		return model.findItemIgnoreCase(col);
	}
	
	public void setExpressionValue(ExpressionValue expr)
	{
		String col = expr.getColumnName();
		int index = 0;
		if (!"*".equals(col)) this.columnInfo.findColumn(col);
		if (index > -1)
		{
			int ddIndex = findColumnInDropDown(col);
			if (ddIndex > -1) this.columnSelector.setSelectedIndex(ddIndex);
			ComparatorListItem item = new ComparatorListItem(expr.getComparator());
			this.comparatorDropDown.setSelectedItem(item);
			this.ignoreCase.setSelected(expr.isIgnoreCase());
			Object value = expr.getFilterValue();
			if (value != null)
			{
				this.valueField.setText(value.toString());
			}
		}
	}

	public void setFocusToColumn()
	{
		this.valueField.requestFocus();
		//this.columnSelector.requestFocus();
	}

	public String getColumnName()
	{
		return (String)columnSelector.getSelectedItem();
	}

	public boolean validateInput()
	{
		ColumnComparator comp = getComparator();
		if (comp == null) return false;
		String value = valueField.getText();
		return comp.validateInput(value);
	}
	
	public String getInputValue()
	{
		return valueField.getText();
	}
	
	public ExpressionValue getExpressionValue()
	{
		String col = this.getColumnName();
		if (col == null) return null;
		ColumnComparator comp = getComparator();
		if (comp == null) return null;
		Object value = this.getFilterValue();
		if (value == null && comp.needsValue()) return null;

		ExpressionValue exp = null;
		if ("*".equals(col))
		{
			exp = new DataRowExpression(comp, value);
		}
		else
		{
			exp = new ColumnExpression(col, comp, value);
		}
		if (this.ignoreCase.isEnabled())
		{
			exp.setIgnoreCase(ignoreCase.isSelected());
		}
		return exp;
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

	public Object getFilterValue()
	{
		String value = valueField.getText();
		String col = getColumnName();
		int colIndex = this.columnInfo.findColumn(col);
		// If the any column entry is selected ("*")
		// colIndex will be -1, and we simply return the value entered
		// because we tried everything as a String
		if (colIndex > -1)
		{
			int type = this.columnInfo.getColumnType(colIndex);
			try
			{
				Object dataValue = this.converter.convertValue(value, type);
				return dataValue;
			}
			catch (Exception e)
			{
				// ignore;
			}
		}
		return value;
	}

	private void buildColumnComparatorDropDown(ColumnIdentifier col)
	{
		Class columnClass = null;
		try
		{
			columnClass = col.getColumnClass();//Class.forName(col.getColumnClassName());
			buildColumnComparatorDropDown(columnClass);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void buildColumnComparatorDropDown(Class columnClass)
	{
		if (lastColumnClass != null && columnClass.equals(lastColumnClass))
		{
			if (comparatorDropDown.getSelectedItem() == null)
			{
				comparatorDropDown.setSelectedIndex(0);
			}
			return;
		}
		int count = comparatorItems.size();
		int added = 0;
		ArrayList l = new ArrayList(count);
		for (int i=0; i < count; i++)
		{
			ComparatorListItem item = (ComparatorListItem)comparatorItems.get(i);
			if (item.getComparator().supportsType(columnClass))
			{
				l.add(item);
				added++;
			}
		}
		this.activeItems.setData(l);
		comparatorDropDown.setModel(this.activeItems);
		if (added > 0) comparatorDropDown.setSelectedIndex(0);
    comparatorDropDown.validate();
    comparatorDropDown.repaint();
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
	public boolean equals(Object other)
	{
		if (other instanceof ComparatorListItem)
		{
			return comparator.equals(((ComparatorListItem)other).comparator);
		}
		return false;
	}
}
