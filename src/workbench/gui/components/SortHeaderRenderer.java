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
	private Border headerBorder;
	
  public SortHeaderRenderer()
  {
    setHorizontalTextPosition(LEFT);
    setHorizontalAlignment(LEFT);
		Border empty = new EmptyBorder(0, 1, 0, 1);
		headerBorder = new CompoundBorder(UIManager.getBorder("TableHeader.cellBorder"), empty);
    setBorder(headerBorder);
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

