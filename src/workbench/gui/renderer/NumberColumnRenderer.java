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
import workbench.util.StringUtil;

/**
 *
 * @author  thomas.kellerer@mgm-edv.de
 */
public class NumberColumnRenderer
	extends ToolTipRenderer
{
	private static DecimalFormat decimalFormatter;
	private HashMap displayCache = new HashMap(500);
	
	public NumberColumnRenderer(int maxDigits)
	{
		String sep = WbManager.getSettings().getDecimalSymbol();
		DecimalFormatSymbols symb = new DecimalFormatSymbols();
		symb.setDecimalSeparator(sep.charAt(0));
		decimalFormatter = new DecimalFormat("0.#", symb);
		decimalFormatter.setMaximumFractionDigits(maxDigits);
		this.setHorizontalAlignment(SwingConstants.RIGHT);
	}
	
	public void clearDisplayCache() 
	{
		this.displayCache.clear();
	}
	
	public String[] getDisplay(Object aValue)
	{
		if (aValue == null)
		{
			return ToolTipRenderer.EMPTY_DISPLAY;
		}
		else
		{
			String nr = null;
			String str = aValue.toString();
			try
			{
				Number n = (Number) aValue;
				nr = (String)this.displayCache.get(n);
				if (nr == null)
				{
					double d = n.doubleValue();
					if (!Double.isNaN(d))
					{
						nr = decimalFormatter.format(d);
					}
					else
					{
						nr = StringUtil.EMPTY_STRING;
						str = null;
					}
					
					this.displayCache.put(n, nr);
				}
				displayResult[0] = nr;
				displayResult[1] = str;
			}
			catch (Throwable th)
			{
				displayResult[0] = str; 
				displayResult[1] = null;
			}
		}
		return displayResult;
	}
	
}
