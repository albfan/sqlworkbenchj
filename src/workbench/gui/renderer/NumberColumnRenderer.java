/*
 * NumberColumnRenderer.java
 *
 * Created on 1. Juli 2002, 13:22
 */

package workbench.gui.renderer;

import java.awt.Color;
import java.awt.Component;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.HashMap;
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
	private HashMap displayCache = new HashMap(1000);
	private Color selectedForeground;
	private Color selectedBackground;
	private Color unselectedForeground;
	private Color unselectedBackground;
	
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
	
		
		if (value instanceof Number)
		{
			String nr = null;
			double d = 0.0;
			Number n = (Number) value;
			nr = (String)this.displayCache.get(n);
			if (nr == null)
			{
				d = n.doubleValue();
				if (!Double.isNaN(d)) nr = formatter.format(d);
				this.displayCache.put(n, nr);
			}
			this.setValue(nr);
			this.setToolTipText(value.toString());
		}
		return this;
	}
	
}
