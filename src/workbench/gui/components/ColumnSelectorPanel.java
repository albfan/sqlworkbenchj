/*
 * ColumnSelectorPanel.java
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

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import workbench.resource.ResourceMgr;

import workbench.db.ColumnIdentifier;

/**
 *
 * @author  Thomas Kellerer
 */
public class ColumnSelectorPanel
	extends JPanel
	implements ActionListener
{
	private JTable selectTable;
	protected JPanel infoPanel;
	private ColumnSelectTableModel model;
	private JButton selectAll;
	private JButton selectNone;
	private JCheckBox selectedOnlyCheckBox;
	private JCheckBox includeHeaderCheckBox;
	private JCheckBox formatText;

	public ColumnSelectorPanel(ColumnIdentifier[] columns)
	{
		this(columns,false,false,false,false,false);
	}

	public ColumnSelectorPanel(ColumnIdentifier[] columns,
          boolean includeHeader,
          boolean selectedOnly,
          boolean showHeaderSelection,
          boolean showSelectedCheckBox,
					boolean showFormatCheckBox)
	{
		super();
    this.setLayout(new GridBagLayout());
		this.selectTable = new JTable();
		this.selectTable.setRowSelectionAllowed(false);
		this.selectTable.setColumnSelectionAllowed(false);
		this.model = new ColumnSelectTableModel(columns);
		this.selectTable.setModel(this.model);

		WbScrollPane scroll = new WbScrollPane(this.selectTable);
		this.infoPanel = new JPanel();
		configureInfoPanel();

		GridBagConstraints mainC = new GridBagConstraints();
    mainC.gridx = 0;
    mainC.gridy = 0;
    mainC.anchor = GridBagConstraints.NORTH;
    mainC.weightx = 0.0;
    mainC.weighty = 0.0;

		this.add(infoPanel, mainC);
    mainC.gridy ++;
    mainC.weighty = 1.0;
    mainC.weightx = 1.0;
    mainC.anchor = GridBagConstraints.NORTHWEST;
    mainC.fill = GridBagConstraints.BOTH;

		this.add(scroll, mainC);

		selectAll = new FlatButton(ResourceMgr.getString("LblSelectAll"));
		selectNone = new FlatButton(ResourceMgr.getString("LblSelectNone"));
		selectAll.addActionListener(this);
		selectNone.addActionListener(this);

		JPanel optionPanel = new JPanel();
		optionPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0.5;
		c.anchor = GridBagConstraints.EAST;
		c.insets = new Insets(0, 0, 0, 5);
		optionPanel.add(selectAll, c);

		c.gridx = 1;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 0.5;
		c.insets = new Insets(0, 5, 0, 0);
		optionPanel.add(selectNone, c);

		JPanel cbxPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));

		if (showSelectedCheckBox)
		{
			selectedOnlyCheckBox = new JCheckBox(ResourceMgr.getString("LblSelectedRowsOnly"));
			selectedOnlyCheckBox.setSelected(selectedOnly);
			selectedOnlyCheckBox.setEnabled(true);
			cbxPanel.add(selectedOnlyCheckBox);
		}

		if (showHeaderSelection)
		{
			includeHeaderCheckBox = new JCheckBox(ResourceMgr.getString("LblExportIncludeHeaders"));
			includeHeaderCheckBox.setSelected(includeHeader);
			cbxPanel.add(includeHeaderCheckBox);
		}

		if (showFormatCheckBox)
		{
			formatText = new JCheckBox("Format text");
			cbxPanel.add(formatText);
		}

		c.gridx = 0;
		c.gridy = 1;
		c.insets = new Insets(3, 0, 0, 3);
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 1;
		c.gridwidth = 2;
		optionPanel.add(cbxPanel, c);

		optionPanel.setBorder(new EmptyBorder(5, 0, 10, 0));

    mainC.gridy ++;
    mainC.weighty = 0.0;
    mainC.weightx = 0.0;
    mainC.anchor = GridBagConstraints.NORTH;
    mainC.fill = GridBagConstraints.NONE;

		this.add(optionPanel, mainC);
    adjustSize(scroll);
	}

  private void adjustSize(JScrollPane scroll)
  {
		this.selectTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

    FontMetrics fm = selectTable.getFontMetrics(selectTable.getFont());
    selectTable.setRowHeight(fm.getHeight());
		TableColumnModel colMod = this.selectTable.getColumnModel();

    TableColumn col = colMod.getColumn(1);
    int labelWidth = fm.stringWidth(model.selectLabel);
    col.setPreferredWidth((int)(labelWidth * 1.3));
    col.setMaxWidth((int)(labelWidth * 1.3));
    col.setMinWidth((int)(labelWidth * 1.3));

    Dimension ps = scroll.getPreferredSize();
    int rows = Math.max(10, selectTable.getRowCount() + 2);
    int height = (selectTable.getRowHeight() * rows) + (selectTable.getRowCount() * selectTable.getRowMargin());
    Dimension preferred = new Dimension( (int)(ps.getWidth()), (int)(height * 1.2));
    scroll.setPreferredSize(preferred);
  }

	protected void configureInfoPanel()
	{
		String msg = ResourceMgr.getString("MsgSelectColumns");
		JLabel infoLabel = new JLabel(msg);
		this.infoPanel.add(infoLabel);
	}

	public void setSelectionLabel(String label)
	{
		this.model.selectLabel = label;
		TableColumnModel colMod = this.selectTable.getColumnModel();
		TableColumn col = colMod.getColumn(1);
		col.setHeaderValue(label);
	}

	public boolean formatTextOutput()
	{
		if (formatText == null) return false;
		return formatText.isSelected();
	}

	public boolean selectedOnly()
	{
		if (this.selectedOnlyCheckBox == null) return false;
		return selectedOnlyCheckBox.isSelected();
	}

	public boolean includeHeader()
	{
		if (this.includeHeaderCheckBox == null)	return false;
		return includeHeaderCheckBox.isSelected();
	}

	/**
	 * Check if the column with the specified index is selected.
	 * @param index the index to be checked
	 * @return true if selected, false otherwise
	 */
	public boolean isColumnSelected(int index)
	{
		return this.model.selected[index];
	}

	/**
	 * Return the number of selected columns.
	 * @return int
	 */
	public int getSelectedCount()
	{
		int selected = 0;
		for (int i=0; i < this.model.selected.length; i++)
		{
			if (this.model.selected[i]) selected++;
		}
		return selected;
	}

	/**
	 * Return the columns that have been selected.
	 * @return the selected columns
	 */
	public List<ColumnIdentifier> getSelectedColumns()
	{
		int selected = this.getSelectedCount();
		List<ColumnIdentifier> result = new ArrayList<>(selected);
		for (int i=0; i < this.model.selected.length; i++)
		{
			if (this.model.selected[i])
			{
				result.add(this.model.columns[i].createCopy());
			}
		}
		return result;
	}

	public void selectAll()
	{
		this.model.selectAll();
	}

	public void selectNone()
	{
		this.model.selectNone();
	}

	public void setColumnSelected(int i, boolean flag)
	{
		this.model.selected[i] = flag;
	}

	public void selectColumns(List columns)
	{
		if (columns == null)
		{
			this.selectAll();
		}
		else
		{
			int cols = this.model.columns.length;
			for (int i=0; i < cols; i++)
			{
				this.model.selected[i] = columns.contains(this.model.columns[i]);
			}
		}
	}

	@Override
	public void actionPerformed(java.awt.event.ActionEvent e)
	{
		if (this.model == null) return;

		if (e.getSource() == this.selectAll)
		{
			this.model.selectAll();
		}
		else if (e.getSource() == this.selectNone)
		{
			this.model.selectNone();
		}
		TableModelEvent evt = new TableModelEvent(model);
		this.selectTable.tableChanged(evt);
	}
}

class ColumnSelectTableModel
	implements TableModel
{
	ColumnIdentifier[] columns;
	boolean[] selected;
	String colLabel = ResourceMgr.getString("LblHeaderKeyColumnColName");
	String selectLabel = ResourceMgr.getString("LblHeaderUseColumn");

	private final int rows;

	ColumnSelectTableModel(ColumnIdentifier[] cols)
	{
		this.rows = cols.length;
		this.columns = cols;
		this.selected = new boolean[this.rows];
	}

	@Override
	public int getColumnCount()
	{
		return 2;
	}

	@Override
	public Object getValueAt(int row, int column)
	{
		if (column == 0)
		{
			String name = this.columns[row].getColumnAlias();
			if (name == null)
			{
				name = this.columns[row].getColumnName();
			}
			return name;
		}
		else if (column == 1)
		{
			return Boolean.valueOf(this.selected[row]);
		}
		return "";
	}

	public void selectAll()
	{
		this.setFlagForAll(true);
	}

	public void selectNone()
	{
		this.setFlagForAll(false);
	}

	private void setFlagForAll(boolean flag)
	{
		for (int i=0; i < this.rows; i++)
		{
			this.selected[i] = flag;
		}
	}

	@Override
	public int getRowCount()
	{
		return this.rows;
	}

	@Override
	public void addTableModelListener(javax.swing.event.TableModelListener l)
	{
	}

	@Override
	public Class getColumnClass(int columnIndex)
	{
		if (columnIndex == 0) return String.class;
		else return Boolean.class;
	}

	@Override
	public String getColumnName(int columnIndex)
	{
		if (columnIndex == 0) return this.colLabel;
		return this.selectLabel;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		return (columnIndex == 1);
	}

	@Override
	public void removeTableModelListener(javax.swing.event.TableModelListener l)
	{
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex)
	{
		if (columnIndex == 1 && aValue instanceof Boolean)
		{
			Boolean b = (Boolean)aValue;
			this.selected[rowIndex] = b.booleanValue();
		}
	}

}
