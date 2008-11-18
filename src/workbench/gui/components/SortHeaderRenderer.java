/*
 * SortHeaderRenderer.java
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

import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

/**
 *
 * @author support@sql-workbench.net
 */
public class SortHeaderRenderer
	extends DefaultTableCellRenderer
{
	public SortHeaderRenderer()
	{
		super();
		setHorizontalTextPosition(LEFT);
		setHorizontalAlignment(LEFT);
		setBorder(UIManager.getBorder("TableHeader.cellBorder"));
	}

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col)
	{
		boolean sorted = false;
		boolean ascending = false;
		boolean primary = false;


		JTableHeader header = table.getTableHeader();
		if (header != null)
		{
			setForeground(header.getForeground());
			setBackground(header.getBackground());

			// This seems to be necessary in order to make sure
			// multi-byte values (e.g. chinese) are displayed correctly
			setFont(header.getFont());
		}

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
				this.setIcon(ascending ? SortArrowIcon.ARROW_DOWN : SortArrowIcon.ARROW_UP);
			}
			else
			{
				this.setIcon(ascending ? SortArrowIcon.SMALL_ARROW_DOWN : SortArrowIcon.SMALL_ARROW_UP);
			}
		}
		else
		{
			this.setIcon(null);
		}
		String text = (value == null) ? "" : value.toString();
		setText(text);

		if (type == null)
		{
			setToolTipText(text);
		}
		else
		{
			StringBuilder tip = new StringBuilder(text.length() + 20);
			tip.append("<html>&nbsp;");
			tip.append(text);
			tip.append("&nbsp;<br>&nbsp;<code>");
			tip.append(type);
			tip.append("</code>&nbsp;</html>");
			setToolTipText(tip.toString());
		}
		return this;
	}
}

