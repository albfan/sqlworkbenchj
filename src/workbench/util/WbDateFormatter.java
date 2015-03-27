/*
 * WbDateFormatter.java
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

import java.text.DateFormatSymbols;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import workbench.resource.Settings;

import workbench.db.exporter.InfinityLiterals;

/**
 *
 * @author Thomas Kellerer
 */
public class WbDateFormatter
	extends SimpleDateFormat
{
	// copied from the PostgreSQL driver
	public static final long DATE_POSITIVE_INFINITY = 9223372036825200000l;
	public static final long DATE_NEGATIVE_INFINITY = -9223372036832400000l;

	private InfinityLiterals infinityLiterals = InfinityLiterals.PG_LITERALS;

	public WbDateFormatter(String pattern, DateFormatSymbols formatSymbols)
	{
		super(pattern, formatSymbols);
	}

	public WbDateFormatter(String pattern, Locale locale)
	{
		super(pattern, locale);
	}

	public WbDateFormatter(String pattern)
	{
		super(pattern);
	}

	public WbDateFormatter()
	{
	}

	public void setInfinityLiterals(InfinityLiterals literals)
	{
		this.infinityLiterals = literals;
	}

	@Override
	public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition pos)
	{
		if (infinityLiterals != null)
		{
			long dt = (date == null ? 0 : date.getTime());
			if (dt == DATE_POSITIVE_INFINITY)
			{
				return toAppendTo.append(infinityLiterals.getPositiveInfinity());
			}
			else if (dt == DATE_NEGATIVE_INFINITY)
			{
				return toAppendTo.append(infinityLiterals.getNegativeInfinity());
			}
		}
    // TODO: properly handle nanoseconds from java.sql.Timestamp instanced
    // e.g. http://stackoverflow.com/a/10074408/330315  or http://stackoverflow.com/a/24453423/330315
		return super.format(date, toAppendTo, pos);
	}

	@Override
	public Date parse(String source)
		throws ParseException
	{
		if (infinityLiterals != null)
		{
			if (source.trim().equalsIgnoreCase(infinityLiterals.getPositiveInfinity()))
			{
				return new Date(DATE_POSITIVE_INFINITY);
			}
			if (source.trim().equalsIgnoreCase(infinityLiterals.getNegativeInfinity()))
			{
				return new Date(DATE_NEGATIVE_INFINITY);
			}
		}
		return super.parse(source);
	}

	public Date parseQuietly(String source)
	{
		try
		{
			return this.parse(source);
		}
		catch (ParseException ex)
		{
			return null;
		}
	}

	public static String getDisplayValue(Object value)
	{
		if (value == null) return "";

		if (value instanceof java.sql.Date)
		{
			String format = Settings.getInstance().getDefaultDateFormat();
			WbDateFormatter formatter = new WbDateFormatter(format);
			return formatter.format((java.sql.Date) value);
		}

		if (value instanceof java.sql.Timestamp)
		{
			String format = Settings.getInstance().getDefaultTimestampFormat();
			WbDateFormatter formatter = new WbDateFormatter(format);
			return formatter.format((java.sql.Timestamp) value);
		}

		if (value instanceof java.util.Date)
		{
			long time = ((java.util.Date)value).getTime();
			if (time == DATE_POSITIVE_INFINITY)
			{
				return InfinityLiterals.PG_POSITIVE_LITERAL;
			}
			if (time == WbDateFormatter.DATE_NEGATIVE_INFINITY)
			{
				return InfinityLiterals.PG_NEGATIVE_LITERAL;
			}
		}

		return value.toString();
	}
}
