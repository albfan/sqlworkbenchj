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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.SwingConstants;
import workbench.resource.Settings;

/**
 * @author  support@sql-workbench.net
 */
public class NumberColumnRenderer
	extends ToolTipRenderer
{
	private DecimalFormat decimalFormatter;
	private DecimalFormatSymbols symb = new DecimalFormatSymbols();
	private FieldPosition startPos = new FieldPosition(0);
	
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
	
	public final void setMaxDigits(int maxDigits)
	{
		if (maxDigits <= 0) maxDigits = 10;
		decimalFormatter.setMaximumFractionDigits(maxDigits);
	}
	
	public void setDecimalSymbol(char aSymbol)
	{
		this.symb.setDecimalSeparator(aSymbol);
		this.decimalFormatter.setDecimalFormatSymbols(this.symb);
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
			synchronized (this.decimalFormatter)
			{
				// BigDecimal cannot be formatted using a DecimalFormatter
				// without possible loss of precission
				if (n instanceof BigDecimal)
				{
					displayValue = ((BigDecimal)n).toString();
				}
				else if (isInteger(n))
				{
					// BigInteger cannot be formatted without a possible 
					// loss of precission as well, but for all other 
					// "Integer" types, toString() will also produce
					// correct results.
					displayValue = n.toString();
				}
				else 
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
