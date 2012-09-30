/*
 * WbNumberFormatter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author Thomas Kellerer
 */
public class WbNumberFormatter
{
	private final DecimalFormat decimalFormatter;
	private final char decimalSeparator;
	private final int maxDigits;

	public WbNumberFormatter(char sep)
	{
		this(0, sep);
	}

	public WbNumberFormatter(int maxDigits, char sep)
	{
		this.decimalSeparator = sep;
		String pattern = null;
		if (maxDigits > 0)
		{
			pattern = StringUtil.padRight("0.", maxDigits + 2, '#');
		}
		else
		{
			pattern = "0.########################################";
		}
		DecimalFormatSymbols symb = new DecimalFormatSymbols();
		symb.setDecimalSeparator(sep);
		this.decimalFormatter = new DecimalFormat(pattern, symb);
		this.maxDigits = maxDigits;
	}

	public char getDecimalSymbol()
	{
		return decimalSeparator;
	}

	public String toFormatterPattern()
	{
		return decimalFormatter.toPattern();
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

	public String format(Number value)
	{
		if (value == null) return "";

		String formatted = null;

		if (value instanceof BigDecimal)
		{
			formatted = format((BigDecimal)value);
		}
		else if (isInteger(value))
		{
			formatted = value.toString();
		}
		else
		{
			synchronized (this.decimalFormatter)
			{
				formatted = decimalFormatter.format(value);
			}
		}
		return formatted;
	}

	/**
	 * Format a BigDecimal value according to the settings.
	 *
	 * A BigDecimal cannot be formatted using DecimalFormat, so this method
	 * applies the formatting rules manually.
	 * maxDigits is used for "maximum" number of digits. If there are less digits,
	 * the formatted value will not be padded with zeros
	 *
	 * @param value  the value to format
	 * @return the formatted value
	 */
	private String format(BigDecimal value)
	{
		String display = value.toPlainString();
		if (maxDigits <= 0) return display; // no maximum given

		int scale = value.scale();
		if (scale <= 0) return display; // no decimal digits, nothing to do

		if (scale > maxDigits)
		{
			BigDecimal rounded = value.setScale(this.maxDigits, RoundingMode.HALF_UP);
			display = rounded.toPlainString();
		}

		if (decimalSeparator != '.' )
		{
			// this is a little bit faster than using a StringBuilder and setCharAt()
			// and as numbers will not contain any two byte characters theres nothing
			// to worry about encoding.
			int pos = display.lastIndexOf('.');
			char[] ca = display.toCharArray();
			ca[pos] = decimalSeparator;
			display = new String(ca);
		}

		return display;
	}

}
