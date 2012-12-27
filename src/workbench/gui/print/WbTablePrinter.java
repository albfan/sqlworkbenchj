/*
 * WbTablePrinter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
package workbench.gui.print;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.text.MessageFormat;
import javax.swing.JTable.PrintMode;
import workbench.gui.components.WbTable;
import workbench.log.LogMgr;

/**
 *
 * @author Thomas Kellerer
 */
public class WbTablePrinter
	implements Pageable
{
	private WbTable table;
	private String footerText;
	private String headerText;
	private PageFormat pageFormat;
	private int pageCount;
	private boolean fitWidth;

	public WbTablePrinter(WbTable table, PageFormat pageSize, String header, String footer)
	{
		this.table = table;
		this.pageFormat = pageSize;
	}

	public void setFitWidth(boolean flag)
	{
		boolean old = this.fitWidth;
		this.fitWidth = flag;
		if (old != fitWidth)
		{
			calculatePages();
		}
	}

	public Font getFont()
	{
		return this.table.getPrintFont();
	}

	public PageFormat getPageFormat()
	{
		return pageFormat;
	}

	public void setPageFormat(PageFormat newFormat)
	{
		this.pageFormat = newFormat;
	}

	public void setFont(Font printFont)
	{
		this.table.setPrintFont(printFont);
		calculatePages();
	}

	public int print(Graphics g, PageFormat format, int pageIndex)
		throws PrinterException
	{
		return getPrintable().print(g, format, pageIndex);
	}

	public Printable getPrintable()
	{
		MessageFormat header = new MessageFormat(headerText);
		return table.getPrintable(fitWidth ? PrintMode.FIT_WIDTH : PrintMode.NORMAL, header, null);
	}

	private void calculatePages()
	{
		Printable printable = getPrintable();
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

	@Override
	public int getNumberOfPages()
	{
		return pageCount;
	}

	@Override
	public PageFormat getPageFormat(int pageIndex)
		throws IndexOutOfBoundsException
	{
		return getPageFormat();
	}

	@Override
	public Printable getPrintable(int pageIndex)
		throws IndexOutOfBoundsException
	{
		return getPrintable();
	}

}
