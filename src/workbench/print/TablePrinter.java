/*
 * TablePrinter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
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

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.RepaintManager;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import workbench.gui.renderer.ToolTipRenderer;

/**
 *	Prints the content of a JTable.
 *	Usage:
<pre>
PrinterJob job = PrinterJob.getPrintJob();
PageFormat format = job.defaultPage();
Font f = new Font("Courier New", Font.PLAIN, 10);
TablePrinter printer = new TablePrinter(theTable, format, printerFont);
printer.setFooterText("Page");
printer.startPrint();
</pre>
 *  The printout will be started in a separate thread on the default printer.
 */
public class TablePrinter
	implements Printable, Pageable
{
	/**
	 *	The PageFormat to be used when printing
	 */
	private PageFormat format;
	private JTable table;
	private int pageCount = -1;

	private Font printFont;
	private String[] colHeaders;
	private String footerText = "Page";
	private String headerText = null;
	private TablePrintPage[] pages = null;

	private int pagesAcross = 0;
	private int lineSpacing = 5;
	private int colSpacing = 6;

	public TablePrinter(JTable toPrint, PageFormat aFormat, Font aFont)
	{
		this.table = toPrint;
		this.printFont = aFont;
		this.setPageFormat(aFormat);
	}

	public void setFooterText(String aText)
	{
		this.footerText = aText;
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

		if (this.printFont == null)	this.printFont = table.getFont();

		FontMetrics fm = this.table.getFontMetrics(this.printFont);
    int lineHeight = fm.getMaxAscent() + this.lineSpacing;

		if (this.headerText != null) pageHeight -= lineHeight;
		if (this.footerText != null) pageHeight -= lineHeight;
		pageHeight -= (lineHeight + 10); // reserve one row for the column headers

    int rowsPerPage = (int)(pageHeight / lineHeight);
    TableColumnModel colModel = table.getColumnModel();
		int colCount = colModel.getColumnCount();

		int rowCount = table.getRowCount();
		int pagesDown = (int)Math.ceil((double)rowCount/(double)rowsPerPage);

		int currentPageWidth = 0;
		int[] width = new int[colCount]; // stores the width for each column

		// stores the column number where a horizontal page needs to be
		// created

		int[] colPageBreaks = new int[colCount];
		this.colHeaders = new String[colCount];
		int[] colHeaderX = new int[colCount];
		this.pagesAcross = 1;

		Rectangle paintIconR = new Rectangle();
		Rectangle paintTextR = new Rectangle();
		Rectangle paintViewR = new Rectangle();

		for (int col=0; col < colCount; col++)
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

			int halign = SwingConstants.LEFT;

			Class clz = this.table.getColumnClass(col);
			TableCellRenderer rend = this.table.getDefaultRenderer(clz);
			if (rend instanceof JLabel)
			{
				halign = ((JLabel)rend).getHorizontalAlignment();
			}
			else if (rend instanceof ToolTipRenderer)
			{
				halign = ((ToolTipRenderer)rend).getHorizontalAlignment();
			}

			this.colHeaders[col] =
					SwingUtilities.layoutCompoundLabel(fm,title,(Icon)null
							,SwingConstants.TOP
							,halign
							,SwingConstants.TOP
							,SwingConstants.RIGHT
							,paintViewR, paintIconR, paintTextR, 0);

			colHeaderX[col] = paintTextR.x;

			//System.out.println("col=" + col + ",colWidth=" + width[col] +",currentPageWidth="+ currentPageWidth + ",width=" + pageWidth);
			if ((currentPageWidth + width[col] + colSpacing) >= pageWidth)
			{
				colPageBreaks[pagesAcross] = col;
				pagesAcross ++;
				currentPageWidth = 0;
			}
			currentPageWidth += (width[col] + colSpacing);
		}

		int currentPage = 0;
		this.pageCount = pagesDown * pagesAcross;
		this.pages = new TablePrintPage[this.pageCount];

		int startRow = 0;

		for (int pd=0; pd<pagesDown; pd++)
		{

			for (int pa=0; pa<pagesAcross;pa++)
			{
				int startCol = colPageBreaks[pa];
				int endCol = 0;

				if (pa + 1 == pagesAcross)
					endCol = colCount - 1;
				else
					endCol = colPageBreaks[pa + 1] - 1;

				int endRow = startRow + rowsPerPage;
				if (endRow >= rowCount) endRow = rowCount -1;
				TablePrintPage p = new TablePrintPage(this.table, startRow, endRow, startCol, endCol);
				p.setPageIndex(currentPage + 1);
				if (pagesAcross > 1)
				{
					p.setPageNumberDown(pd + 1);
					p.setPageNumberAcross(pa  + 1);
				}
				p.setSpacing(lineSpacing, colSpacing);
				p.setColumnHeaders(this.colHeaders);
				p.setColumnWidths(width);
				p.setColumnLabelsXPos(colHeaderX);
				p.setFont(this.printFont);
				this.pages[currentPage] = p;
				currentPage ++;
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

    double wPage = pageFormat.getImageableWidth();
    double hPage = pageFormat.getImageableHeight();

		pg.setClip((int)startx, (int)starty, (int)wPage, (int)hPage);
		pg.translate(startx, starty);
		AffineTransform oldTransform= pg.getTransform();

    pg.setColor(Color.BLACK);
		pg.setFont(this.printFont);
		TablePrintPage p = this.pages[pageIndex];

		StringBuffer footer = new StringBuffer(100);
		footer.append(this.footerText);
		footer.append(" ");
		if (pagesAcross > 1)
		{
			footer.append("(");
			footer.append(p.getPageIndexDisplay());
			footer.append("/");
			footer.append(this.pagesAcross);
			footer.append(")");
		}
		else
		{
			footer.append(p.getPageIndexDisplay());
		}
		footer.append("/");
		footer.append(this.pageCount);

		//String footer = this.footerText + " " +  + + (this.pageCount);
		FontMetrics fm = pg.getFontMetrics(this.printFont);
		Rectangle2D bounds = fm.getStringBounds(footer.toString(), pg);
		double len = bounds.getWidth();

    pg.drawString(footer.toString(), (int)((wPage - len)/2), (int)(hPage - lineSpacing) );

		if (this.headerText != null)
		{
			bounds = fm.getStringBounds(this.headerText, pg);
			len = bounds.getWidth();
			pg.drawString(this.headerText, (int)((wPage - len)/2), (int)(lineSpacing));
			pg.translate(0,(int)(lineSpacing  + 5));
		}
		p.print(pg);

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
