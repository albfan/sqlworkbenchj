/*
=====================================================================

  SortHeaderRenderer.java
  
  Created by Claude Duguay
  Copyright (c) 2002
  
=====================================================================
*/

package workbench.gui.components;

import java.awt.EventQueue;
import javax.swing.Icon;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import workbench.gui.components.WbTable;
import workbench.gui.components.SortArrowIcon;


public class SortHeaderRenderer 
	extends javax.swing.table.DefaultTableCellRenderer
{
  public static Icon NONSORTED = new SortArrowIcon(SortArrowIcon.NONE);
  public static Icon ASCENDING = new SortArrowIcon(SortArrowIcon.DOWN);
  public static Icon DESCENDING = new SortArrowIcon(SortArrowIcon.UP);
	private Border headerBorder;
	
  public SortHeaderRenderer()
  {
    setHorizontalTextPosition(LEFT);
    setHorizontalAlignment(LEFT);
		Border empty = new EmptyBorder(0, 1, 0, 1);
		headerBorder = new CompoundBorder(UIManager.getBorder("TableHeader.cellBorder"), empty);
    setBorder(headerBorder);
  }
  
  public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col)
  {
		int column = col;
    int index = -1;
    boolean ascending = true;
    if (table instanceof WbTable)
    {
      WbTable sortTable = (WbTable)table;
      index = sortTable.getSortedViewColumnIndex();
      ascending = sortTable.isSortedColumnAscending();
    }
		/*
    if (table != null)
    {
      javax.swing.table.JTableHeader header = table.getTableHeader();
      if (header != null)
      {
        setForeground(header.getForeground());
        setBackground(header.getBackground());
        setFont(header.getFont());
      }
    }
		*/
		Icon icon = ascending ? ASCENDING : DESCENDING;
		if (column == index)
		{
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

