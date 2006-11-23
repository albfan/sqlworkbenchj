/*
 * NumberColumnRenderer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.renderer;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

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
	
	public void prepareDisplay(Object aValue)
	{
		try
		{
			Number n = (Number) aValue;
			synchronized (this.decimalFormatter)
			{
				displayValue = decimalFormatter.format(n.doubleValue());
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
