/*
 * NumberColumnRenderer.java
 *
 * Created on 1. Juli 2002, 13:22
 */

package workbench.gui.renderer;

import java.awt.Component;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author  thomas.kellerer@mgm-edv.de
 */
public class NumberColumnRenderer
	extends DefaultTableCellRenderer
{
	
	public DecimalFormat formatter = new DecimalFormat();
	
	/** Creates a new instance of NumberColumnRenderer */
	public NumberColumnRenderer()
	{
		this.formatter.setMaximumFractionDigits(2);
		this.formatter.setMinimumFractionDigits(2);
	}
	
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	{
		Component result = super.getTableCellRendererComponent(table,value,isSelected, hasFocus, row, column);
		if (result instanceof JLabel)
		{
			((JLabel)result).setHorizontalAlignment(SwingConstants.RIGHT);
			if (value instanceof Number)
			{
				String nr = null;
				Number n = (Number) value;
				double d = n.doubleValue();
				if (!Double.isNaN(d)) nr = formatter.format(d);
				this.setValue(nr);
			}
		}
		return result;
	}
	
}
