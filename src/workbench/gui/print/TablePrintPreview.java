/*
 * TablePreview.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.print;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.text.MessageFormat;
import javax.swing.JTable;
import javax.swing.JTable.PrintMode;
import workbench.log.LogMgr;

/**
 *
 * @author Thomas Kellerer
 */
public class TablePrintPreview
{

	private JTable table;
	private String footerText;
	private String headerText;
	private PageFormat pageFormat;
	private int pageCount;
	private boolean fitWidth;

	public TablePrintPreview(JTable table, PageFormat pageSize, String header, String footer)
	{
		this.table = table;
		pageFormat = pageSize;
	}

	public void setFitWidth(boolean flag)
	{
		this.fitWidth = flag;
	}
	
	private void calculatePages()
	{
		MessageFormat header = new MessageFormat(headerText);
		Printable printable = table.getPrintable(fitWidth ? PrintMode.FIT_WIDTH : PrintMode.NORMAL, header, null);
		int width = (int) pageFormat.getWidth();
		int height = (int) pageFormat.getHeight();

		pageCount = 0;
		Image pageImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics g = pageImage.getGraphics();
		int result;
		try
		{
			result = printable.print(g, pageFormat, pageCount);
			while (result != Printable.PAGE_EXISTS)
			{
				pageCount ++;
				result = printable.print(g, pageFormat, pageCount);
			}
		}
		catch (PrinterException ex)
		{
			LogMgr.logError("TablePrintPreview.calculatePages()", "Could not calculate pages", ex);
		}
	}

}
