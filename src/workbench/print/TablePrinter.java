/*
 * TablePrinter.java
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
package workbench.print;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.RepaintManager;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.gui.components.ColumnWidthOptimizer;
import workbench.gui.components.WbTable;

import workbench.util.WbThread;

/**
 *	Prints the content of a Table.
 *	Usage:
<pre>
PrinterJob job = PrinterJob.getPrintJob();
PageFormat format = job.defaultPage();
Font f = new Font("Courier New", Font.PLAIN, 10);
TablePrinter printer = new TablePrinter(theTable, format, printerFont);
printer.startPrint();
</pre>
 *  The printout will be started in a separate thread on the default printer.
 *
 * @author Thomas Kellerer
 */
public class TablePrinter
	implements Printable, Pageable
{
	private PageFormat format;
	protected WbTable table;
	private int pageCount = -1;

	private Font printFont;
	private String headerText = null;
	private List<String> wrappedHeader = null;
	private TablePrintPage[] pages = null;

	private int pagesAcross = 0;
	private int pagesDown = 0;
	private int lineSpacing = 2;
	private int colSpacing = 5;
	private boolean showHeader;
	private boolean autoAdjustColumns;
	private boolean useAlternateColor;

	public TablePrinter(WbTable toPrint)
	{
		autoAdjustColumns = GuiSettings.getAutomaticOptimalWidth();
		useAlternateColor = GuiSettings.getAlternateRowColor() != null;
		PageFormat pformat = Settings.getInstance().getPageFormat();
		Font printerFont = Settings.getInstance().getPrinterFont();
		init(toPrint, pformat, printerFont);
	}

	private void init(WbTable toPrint, PageFormat aFormat, Font aFont)
	{
		this.table = toPrint;
		this.printFont = aFont;
		this.format = aFormat;
		String header = this.table.getPrintHeader();
		if (header != null)
		{
			setHeaderText(header);
		}
		calculatePages();
	}

	public void setUseAlternateColor(boolean flag)
	{
		if (flag != useAlternateColor)
		{
			this.useAlternateColor = flag;
			if (this.pages != null)
			{
				for (TablePrintPage page : pages)
				{
					page.setUseAlternateColor(flag);
				}
			}
		}
	}

	public void setShowHeader(boolean flag)
	{
		if (flag != this.showHeader)
		{
			this.showHeader = flag;
			calculatePages();
		}
	}

	public void setAutoadjustColumns(boolean flag)
	{
		if (flag != autoAdjustColumns)
		{
			autoAdjustColumns = flag;
			calculatePages();
		}
	}

	public void setHeaderText(String aText)
	{
		this.headerText = aText;
	}

	public void setFont(Font aFont)
	{
		this.printFont = aFont;
		this.calculatePages();
	}

	public Font getFont()
	{
		return this.printFont;
	}

	public void startPrint()
	{
		final PrinterJob pj = PrinterJob.getPrinterJob();
		if (this.format == null)
		{
			this.setPageFormat(pj.defaultPage());
		}
		pj.setPrintable(this, this.format);
		pj.setPageable(this);

		Thread pt = new WbThread("Print Thread")
		{
			@Override
			public void run()
			{
				try
				{
					if (pj.printDialog())
					{
						RepaintManager.currentManager(table).setDoubleBufferingEnabled(false);
						pj.print();
						RepaintManager.currentManager(table).setDoubleBufferingEnabled(true);
					}
				}
				catch (Exception e)
				{
					LogMgr.logWarning("TablePrinter.startPrint()", "Error during printing", e);
				}
			}
		};
		pt.start();
	}

	public int getPagesAcross()
	{
		return this.pagesAcross;
	}

	public int getPreviousVerticalPage(int index)
	{
		if (index < 1) return -1;
		if (index >= this.pageCount) return -1;
		if (this.pagesAcross == 1)
		{
			return index - 1;
		}
		else
		{
			int pageDown = this.pages[index].getPageNumberDown();
			for (int i=index; i > 0; i--)
			{
				int pd = this.pages[i].getPageNumberDown();
				if (pd < pageDown) return i;
			}
			return -1;
		}
	}

	public int getNextVerticalPage(int index)
	{
		if (index < 0) return -1;
		if (index >= this.pageCount - 1) return -1;
		if (this.pagesAcross == 1)
		{
			return index + 1;
		}
		else
		{
			int pageDown = this.pages[index].getPageNumberDown();
			for (int i=index; i < this.pageCount; i++)
			{
				int pd = this.pages[i].getPageNumberDown();
				if (pd > pageDown) return i;
			}
			return -1;
		}
	}

	public int getNextHorizontalPage(int index)
	{
		if (this.pagesAcross == 1) return -1;
		if (index < 0) return -1;
		if (index >= this.pageCount - 1) return -1;
		int currentAcross = this.pages[index].getPageNumberAcross();
		if (currentAcross == this.pagesAcross) return -1;
		return index + 1;
	}

	public int getPreviousHorizontalPage(int index)
	{
		if (this.pagesAcross == 1) return -1;
		if (index < 1) return -1;
		int currentAcross = this.pages[index].getPageNumberAcross();
		if (currentAcross == 1) return -1;
		return index - 1;
	}

	public PageFormat getPageFormat()
	{
		return this.format;
	}

	public void setPageFormat(PageFormat aFormat)
	{
		this.format = aFormat;
		if (this.format == null)
		{
			this.format = PrinterJob.getPrinterJob().defaultPage();
		}
		this.calculatePages();
	}

	private void calculateHeaderLines(FontMetrics fm, int maxWidth)
	{
		if (this.headerText == null || !showHeader)
		{
			this.wrappedHeader = null;
			return;
		}
		this.wrappedHeader = RenderUtils.wrap(headerText, fm, maxWidth);
	}

	private void calculatePages()
	{
		if (this.format == null) return;
		if (this.table == null) return;

		int pageWidth = (int)format.getImageableWidth();
		int pageHeight = (int)format.getImageableHeight();

		if (this.printFont == null)
		{
			this.printFont = table.getFont();
		}

		FontMetrics fm = this.table.getFontMetrics(this.printFont);

		int lineHeight = fm.getHeight() + this.lineSpacing;

		pageHeight -= (lineHeight + 10); // reserve one row for the column headers

		int rowsPerPage = (pageHeight / lineHeight);
		int titleRows = 0;

		if (this.headerText != null && showHeader)
		{
			calculateHeaderLines(fm, pageWidth);
			titleRows = wrappedHeader.size();
		}

		rowsPerPage--; // one line for the page information

		TableColumnModel colModel = table.getColumnModel();
		int colCount = colModel.getColumnCount();

		int rowCount = table.getRowCount();
		pagesDown = (int)Math.ceil((double)(rowCount + titleRows) / (double)rowsPerPage);

		int currentPageWidth = 0;
		int[] colWidths = calculateColumnWidths(fm);

		// the key to the map is the horizontal page number
		// the value will be the column were that page starts
		Map<Integer, Integer> horizontalBrakeColumns = new HashMap<>();

		// First page always starts at column 0
		horizontalBrakeColumns.put(Integer.valueOf(0), Integer.valueOf(0));

		String[] colHeaders = new String[colCount];
		this.pagesAcross = 1;

		Rectangle paintIconR = new Rectangle();
		Rectangle paintTextR = new Rectangle();
		Rectangle paintViewR = new Rectangle();

		for (int col = 0; col < colCount; col++)
		{
			TableColumn column = colModel.getColumn(col);
			String title = (String)column.getIdentifier();

			paintViewR.x = 0;
			paintViewR.y = 0;
			paintViewR.width = colWidths[col];
			paintViewR.height = lineHeight;

			paintIconR.setBounds(0, 0, 0, 0);
			paintTextR.setBounds(0, 0, 0, 0);

			colHeaders[col] =
					SwingUtilities.layoutCompoundLabel(fm,title,(Icon)null
							,SwingConstants.TOP
							,SwingConstants.LEFT
							,SwingConstants.TOP
							,SwingConstants.RIGHT
							,paintViewR, paintIconR, paintTextR, 0);

			if (currentPageWidth + colWidths[col] + colSpacing >= pageWidth)
			{
				horizontalBrakeColumns.put(Integer.valueOf(pagesAcross), Integer.valueOf(col));
				pagesAcross++;
				currentPageWidth = 0;
			}
			currentPageWidth += (colWidths[col] + colSpacing);
		}

		int currentPage = 0;
		this.pageCount = pagesDown * pagesAcross;
		this.pages = new TablePrintPage[this.pageCount];

		int startRow = 0;

		for (int pd = 0; pd < pagesDown; pd++)
		{
			for (int pa = 0; pa < pagesAcross; pa++)
			{
				int startCol = horizontalBrakeColumns.get(pa);
				int endCol;

				if (pa + 1 >= pagesAcross)
				{
					endCol = colCount - 1;
				}
				else
				{
					endCol = horizontalBrakeColumns.get(pa + 1) - 1;
				}
				int endRow = startRow + rowsPerPage;
				if (currentPage == 0)
				{
					endRow -= titleRows;
				}
				if (endRow >= rowCount)
				{
					endRow = rowCount - 1;
				}
				TablePrintPage p = new TablePrintPage(this.table, startRow, endRow, startCol, endCol, colWidths);
				p.setPageIndex(currentPage + 1);
				if (pagesAcross > 1)
				{
					p.setPageNumberDown(pd + 1);
					p.setPageNumberAcross(pa + 1);
				}
				p.setSpacing(lineSpacing, colSpacing);
				p.setColumnHeaders(colHeaders);
				p.setFont(this.printFont);
				this.pages[currentPage] = p;
				currentPage++;
			}
			startRow += rowsPerPage + 1;
		}
	}

	private int[] calculateColumnWidths(FontMetrics fm)
	{
		int colCount = table.getColumnCount();
		int[] colwidth = new int[colCount];
		int pageWidth = (int) format.getImageableWidth();

		int min = GuiSettings.getMinColumnWidth();
		int max = pageWidth;

		if (printFont.equals(table.getFont()) || !autoAdjustColumns)
		{
			TableColumnModel colMod = table.getColumnModel();
			for (int col = 0; col < colCount; col++)
			{
				TableColumn column = colMod.getColumn(col);
				colwidth[col] = Math.min(column.getWidth(), pageWidth);
			}
		}
		else
		{
			ColumnWidthOptimizer opt = new ColumnWidthOptimizer(table);
			for (int col = 0; col < colCount; col++)
			{
				colwidth[col] = opt.calculateOptimalColumnWidth(col, min, max, true, fm);
			}
		}
		return colwidth;
	}

	@Override
	public int print(Graphics g, PageFormat pageFormat, int pageIndex)
		throws PrinterException
	{
		Graphics2D pg = (Graphics2D)g;
		if (pageIndex >= this.pageCount) return NO_SUCH_PAGE;

		double startx = pageFormat.getImageableX();
		double starty = pageFormat.getImageableY();

		int wPage = (int) pageFormat.getImageableWidth();
		int hPage = (int) pageFormat.getImageableHeight();

		pg.setClip((int) startx, (int) starty, wPage, hPage);
		pg.translate(startx, starty);
		AffineTransform oldTransform = pg.getTransform();

		pg.setColor(Color.BLACK);
		pg.setFont(this.printFont);
		TablePrintPage currentPage = this.pages[pageIndex];

		StringBuilder footer = new StringBuilder(100);
		if (pagesAcross > 1)
		{
			footer.append(ResourceMgr.getFormattedString("TxtPageFooterHor",
				currentPage.getPageNumberAcross(), this.pagesAcross,
				currentPage.getPageNumberDown(), this.pagesDown));
		}
		else
		{
			footer.append(ResourceMgr.getFormattedString("TxtPageFooterNormal", currentPage.getPageIndex(), this.pageCount));
		}

		FontMetrics fm = pg.getFontMetrics(this.printFont);
		Rectangle2D bounds = fm.getStringBounds(footer.toString(), pg);
		double len = bounds.getWidth();

		pg.drawString(footer.toString(), (int) ((wPage - len) / 2), hPage - fm.getDescent());

		if (this.wrappedHeader != null && showHeader && pageIndex == 0)
		{
			int y = fm.getAscent();
			for (String line : wrappedHeader)
			{
				bounds = fm.getStringBounds(line, pg);
				pg.drawString(line, 0, y);
				y += fm.getAscent() + lineSpacing;
			}
			pg.translate(0, y + lineSpacing);
		}
		currentPage.print(pg);

		pg.setTransform(oldTransform);
		pg.setClip(null);

		return PAGE_EXISTS;
	}

	@Override
	public int getNumberOfPages()
	{
		return this.pageCount;
	}

	@Override
	public PageFormat getPageFormat(int pageIndex)
		throws IndexOutOfBoundsException
	{
		return this.format;
	}

	@Override
	public Printable getPrintable(int pageIndex)
		throws IndexOutOfBoundsException
	{
		return this;
	}

}
