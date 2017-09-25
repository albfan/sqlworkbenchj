/*
 * LineNumberTable.java
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

	@Override
	public int getRowHeight(int row)
	{
		return mainTable.getRowHeight(row);
	}

	class RowNumberTableModel
		implements TableModel
	{
		@Override
		public int getRowCount()
		{
			TableModel m = mainTable.getModel();
			if (m == null) return 0;
			return m.getRowCount();
		}

		@Override
		public int getColumnCount()
		{
			return 1;
		}

		@Override
		public Object getValueAt(int row, int column)
		{
			return Integer.valueOf(row + 1);
		}

		@Override
		public String getColumnName(int columnIndex)
		{
			return "";
		}

		@Override
		public Class getColumnClass(int columnIndex)
		{
			return String.class;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex)
		{
			return false;
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex)
		{
		}

		@Override
		public void addTableModelListener(TableModelListener l)
		{
		}

		@Override
		public void removeTableModelListener(TableModelListener l)
		{
		}
	}

}
