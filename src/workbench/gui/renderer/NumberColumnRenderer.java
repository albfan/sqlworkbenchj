/*
 * NumberColumnRenderer.java
 *
 * Created on 1. Juli 2002, 13:22
 */

package workbench.gui.renderer;

import java.awt.Component;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import workbench.WbManager;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.WbTable;

/**
 *
 * @author  thomas.kellerer@mgm-edv.de
 */
public class NumberColumnRenderer
	extends DefaultTableCellRenderer
{
	
	public DecimalFormat formatter;
	
	/** Creates a new instance of NumberColumnRenderer */
	public NumberColumnRenderer(int maxDigits)
	{
		this.setHorizontalAlignment(SwingConstants.RIGHT);
		String sep = WbManager.getSettings().getDecimalSymbol();
		DecimalFormatSymbols symb = new DecimalFormatSymbols();
		symb.setDecimalSeparator(sep.charAt(0));
		formatter = new DecimalFormat("0.#", symb);
		this.formatter.setMaximumFractionDigits(maxDigits);
	}
	
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	{
		Component result = super.getTableCellRendererComponent(table,value,isSelected, false, row, column);
		//JLabel label = (JLabel)result;
		if (value instanceof Number)
		{
			String nr = null;
			Number n = (Number) value;
			double d = n.doubleValue();
			if (!Double.isNaN(d)) nr = formatter.format(d);
			this.setValue(nr);
			this.setToolTipText(Double.toString(d));
		}
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
