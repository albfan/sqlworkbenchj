/*
 * TablePrinter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
import java.util.Map;
import javax.swing.Icon;
import javax.swing.RepaintManager;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import workbench.gui.components.WbTable;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

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
 * @author support@sql-workbench.et
 */
public class TablePrinter
	implements Printable, Pageable
{
	private PageFormat format;
	protected WbTable table;
	private int pageCount = -1;

	private Font printFont;
	private String headerText = null;
	private TablePrintPage[] pages = null;

	private int pagesAcross = 0;
	private int pagesDown = 0;
	private int lineSpacing = 2;
	private int colSpacing = 4;

	public TablePrinter(WbTable toPrint)
	{
		PageFormat pformat = Settings.getInstance().getPageFormat();
		Font printerFont = Settings.getInstance().getPrinterFont();
		init(toPrint, pformat, printerFont);
	}

	protected void init(WbTable toPrint, PageFormat aFormat, Font aFont)
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
	
	public void setHeaderText(String aText)
	{
		this.headerText = aText;
	}

	public void setFont(Font aFont)
	{
		this.printFont = aFont;
		this.calculatePages();
	}

	public Font getFont() { return this.printFont; }

	public void startPrint()
	{
		final PrinterJob pj=PrinterJob.getPrinterJob();
		if (this.format == null)
		{
			this.setPageFormat(pj.defaultPage());
		}
		pj.setPrintable(this, this.format);
		pj.setPageable(this);

		Thread pt = new Thread()
		{
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
				}
			}
		};
		pt.setDaemon(true);
		pt.setName("Print Thread");
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

	private void calculatePages()
	{
		if (this.format == null) return;
		
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

		if (this.headerText != null)
		{
			rowsPerPage--;
		}
		rowsPerPage--; // one line for the page information

		TableColumnModel colModel = table.getColumnModel();
		int colCount = colModel.getColumnCount();

		int rowCount = table.getRowCount();
		pagesDown = (int)Math.ceil((double)rowCount / (double)rowsPerPage);

		int currentPageWidth = 0;
		int[] width = new int[colCount]; // stores the width for each column

		// the key to the map is the horizontal page number
		// the value will be the column were that page starts
		Map<Integer, Integer> horizontalBrakeColumns = new HashMap<Integer, Integer>();

		// First page always starts at column 0 
		horizontalBrakeColumns.put(Integer.valueOf(0), Integer.valueOf(0));

		String[] colHeaders = new String[colCount];
		this.pagesAcross = 1;

		Rectangle paintIconR = new Rectangle();
		Rectangle paintTextR = new Rectangle();
		Rectangle paintViewR = new Rectangle();

		// TODO: horizontal pages do not work when a column exceeds the horizontal space
		for (int col = 0; col < colCount; col++)
		{
			TableColumn column = colModel.getColumn(col);
			String title = (String)column.getIdentifier();

			width[col] = column.getWidth();

			paintViewR.x = 0;
			paintViewR.y = 0;
			paintViewR.width = width[col];
			paintViewR.height = lineHeight;

			paintIconR.x = paintIconR.y = paintIconR.width = paintIconR.height = 0;
			paintTextR.x = paintTextR.y = paintTextR.width = paintTextR.height = 0;

			colHeaders[col] =
					SwingUtilities.layoutCompoundLabel(fm,title,(Icon)null
							,SwingConstants.TOP
							,SwingConstants.LEFT
							,SwingConstants.TOP
							,SwingConstants.RIGHT
							,paintViewR, paintIconR, paintTextR, 0);

			if ((currentPageWidth + width[col] + colSpacing) >= pageWidth)
			{
				horizontalBrakeColumns.put(Integer.valueOf(pagesAcross), Integer.valueOf(col));
				pagesAcross++;
				currentPageWidth = 0;
			}
			currentPageWidth += (width[col] + colSpacing);
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
				int endCol = 0;

				if (pa + 1 >= pagesAcross)
				{
					endCol = colCount - 1;
				}
				else
				{
					endCol = horizontalBrakeColumns.get(pa + 1) - 1;
				}
				int endRow = startRow + rowsPerPage;
				if (endRow >= rowCount)
				{
					endRow = rowCount - 1;
				}
				TablePrintPage p = new TablePrintPage(this.table, startRow, endRow, startCol, endCol);
				p.setPageIndex(currentPage + 1);
				if (pagesAcross > 1)
				{
					p.setPageNumberDown(pd + 1);
					p.setPageNumberAcross(pa + 1);
				}
				p.setSpacing(lineSpacing, colSpacing);
				p.setColumnHeaders(colHeaders);
				p.setColumnWidths(width);
				p.setFont(this.printFont);
				this.pages[currentPage] = p;
				currentPage++;
			}
			startRow += rowsPerPage + 1;
		}
	}

	public int print(Graphics g, PageFormat pageFormat, int pageIndex)
		throws PrinterException
	{
		Graphics2D pg = (Graphics2D)g;
    if (pageIndex >= this.pageCount) return NO_SUCH_PAGE;

    double startx = pageFormat.getImageableX();
		double starty = pageFormat.getImageableY();

    int wPage = (int)pageFormat.getImageableWidth();
    int hPage = (int)pageFormat.getImageableHeight();

		pg.setClip((int)startx, (int)starty, wPage, hPage);
		pg.translate(startx, starty);
		AffineTransform oldTransform= pg.getTransform();

    pg.setColor(Color.BLACK);
		pg.setFont(this.printFont);
		TablePrintPage currentPage = this.pages[pageIndex];

		StringBuilder footer = new StringBuilder(100);
		if (pagesAcross > 1)
		{
			footer.append(ResourceMgr.getFormattedString("TxtPageFooterHor", 
				currentPage.getPageNumberAcross(), this.pagesAcross,
				currentPage.getPageNumberDown(), this.pagesDown
			));
		}
		else
		{
			footer.append(ResourceMgr.getFormattedString("TxtPageFooterNormal",currentPage.getPageIndex(),this.pageCount));
		}

		FontMetrics fm = pg.getFontMetrics(this.printFont);
		Rectangle2D bounds = fm.getStringBounds(footer.toString(), pg);
		double len = bounds.getWidth();

    pg.drawString(footer.toString(), (int)((wPage - len)/2), hPage - fm.getDescent());

		if (this.headerText != null)
		{
			bounds = fm.getStringBounds(this.headerText, pg);
			len = bounds.getWidth();
			pg.drawString(this.headerText, 0, fm.getAscent());
			pg.translate(0, lineSpacing + fm.getAscent() + 5);
		}
		currentPage.print(pg);

		pg.setTransform(oldTransform);
		pg.setClip(null);

		return PAGE_EXISTS;
  }

	public int getNumberOfPages()
	{
		return this.pageCount;
	}

	public PageFormat getPageFormat(int pageIndex)
		throws IndexOutOfBoundsException
	{
		return this.format;
	}

	public Printable getPrintable(int pageIndex)
		throws IndexOutOfBoundsException
	{
		return this;
	}

}
