/*
 * DurationFormatter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
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
 * @author support@sql-workbench.net
 */
public class DurationFormatter
{
	private final DecimalFormat numberFormatter;
	private final long oneSecond = 1000;
	private final long oneMinute = oneSecond * 60;
	private final long oneHour = oneMinute * 60;

	public DurationFormatter()
	{
		numberFormatter = createTimingFormatter();
	}

	public static final DecimalFormat createTimingFormatter()
	{
		DecimalFormatSymbols symb = new DecimalFormatSymbols();
		String sep = Settings.getInstance().getProperty("workbench.gui.timining.decimal", ".");
		symb.setDecimalSeparator(sep.charAt(0));
		DecimalFormat numberFormatter = new DecimalFormat("0.#s", symb);
		numberFormatter.setMaximumFractionDigits(2);
		return numberFormatter;
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
		long hours = (millis / oneHour);
		millis = millis - (hours * oneHour);
		long minutes = millis / oneMinute;
		millis = millis - (minutes * oneMinute);

		StringBuilder result = new StringBuilder(17);

		if (hours > 0)
		{
			result.append(hours);
			result.append("h ");
		}

		if (minutes > 0)
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
