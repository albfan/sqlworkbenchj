/*
 * ColumnSelectorPanel.java
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Box;
import javax.swing.JButton;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import workbench.db.ColumnIdentifier;
import workbench.resource.ResourceMgr;

/**
 *
 * @author  support@sql-workbench.net
 */
public class ColumnSelectorPanel
	extends JPanel
	implements ActionListener
{
	private JTable selectTable;
	private JLabel infoLabel;
	private ColumnSelectTableModel model;
	private JButton selectAll;
	private JButton selectNone;
	
	public ColumnSelectorPanel(ColumnIdentifier[] columns)
	{
		this.setLayout(new BorderLayout());
		this.selectTable = new JTable();
		this.selectTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		this.selectTable.setRowSelectionAllowed(false);
		this.selectTable.setColumnSelectionAllowed(false);
		this.model = new ColumnSelectTableModel(columns);
		this.selectTable.setModel(this.model);
		TableColumnModel colMod = this.selectTable.getColumnModel();
		TableColumn col = colMod.getColumn(0);
		col.setPreferredWidth(150);
		col.setMinWidth(50);
		col = colMod.getColumn(1);
		col.setPreferredWidth(100);
		col.setMinWidth(50);
		WbScrollPane scroll = new WbScrollPane(this.selectTable);
		String msg = ResourceMgr.getString("MsgSelectColumns");
		this.infoLabel = new JLabel(msg);
		this.add(this.infoLabel, BorderLayout.NORTH);
		this.add(scroll, BorderLayout.CENTER);
		
		selectAll = new JButton(ResourceMgr.getString("LabelSelectAll"));
		selectNone = new JButton(ResourceMgr.getString("LabelSelectNone"));
		selectAll.addActionListener(this);
		selectNone.addActionListener(this);
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER,0,0));
		buttonPanel.add(selectAll);
		buttonPanel.add(Box.createHorizontalStrut(5));
		buttonPanel.add(selectNone);
		buttonPanel.setBorder(new EmptyBorder(5,0,15,0));
		this.add(buttonPanel, BorderLayout.SOUTH);
		
		Dimension d = new Dimension(300, 190);
		this.setPreferredSize(d);
	}

	public void setInfoText(String text)
	{
		this.infoLabel.setText(text);
	}
	
	public void setSelectionLabel(String label)
	{
		this.model.selectLabel = label;
	}
	
	public boolean isColumnSelected(int i) 
	{
		return this.model.selected[i];
	}

	public int getSelectedCount()
	{
		int selected = 0;
		for (int i=0; i < this.model.selected.length; i++)
		{
			if (this.model.selected[i]) selected++;
		}
		return selected;
	}
	
	public List getSelectedColumns()
	{
		int selected = this.getSelectedCount();
		List result = new ArrayList(selected);
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
	private String colLabel = ResourceMgr.getString("LabelHeaderKeyColumnColName");
	String selectLabel = ResourceMgr.getString("LabelHeaderUseColumn");
	private int rows;
	public ColumnSelectTableModel(ColumnIdentifier[] cols)
	{
		this.rows = cols.length;
		this.columns = cols;
		this.selected = new boolean[this.rows];
	}
	
	public int getColumnCount()
	{
		return 2;
	}
	
	public Object getValueAt(int row, int column)
	{
		if (column == 0)
		{
			return this.columns[row].getColumnName();
		}
		else if (column == 1)
		{
			return new Boolean(this.selected[row]);
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
	
	
	public int getRowCount()
	{
		return this.rows;
	}
	
	public void addTableModelListener(javax.swing.event.TableModelListener l)
	{
	}
	
	public Class getColumnClass(int columnIndex)
	{
		if (columnIndex == 0) return String.class;
		else return Boolean.class;
	}
	
	public String getColumnName(int columnIndex)
	{
		if (columnIndex == 0) return this.colLabel;
		return this.selectLabel;
	}
	
	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		return (columnIndex == 1);
	}
	
	public void removeTableModelListener(javax.swing.event.TableModelListener l)
	{
	}
	
	public void setValueAt(Object aValue, int rowIndex, int columnIndex)
	{
		if (columnIndex == 1 && aValue instanceof Boolean)
		{
			Boolean b = (Boolean)aValue;
			this.selected[rowIndex] = b.booleanValue();
		}
	}
	
}
