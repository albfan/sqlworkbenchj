/*
 * RowHeightOptimizer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import javax.swing.JTextArea;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.BadLocationException;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.util.StringUtil;


/**
 * A class to optimize the height of each table row according to their values.
 * 
 * @author support@sql-workbench.net
 */
public class RowHeightOptimizer 
{
	private WbTable table;

	public RowHeightOptimizer(WbTable client)
	{
		this.table = client;
	}

	public void optimizeAllRows()
	{
		int count = this.table.getRowCount();
		int maxLines = GuiSettings.getAutRowHeightMaxLines();
		if (count == 0) return;
		boolean ignore = GuiSettings.getIgnoreWhitespaceForAutoRowHeight();
		for (int row = 0; row < count; row ++)
		{
			optimizeRowHeight(row, maxLines, ignore);
		}
	}

	public void optimizeRowHeight(int row)
	{
		int maxLines = GuiSettings.getAutRowHeightMaxLines();
		boolean ignore = GuiSettings.getIgnoreWhitespaceForAutoRowHeight();
		optimizeRowHeight(row, maxLines, ignore);
	}
	
	public void optimizeRowHeight(int row, int maxLines, boolean ignoreEmptyLines)
	{
		int colCount = this.table.getColumnCount();

		int defaultHeight = table.getRowHeight();
		int optHeight = defaultHeight;
		
		for (int col = 0; col < colCount; col++)
		{
			TableCellRenderer rend = this.table.getCellRenderer(row, col);

			// The renderer might not be identical to the componenent
			// that is used to draw the actual value (e.g. for TextAreaRenderer)
			// so we need to retrieve the real component in order to be able to get
			// the font that is used to render the text
			Component c = rend.getTableCellRendererComponent(this.table, table.getValueAt(row, col), false, false, row, col);
			Font f = c.getFont();
			FontMetrics fm = c.getFontMetrics(f);
			
			int lines = 1;

			// Calculating the lines for the value only makes sense
			// when a multi-line renderer is used which is essentially a JTextArea
			if (c instanceof JTextArea)
			{
				JTextArea text = (JTextArea)c;
				if (ignoreEmptyLines)
				{
					lines = getLineCount(text);
				}
				else
				{
					lines = text.getLineCount();
				}
			}
			
			if (lines > maxLines) lines = maxLines;
			int fheight = (fm == null ? 16 : fm.getHeight());
			int stringHeight = lines * fheight;// + addHeight;
			
			optHeight = Math.max(optHeight, stringHeight);
		}
		
		if (optHeight > 0)
		{
			table.setRowHeight(row, optHeight);
		}
	}

	private int getLineCount(JTextArea text)
	{
		int lines = text.getLineCount();
		if (lines < 1) return lines;

		int line = lines;

		try
		{
			int len = 0;
			while (line > 0 && len == 0)
			{
				line --;
				int start = text.getLineStartOffset(line);
				int end = text.getLineEndOffset(line);
				String content = text.getText(start, end - start);
				if (StringUtil.isBlank(content))
				{
					len = 0;
				}
				else
				{
					len = 1;
				}
			}
		}
		catch (BadLocationException e)
		{
			LogMgr.logError("RowHeightOptimizer.getLineCount()", "Error when calculating lines", e);
			return lines;
		}
		return line + 1;
	}
	
}
