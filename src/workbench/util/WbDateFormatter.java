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

import java.sql.Timestamp;
import java.text.AttributedCharacterIterator;
import java.text.CharacterIterator;
import java.text.DateFormat;
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

  private int millisStart = -1;
  private int millisLength = -1;

	public WbDateFormatter(String pattern, DateFormatSymbols formatSymbols)
	{
		super(pattern, formatSymbols);
    checkMicroSeconds();
	}

	public WbDateFormatter(String pattern, Locale locale)
	{
		super(pattern, locale);
    checkMicroSeconds();
	}

	public WbDateFormatter(String pattern)
	{
		super(pattern);
    checkMicroSeconds();
	}

	public WbDateFormatter()
	{
	}

  @Override
  public void applyLocalizedPattern(String pattern)
  {
    super.applyLocalizedPattern(pattern);
    checkMicroSeconds();
  }

  @Override
  public void applyPattern(String pattern)
  {
    super.applyPattern(pattern);
    checkMicroSeconds();
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

    if (date instanceof Timestamp && millisLength > 3)
    {
      // SimpleDateFormat does not take nanoseconds from java.sql.Timestamp into account
      return formatTimestamp((Timestamp)date, toAppendTo, pos);
    }

		return super.format(date, toAppendTo, pos);
	}

  private StringBuffer formatTimestamp(Timestamp date, StringBuffer toAppendTo, FieldPosition pos)
  {
    StringBuffer result = super.format(date, toAppendTo, pos);

    // Timestamp.toString() always returns the timestamp in ISO format with 6 fractional digits
    String value = date.toString();

    // extract the nanoseconds as formatted by java.sql.Timestamp
    // and replace the "formatted" milliseconds from the original result
    // with the correct value
    String fractionalSeconds = value.substring(value.lastIndexOf('.') + 1);

    int len = Math.min(millisLength, fractionalSeconds.length());
    String display = fractionalSeconds.substring(0, len);

    result.delete(millisStart, millisStart + millisLength);
    result.insert(millisStart, display);

    return result;
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


  /**
   * Find the start and the length of the milliseconds pattern.
   *
   */
  private void checkMicroSeconds()
  {
    millisStart = -1;
    millisLength = 0;

    // using formatToCharacterIterator() is the only safe way to get the positions
    // as that will take all valid formatting options into account including
    // string literals enclosed in single quotes and other things.
    AttributedCharacterIterator itr = formatToCharacterIterator(Timestamp.valueOf("2001-01-01 00:00:00.123456789"));
    int pos = 0;

    for (char c = itr.first(); c != CharacterIterator.DONE; c = itr.next())
    {
      Object attribute = itr.getAttribute(DateFormat.Field.MILLISECOND);
      if (attribute != null)
      {
        if (millisStart == -1)
        {
          millisStart = pos;
          millisLength = 1;
        }
        else if (millisStart > -1)
        {
          millisLength ++;
        }
      }
      pos ++;
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
