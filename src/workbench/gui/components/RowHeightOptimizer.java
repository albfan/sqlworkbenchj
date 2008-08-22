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
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.table.TableCellRenderer;
import workbench.gui.renderer.WbRenderer;
import workbench.resource.Settings;
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
		int maxLines = Settings.getInstance().getAutRowHeightMaxLines();
		if (count == 0) return;
		for (int row = 0; row < count; row ++)
		{
			optimizeRowHeight(row, maxLines);
		}
	}
	
	public void optimizeRowHeight(int row, int maxLines)
	{
		int colCount = this.table.getColumnCount();

		int defaultHeight = table.getRowHeight();
		int optHeight = defaultHeight;
//		int addHeight = this.getAdditionalRowSpace();
		
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
			
			String s;
			int lines = 1;

			// The value that is displayed in the table through the renderer
			// is not necessarily identical to the String returned by table.getValueAsString()
			// so we'll first ask the Renderer or its component for the displayed value.
			if (c instanceof WbRenderer)
			{
				WbRenderer wb = (WbRenderer)c;
				s = wb.getDisplayValue();
			}
			else if (c instanceof JTextArea)
			{
				JTextArea text = (JTextArea)c;
				lines = text.getLineCount();
				s = text.getText();
			}
			else if (c instanceof JLabel)
			{
				// DefaultCellRenderer is a JLabel
				s = ((JLabel)c).getText();
			}
			else
			{
				s = this.table.getValueAsString(row, col);
			}

			int stringHeight;
			if (!StringUtil.isEmptyString(s))
			{
				if (lines > maxLines) lines = maxLines;
				int fheight = (fm == null ? 16 : fm.getHeight());
//				Rectangle2D rect = fm.getStringBounds(s, table.getGraphics());
//				stringHeight = lines * (int)rect.getHeight() + addHeight;
				if (lines > maxLines) lines = maxLines;
				stringHeight = lines * fheight;// + addHeight;
			}
			else
			{
				stringHeight = table.getRowHeight();
			}

			optHeight = Math.max(optHeight, stringHeight);
		}
		
		if (optHeight > 0)
		{
			table.setRowHeight(row, optHeight);
		}
	}

//	private int getAdditionalRowSpace()
//	{
//		int addWidth = this.table.getIntercellSpacing().height * 2;
//		if (this.table.getShowHorizontalLines())
//		{
//			addWidth += 2;
//		}
//		return addWidth;
//	}
	
}
