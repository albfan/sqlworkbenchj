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
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.geom.Rectangle2D;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import workbench.gui.renderer.ToolTipRenderer;
import workbench.resource.GuiSettings;
import workbench.util.NumberStringCache;

/**
 *
 * @author Thomas Kellerer
 */
public class RowHeaderRenderer
	extends ToolTipRenderer
{
	private JTable table;
	private TableRowHeader rowHeader;
	private int colWidth = -1;
	private Color backgroundColor;
	private Color textColor;

	public RowHeaderRenderer(TableRowHeader rowHead, JTable client)
	{
		table = client;
		rowHeader = rowHead;

		JTableHeader header = table.getTableHeader();
		setFont(header.getFont());
		setOpaque(true);
		setHorizontalAlignment(SwingConstants.RIGHT);

		textColor = header.getForeground();
		backgroundColor = header.getBackground();

		rightMargin = GuiSettings.getRowNumberMargin();
		calculateWidth();
		setTooltip(null);
	}

	@Override
	protected void initDisplay(JTable table, Object value, boolean selected, boolean focus, int row, int col)
	{
		// nothing to do...
	}

	@Override
	protected Color getBackgroundColor()
	{
		return backgroundColor;
	}

	@Override
	protected Color getForegroundColor()
	{
		return textColor;
	}


	public synchronized void calculateWidth()
	{
		FontMetrics fm = getFontMetrics(getFont());
		int width = 12;
		try
		{
			if (fm != null)
			{
				Rectangle2D r = fm.getStringBounds("0", getGraphics());
				width = r.getBounds().width;
			}
		}
		catch (Exception e)
		{
			width = 12;
		}
		String max = NumberStringCache.getNumberString(table.getRowCount());
		colWidth = (max.length() * width) + width + rightMargin + 1;

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

	public void prepareDisplay(Object aValue)
	{
		try
		{
			this.displayValue = (String)aValue;
		}
		catch (Throwable e)
		{
			displayValue = (aValue == null ? null : aValue.toString());
		}
	}

}
