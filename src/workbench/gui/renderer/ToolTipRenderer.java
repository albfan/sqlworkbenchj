package workbench.gui.renderer;

import javax.swing.table.TableCellRenderer;

import javax.swing.JTable;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.BorderFactory;
import javax.swing.border.Border;

import java.awt.Component;
import java.awt.Color;
import javax.swing.JComponent;
import javax.swing.table.DefaultTableCellRenderer;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.TextComponentMouseListener;
import workbench.gui.components.WbTable;

/**
 * Displays a string in a table cell and shows a tool
 * tip if the string is too long to fit in the cell.
 */
public class ToolTipRenderer 
	extends DefaultTableCellRenderer 
{

	public Component getTableCellRendererComponent(	JTable table,
																									Object value,
																									boolean isSelected,
																									boolean hasFocus,
																									int row,
																									int col)
	{
		Component result = super.getTableCellRendererComponent(table, value, isSelected, false, row, col);

		String display;
		if (value == null)
			display = "";
		else
			display = value.toString();

		this.setToolTipText(display);
		if (hasFocus)
		{
			this.setBorder(WbTable.FOCUSED_CELL_BORDER);
		}
		else
		{
			this.setBorder(WbSwingUtilities.EMPTY_BORDER);
		}
		
		return result;
	}
}
