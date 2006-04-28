/*
 * SortHeaderRenderer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
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
import workbench.storage.DataStore;
import workbench.storage.ResultInfo;


public class SortHeaderRenderer 
	extends DefaultTableCellRenderer
{
	private static Border DEFAULT_HEADER_BORDER = new CompoundBorder(UIManager.getBorder("TableHeader.cellBorder"), new EmptyBorder(0, 1, 0, 1));
	
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
		String type = null;
		
    if (table instanceof WbTable)
    {
      WbTable sortTable = (WbTable)table;
      index = sortTable.getSortedViewColumnIndex();
      ascending = sortTable.isSortedColumnAscending();
			DataStoreTableModel model = sortTable.getDataStoreTableModel();
			if (model != null) type = model.getDbmsType(col);
    }
		
		if (col == index)
		{
			this.setIcon(ascending ? SortArrowIcon.ARROW_DOWN : SortArrowIcon.ARROW_UP);
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
			StringBuffer tip = new StringBuffer(text.length() + 20);
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

