/*
=====================================================================

  SortHeaderRenderer.java
  
  Created by Claude Duguay
  Copyright (c) 2002
  
=====================================================================
*/

package workbench.gui.components;

import javax.swing.Icon;
import workbench.gui.components.DwTable;
import workbench.gui.components.SortArrowIcon;


public class SortHeaderRenderer 
	extends javax.swing.table.DefaultTableCellRenderer
{
  public static Icon NONSORTED = new SortArrowIcon(SortArrowIcon.NONE);
  public static Icon ASCENDING = new SortArrowIcon(SortArrowIcon.ASCENDING);
  public static Icon DESCENDING = new SortArrowIcon(SortArrowIcon.DESCENDING);
  
  public SortHeaderRenderer()
  {
    setHorizontalTextPosition(LEFT);
    setHorizontalAlignment(CENTER);
  }
  
  public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col)
  {
    int index = -1;
    boolean ascending = true;
    if (table instanceof DwTable)
    {
      DwTable sortTable = (DwTable)table;
      index = sortTable.getSortedColumnIndex();
      ascending = sortTable.isSortedColumnAscending();
    }
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
    Icon icon = ascending ? ASCENDING : DESCENDING;
    setIcon(col == index ? icon : NONSORTED);
    setText((value == null) ? "" : value.toString());
    setBorder(javax.swing.UIManager.getBorder("TableHeader.cellBorder"));
    return this;
  }
}

