/*
 * SortHeaderRenderer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;


public class SortHeaderRenderer extends DefaultTableCellRenderer
{
	private static Border DEFAULT_HEADER_BORDER;
	static
	{
		Border empty = new EmptyBorder(0, 1, 0, 1);
		DEFAULT_HEADER_BORDER = new CompoundBorder(UIManager.getBorder("TableHeader.cellBorder"), empty);
	}
	
  public SortHeaderRenderer()
  {
    setHorizontalTextPosition(LEFT);
    setHorizontalAlignment(LEFT);
    setBorder(DEFAULT_HEADER_BORDER);
  }
  
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col)
  {
    int index = -1;
    boolean ascending = true;
    if (table instanceof WbTable)
    {
      WbTable sortTable = (WbTable)table;
      index = sortTable.getSortedViewColumnIndex();
      ascending = sortTable.isSortedColumnAscending();
    }
		if (col == index)
		{
			Icon icon = ascending ? SortArrowIcon.ARROW_DOWN : SortArrowIcon.ARROW_UP;
			this.setIcon(icon);
		}
		else
		{
			this.setIcon(null);
		}
		String text = (value == null) ? "" : value.toString();
		setText(text);
		setToolTipText(text);
    return this;
  }
}

