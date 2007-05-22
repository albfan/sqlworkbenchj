/*
 * NumberColumnRenderer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.renderer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.SwingConstants;
import workbench.resource.Settings;
import workbench.util.StringUtil;

/**
 * @author  support@sql-workbench.net
 */
public class NumberColumnRenderer
	extends ToolTipRenderer
{
	private DecimalFormat decimalFormatter;
	private DecimalFormatSymbols symb = new DecimalFormatSymbols();
	private int maxDigits = -1;
	
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
	
	public final void setMaxDigits(int digits)
	{
		synchronized (this.decimalFormatter)
		{
			if (digits <= 0) this.maxDigits = 10;
			else this.maxDigits = digits;
			decimalFormatter.setMaximumFractionDigits(maxDigits);
		}
	}
	
	public void setDecimalSymbol(char aSymbol)
	{
		synchronized (this.decimalFormatter)
		{
			this.symb.setDecimalSeparator(aSymbol);
			this.decimalFormatter.setDecimalFormatSymbols(this.symb);
		}
	}
	
	private boolean isInteger(Number n)
	{
		return (n instanceof Integer 
			|| n instanceof Long 
			|| n instanceof Short 
			|| n instanceof BigInteger
			|| n instanceof AtomicInteger
			|| n instanceof AtomicLong);
	}
	
	public void prepareDisplay(Object aValue)
	{
		try
		{
			Number n = (Number) aValue;
			
			// BigDecimal cannot be formatted using a DecimalFormatter
			// without possible loss of precission
			if (n instanceof BigDecimal)
			{
				BigDecimal d = (BigDecimal)n;

				// Oracle returns all numeric values as BigDecimal
				// but if the value is actual an "Integer" toString() will
				// return a String without a decimal separator
				// in that case we won't apply the rounding to the 
				// required number of decimal digits
				String v = d.toString();
				if (v.lastIndexOf('.') > -1)
				{
					char sepChar = this.symb.getDecimalSeparator();
					// if a decimal point was found, then we have to apply rounding rules
					BigDecimal rounded = d.setScale(this.maxDigits, RoundingMode.HALF_UP);

					v = rounded.toString();
					// toString() will use a dot as the decimal separator
					// if the user configured a different one, we have to 
					// replace the last dot in the string with the user
					// defined decimal separator.
					if (sepChar != '.')
					{
						int pos = v.lastIndexOf('.');
						if (pos > -1)
						{
							char[] ca = v.toCharArray();
							ca[pos] = sepChar;
							v = new String(ca);
						}
					}
				}
				
				this.displayValue = v;
			}
			else if (isInteger(n))
			{
				// BigInteger cannot be formatted without a possible 
				// loss of precission as well, but for "Integer" types, 
				// toString() should produce the correct results
				displayValue = n.toString();
			}
			else 
			{
				synchronized (this.decimalFormatter)
				{
					displayValue = decimalFormatter.format(n.doubleValue());
				}
			}
			this.tooltip = aValue.toString();
		}
		catch (Throwable th)
		{
			displayValue = aValue.toString(); 
			this.tooltip = null;
		}
	}
}
