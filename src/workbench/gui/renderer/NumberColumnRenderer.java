/*
 * NumberColumnRenderer.java
 *
 * Created on 1. Juli 2002, 13:22
 */

package workbench.gui.renderer;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import javax.swing.SwingConstants;

import workbench.WbManager;
import workbench.resource.Settings;

/**
 * @author  workbench@kellerer.org
 */
public class NumberColumnRenderer
	extends ToolTipRenderer
{
	private DecimalFormat decimalFormatter;
	//private HashMap displayCache = new HashMap(500);
	private DecimalFormatSymbols symb = new DecimalFormatSymbols();

	public NumberColumnRenderer()
	{
		String sep = Settings.getInstance().getDecimalSymbol();
		this.symb.setDecimalSeparator(sep.charAt(0));
		decimalFormatter = new DecimalFormat("0.#", symb);
		this.setMaxDigits(4);
		this.setHorizontalAlignment(SwingConstants.RIGHT);
	}

	public NumberColumnRenderer(int maxDigits)
	{
		String sep = Settings.getInstance().getDecimalSymbol();
		this.symb.setDecimalSeparator(sep.charAt(0));
		decimalFormatter = new DecimalFormat("0.#", symb);
		this.setMaxDigits(maxDigits);
		this.setHorizontalAlignment(SwingConstants.RIGHT);
	}

	public NumberColumnRenderer(int maxDigits, char sep)
	{
		this.symb.setDecimalSeparator(sep);
		decimalFormatter = new DecimalFormat("0.#", symb);
		this.setMaxDigits(maxDigits);
		this.setHorizontalAlignment(SwingConstants.RIGHT);
	}
	
	public void setMaxDigits(int maxDigits)
	{
		if (maxDigits <= 0) maxDigits = 10;
		decimalFormatter.setMaximumFractionDigits(maxDigits);
	}
	
	public void setDecimalSymbol(char aSymbol)
	{
		this.symb.setDecimalSeparator(aSymbol);
		this.decimalFormatter.setDecimalFormatSymbols(this.symb);
	}
	
	public void prepareDisplay(Object aValue)
	{
		// this method will not be called with a null value, so we do not need
		// to check it here!
		try
		{
			Number n = (Number) aValue;
			displayValue = decimalFormatter.format(n.doubleValue());
			tooltip = aValue.toString();
		}
		catch (Throwable th)
		{
			displayValue = aValue.toString(); 
			tooltip = null;
		}
	}
}
