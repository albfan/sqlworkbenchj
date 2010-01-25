/*
 * RowHeaderRenderer
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.components;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.geom.Rectangle2D;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import workbench.resource.GuiSettings;
import workbench.util.NumberStringCache;

/**
 *
 * @author Thomas Kellerer
 */
public class RowHeaderRenderer
	implements TableCellRenderer
{
	private JLabel label;
	private JTable table;
	private TableRowHeader rowHeader;
	private int colWidth = -1;
	private boolean useButtonStyle;

	public RowHeaderRenderer(TableRowHeader rowHead, JTable client)
	{
		table = client;
		rowHeader = rowHead;
		label = new JLabel();
		useButtonStyle = GuiSettings.getUseButtonStyleRowNumbers();

		JTableHeader header = table.getTableHeader();
		label.setFont(header.getFont());
		label.setOpaque(true);
		label.setHorizontalAlignment(SwingConstants.RIGHT);

		label.setForeground(header.getForeground());
		label.setBackground(header.getBackground());

		if (useButtonStyle)
		{
			Border b = new CompoundBorder(UIManager.getBorder("TableHeader.cellBorder"), new EmptyBorder(0, 1, 0, 2));
			label.setBorder(b);
		}
		else
		{
			label.setBorder(new EmptyBorder(1, 0, 0, 1));
		}
		calculateWidth();
	}

	public synchronized void calculateWidth()
	{
		FontMetrics fm = label.getFontMetrics(label.getFont());
		int width = 8;
		try
		{
			if (fm != null)
			{
				Rectangle2D r = fm.getStringBounds("0", label.getGraphics());
				width = r.getBounds().width;
			}
		}
		catch (Exception e)
		{
			width = 8;
		}
		String max = NumberStringCache.getNumberString(table.getRowCount());
		colWidth = max.length() * width;

		if (useButtonStyle)
		{
			colWidth += (width * 2);
		}
		else
		{
			colWidth += width;
		}

		try
		{
			TableColumn col = rowHeader.getColumnModel().getColumn(0);
			col.setPreferredWidth(colWidth);
			col.setMaxWidth(colWidth);

			col = rowHeader.getTableHeader().getColumnModel().getColumn(0);
			col.setPreferredWidth(colWidth);
			col.setMaxWidth(colWidth);

			Dimension psize = rowHeader.getPreferredSize();
			Dimension size = new Dimension(colWidth, psize.height);

			rowHeader.setMaximumSize(size);
			rowHeader.setSize(size);
			rowHeader.setPreferredScrollableViewportSize(size);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	{
		label.setText(NumberStringCache.getNumberString(row + 1));
		return label;
	}

}
