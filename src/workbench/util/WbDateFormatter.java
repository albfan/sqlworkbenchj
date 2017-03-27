/*
 * WbDateFormatter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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

import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import workbench.resource.Settings;

import workbench.db.exporter.InfinityLiterals;


/**
 *
 * @author Thomas Kellerer
 */
public class WbDateFormatter
{
  private String pattern;
  private DateTimeFormatter formatter;

  // copied from the PostgreSQL driver
  public static final long DATE_POSITIVE_INFINITY = 9223372036825200000l;
  public static final long DATE_NEGATIVE_INFINITY = -9223372036832400000l;

  private InfinityLiterals infinityLiterals = InfinityLiterals.PG_LITERALS;

  private boolean illegalDateAsNull;
  private boolean containsTimeFields;

  private final String timeFields = "ahKkHmsSAnNVzOXxZ";
  private Locale localeToUse;

  public WbDateFormatter(String pattern)
  {
    applyPattern(pattern, false);
  }

  public WbDateFormatter(String pattern, Locale locale)
  {
    localeToUse = locale;
    applyPattern(pattern, false);
  }

  public WbDateFormatter(String pattern, boolean allowVariableLengthFractions)
  {
    applyPattern(pattern, allowVariableLengthFractions);
  }

  public WbDateFormatter()
  {
    applyPattern(StringUtil.ISO_DATE_FORMAT);
  }

  public void setLocale(Locale locale)
  {
    localeToUse = locale;
  }

  public void setIllegalDateIsNull(boolean flag)
  {
    illegalDateAsNull = flag;
    if (illegalDateAsNull)
    {
      formatter = formatter.withResolverStyle(ResolverStyle.STRICT);
    }
    else
    {
      formatter = formatter.withResolverStyle(ResolverStyle.SMART);
    }
  }

  public void applyPattern(String pattern)
  {
    applyPattern(pattern, false);
  }

  public void applyPattern(String pattern, boolean allowVariableLengthFraction)
  {
    String patternToUse = pattern;

    int len = -1;
    if (allowVariableLengthFraction)
    {
      Pattern p = Pattern.compile("\\.S{1,6}");
      Matcher matcher = p.matcher(patternToUse);
      // Make any millisecond/microsecond definition optional
      // so that inputs with a variable length of milli/microseconds can be parsed
      if (matcher.find())
      {
        int start = matcher.start();
        int end = matcher.end();
        // remove the .SSSSS from the pattern so that we can re-add it with a variable length using appendFraction
        patternToUse = matcher.replaceAll("");
        len = end - start;
      }
    }
    DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder().appendPattern(patternToUse);
    if (len > -1)
    {
      builder.appendFraction(ChronoField.MICRO_OF_SECOND, 0, len - 1, true);
    }
    DateTimeFormatter dtf = null;
    if (localeToUse != null)
    {
      dtf = builder.toFormatter(localeToUse);
    }
    else
    {
      dtf = builder.toFormatter();
    }
    this.formatter = dtf.withResolverStyle(ResolverStyle.SMART);
    this.pattern = pattern;
    this.containsTimeFields = checkForTimeFields();
  }

  private boolean checkForTimeFields()
  {
    for (int i=0; i < timeFields.length(); i++)
    {
      if (pattern.indexOf(timeFields.charAt(i)) > -1) return true;
    }
    return false;
  }

  public void setInfinityLiterals(InfinityLiterals literals)
  {
    this.infinityLiterals = literals;
  }

  public String formatTime(java.sql.Time time)
  {
    if (time == null) return "";

    return formatter.format(time.toLocalTime());
  }

  public String formatUtilDate(java.util.Date date)
  {
    if (date == null) return "";

    String result = getInfinityValue(date.getTime());
    if (result != null)
    {
      return result;
    }

    LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    return formatter.format(ldt);
  }

  public String formatDate(java.sql.Date date)
  {
    if (date == null) return "";

    String result = getInfinityValue(date.getTime());
    if (result != null)
    {
      return result;
    }

    // If the pattern for a java.sql.Date contains time fields DateTimeFormatter will
    // throw an exception.
    // So we convert the java.sql.Date to a java.util.Date as a fallback.
    // It's unlikely the java.sql.Date instance will have a proper time value,
    // but at least the date part will be formatted the way the user expects it
    if (containsTimeFields)
    {
      java.util.Date ud = new java.util.Date(date.getTime());
      LocalDateTime ldt = LocalDateTime.ofInstant(ud.toInstant(), ZoneId.systemDefault());
      return formatter.format(ldt);
    }
    return formatter.format(date.toLocalDate());
  }

  private String getInfinityValue(long dt)
  {
    if (infinityLiterals != null)
    {
      if (dt == DATE_POSITIVE_INFINITY)
      {
        return infinityLiterals.getPositiveInfinity();
      }
      else if (dt == DATE_NEGATIVE_INFINITY)
      {
        return infinityLiterals.getNegativeInfinity();
      }
    }
    return null;
  }

  public String formatTimestamp(java.time.LocalDate ts)
  {
    if (ts == null) return "";

    return formatter.format(ts);
  }

  public String formatTimestamp(java.sql.Timestamp ts)
  {
    if (ts == null) return "";

    String result = getInfinityValue(ts.getTime());
    if (result != null)
    {
      return result;
    }
    return formatter.format(ts.toLocalDateTime());
  }

  public String formatTimestamp(java.time.LocalDateTime ts)
  {
    if (ts == null) return "";

    String result = getInfinityValue(ts.toEpochSecond(ZoneOffset.from(ts)));
    if (result != null)
    {
      return result;
    }
    return formatter.format(ts);
  }

  public java.sql.Time parseTimeQuitely(String source)
  {
    try
    {
      return parseTime(source);
    }
    catch (ParseException ex)
    {
      return null;
    }
  }

  public java.sql.Time parseTime(String source)
    throws ParseException
  {
    LocalTime lt = LocalTime.parse(source, formatter);
    return java.sql.Time.valueOf(lt);
  }

  public java.sql.Date parseDateQuietely(String source)
  {
    source = StringUtil.trimToNull(source);
    if (source == null) return null;
    
    try
    {
      return parseDate(source);
    }
    catch (DateTimeParseException ex)
    {
      return null;
    }
  }

  public java.sql.Date parseDate(String source)
    throws DateTimeParseException
  {
    source = StringUtil.trimToNull(source);
    if (source == null) return null;

    if (infinityLiterals != null)
    {
      if (source.equalsIgnoreCase(infinityLiterals.getPositiveInfinity()))
      {
        return new java.sql.Date(DATE_POSITIVE_INFINITY);
      }
      if (source.equalsIgnoreCase(infinityLiterals.getNegativeInfinity()))
      {
        return new java.sql.Date(DATE_NEGATIVE_INFINITY);
      }
    }

    try
    {
      LocalDate ld = LocalDate.parse(source, formatter);
      return java.sql.Date.valueOf(ld);
    }
    catch (DateTimeParseException ex)
    {
      if (illegalDateAsNull) return null;
      throw ex;
    }
  }

  public java.sql.Timestamp parseTimestampQuietly(String source)
  {
    try
    {
      return parseTimestamp(source);
    }
    catch (DateTimeParseException ex)
    {
      return null;
    }
  }

  public java.sql.Timestamp parseTimestamp(String source)
    throws DateTimeParseException
  {
    if (source == null) return null;

    if (!containsTimeFields)
    {
      // a format mask that does not include time values cannot be parsed using LocalDateTime
      // it must be done through LocalDate
      java.sql.Date dt = parseDate(source);
      return new java.sql.Timestamp(dt.getTime());
    }

    if (infinityLiterals != null)
    {
      if (source.trim().equalsIgnoreCase(infinityLiterals.getPositiveInfinity()))
      {
        return new java.sql.Timestamp(DATE_POSITIVE_INFINITY);
      }

      if (source.trim().equalsIgnoreCase(infinityLiterals.getNegativeInfinity()))
      {
        return new java.sql.Timestamp(DATE_NEGATIVE_INFINITY);
      }
    }

    try
    {
      LocalDateTime ldt = LocalDateTime.parse(source, formatter);
      return java.sql.Timestamp.valueOf(ldt);
    }
    catch (DateTimeParseException ex)
    {
      if (illegalDateAsNull) return null;
      throw ex;
    }
  }

  public String toPattern()
  {
    return pattern;
  }

  public static String getDisplayValue(Object value)
  {
    if (value == null) return "";

    if (value instanceof java.sql.Date)
    {
      String format = Settings.getInstance().getDefaultDateFormat();
      WbDateFormatter formatter = new WbDateFormatter(format);
      return formatter.formatDate((java.sql.Date) value);
    }

    if (value instanceof java.sql.Timestamp)
    {
      String format = Settings.getInstance().getDefaultTimestampFormat();
      WbDateFormatter formatter = new WbDateFormatter(format);
      return formatter.formatTimestamp((java.sql.Timestamp) value);
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
