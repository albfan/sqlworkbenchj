/*
 * LineNumberTable.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.Color;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

/**
 * 
 * @author Thomas Kellerer  
 */ 
public class LineNumberTable 
	extends JTable
{
	protected JTable mainTable;
	
	public LineNumberTable(JTable table)
	{
		super();
		mainTable = table;
		setModel(new RowNumberTableModel());
		DefaultTableCellRenderer rend = new DefaultTableCellRenderer();
		//header.setBorder(new LineBorder(Color.LIGHT_GRAY, 1));
		rend.setBackground(new Color(238,240,238));
		rend.setHorizontalAlignment(SwingConstants.RIGHT);
		setDefaultRenderer(Integer.class, rend);
		getColumnModel().getColumn(0).setCellRenderer(rend);
		setRowSelectionAllowed(false);
		setColumnSelectionAllowed(false);
	}
	
	public int getRowHeight(int row)
	{
		return mainTable.getRowHeight(row);
	}
	
	class RowNumberTableModel 
		implements TableModel
	{
		public int getRowCount()
		{
			TableModel m = mainTable.getModel();
			if (m == null) return 0;
			return m.getRowCount();
		}
		
		public int getColumnCount()
		{
			return 1;
		}
		
		public Object getValueAt(int row, int column)
		{
			return Integer.valueOf(row + 1);
		}

		public String getColumnName(int columnIndex)
		{
			return "";
		}

		public Class getColumnClass(int columnIndex)
		{
			return String.class;
		}

		public boolean isCellEditable(int rowIndex, int columnIndex)
		{
			return false;
		}

		public void setValueAt(Object aValue, int rowIndex, int columnIndex)
		{
		}

		public void addTableModelListener(TableModelListener l)
		{
		}

		public void removeTableModelListener(TableModelListener l)
		{
		}
	}

}
