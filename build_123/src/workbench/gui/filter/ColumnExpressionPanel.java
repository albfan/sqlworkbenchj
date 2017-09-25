/*
 * ColumnExpressionPanel.java
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
package workbench.gui.filter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import workbench.interfaces.ValueProvider;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.db.ColumnIdentifier;

import workbench.gui.components.FlatButton;
import workbench.gui.components.TextComponentMouseListener;
import workbench.gui.components.WbTraversalPolicy;

import workbench.storage.ResultInfo;
import workbench.storage.filter.ColumnComparator;
import workbench.storage.filter.ColumnExpression;
import workbench.storage.filter.ComparatorFactory;
import workbench.storage.filter.DataRowExpression;
import workbench.storage.filter.ExpressionValue;

import workbench.util.ValueConverter;

/**
 * @author Thomas Kellerer
 */
public class ColumnExpressionPanel
	extends JPanel
	implements ActionListener
{
	private final ComparatorFactory factory = new ComparatorFactory();
	private JComboBox comparatorDropDown;
	private JCheckBox ignoreCase;
	protected JComboBox columnSelector;
	private List<ComparatorListItem> comparatorItems;
	private ListComboBoxModel activeItems;
	private ValueProvider filterData;
	protected JTextField valueField;
	protected JButton selectValueButton;

	private ValueConverter converter = new ValueConverter();
	private boolean ignoreComparatorChange;

	public ColumnExpressionPanel(ValueProvider data, ExpressionValue filter)
	{
		super();
		filterData = data;
		ResultInfo info = filterData.getResultInfo();
		comparatorDropDown = new JComboBox();
		activeItems = new ListComboBoxModel();

		List<ColumnComparator> comps = factory.getAvailableComparators();
		comparatorItems = new ArrayList<>(comps.size());
		for (ColumnComparator comp : comps)
		{
			comparatorItems.add(new ComparatorListItem(comp));
		}

		// pre-fill dropdown to calculate space
		buildColumnComparatorDropDown(String.class);

		Dimension d = comparatorDropDown.getPreferredSize();
		comparatorDropDown.setPreferredSize(d);
		comparatorDropDown.setMinimumSize(d);

		columnSelector = new JComboBox();
		int count = info.getColumnCount();
		ArrayList<String> l = new ArrayList<>(count);
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

		JPanel valuePanel = new JPanel(new BorderLayout(0,0));

		valueField = new JTextField(10);
		TextComponentMouseListener ml = new TextComponentMouseListener();
		valueField.addMouseListener(ml);
		valuePanel.add(valueField, BorderLayout.CENTER);

		selectValueButton = new FlatButton("...");
		selectValueButton.addActionListener(this);

		valuePanel.add(selectValueButton, BorderLayout.EAST);

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
		this.add(valuePanel, c);

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
		this.setFocusTraversalPolicy(pol);
	}

	public void selectColumn(String col)
	{
		final int index = findColumnInDropDown(col);
		if (index > -1)
		{
			SwingUtilities.invokeLater(() ->
      {
        columnSelector.setSelectedIndex(index);
      });
		}
	}

	@Override
	public void actionPerformed(ActionEvent evt)
	{
		if (evt.getSource() == this.columnSelector)
		{
			try
			{
				ignoreComparatorChange = true;
				int index = this.columnSelector.getSelectedIndex();
				if (index == 0)
				{
					buildColumnComparatorDropDown(String.class);
				}
				else
				{
					ColumnIdentifier col = this.filterData.getResultInfo().getColumn(index-1);
					buildColumnComparatorDropDown(col);
				}
			}
			finally
			{
				ignoreComparatorChange = false;
			}
			checkComparator();
		}
		else if (evt.getSource() == selectValueButton)
		{
			selectColumnValue();
		}
		else if (!ignoreComparatorChange && evt.getSource() == this.comparatorDropDown)
		{
			checkComparator();
		}
	}

	private void selectColumnValue()
	{
		String column = getColumnName();
		if ("*".equals(column)) return;
		String value = ListValuePicker.pickValue(this, column, filterData, (String) getFilterValue());
		if (value != null)
		{
			valueField.setText(value);
		}
	}

	private void checkComparator()
	{
		try
		{
			ColumnComparator comp = this.getComparator();

			if (comp != null)
			{
				ignoreCase.setSelected(comp.supportsIgnoreCase());
				ignoreCase.setEnabled(comp.supportsIgnoreCase());
				boolean needsValue = comp.needsValue();
				valueField.setEnabled(needsValue);
				if (needsValue)
				{
					valueField.setBackground(Color.WHITE);
				}
				else
				{
					valueField.setBackground(this.getBackground());
				}
				String col = getColumnName();
				if ("*".equals(col))
				{
					selectValueButton.setEnabled(false);
				}
				else
				{
					selectValueButton.setEnabled(true);
				}
			}
			else
			{
				ignoreCase.setSelected(false);
				ignoreCase.setEnabled(false);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("ColumnExpressionPanel.actionPerformed()", "Error when updating comparator", e);
		}
	}

	private int findColumnInDropDown(String col)
	{
		ListComboBoxModel model = (ListComboBoxModel)this.columnSelector.getModel();
		return model.findItemIgnoreCase(col);
	}

	public final void setExpressionValue(ExpressionValue expr)
	{
		String col = expr.getColumnName();
		int index = 0;
		if (!"*".equals(col))
		{
			index = this.filterData.getResultInfo().findColumn(col);
		}
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
		Object value = getFilterValue();
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
		String value = getInputValue();
		String col = getColumnName();
		int colIndex = this.filterData.getResultInfo().findColumn(col);
		// If the any column ("*") entry is selected
		// colIndex will be -1, and we simply return the value entered
		// because we tried everything as a String
		if (colIndex > -1)
		{
			int type = this.filterData.getResultInfo().getColumnType(colIndex);
			try
			{
				Object dataValue = this.converter.convertValue(value, type);
				return dataValue;
			}
			catch (Exception e)
			{
				LogMgr.logWarning("ColumnExpressionPanel.getFilterValue()", "Error converting input value", e);
			}
		}
		return value;
	}

	private void buildColumnComparatorDropDown(ColumnIdentifier col)
	{
		Class columnClass = null;
		try
		{
			columnClass = col.getColumnClass();
			buildColumnComparatorDropDown(columnClass);
		}
		catch (Exception e)
		{
			LogMgr.logError("ColumnExpressionPanel.buildColumnComparatorDropwDown()", "Error finding column class", e);
		}
	}

	private void buildColumnComparatorDropDown(Class columnClass)
	{
		try
		{
			int count = comparatorItems.size();
			int added = 0;
			final ArrayList<ComparatorListItem> l = new ArrayList<>(count);
			for (int i=0; i < count; i++)
			{
				ComparatorListItem item = comparatorItems.get(i);
				if (item.getComparator().supportsType(columnClass))
				{
					l.add(item);
					added++;
				}
			}

			activeItems.setData(l);
			comparatorDropDown.setModel(activeItems);
			if (added > 0)
			{
				comparatorDropDown.setSelectedIndex(0);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("ColumnExpressionPanel.buildColumnComparatorDropwDown()", "Error building dropdown", e);
		}
	}
}
