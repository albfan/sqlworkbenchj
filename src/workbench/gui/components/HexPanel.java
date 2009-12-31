/*
 * HexPanel.java
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
		//header.setBorder(new LineBorder(Color.LIGHT_GRAY, 1));
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
	private byte[] data;
	private int rowCount;
	private int columns = 16;
	private String[] labels;

	public ByteBufferTableModel(byte[] buffer)
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

	public int getRowCount()
	{
		return rowCount;
	}

	public int getColumnCount()
	{
		return columns + 1;
	}

	public String getColumnName(int columnIndex)
	{
		return labels[columnIndex];
	}

	public Class getColumnClass(int columnIndex)
	{
		return String.class;
	}

	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		return false;
	}

	public Object getValueAt(int rowIndex, int columnIndex)
	{
		if (columnIndex == columns)
		{
			int rowStart = rowIndex * columns;
			int rowEnd = Math.min(rowIndex * columns + (columns - 1), data.length);
			StringBuilder result = new StringBuilder(16);
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

//		if (c < 16) return "0" + Integer.toHexString(c);
//		else return Integer.toHexString(c);
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
