/*
 * TablePrinter.java
 *
 * Created on July 22, 2003, 6:28 PM
 */

package workbench.print;

/**
 *
 * @author  thomas
 */
import javax.swing.*;
import javax.swing.table.*;
import java.awt.print.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import javax.swing.DebugGraphics;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import workbench.resource.ResourceMgr;

public class TablePrinter implements Printable, Pageable
{
	private static final int PRINT_DATA = 0;
	private static final int PRINT_COMPONENT = 1;
	
	private PageFormat format;
	private JTable table;
	private int pageCount = -1;

	private Font printFont;
	private String[] colHeaders;
	private String footerText = "Page";
	private String headerText = null;
	private TablePrintPage[] pages = null;

	private int lineSpacing = 5;
	private int colSpacing = 2;
	
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
		pt.start();
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
		
		FontMetrics fm = this.table.getGraphics().getFontMetrics(this.printFont);
    int lineHeight = fm.getAscent() + this.lineSpacing;
		
		if (this.headerText != null) pageHeight -= lineHeight;
		if (this.footerText != null) pageHeight -= lineHeight;
		pageHeight -= lineHeight; // reserve one row for the column headers
		
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
		int pagesAcross = 1;
		
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

			this.colHeaders[col] = 
					SwingUtilities.layoutCompoundLabel(fm,title,(Icon)null
							,SwingConstants.TOP 
							,SwingConstants.LEFT
							,SwingConstants.TOP
							,SwingConstants.RIGHT
							,paintViewR, paintIconR, paintTextR, 0);
			
			
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
		
		pg.translate(startx, starty);
		AffineTransform oldTransform= pg.getTransform();
		
		TablePrintPage p = this.pages[pageIndex];
		p.print(pg);
		
		pg.setTransform(oldTransform);
		pg.setClip(null);
		
    pg.setColor(Color.BLACK);
		String footer = this.footerText + " " + p.getPageIndexDisplay() + "/" + (this.pageCount);
    pg.drawString(footer, (int)(wPage/2-footer.length()/2), (int)(hPage - lineSpacing) );

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

	public static void main(String[] args)
	{
		try
		{
			PageFormat format = PrinterJob.getPrinterJob().defaultPage();
			System.out.println("width=" + format.getWidth());
			System.out.println("height=" + format.getHeight());
			System.out.println("x=" + format.getImageableX());
			System.out.println("y=" + format.getImageableY());
			System.out.println("i-width=" + format.getImageableWidth() );
			System.out.println("i-height=" + format.getImageableHeight());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	public static void _main(String[] args)
	{
		int cols = 5;
		int rows = 15;
		
		DefaultTableModel data = new DefaultTableModel(rows, cols);
		for (int row = 0; row < rows; row ++)
		{
			for (int c = 0; c < cols; c++)
			{
				data.setValueAt("Test" + row + "/" + c, row, c);
			}
		}
		JTable tbl = new JTable(data);
		TableColumnModel mod = tbl.getColumnModel();
		for (int c = 0; c < cols; c++)
		{
			mod.getColumn(c).setWidth(85);
		}

		try
		{
			PrinterJob pj=PrinterJob.getPrinterJob();
			PageFormat page = pj.defaultPage();
			Paper p = page.getPaper();
			//p.setImageableArea(PrintUtil.millimeterToPoints(10), PrintUtil.millimeterToPoints(10), PrintUtil.millimeterToPoints(190), PrintUtil.millimeterToPoints(265));
			//page.setPaper(p);
			TablePrinter printer = new TablePrinter(tbl, page, new Font("Courier New", Font.PLAIN, 12));
			printer.setFooterText("Page");
			//printer.setPageFormat(page);
			
			PrintPreview preview = new PrintPreview((JFrame)null, printer);
			System.out.println("done.");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		System.exit(0);
		//PrintPreview preview = new PrintPreview(printer);
	}
}