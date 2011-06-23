/*
 * DurationFormatter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import workbench.resource.Settings;

/**
 *
 * @author Thomas Kellerer
 */
public class DurationFormatter
{
	private final DecimalFormat numberFormatter;
	public static final long ONE_SECOND = 1000;
	public static final long ONE_MINUTE = ONE_SECOND * 60;
	public static final long ONE_HOUR = ONE_MINUTE * 60;

	public DurationFormatter()
	{
		numberFormatter = createTimingFormatter();
	}

	public DurationFormatter(char decimalSep)
	{
		numberFormatter = createTimingFormatter(decimalSep);
	}

	public static DecimalFormat createTimingFormatter(char decimalSep)
	{
		DecimalFormatSymbols symb = new DecimalFormatSymbols();
		symb.setDecimalSeparator(decimalSep);
		DecimalFormat numberFormatter = new DecimalFormat("0.#s", symb);
		numberFormatter.setMaximumFractionDigits(2);
		return numberFormatter;
	}

	/**
	 * Create a timing formatter using the decimal separator defined
	 * through the settings property <tt>workbench.gui.timining.decimal</tt>
	 *
	 * @return a properly initialized DecimalFormat
	 */
	public static DecimalFormat createTimingFormatter()
	{
		String sep = Settings.getInstance().getProperty("workbench.gui.timining.decimal", ".");
		return createTimingFormatter(sep.charAt(0));
	}

	public String getDurationAsSeconds(long millis)
	{
		double time = ((double)millis) / 1000.0;
		synchronized (numberFormatter)
		{
			return numberFormatter.format(time);
		}
	}

	public String formatDuration(long millis, boolean includeFractionalSeconds)
	{
		long hours = (millis / ONE_HOUR);
		millis -= (hours * ONE_HOUR);
		long minutes = millis / ONE_MINUTE;
		millis -= (minutes * ONE_MINUTE);

		StringBuilder result = new StringBuilder(17);

		if (hours > 0)
		{
			result.append(hours);
			result.append("h ");
		}

		if (minutes == 0 && hours > 0 || minutes > 0)
		{
			result.append(minutes);
			result.append("m ");
		}

		if (includeFractionalSeconds)
		{
			synchronized (numberFormatter)
			{
				result.append(numberFormatter.format(millis / 1000.0));
			}
		}
		else
		{
			result.append(Long.toString(millis / 1000));
			result.append('s');
		}

		return result.toString();
	}
}
