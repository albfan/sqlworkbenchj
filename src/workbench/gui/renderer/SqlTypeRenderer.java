/*
 * NumberColumnRenderer.java
 *
 * Created on 1. Juli 2002, 13:22
 */

package workbench.gui.renderer;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.WbTable;
import workbench.util.SqlUtil;

/**
 *
 * @author  thomas.kellerer@mgm-edv.de
 */
public class SqlTypeRenderer extends DefaultTableCellRenderer
{
	private Color selectedForeground;
	private Color selectedBackground;
	private Color unselectedForeground;
	private Color unselectedBackground;
	
	/** Creates a new instance of NumberColumnRenderer */
	public SqlTypeRenderer()
	{
	}
	
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	{
		try
		{
			int type = ((Integer)value).intValue();
			String display = SqlUtil.getTypeName(type);
			this.setText(display);
			this.setToolTipText(display);
			
			if (hasFocus)
			{
				this.setBorder(WbTable.FOCUSED_CELL_BORDER);
			}
			else
			{
				this.setBorder(WbSwingUtilities.EMPTY_BORDER);
			}

			if (isSelected)
			{
				if (selectedForeground == null)
				{
					this.selectedForeground = table.getSelectionForeground();
					this.selectedBackground = table.getSelectionBackground();
				}
				super.setForeground(this.selectedForeground);
				super.setBackground(this.selectedBackground);
			}
			else
			{
				if (selectedForeground == null)
				{
					this.unselectedForeground = table.getForeground();
					this.unselectedBackground = table.getBackground();
				}
				super.setForeground(this.unselectedForeground);
				super.setBackground(this.unselectedBackground);
			}
			
		}
		catch (Exception e)
		{
		}
		return this;
	}
	
}
