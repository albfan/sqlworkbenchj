/*
 * SortHeaderRenderer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.Component;

import java.sql.Types;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;
import workbench.db.ColumnIdentifier;
import workbench.resource.Settings;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A renderer for table headers to be able to display a sort indicator and customized
 * tooltips that show the data type of the column.
 *
 * If adjusts the display of the default renderer to display the sort indicator if
 * the default renderer returns a JLabel.
 *
 * Otherwise a separate JLabel is used to display the header that is made to look
 * like the original component returned from the default renderer.
 *
 * @author support@sql-workbench.net
 */
public class SortHeaderRenderer
	implements TableCellRenderer
{
	private JLabel displayLabel = new JLabel();
	private boolean showFullTypeInfo;

	public SortHeaderRenderer()
	{
		showFullTypeInfo = Settings.getInstance().getBoolProperty("workbench.gui.db.showfulltypeinfo", false);
	}

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
			displayLabel.setFont(c.getFont());
			displayLabel.setBorder(c.getBorder());
			displayLabel.setForeground(c.getForeground());
			displayLabel.setBackground(c.getBackground());
			displayLabel.setText(text);
			displayLabel.setOpaque(c.isOpaque());
			display = displayLabel;
		}
		
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
				ColumnIdentifier colId = model.getDataStore().getResultInfo().getColumn(col);
				type = colId.getDbmsType();
				javaType = colId.getDataType();
				javaTypeName = SqlUtil.getTypeName(javaType);
				remarks = colId.getComment();
			}
		}

		if (sorted)
		{
			if (primary)
			{
				display.setIcon(ascending ? SortArrowIcon.ARROW_DOWN : SortArrowIcon.ARROW_UP);
			}
			else
			{
				display.setIcon(ascending ? SortArrowIcon.SMALL_ARROW_DOWN : SortArrowIcon.SMALL_ARROW_UP);
			}
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
				tip.append(javaTypeName + " (" + javaType + ")");
			}
			tip.append("</code></html>");
			display.setToolTipText(tip.toString());
		}
		return display;
	}
}

