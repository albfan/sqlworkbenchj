/*
 * ColumnWidthOptimizer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import workbench.gui.renderer.WbRenderer;
import workbench.resource.GuiSettings;
import workbench.util.StringUtil;

/**
 * A class to adjust the column width of a WbTable to the displayed values.
 *
 * @author Thomas Kellerer
 */
public class ColumnWidthOptimizer
{
	private WbTable table;

	public ColumnWidthOptimizer(WbTable client)
	{
		this.table = client;
	}

	public void optimizeAllColWidth()
	{
		this.optimizeAllColWidth(GuiSettings.getMinColumnWidth(), GuiSettings.getMaxColumnWidth(), GuiSettings.getIncludeHeaderInOptimalWidth());
	}

	public void optimizeAllColWidth(boolean respectColName)
	{
		this.optimizeAllColWidth(GuiSettings.getMinColumnWidth(), GuiSettings.getMaxColumnWidth(), respectColName);
	}

	public void optimizeAllColWidth(int minWidth, int maxWidth, boolean respectColName)
	{
		int count = this.table.getColumnCount();
		for (int i = 0; i < count; i++)
		{
			this.optimizeColWidth(i, minWidth, maxWidth, respectColName);
		}
	}

	public void optimizeColWidth(int aColumn)
	{
		this.optimizeColWidth(aColumn, 0, -1, false);
	}

	public void optimizeColWidth(int aColumn, boolean respectColName)
	{
		this.optimizeColWidth(aColumn, GuiSettings.getMinColumnWidth(), GuiSettings.getMaxColumnWidth(), respectColName);
	}

	public void optimizeColWidth(int aColumn, int minWidth, int maxWidth)
	{
		this.optimizeColWidth(aColumn, minWidth, maxWidth, false);
	}

	public void optimizeColWidth(int col, int minWidth, int maxWidth, boolean respectColumnName)
	{
		if (table == null || col < 0 || col > table.getColumnCount() - 1)
		{
			return;
		}

		int optWidth = minWidth;

		if (respectColumnName)
		{
			optWidth = optimizeHeaderColumn(col);
		}

		TableColumnModel colMod = this.table.getColumnModel();
		TableColumn column = colMod.getColumn(col);

		int rowCount = this.table.getRowCount();
		int maxLines = GuiSettings.getAutRowHeightMaxLines();
		String s = null;
		int stringWidth = 0;

		int addWidth = getAdditionalColumnSpace(col);

		for (int row = 0; row < rowCount; row++)
		{
			TableCellRenderer rend = this.table.getCellRenderer(row, col);
			Component c = rend.getTableCellRendererComponent(this.table, table.getValueAt(row, col), false, false, row, col);
			Font f = c.getFont();
			FontMetrics fm = c.getFontMetrics(f);

			// The value that is displayed in the table through the renderer
			// is not necessarily identical to the String returned by table.getValueAsString()
			// so we'll first ask the Renderer or its component for the displayed value.
			if (c instanceof WbRenderer)
			{
				s = ((WbRenderer)c).getDisplayValue();
			}
			else if (c instanceof JTextArea)
			{
				JTextArea text = (JTextArea)c;
				String t = text.getText();
				s = StringUtil.getLongestLine(t, maxLines);
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

			if (s == null || s.length() == 0)
			{
				stringWidth = 0;
			}
			else
			{
				String visible = StringUtil.rtrim(s);
				stringWidth = fm.stringWidth(visible);
				if (visible.length() < s.length())
				{
					stringWidth += fm.stringWidth("www");
				}
			}

			optWidth = Math.max(optWidth, stringWidth + addWidth);
		}

		if (maxWidth > 0)
		{
			optWidth = Math.min(optWidth, maxWidth);
		}

		if (optWidth > 0)
		{
			column.setPreferredWidth(optWidth);
		}
	}

	public void optimizeHeader()
	{
		if (table == null) return;
		TableColumnModel colMod = this.table.getColumnModel();
		if (colMod == null) return;
		for (int col = 0; col < table.getColumnCount(); col ++)
		{
			TableColumn column = colMod.getColumn(col);

			// This method is only used to adjust the column header after
			// the sort indicators have been displayed.
			// as the current width is most probably already adjusted (and reflects the size of the data in this column)
			// the new width may not be smaller than the oldwidth
			int oldWidth = column.getWidth();
			int width = optimizeHeaderColumn(col);

			if (width > oldWidth)
			{
				column.setPreferredWidth(width);
			}
		}
	}

	public int optimizeHeaderColumn(int col)
	{
		if (table == null || col < 0 || col > table.getColumnCount() - 1)
		{
			return -1;
		}

		JTableHeader th = table.getTableHeader();
		TableCellRenderer rend = th.getDefaultRenderer();
		String colName = table.getColumnName(col);

		JComponent c = (JComponent)rend.getTableCellRendererComponent(table, colName, false, false, -1, col);
		Font headerFont = c.getFont();
		FontMetrics hfm = c.getFontMetrics(headerFont);
		Insets ins = c.getInsets();
		int headerWidth = hfm.stringWidth(colName) + getAdditionalHeaderSpace(col) + ins.left + ins.right;
		if (table.isViewColumnSorted(col))
		{
			if (table.isPrimarySortColumn(col))
			{
				headerWidth += SortArrowIcon.ARROW_UP.width;
			}
			else
			{
				headerWidth += SortArrowIcon.SMALL_ARROW_UP.width;
			}
			headerWidth ++;
		}

		return headerWidth;
	}

	private int getAdditionalHeaderSpace(int col)
	{
		int addWidth = table.getIntercellSpacing().width;
		if (table.getShowVerticalLines())
		{
			addWidth += table.getColumnCount() + 2;
		}

		JTableHeader header = table.getTableHeader();
		TableColumnModel headerCols = header.getColumnModel();

		int headerMargin = headerCols.getColumnMargin();

		addWidth += headerMargin;

		return addWidth;
	}

	private int getAdditionalColumnSpace(int col)
	{
		int addWidth = table.getIntercellSpacing().width;
		if (table.getShowVerticalLines())
		{
			addWidth += table.getColumnCount() + 2;
		}
		addWidth += table.getColumnModel().getColumnMargin();
		return addWidth;
	}

	/**
	 * Adjusts the columns to the width defined from the
	 * underlying tables (i.e. getColumnWidth() for each column)
	 * This does not adjust the width of the columns to the content.
	 *
	 * @see #optimizeAllColWidth()
	 */
	public void adjustColumns(boolean adjustToColumnLabel)
	{
		if (this.table.getModel() == null) return;

		DataStoreTableModel dwModel = this.table.getDataStoreTableModel();
		if (dwModel == null) return;

		Font f = this.table.getFont();
		FontMetrics fm = this.table.getFontMetrics(f);
		int charWidth = fm.stringWidth("n");
		TableColumnModel colMod = this.table.getColumnModel();
		if (colMod == null) return;

		int minWidth = GuiSettings.getMinColumnWidth();
		int maxWidth = GuiSettings.getMaxColumnWidth();

		for (int i = 0; i < colMod.getColumnCount(); i++)
		{
			TableColumn col = colMod.getColumn(i);
			int addWidth = this.getAdditionalColumnSpace(i);
			int addHeaderWidth = this.getAdditionalColumnSpace(i);

			int lblWidth = 0;
			if (adjustToColumnLabel)
			{
				String s = dwModel.getColumnName(i);
				lblWidth = fm.stringWidth(s) + addHeaderWidth;
			}
			int width = (dwModel.getColumnWidth(i) * charWidth) + addWidth;
			int w = Math.max(width, lblWidth);
			if (maxWidth > 0)
			{
				w = Math.min(w, maxWidth);
			}
			if (minWidth > 0)
			{
				w = Math.max(w, minWidth);
			}
			col.setPreferredWidth(w);
		}
	}
}
