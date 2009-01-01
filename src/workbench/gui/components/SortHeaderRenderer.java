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

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;

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
	
	public SortHeaderRenderer()
	{
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
				type = model.getDbmsType(col);
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
			tip.append("<html>&nbsp;");
			tip.append(text);
			tip.append("&nbsp;<br>&nbsp;<code>");
			tip.append(type);
			tip.append("</code>&nbsp;</html>");
			display.setToolTipText(tip.toString());
		}
		return display;
	}
}

