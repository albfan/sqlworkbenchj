/*
 * HexPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import workbench.gui.WbSwingUtilities;

import workbench.util.NumberStringCache;

/**
 * @author Thomas Kellerer
 */
public class HexPanel
	extends JPanel
{
	private JTable dataTable;
	private LineNumberTable lines;

	public HexPanel()
	{
		super();
		setBorder(WbSwingUtilities.EMPTY_BORDER);
		dataTable = new JTable();
		dataTable.setAutoCreateColumnsFromModel(true);
		dataTable.setCellSelectionEnabled(false);
		dataTable.setShowGrid(false);
		dataTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		Font dataFont = new Font("Monospaced", 0, 12);
		dataTable.setFont(dataFont);
		setLayout(new BorderLayout());
		lines = new LineNumberTable(dataTable);
		WbScrollPane scroll = new WbScrollPane(dataTable);
		scroll.setRowHeaderView(lines);
		JTableHeader header = dataTable.getTableHeader();
		DefaultTableCellRenderer rend = new DefaultTableCellRenderer();
		rend.setBackground(new Color(238,240,238));
		header.setDefaultRenderer(rend);
		this.add(header, BorderLayout.NORTH);
		this.add(scroll, BorderLayout.CENTER);
	}

	public HexPanel(byte[] buffer)
	{
		this();
		setData(buffer);
	}

	public void setData(byte[] buffer)
	{
		ByteBufferTableModel model = new ByteBufferTableModel(buffer);
		dataTable.setModel(model);
		TableColumnModel tmod = dataTable.getColumnModel();
		int cols = tmod.getColumnCount();
		Font dataFont = dataTable.getFont();
		FontMetrics fm = dataTable.getFontMetrics(dataFont);
		int width = fm.stringWidth("000");
		for (int i = 0; i < cols - 1; i++)
		{
			TableColumn col = tmod.getColumn(i);
			col.setPreferredWidth(width);
			col.setMinWidth(width);
			col.setMaxWidth(width);
		}

		width = fm.stringWidth("MMMMMMMMMMMMMMMMM");
		TableColumn col = tmod.getColumn(cols - 1);
		col.setPreferredWidth(width);
		col.setMinWidth(width);
		col.setMaxWidth(width);

		String rowCount = NumberStringCache.getNumberString(model.getRowCount());
		width = fm.stringWidth(rowCount);
		lines.setPreferredScrollableViewportSize(new Dimension(width + 5,32768));
	}

}

class ByteBufferTableModel
	implements TableModel
{
	private final byte[] data;
	private final int rowCount;
	private final int columns = 16;
	private final String[] labels;

	ByteBufferTableModel(byte[] buffer)
	{
		data = buffer;
		rowCount = (buffer.length / columns) + 1;
		labels = new String[columns + 1];
		for (int i=0; i < columns; i++)
		{
			labels[i] = NumberStringCache.getHexString(i);
		}
		labels[columns] = "0123456789abcdef";
	}

	@Override
	public int getRowCount()
	{
		return rowCount;
	}

	@Override
	public int getColumnCount()
	{
		return columns + 1;
	}

	@Override
	public String getColumnName(int columnIndex)
	{
		return labels[columnIndex];
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
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		if (columnIndex == columns)
		{
			int rowStart = rowIndex * columns;
			int rowEnd = Math.min(rowIndex * columns + columns, data.length);
			StringBuilder result = new StringBuilder(columns);
			for (int i = rowStart; i < rowEnd; i++)
			{
				int c = (data[i] < 0 ? 256 + data[i] : data[i]);
				if ( c > 31 && c < 128 || c > 128 && c < 255)
				{
					result.append((char)c);
				}
				else
				{
					result.append('.');
				}
			}
			return result.toString();
		}
		int offset = rowIndex * columns + columnIndex;
		if (offset >= data.length) return "";
		int c = (data[offset] < 0 ? 256 + data[offset] : data[offset]);
		return NumberStringCache.getHexString(c);
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
