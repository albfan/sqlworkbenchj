/*
 * SortHeaderRenderer.java
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
package workbench.gui.renderer;

import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.Types;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;

import workbench.resource.GuiSettings;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;

import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.SortArrowIcon;
import workbench.gui.components.WbTable;

import workbench.storage.DataStore;
import workbench.storage.ResultInfo;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A renderer for table headers to be able to display a sort indicator and customized
 * tooltips that show the data type of the column.
 *
 * It relies on the default header renderer and only adds the sort indicator to it.
 *
 * As a fallback in case the default renderer is not using a JLabel the SortHeaderRenderer is using
 * it's own JLabel instance which is returned instead in getTableCellRendererComponent().
 * This should usually not happen though.
 *
 * @author Thomas Kellerer
 */
public class SortHeaderRenderer
	implements TableCellRenderer, PropertyChangeListener
{
	private final JLabel displayLabel = new JLabel();
	private boolean showFullTypeInfo;
	private boolean showBoldHeader;
	private boolean highlightPk;

	public SortHeaderRenderer()
	{
		readSettings();
		Settings.getInstance().addPropertyChangeListener(this, GuiSettings.PROP_TABLE_HEADER_BOLD, GuiSettings.PROP_TABLE_HEADER_FULL_TYPE_INFO);
	}

	public void setShowPKIcon(boolean flag)
	{
		this.highlightPk = flag;
	}

	private void readSettings()
	{
		showBoldHeader = GuiSettings.showTableHeaderInBold();
		showFullTypeInfo = Settings.getInstance().getBoolProperty(GuiSettings.PROP_TABLE_HEADER_FULL_TYPE_INFO, false);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		readSettings();
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col)
	{
		TableCellRenderer realRenderer = table.getTableHeader().getDefaultRenderer();

		JComponent c = (JComponent)realRenderer.getTableCellRendererComponent (table,value, isSelected, hasFocus, row, col);

		boolean sorted = false;
		boolean ascending = false;
		boolean primary = false;

		String text = (value == null ? "" : value.toString());
		JLabel display = null;
		if (c instanceof JLabel)
		{
			display = (JLabel)c;
		}
		else
		{
			// this is a fallback and should not happen
			displayLabel.setFont(c.getFont());
			displayLabel.setBorder(c.getBorder());
			displayLabel.setForeground(c.getForeground());
			displayLabel.setBackground(c.getBackground());
			displayLabel.setText(text);
			displayLabel.setOpaque(c.isOpaque());
			display = displayLabel;
		}

		if (showBoldHeader)
		{
			display.setFont(display.getFont().deriveFont(Font.BOLD));
		}
		display.setIconTextGap(5);
		display.setHorizontalTextPosition(SwingConstants.LEFT);
		display.setHorizontalAlignment(SwingConstants.LEFT);

		String type = null;
		String javaTypeName = null;
		String remarks = null;

		int javaType = Types.OTHER;

		if (table instanceof WbTable)
		{
			WbTable sortTable = (WbTable)table;

			sorted = sortTable.isViewColumnSorted(col);
			if (sorted)
			{
				ascending = sortTable.isViewColumnSortAscending(col);
				primary = sortTable.isPrimarySortColumn(col);
			}
			DataStoreTableModel model = sortTable.getDataStoreTableModel();

			if (model != null)
			{
				int realCol = table.convertColumnIndexToModel(col) - model.getRealColumnStart();
				if (realCol >= 0)
				{
					DataStore ds = model.getDataStore();
					ResultInfo info = (ds == null ? null : ds.getResultInfo());

					ColumnIdentifier colId = (info == null ? null : info.getColumn(realCol));
					if (colId != null)
					{
						type = colId.getDbmsType();
						javaType = colId.getDataType();
						javaTypeName = SqlUtil.getTypeName(javaType);
						remarks = colId.getComment();
						if (highlightPk && colId.isPkColumn())
						{
							Font f = display.getFont().deriveFont(Font.ITALIC);
							display.setFont(f);
						}
					}
				}
			}
		}

		if (sorted)
		{
			SortArrowIcon icon = null;
			Font f = display.getFont();
			FontMetrics fm = display.getFontMetrics(f);
			int height = getArrowSize(fm, primary);
			icon = SortArrowIcon.getIcon(ascending ? SortArrowIcon.Direction.UP : SortArrowIcon.Direction.DOWN, height);
			display.setIcon(icon);
		}
		else
		{
			display.setIcon(null);
		}

		if (type == null)
		{
			display.setToolTipText(text);
		}
		else
		{
			StringBuilder tip = new StringBuilder(text.length() + 20);
			tip.append("<html><code>");
			tip.append(text);
			tip.append("<br>");
			tip.append(type);
			if (StringUtil.isNonBlank(remarks))
			{
				tip.append("<br>\"<i>");
				tip.append(remarks);
				tip.append("</i>\"");
			}

			if (showFullTypeInfo)
			{
				tip.append("<br>");
				tip.append(table.getColumnClass(col).getName());
				tip.append("<br>");
				tip.append(javaType + "/" + javaTypeName);
			}
			tip.append("</code></html>");
			display.setToolTipText(tip.toString());
		}
		return display;
	}

	public static int getArrowSize(FontMetrics fm, boolean primary)
	{
		if (fm == null)
		{
			return primary ? 16 : 8;
		}
		int headerHeight = fm.getHeight();
		if (primary)
		{
			return (int) (headerHeight * 0.6);
		}
		return (int) (headerHeight * 0.5);
	}
}

