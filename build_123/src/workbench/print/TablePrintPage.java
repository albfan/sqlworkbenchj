/*
 * TablePrintPage.java
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
package workbench.print;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;

import javax.swing.Icon;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellRenderer;

import workbench.resource.GuiSettings;
import workbench.resource.Settings;

import workbench.gui.components.WbTable;
import workbench.gui.renderer.WbRenderer;


/**
 *	This class is responsible for keeping a page definition while printing a JTable.
 *
 *	When printing this page, the TablePrintPage assumes that the clipping is set in
 *  a way that it can start printing at 0,0 and can print over the whole Graphics object
 *  This means the caller needs to set the margins according to the page layout.
 *  It is also not checked if the definition actually fits on the graphics context
 *  this calculation needs to be done prior to creating the TablePrintPages
 *
 * @see TablePrinter
 * @author Thomas Kellerer
 */
public class TablePrintPage
{

	final private WbTable table;
	final private int startRow;
	final private int endRow;
	final private int startCol;
	final private int endCol;
	final private int[] colWidth;
	private Font printFont;
	private int pageNumDown = -1;
	private int pageNumAcross = -1;
	private int pageIndex;
	private int lineSpacing;
	private int colSpacing;
	private String[] colHeaders;

	private Color alternateBackground = GuiSettings.getAlternateRowColor();
	private boolean useAlternatingColors = false;

	public TablePrintPage(WbTable source, int startRow, int endRow, int startColumn, int endColumn, int[] widths)
	{
		this.table = source;
		this.startRow = startRow;
		this.endRow = endRow;
		this.startCol = startColumn;
		this.endCol = endColumn;
		this.colWidth = widths;
		useAlternatingColors = Settings.getInstance().getBoolProperty(PrintPreview.PROP_ALTERNATE_COLORS, false);
	}

	public void setUseAlternateColor(boolean flag)
	{
		this.useAlternatingColors = flag;
	}

	public void setFont(Font aFont)
	{
		this.printFont = aFont;
	}

	public Font getFont()
	{
		return this.printFont;
	}

	public void setPageNumberAcross(int aNum)
	{
		this.pageNumAcross = aNum;
	}

	public int getPageNumberAcross()
	{
		return this.pageNumAcross;
	}

	public void setPageNumberDown(int aNum)
	{
		this.pageNumDown = aNum;
	}

	public int getPageNumberDown()
	{
		return this.pageNumDown;
	}

	public void setPageIndex(int aNum)
	{
		this.pageIndex = aNum;
	}


	public int getPageIndex()
	{
		return this.pageIndex;
	}

	public void setColumnHeaders(String[] headers)
	{
		this.colHeaders = headers;
	}

	public void setSpacing(int line, int column)
	{
		this.lineSpacing = line;
		this.colSpacing = column;
	}

	public void print(Graphics2D pg)
	{
		Font dataFont = this.printFont;
		if (dataFont == null) dataFont = this.table.getFont();

		Font headerFont = dataFont.deriveFont(Font.BOLD);
		FontMetrics fm = pg.getFontMetrics(headerFont);
		int lineHeight = fm.getHeight();

		AffineTransform oldTransform = pg.getTransform();

		pg.setFont(headerFont);
		pg.setColor(Color.BLACK);

		int x = 0;
		int y = 0;
		pg.translate(0, lineHeight);

		int rowWidth = 0;
		for (int col= this.startCol; col <= this.endCol; col++)
		{
			if (this.colHeaders[col] != null)
			{
				pg.drawString(this.colHeaders[col], x, 0);
			}
			x += this.colWidth[col] + this.colSpacing;
			rowWidth += colWidth[col] + colSpacing;
		}

		Stroke s = pg.getStroke();
		pg.setStroke(new BasicStroke(0.3f));
		pg.drawLine(0, 1, x, 1);
		if (s != null) pg.setStroke(s);
		fm = pg.getFontMetrics(dataFont);
		lineHeight = fm.getHeight();

		pg.setTransform(oldTransform);
		y += (lineHeight + lineSpacing);
		pg.translate(0, y);
		pg.setFont(dataFont);

		Rectangle paintIconR = new Rectangle();
		Rectangle paintTextR = new Rectangle();
		Rectangle paintViewR = new Rectangle();

		for (int row = this.startRow; row <= this.endRow; row++)
		{
			if (useAlternatingColors && alternateBackground != null)
			{
				boolean isAlternatingRow = ((row % 2) == 1);
				if (isAlternatingRow)
				{
					pg.setColor(alternateBackground);
				}
				else
				{
					pg.setColor(Color.WHITE);
				}
				pg.fillRect(0, lineSpacing * 2, rowWidth, lineHeight + lineSpacing);
				pg.setColor(Color.BLACK);
			}

			int cx = 0;
			for (int col= this.startCol; col <= this.endCol; col++)
			{
				Object value = this.table.getValueAt(row, col);
				if (value == null) continue;
				TableCellRenderer rend = table.getCellRenderer(row, col);
				Component c = rend.getTableCellRendererComponent(table, value, false, false, row, col);
				if (rend instanceof WbRenderer)
				{
					WbRenderer wb = (WbRenderer) rend;

					// Clip the value returned by the renderer
					// according to the current column's width
					String data = wb.getDisplayValue();

					paintViewR.x = 0;
					paintViewR.y = 0;
					paintViewR.width = colWidth[col];
					paintViewR.height = lineHeight;

					paintIconR.x = paintIconR.y = paintIconR.width = paintIconR.height = 0;
					paintTextR.x = paintTextR.y = paintTextR.width = paintTextR.height = 0;

					String display = SwingUtilities.layoutCompoundLabel(fm, data, (Icon) null,
						SwingConstants.TOP,
						wb.getHorizontalAlignment(),
						SwingConstants.TOP,
						SwingConstants.RIGHT,
						paintViewR, paintIconR, paintTextR, 0);

					pg.drawString(display, cx + paintTextR.x, lineHeight);

					cx += this.colWidth[col] + colSpacing;
				}
				else
				{
					c.setSize(this.colWidth[col], lineHeight);
					c.print(pg);
					pg.translate(this.colWidth[col] + colSpacing, 0);
				}
			}
			pg.setTransform(oldTransform);
			y += (lineHeight + lineSpacing);
			pg.translate(0, y);
		}
	}

	@Override
	public String toString()
	{
		return "V-page:" + this.pageNumDown + ", H-page:" + this.pageNumAcross +
			", from row: " + this.startRow + " to row: " + this.endRow +
			", from column: " + this.startCol + " to column: " + this.endCol;
	}


}
