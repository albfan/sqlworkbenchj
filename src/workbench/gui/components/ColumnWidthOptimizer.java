/*
 * ColumnWidthOptimizer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.gui.components;

import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import workbench.resource.GuiSettings;

import workbench.gui.renderer.SortHeaderRenderer;
import workbench.gui.renderer.WbRenderer;

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

	public void optimizeColWidth(int col, int minWidth, int maxWidth, boolean respectColumnName)
	{
		int width = calculateOptimalColumnWidth(col, minWidth, maxWidth, respectColumnName, null);
		if (width > 0)
		{
			TableColumnModel colMod = this.table.getColumnModel();
			TableColumn column = colMod.getColumn(col);
			column.setPreferredWidth(width);
		}
	}

	public int calculateOptimalColumnWidth(int col, int minWidth, int maxWidth, boolean respectColumnName, FontMetrics fontInfo)
	{
		if (table == null || col < 0 || col > table.getColumnCount() - 1)
		{
			return -1;
		}

		int optWidth = minWidth;

		if (respectColumnName)
		{
			optWidth = optimizeHeaderColumn(col, fontInfo);
		}

		int rowCount = this.table.getRowCount();
		int maxLines = GuiSettings.getAutRowHeightMaxLines();
		String s = null;
		int stringWidth = 0;

		int addWidth = getAdditionalColumnSpace();

		for (int row = 0; row < rowCount; row++)
		{
			TableCellRenderer rend = this.table.getCellRenderer(row, col);
			Component c = rend.getTableCellRendererComponent(this.table, table.getValueAt(row, col), false, false, row, col);
			FontMetrics fm = fontInfo;
			if (fm == null)
			{
				Font f = c.getFont();
				fm = c.getFontMetrics(f);
			}

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
		return optWidth;
	}

	/**
	 * Adjust the column header width after sorting.
	 */
	public void optimizeHeader()
	{
		if (table == null) return;
		TableColumnModel colMod = this.table.getColumnModel();
		if (colMod == null) return;
		for (int col = 0; col < table.getColumnCount(); col ++)
		{
			TableColumn column = colMod.getColumn(col);

			// This method is only used to adjust the column header after the sort indicators have been displayed.
			// As the current width is most probably already adjusted (and reflects the size of the data in this column)
			// the new width should not be smaller than the old width (because the row data is not evaluated here!)
			int oldWidth = column.getWidth();
			int width = optimizeHeaderColumn(col, null);

			if (width > oldWidth)
			{
				column.setPreferredWidth(width);
			}
		}
	}

	public int optimizeHeaderColumn(int col, FontMetrics fm)
	{
		if (table == null || col < 0 || col > table.getColumnCount() - 1)
		{
			return -1;
		}

		// JTableHeader.getDefaultRenderer() does not return our own sort renderer
		// therefor we use the "cached" instance directly
		// Only if that is not initialized for some reason, the default renderer is used
		TableCellRenderer rend = table.getHeaderRenderer();
		if (rend == null)
		{
			JTableHeader th = table.getTableHeader();
			rend = th.getDefaultRenderer();
		}
		String colName = table.getColumnName(col);

		JComponent c = (JComponent)rend.getTableCellRendererComponent(table, colName, false, false, -1, col);

		FontMetrics hfm = fm;
		if (hfm == null)
		{
			Font headerFont = c.getFont();
			hfm = c.getFontMetrics(headerFont);
		}
		Insets ins = c.getInsets();
		int headerWidth = hfm.stringWidth(colName) + getAdditionalHeaderSpace() + ins.left + ins.right;
		if (table.isViewColumnSorted(col))
		{
			int iconWidth = (int)(SortHeaderRenderer.getArrowSize(fm, table.isPrimarySortColumn(col)) * 1.15);
			headerWidth += iconWidth;
		}

		return headerWidth;
	}

	private int getAdditionalHeaderSpace()
	{
		int addWidth = table.getIntercellSpacing().width;
		if (table.getShowVerticalLines())
		{
			addWidth += 4;
		}

		JTableHeader header = table.getTableHeader();
		TableColumnModel headerCols = header.getColumnModel();

		int headerMargin = headerCols.getColumnMargin();

		addWidth += headerMargin;

		return addWidth;
	}

	private int getAdditionalColumnSpace()
	{
		int addWidth = table.getIntercellSpacing().width;
		if (table.getShowVerticalLines())
		{
			addWidth += 4;
		}
		addWidth += table.getColumnModel().getColumnMargin();
		return addWidth;
	}

	/**
	 * Adjusts the columns to the width defined from the
	 * underlying tables.
	 *
	 * This will use getColumnWidth() for each column, it does not take the columns content into account
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
		int charWidth = Math.max(fm.getMaxAdvance(), fm.charWidth('M'));
		TableColumnModel colMod = this.table.getColumnModel();
		if (colMod == null) return;

		int minWidth = GuiSettings.getMinColumnWidth();
		int maxWidth = GuiSettings.getMaxColumnWidth();

		int addWidth = this.getAdditionalColumnSpace();

		for (int i = 0; i < colMod.getColumnCount(); i++)
		{
			TableColumn col = colMod.getColumn(i);
			int lblWidth = 0;
			if (adjustToColumnLabel)
			{
				String s = dwModel.getColumnName(i);
				lblWidth = fm.stringWidth(s) + addWidth;
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
