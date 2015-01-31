/*
 * WbNumberFormatter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
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
	private final boolean fixedDigits;

	public WbNumberFormatter(char sep)
	{
		this(0, sep);
	}

	public WbNumberFormatter(int maxDigits, char sep)
	{
		this(maxDigits, sep, false);
	}

	public WbNumberFormatter(int maxDigits, char sep, boolean fixedDigits)
	{
		char filler = fixedDigits ? '0' : '#';
		this.decimalSeparator = sep;
		String pattern = StringUtil.padRight("0.", maxDigits > 0 ? maxDigits + 2 : 22, filler);
		DecimalFormatSymbols symb = new DecimalFormatSymbols();
		symb.setDecimalSeparator(sep);
		this.decimalFormatter = new DecimalFormat(pattern, symb);
		this.maxDigits = maxDigits;
		this.fixedDigits = fixedDigits;
	}

	public char getDecimalSymbol()
	{
		return decimalSeparator;
	}

	/**
	 * Returns a pattern suitable to be applied for a DecimalFormat isntance.
	 *
	 * @return  the pattern;
	 */
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
		if (maxDigits <= 0 && decimalSeparator == '.') return display; // no maximum given

		int scale = value.scale();
		if (scale <= 0 && !fixedDigits) return display; // no decimal digits, nothing to do

		if (scale > maxDigits && maxDigits > 0)
		{
			BigDecimal rounded = value.setScale(this.maxDigits, RoundingMode.HALF_UP);
			display = rounded.toPlainString();
		}

		if (fixedDigits && maxDigits > 0)
		{
			int pos = display.lastIndexOf('.');
			int digits;
			if (pos == -1)
			{
				digits = 0;
			}
			else
			{
				digits = display.length() - pos - 1;
			}

			if (digits < maxDigits)
			{
				int num = (maxDigits - digits);
				StringBuilder sb = new StringBuilder(display.length() + num);
				sb.append(display);
				if (digits == 0)
				{
					sb.append(decimalSeparator);
				}

				for (int i=0; i < num; i++)
				{
					sb.append('0');
				}
				if (decimalSeparator != '.' && digits > 0)
				{
					sb.setCharAt(pos, decimalSeparator);
				}
				return sb.toString();
			}
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
