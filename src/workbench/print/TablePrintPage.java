/*
 * TablePageDefinition.java
 *
 * Created on July 25, 2003, 6:27 PM
 */

package workbench.print;

import java.awt.*;
import java.awt.geom.AffineTransform;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;


/**
 *	This class is responsible for keeping a page definition while printing a JTable. 
 *	When printing this page, the TablePrintPage assumes that the clipping is set in 
 *  a way that it can start printing at 0,0 and can print over the whole Graphics object
 *  This means the caller needs to set the margins according to the page layout.
 *  It is also not checked if the definition actually fits on the graphics context
 *  this calculation needs to be done prior to creating the TablePrintPages
 *
 * @see TablePrinter
 * @author  thomas
 */
public class TablePrintPage
{
	
	private JTable table;
	private int startRow;
	private int endRow; 
	private int startCol;
	private int endCol;
	private int[] colWidth;
	private Font printFont;
	private int pageNumDown = -1;
	private int pageNumAcross = -1;
	private int pageIndex;
	private int lineSpacing;
	private int colSpacing;
	private String[] colHeaders;
	private int[] colHeadersX;
	
	public TablePrintPage(JTable source, int startRow, int endRow, int startColumn, int endColumn)
	{
		this(source, startRow, endRow, startColumn, endColumn, null);
	}
	public TablePrintPage(JTable source, int startRow, int endRow, int startColumn, int endColumn, int[] width)
	{
		this.table = source;
		this.startRow = startRow;
		this.endRow = endRow;
		this.startCol = startColumn;
		this.endCol = endColumn;
		this.colWidth = width;
	}

	public void setFont(Font aFont) { this.printFont = aFont; }
	public Font getFont() { return this.printFont; }

	public void setPageNumberAcross(int aNum) { this.pageNumAcross = aNum; }
	public int getPageNumberAcross() { return this.pageNumAcross; }
	
	public void setPageNumberDown(int aNum) { this.pageNumDown = aNum; }
	public int getPageNumberDown() { return this.pageNumDown; }
	
	public void setPageIndex(int aNum) { this.pageIndex = aNum; }
	public int getPageIndex() { return this.pageIndex; }

	public String getPageIndexDisplay()
	{
		if (this.pageNumAcross == -1 && this.pageNumDown == -1)
		{
			return Integer.toString(this.pageIndex);
		}
		else
		{
			return this.pageNumDown + "-" + this.pageNumAcross;
		}
	}
	public void setColumnHeaders(String[] headers)
	{
		this.colHeaders = headers;
	}

	public void setColumnWidths(int[] widths)
	{
		this.colWidth = widths;
	}
	
	public void setColumnLabelsXPos(int[] xpos)
	{
		this.colHeadersX = xpos;
	}
	
	public String toString()
	{
		return "Page V:" + this.pageNumDown + ", H:" + this.pageNumAcross + ", from row " + this.startRow + " to " + this.endRow + ", from column " + this.startCol + " to " + this.endCol;
	}
	public void setSpacing(int line, int column)
	{
		this.lineSpacing = line;
		this.colSpacing = column;
	}
	
	private void calculateColWidth()
	{
		TableColumnModel model =  this.table.getColumnModel();
		int colCount = model.getColumnCount();
		this.colWidth = new int[colCount];
		for (int col = 0; col < colCount; col++)
		{
			this.colWidth[col] = model.getColumn(col).getWidth();
		}
	}
	
	public void print(Graphics2D pg)
	{
		Font dataFont = this.printFont;
		if (dataFont == null) dataFont = this.table.getFont();

		if (this.colWidth == null)
		{
			this.calculateColWidth();
		}
		
		Font headerFont = dataFont.deriveFont(Font.BOLD);
		FontMetrics fm = pg.getFontMetrics(headerFont);
    int lineHeight = fm.getAscent();
		
		AffineTransform oldTransform= pg.getTransform();
		
		pg.setFont(headerFont);
    pg.setColor(Color.BLACK);

		double x = 0;
		double y = 0;
		pg.translate(0, lineHeight);
		for (int col= this.startCol; col <= this.endCol; col++)
		{
			if (this.colHeaders[col] != null)
			{
				pg.drawString(this.colHeaders[col], (int)x + this.colHeadersX[col], (int)y);
			}
			x += this.colWidth[col] + this.colSpacing;
		}

		Stroke s = pg.getStroke();
		pg.setStroke(new BasicStroke(0.3f));
		pg.drawLine(0, (int)y + 1, (int)x, (int)y + 1);
		if (s != null) pg.setStroke(s);
		fm = pg.getFontMetrics(dataFont);
    lineHeight = fm.getAscent();
		
		pg.setTransform(oldTransform);
		y += (lineHeight + lineSpacing);
		pg.translate(0, y);
		pg.setFont(dataFont);
		
		for (int row = this.startRow; row <= this.endRow; row++) 
		{
			for (int col= this.startCol; col <= this.endCol; col++)
			{
				Object value = this.table.getValueAt(row, col);
				if (value == null) continue;
				TableCellRenderer rend = table.getCellRenderer(row, col);
				Component c = rend.getTableCellRendererComponent(table, value, false, false, row, col);
				c.setSize(this.colWidth[col], lineHeight);
				c.print(pg);
				pg.translate(this.colWidth[col] + colSpacing, 0);
			}
			pg.setTransform(oldTransform);
			y += (lineHeight + lineSpacing);
			pg.translate(0, y);
		}
	}
}
