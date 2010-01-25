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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.geom.Rectangle2D;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import workbench.resource.GuiSettings;
import workbench.util.NumberStringCache;

/**
 *
 * @author Thomas Kellerer
 */
public class RowHeaderRenderer
	implements ListCellRenderer, TableModelListener
{
	private JLabel label;
	private JTable table;
	private TableRowHeader rowHeader;
	private boolean useAlternateColor;
	private Color baseColor;
	private Color alternateColor;
	private int colWidth = -1;
	private boolean useButtonStyle;

	public RowHeaderRenderer(TableRowHeader rowHead, JTable client)
	{
		table = client;
		table.getModel().addTableModelListener(this);
		label = new JLabel();
		rowHeader = rowHead;
		JTableHeader header = client.getTableHeader();
		label.setFont(header.getFont());
		label.setOpaque(true);
		label.setHorizontalAlignment(SwingConstants.RIGHT);

		if (table.getRowCount() == 0)
		{
			colWidth = 16;
		}

		label.setForeground(header.getForeground());
		label.setBackground(header.getBackground());
		
		useButtonStyle = GuiSettings.getUseButtonStyleRowNumbers();
		
		if (useButtonStyle)
		{
			Border b = new CompoundBorder(UIManager.getBorder("TableHeader.cellBorder"), new EmptyBorder(1, 1, 0, 1));
			label.setBorder(b);
		}
		else
		{
			Color grid = UIManager.getColor("Table.gridColor");
			Border b = new CompoundBorder(new SingleLineBorder(SingleLineBorder.BOTTOM, grid), new EmptyBorder(0, 0, 1, 0));
			label.setBorder(b);
//			useAlternateColor = true;
//			baseColor = table.getBackground();
//			alternateColor = GuiSettings.getAlternateRowColor();
//			label.setBackground(headerColor);
//			label.setForeground(Color.DARK_GRAY.brighter());
		}
	}

	private synchronized void calculateWidth()
	{
		FontMetrics fm = label.getFontMetrics(label.getFont());
		int width = 8;
		try
		{
			if (fm != null)
			{
				Rectangle2D r = fm.getStringBounds("9", label.getGraphics());
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
	}

	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
	{
		if (colWidth == -1)
		{
			calculateWidth();
		}

		if (useAlternateColor)
		{
			if ((index % 2) == 1)
			{
				label.setBackground(alternateColor);
			}
			else
			{
				label.setBackground(baseColor);
			}
		}
		label.setText(NumberStringCache.getNumberString(index + 1));
		int height = table.getRowHeight(index);
		//int margin = table.getRowMargin();
		//height += margin;
		Dimension size = new Dimension(colWidth, height);
		label.setPreferredSize(size);
		label.setMaximumSize(size);
		label.setMinimumSize(size);
		return label;
	}

	@Override
	public void tableChanged(TableModelEvent e)
	{
		rowHeader.tableChanged(e.getFirstRow());
		calculateWidth();
	}

	public void dispose()
	{
		if (table == null) return;
		TableModel m = table.getModel();
		if (m == null) return;
		m.removeTableModelListener(this);
	}
}
