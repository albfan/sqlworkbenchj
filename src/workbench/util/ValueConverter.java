/*
 * ValueConverter.java
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

import java.io.File;
import java.math.BigDecimal;
import java.sql.Types;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import workbench.log.LogMgr;
import workbench.resource.Settings;

/**
 * Utility class to parse Strings into approriate Java classes according
 * to a type from java.sql.Type
 *
 * This class is not thread safe for parsing dates and timestamps due to the
 * fact that SimpleDateFormat is not thread safe.
 *
 * @author  Thomas Kellerer
 */
public class ValueConverter
{

	public static final String DETECT_FIRST = "detect_once";
	public static final String ALWAYS_CHECK_INTERNAL = "detect";

	/**
	 *	Often used date formats which are tried when parsing a Date
	 *  or a TimeStamp column
	 */
	private final List<String> timestampFormats = CollectionUtil.arrayList(
														"yyyy-MM-dd HH:mm:ss.SSS",
														"yyyy-MM-dd HH:mm:ss",
														"yyyy-MM-dd HH:mm",
														"dd.MM.yyyy HH:mm:ss.SSS",
														"dd.MM.yyyy HH:mm:ss",
														"dd.MM.yy HH:mm:ss.SSS",
														"dd.MM.yy HH:mm:ss",
														"dd.MM.yy HH:mm",
														"MM/dd/yyyy HH:mm:ss.SSS",
														"MM/dd/yyyy HH:mm:ss",
														"MM/dd/yy HH:mm:ss.SSS",
														"MM/dd/yy HH:mm:ss",
														"MM/dd/yy HH:mm",
														"yyyy-MM-dd",
														"dd.MM.yyyy",
														"dd.MM.yy",
														"MM/dd/yy",
														"MM/dd/yyyy");

	private final List<String> dateFormats = CollectionUtil.arrayList(
														"yyyy-MM-dd",
														"dd.MM.yyyy",
														"dd.MM.yy",
														"MM/dd/yy",
														"MM/dd/yyyy",
														"dd-MMM-yyyy",
														"yyyy-MM-dd HH:mm:ss.SSS",
														"yyyy-MM-dd HH:mm:ss",
														"dd.MM.yyyy HH:mm:ss",
														"MM/dd/yy HH:mm:ss",
														"MM/dd/yyyy HH:mm:ss");

	private final String[] timeFormats = new String[] { "HH:mm:ss.SS", "HH:mm:ss", "HH:mm", "HHmm", "HH" };

	private String defaultDateFormat;
	private String defaultTimestampFormat;
	private char decimalCharacter = '.';

	private final WbDateFormatter dateFormatter = new WbDateFormatter();
	private final WbDateFormatter timestampFormatter = new WbDateFormatter();
	private final WbDateFormatter formatter = new WbDateFormatter();
	private boolean autoConvertBooleanNumbers = true;
	private final Map<String, Boolean> booleanValues = new HashMap<>();
	private boolean booleanUserMap;

	private Integer integerTrue = Integer.valueOf(1);
	private Integer integerFalse = Integer.valueOf(0);
	private Long longTrue = Long.valueOf(1);
	private Long longFalse = Long.valueOf(0);
	private BigDecimal bigDecimalTrue = BigDecimal.valueOf(1);
	private BigDecimal bigDecimalFalse = BigDecimal.valueOf(0);

	private static final String FORMAT_MILLIS = "millis";
	private boolean checkBuiltInFormats = true;
	private boolean useFirstMatching = true;
	private boolean illegalDateIsNull;
	private boolean cleanupNumbers = false;

	public ValueConverter()
	{
		Settings sett = Settings.getInstance();
		this.setDefaultDateFormat(sett.getDefaultDateFormat());
		this.setDefaultTimestampFormat(sett.getDefaultTimestampFormat());
		cleanupNumbers = Settings.getInstance().getBoolProperty("workbench.converter.cleanupdecimals", false);
		readConfiguredBooleanValues();
	}

	private void readConfiguredBooleanValues()
	{
		List<String> trueValues = Settings.getInstance().getListProperty("workbench.converter.boolean.true", true, "1,t,true");
		List<String> falseValues = Settings.getInstance().getListProperty("workbench.converter.boolean.false", true, "0,f,false");
		fillBooleanMap(trueValues, falseValues);
		booleanUserMap = false;
	}

	private void fillBooleanMap(Collection<String> trueValues, Collection<String> falseValues)
	{
		booleanValues.clear();
		for (String value : trueValues)
		{
			booleanValues.put(value, Boolean.TRUE);
		}
		for (String value : falseValues)
		{
			booleanValues.put(value, Boolean.FALSE);
		}
	}

	public ValueConverter(String aDateFormat, String aTimeStampFormat)
	{
		if (StringUtil.isEmptyString(aDateFormat))
		{
			this.setDefaultDateFormat(Settings.getInstance().getDefaultDateFormat());
		}
		else
		{
			this.setDefaultDateFormat(aDateFormat);
		}

		if (StringUtil.isEmptyString(aTimeStampFormat))
		{
			this.setDefaultTimestampFormat(Settings.getInstance().getDefaultTimestampFormat());
		}
		else
		{
			this.setDefaultTimestampFormat(aTimeStampFormat);
		}
	}

	public void setIllegalDateIsNull(boolean flag)
	{
		this.illegalDateIsNull = flag;
	}

	public void setCheckBuiltInFormats(boolean flag)
	{
		this.checkBuiltInFormats = flag;
	}

	public final void setDefaultDateFormat(String dtFormat)
		throws IllegalArgumentException
	{
		if (DETECT_FIRST.equalsIgnoreCase(dtFormat))
		{
			this.defaultDateFormat = null;
			this.checkBuiltInFormats = true;
			this.useFirstMatching = true;
		}
		else if (ALWAYS_CHECK_INTERNAL.equalsIgnoreCase(dtFormat))
		{
			this.defaultDateFormat = null;
			this.checkBuiltInFormats = true;
			this.useFirstMatching = false;
		}
		else if (dtFormat.equalsIgnoreCase(FORMAT_MILLIS))
		{
			this.defaultDateFormat = FORMAT_MILLIS;
			this.checkBuiltInFormats = false;
		}
		else if (StringUtil.isNonEmpty(dtFormat))
		{
			this.checkBuiltInFormats = false;
			this.defaultDateFormat = dtFormat;
			this.dateFormatter.applyPattern(dtFormat);
		}
	}

	public final void setDefaultTimestampFormat(String tsFormat)
		throws IllegalArgumentException
	{
		if (DETECT_FIRST.equalsIgnoreCase(tsFormat))
		{
			this.defaultTimestampFormat = null;
			this.checkBuiltInFormats = true;
			this.useFirstMatching = true;
		}
		else if (ALWAYS_CHECK_INTERNAL.equalsIgnoreCase(tsFormat))
		{
			this.defaultTimestampFormat = null;
			this.checkBuiltInFormats = true;
			this.useFirstMatching = false;
		}
		else if (tsFormat.equalsIgnoreCase(FORMAT_MILLIS))
		{
			this.defaultTimestampFormat = FORMAT_MILLIS;
			this.checkBuiltInFormats = false;
		}
		else if (StringUtil.isNonEmpty(tsFormat))
		{
			this.checkBuiltInFormats = false;
			this.defaultTimestampFormat = tsFormat;
			this.timestampFormatter.applyPattern(tsFormat);
		}

	}

	public void setNumericBooleanValues(int falseValue, int trueValue)
	{
		integerFalse = Integer.valueOf(falseValue);
		integerTrue = Integer.valueOf(trueValue);

		longFalse = Long.valueOf(falseValue);
		longTrue = Long.valueOf(trueValue);

		bigDecimalFalse = BigDecimal.valueOf(falseValue);
		bigDecimalTrue = BigDecimal.valueOf(trueValue);
	}

	public char getDecimalCharacter()
	{
		return this.decimalCharacter;
	}

	public void setDecimalCharacter(char aChar)
	{
		this.decimalCharacter = aChar;
	}

	public void setAutoConvertBooleanNumbers(boolean flag)
	{
		this.autoConvertBooleanNumbers = flag;
	}

	/**
	 * Define a list of literals that should be treated as true or
	 * false when converting input values.
	 * If either collection is null, both are considered null
	 * If these values are not defined, the default boolean conversion implemented
	 * in {@link workbench.util.StringUtil#stringToBool(String)} is used (this is the
	 * default)
	 * @param trueValues String literals to be considered as <tt>true</tt>
	 * @param falseValues String literals to be considered as <tt>false</tt>
	 */
	public void setBooleanLiterals(Collection<String> trueValues, Collection<String> falseValues)
	{
		if (CollectionUtil.isEmpty(trueValues) || CollectionUtil.isEmpty(falseValues))
		{
			LogMgr.logWarning("ValueConverter.setBooleanLiterals()", "Ignoring attempt to set boolean literals because at least one collection is empty or null");
			readConfiguredBooleanValues();
		}
		else
		{
			fillBooleanMap(trueValues, falseValues);
			booleanUserMap = true;
		}
	}

	private Number getNumberFromString(String value, boolean useInt)
	{
		if (value == null) return null;

		try
		{
			BigDecimal d = new BigDecimal(this.adjustDecimalString(value));
			if (useInt)
			{
				return Integer.valueOf(d.intValueExact());
			}
			else
			{
				return Long.valueOf(d.longValueExact());
			}
		}
		catch (Exception e)
		{
		// Ignore
		}
		return null;
	}

	public Number getLong(String value)
		throws ConverterException
	{
		if (value.length() == 0) return null;

		try
		{
			return new Long(value);
		}
		catch (NumberFormatException e)
		{
			// Maybe the long value is disguised as a decimal
			Number n = getNumberFromString(value, false);
			if (n != null)
			{
				return n;
			}

			// When exporting from a database that supports the boolean datatype
			// into a database that maps this to an integer, we assume that
			// true/false should be 1/0
			if (autoConvertBooleanNumbers)
			{
				Boolean b = getBoolean(value, Types.BOOLEAN);
				if (b != null)
				{
					if (b.booleanValue())
					{
						return longTrue;
					}
					else
					{
						return longFalse;
					}
				}
			}
			throw new ConverterException(value, Types.BIGINT, e);
		}
	}

	public Number getInt(String value, int type)
		throws ConverterException
	{
		if (value.length() == 0) return null;

		try
		{
			return Integer.valueOf(value);
		}
		catch (NumberFormatException e)
		{
			// Maybe the integer value is disguised as a decimal
			Number n = getNumberFromString(value, true);
			if (n != null) return n;

			// When exporting from a database that supports the boolean datatype
			// into a database that maps this to an integer, we assume that
			// true/false should be 1/0
			if (autoConvertBooleanNumbers)
			{
				Boolean b = getBoolean(value, Types.BOOLEAN);
				if (b != null)
				{
					if (b.booleanValue())
					{
						return integerTrue;
					}
					else
					{
						return integerFalse;
					}
				}
			}
			throw new ConverterException(value, type, e);
		}
	}

	public BigDecimal getBigDecimal(String value, int type)
		throws ConverterException
	{
		if (value.length() == 0) return null;

		try
		{
			return new BigDecimal(this.adjustDecimalString(value));
		}
		catch (NumberFormatException e)
		{
			// When exporting from a database that supports the boolean datatype
			// into a database that maps this to an integer, we assume that
			// true/false should be 1/0
			if (autoConvertBooleanNumbers)
			{
				Boolean b = getBoolean(value, Types.BOOLEAN);
				if (b != null)
				{
					if (b.booleanValue())
					{
						return bigDecimalTrue;
					}
					else
					{
						return bigDecimalFalse;
					}
				}
			}
			throw new ConverterException(value, type, e);
		}
	}

	private String makeString(Object value)
	{
		return value.toString().trim();
	}

	/**
	 * Convert the given input value to a class instance
	 * according to the given type (from java.sql.Types)
	 * If the value is a blob file parameter as defined by {@link workbench.util.LobFileParameter}
	 * then a File object is returned that points to the data file (as passed in the
	 * blob file parameter)
	 * @see workbench.storage.DataStore#convertCellValue(Object, int)
	 */
	public Object convertValue(Object value, int type)
		throws ConverterException
	{
		if (value == null)
		{
			return null;
		}

		switch (type)
		{
			case Types.BIGINT:
				return getLong(makeString(value));

			case Types.INTEGER:
			case Types.SMALLINT:
			case Types.TINYINT:
				return getInt(makeString(value), type);

			case Types.NUMERIC:
			case Types.DECIMAL:
			case Types.DOUBLE:
			case Types.REAL:
			case Types.FLOAT:
				return getBigDecimal(makeString(value), type);

			case Types.CHAR:
			case Types.VARCHAR:
			case Types.LONGVARCHAR:
				return value.toString();

			case Types.DATE:
				if (value instanceof java.util.Date)
				{
					return value;
				}
				if (StringUtil.isBlank(makeString(value))) return null;

				try
				{
					return this.parseDate(makeString(value));
				}
				catch (Exception e)
				{
					throw new ConverterException(value, type, e);
				}

			case Types.TIMESTAMP:
				if (value instanceof java.util.Date)
				{
					return value;
				}
				String ts = makeString(value);
				if (StringUtil.isBlank(ts)) return null;
				try
				{
					return this.parseTimestamp(ts);
				}
				catch (Exception e)
				{
					throw new ConverterException(value, type, e);
				}

			case Types.TIME:
				if (value instanceof java.util.Date)
				{
					return value;
				}
				String t = makeString(value);
				if (StringUtil.isBlank(t)) return null;

				try
				{
					return this.parseTime(t);
				}
				catch (Exception e)
				{
					throw new ConverterException(value, type, e);
				}

			case Types.BLOB:
			case Types.BINARY:
			case Types.LONGVARBINARY:
			case Types.VARBINARY:
				if (value instanceof String)
				{
					LobFileParameterParser p = null;
					try
					{
						p = new LobFileParameterParser(value.toString());
					}
					catch (Exception e)
					{
						throw new ConverterException(value, type, e);
					}
					LobFileParameter[] parms = p.getParameters();
					if (parms == null) return null;
					String fname = parms[0].getFilename();
					if (fname == null) return null;
					return new File(fname);
				}
				else if (value instanceof File)
				{
					return value;
				}
				else if (value instanceof byte[])
				{
					return value;
				}
				return null;

			case Types.BIT:
			case Types.BOOLEAN:
				String b = makeString(value);
				return getBoolean(b, type);

			default:
				return value;
		}
	}

	public String getDatePattern()
	{
		return this.defaultDateFormat;
	}

	public String getTimestampPattern()
	{
		return this.defaultTimestampFormat;
	}

	public java.sql.Time parseTime(String time)
		throws ParseException
	{
		if (isCurrentTime(time))
		{
			Calendar c = Calendar.getInstance();
			c.clear(Calendar.YEAR);
			c.clear(Calendar.DAY_OF_MONTH);
			c.clear(Calendar.MONTH);
			java.util.Date now = c.getTime();
			return new java.sql.Time(now.getTime());
		}

		java.util.Date parsed = null;
		for (String timeFormat : timeFormats)
		{
			try
			{
				this.formatter.applyPattern(timeFormat);
				parsed = this.formatter.parse(time);
				LogMgr.logDebug("ValueConverter.parseTime()", "Succeeded parsing the time string [" + time + "] using the format: " + formatter.toPattern());
				break;
			}
			catch (Exception e)
			{
				parsed = null;
			}
		}

		if (parsed != null)
		{
			return new java.sql.Time(parsed.getTime());
		}
		throw new ParseException("Could not parse [" + time + "] as a time value!", 0);
	}

	private java.sql.Date getToday()
	{
		Calendar c = Calendar.getInstance();
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.HOUR, 0);
		c.clear(Calendar.MINUTE);
		c.clear(Calendar.SECOND);
		c.clear(Calendar.MILLISECOND);
		java.util.Date now = c.getTime();
		return new java.sql.Date(now.getTime());
	}

	public java.sql.Timestamp parseTimestamp(String timestampInput)
		throws ParseException, NumberFormatException
	{
		if (isCurrentTimestamp(timestampInput))
		{
			java.sql.Timestamp ts = new java.sql.Timestamp(System.currentTimeMillis());
			return ts;
		}

		if (isCurrentDate(timestampInput))
		{
			return new java.sql.Timestamp(getToday().getTime());
		}

		// when lenient is set to true, the parser will simply return null
		// but will not throw an error for properly formatted values
		timestampFormatter.setLenient(!illegalDateIsNull);

		java.util.Date result = null;

		if (this.defaultTimestampFormat != null)
		{
			try
			{
				if (FORMAT_MILLIS.equalsIgnoreCase(defaultTimestampFormat))
				{
					long value = Long.parseLong(timestampInput);
					result = new java.util.Date(value);
				}
				else
				{
					result = this.timestampFormatter.parse(timestampInput);
				}
			}
			catch (Exception e)
			{
				LogMgr.logWarning("ValueConverter.parseTimestamp()", "Could not parse '" + timestampInput + "' using default format " + this.timestampFormatter.toPattern() + ". Trying to recognize the format...", null);
				result = null;
			}
		}


		if (result == null && illegalDateIsNull)
		{
			LogMgr.logInfo("ValueConverter.parseTimestamp()", "Illegal timestamp value '" + timestampInput + "' set to null");
			return null;
		}

		if (result == null && checkBuiltInFormats)
		{
			for (String format : timestampFormats)
			{
				try
				{
					this.formatter.applyPattern(format);
					result = this.formatter.parse(timestampInput);
					LogMgr.logDebug("ValueConverter.parseTimestamp()", "Succeeded parsing '" + timestampInput + "' using the format: " + format);
					if (useFirstMatching)
					{
						this.defaultTimestampFormat = format;
					}
					break;
				}
				catch (ParseException e)
				{
					result = null;
				}
			}
		}

		if (result != null)
		{
			return new java.sql.Timestamp(result.getTime());
		}

		throw new ParseException("Could not convert [" + timestampInput + "] to a timestamp value!", 0);
	}

	public java.sql.Date parseDate(String dateInput)
		throws ParseException
	{
		if (isCurrentDate(dateInput))
		{
			return getToday();
		}

		if (isCurrentTimestamp(dateInput))
		{
			return new java.sql.Date(System.currentTimeMillis());
		}

		java.util.Date result = null;

		dateFormatter.setLenient(!illegalDateIsNull);

		if (this.defaultDateFormat != null)
		{
			try
			{
				if (FORMAT_MILLIS.equalsIgnoreCase(defaultTimestampFormat))
				{
					long value = Long.parseLong(dateInput);
					result = new java.util.Date(value);
				}
				else
				{
					result = this.dateFormatter.parse(dateInput);
				}
			}
			catch (Exception e)
			{
				LogMgr.logWarning("ValueConverter.parseDate()", "Could not parse [" + dateInput + "] using: " + this.dateFormatter.toPattern(), null);
				result = null;
			}
		}

		// Apparently not a date, try the timestamp parser
		if (result == null && this.defaultTimestampFormat != null)
		{
			try
			{
				result = this.timestampFormatter.parse(dateInput);
			}
			catch (ParseException e)
			{
				LogMgr.logWarning("ValueConverter.parseDate()", "Could not parse [" + dateInput + "] using: " + this.timestampFormatter.toPattern() + ". Trying to recognize the format...", null);
			}
		}

		if (result == null && illegalDateIsNull)
		{
			LogMgr.logInfo("ValueConverter.parseDate()", "Illegal date value '" + dateInput + "' set to null");
			return null;
		}

		// Still no luck, try to detect the format by trying the built-in formats
		if (result == null && checkBuiltInFormats)
		{
			for (String format : dateFormats)
			{
				this.formatter.applyPattern(format);
				result = this.formatter.parseQuietly(dateInput);
				if (result != null)
				{
					if (useFirstMatching)
					{
						this.defaultDateFormat = format;
					}
					LogMgr.logDebug("ValueConverter.parseDate()", "Succeeded parsing [" + dateInput + "] using the format: " + format);
					break;
				}
			}

			// no luck with dates, try timestamps
			if (result == null)
			{
				for (String format : timestampFormats)
				{
					this.formatter.applyPattern(format);
					result = this.formatter.parseQuietly(dateInput);
					if (result != null)
					{
						if (useFirstMatching)
						{
							this.defaultDateFormat = format;
						}
						LogMgr.logDebug("ValueConverter.parseDate()", "Succeeded parsing [" + dateInput + "] using the format: " + format);
						break;
					}
				}
			}
		}

		if (result != null)
		{
			return new java.sql.Date(result.getTime());
		}

		throw new ParseException("Could not convert [" + dateInput + "] to a date", 0);
	}

	private boolean isCurrentTime(String arg)
	{
		return isKeyword("current_time", arg);
	}

	private boolean isCurrentDate(String arg)
	{
		return isKeyword("current_date", arg);
	}

	private boolean isCurrentTimestamp(String arg)
	{
		return isKeyword("current_timestamp", arg);
	}

	private boolean isKeyword(String type, String arg)
	{
		if (StringUtil.isEmptyString(arg))
		{
			return false;
		}

		List<String> keywords = Settings.getInstance().getListProperty("workbench.db.keyword." + type, true);
		return keywords.contains(arg.toLowerCase());
	}

	private String adjustDecimalString(String input)
	{
		if (input == null)
		{
			return input;
		}

		int len = input.length();
		if (len == 0)
		{
			return input;
		}

		if (cleanupNumbers)
		{
			return cleanupNumberString(input);
		}

		if (decimalCharacter == '.')
		{
			// no need to search and replace the decimal character
			return input;
		}

		int pos = input.lastIndexOf(this.decimalCharacter);
		if (pos < 0) return input;

		StringBuilder result = new StringBuilder(input);
		// replace the decimal char with a . as that is required by BigDecimal(String)
		// this way we only leave the last decimal character
		result.setCharAt(pos, '.');
		return result.toString();
	}

	private String cleanupNumberString(String value)
	{
		int len = value.length();
		StringBuilder result = new StringBuilder(len);
		int pos = value.lastIndexOf(this.decimalCharacter);
		for (int i = 0; i < len; i++)
		{
			char c = value.charAt(i);
			if (i == pos)
			{
				// replace the decimal char with a . as that is required by BigDecimal(String)
				// this way we only leave the last decimal character
				result.append('.');
			}
			// filter out everything but valid number characters
			else if ("+-0123456789eE".indexOf(c) > -1)
			{
				result.append(c);
			}
		}
		return result.toString();
	}

	private Boolean getBoolean(String value, int type)
		throws ConverterException
	{
		if (!booleanUserMap)
		{
			value = value.toLowerCase().trim();
		}
		Boolean bool = booleanValues.get(value);
		if (booleanUserMap && bool == null)
		{
			throw new ConverterException("Input value [" + value + "] not in the list of defined true or false literals", type, null);
		}
		return bool;
	}

}
