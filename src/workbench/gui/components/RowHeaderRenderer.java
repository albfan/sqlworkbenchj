/*
 * RowHeaderRenderer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import java.awt.Dimension;
import java.awt.Font;
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
 * A TableRowHeader to show row numbers in a JTable
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

	@Override
	public void setFont(Font f)
	{
		super.setFont(f);
		calculateWidth();
	}

	public final void calculateWidth()
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

	@Override
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
