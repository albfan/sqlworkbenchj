package workbench.gui.renderer;

import javax.swing.table.TableCellRenderer;

import javax.swing.JTable;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.BorderFactory;
import javax.swing.border.Border;

import java.awt.Component;
import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

	private Color selectedForeground;
	private Color selectedBackground;
	private Color unselectedForeground;
	private Color unselectedBackground;

	private static Pattern CRLF = Pattern.compile("(\r\n|\r|\n|\n\r)");
	
	public ToolTipRenderer()
	{
		this.setVerticalAlignment(SwingConstants.TOP);
		this.setHorizontalAlignment(SwingConstants.LEFT);
	}
	
	public Component getTableCellRendererComponent(	JTable table,
																									Object value,
																									boolean isSelected,
																									boolean hasFocus,
																									int row,
																									int col)
	{
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

		String display;
		String toolTip;
		
		if (value == null)
		{
			toolTip = null;
			display = "";
		}
		else
		{
			display = value.toString();
			
			if (display.length() > 0)
			{
				Matcher m = CRLF.matcher(display);
				if (m.find())
				{
					StringBuffer tip = new StringBuffer(display.length() + 50);
					tip.append("<html><body>");
					tip.append(m.replaceAll("<br>"));
					tip.append("</body></html>");
					toolTip = tip.toString();
					display = toolTip;
				}
				else
				{
					toolTip = display;
				}
			}
			else
			{
				// there is a difference in setting the tooltip to null
				// or to an empty string. If you set it to an empty string
				// it will display an ugly looking very small empty tooltip window!
				toolTip = null;
			}

		}
		this.setToolTipText(toolTip);
		this.setText(display);
		
		return this;
	}
	
}
