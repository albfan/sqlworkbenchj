/*
 * ValueConverter.java
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

import java.io.File;
import java.math.BigDecimal;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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
	/**
	 *	Often used date formats which are tried when parsing a Date
	 *  or a TimeStamp column
	 */
	private final String[] dateFormats = new String[] {
														"yyyy-MM-dd",
														"dd.MM.yyyy",
														"dd.MM.yy",
														"MM/dd/yy",
														"MM/dd/yyyy",
														"dd-MMM-yyyy",
														"yyyy-MM-dd HH:mm:ss.SS",
														"yyyy-MM-dd HH:mm:ss",
														"dd.MM.yyyy HH:mm:ss",
														"MM/dd/yy HH:mm:ss",
														"MM/dd/yyyy HH:mm:ss"
													};

	private final String[] timestampFormats = new String[] {
														"yyyy-MM-dd HH:mm:ss.SS",
														"yyyy-MM-dd HH:mm:ss",
														"dd.MM.yyyy HH:mm:ss.SS",
														"dd.MM.yyyy HH:mm:ss",
														"dd.MM.yy HH:mm:ss.SS",
														"dd.MM.yy HH:mm:ss",
														"MM/dd/yyyy HH:mm:ss.SS",
														"MM/dd/yyyy HH:mm:ss",
														"MM/dd/yy HH:mm:ss.SS",
														"MM/dd/yy HH:mm:ss",
														"yyyy-MM-dd",
														"dd.MM.yyyy",
														"dd.MM.yy",
														"MM/dd/yy",
														"MM/dd/yyyy",
													};

	private final String[] timeFormats = new String[] { "HH:mm:ss.SS", "HH:mm:ss", "HH:mm" };

	private String defaultDateFormat;
	private String defaultTimestampFormat;
	private char decimalCharacter = '.';

	private final SimpleDateFormat dateFormatter = new SimpleDateFormat();
	private final SimpleDateFormat timestampFormatter = new SimpleDateFormat();
	private final SimpleDateFormat formatter = new SimpleDateFormat();
	private boolean autoConvertBooleanNumbers = true;
	private Collection<String> booleanTrueValues = null;
	private Collection<String> booleanFalseValues = null;

	private Integer integerTrue = Integer.valueOf(1);
	private Integer integerFalse = Integer.valueOf(0);
	private Long longTrue = Long.valueOf(1);
	private Long longFalse = Long.valueOf(0);
	private BigDecimal bigDecimalTrue = BigDecimal.valueOf(1);
	private BigDecimal bigDecimalFalse = BigDecimal.valueOf(0);

	private static final String FORMAT_MILLIS = "millis";
	private boolean checkBuiltInFormats = true;
	private boolean illegalDateIsNull;

	public ValueConverter()
	{
		Settings sett = Settings.getInstance();
		this.setDefaultDateFormat(sett.getDefaultDateFormat());
		this.setDefaultTimestampFormat(sett.getDefaultTimestampFormat());
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

	public void setDefaultDateFormat(String aFormat)
		throws IllegalArgumentException
	{
		if (!StringUtil.isEmptyString(aFormat))
		{
			if (aFormat.equalsIgnoreCase(FORMAT_MILLIS))
			{
				this.defaultTimestampFormat = FORMAT_MILLIS;
			}
			else
			{
				this.defaultDateFormat = aFormat;
				this.dateFormatter.applyPattern(aFormat);
			}
		}
	}

	public void setDefaultTimestampFormat(String aFormat)
		throws IllegalArgumentException
	{
		if (!StringUtil.isEmptyString(aFormat))
		{
			if (aFormat.equalsIgnoreCase(FORMAT_MILLIS))
			{
				this.defaultTimestampFormat = FORMAT_MILLIS;
				//this.dateFormatter = null;
			}
			else
			{
				this.defaultTimestampFormat = aFormat;
				this.timestampFormatter.applyPattern(aFormat);
			}
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
		if (trueValues == null || falseValues == null || trueValues.isEmpty() || falseValues.isEmpty())
		{
			LogMgr.logWarning("ValueConverter.setBooleanLiterals()", "Ignoring call as at least one collection is empty or null");
			this.booleanFalseValues = null;
			this.booleanTrueValues = null;
		}
		else
		{
			this.booleanFalseValues = new HashSet<String>(falseValues);
			this.booleanTrueValues = new HashSet<String>(trueValues);
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

	private Number getLong(String value)
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

	private Number getInt(String value, int type)
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

	private Number getBigDecimal(String value, int type)
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

		for (int i = 0; i < timeFormats.length; i++)
		{
			try
			{
				this.formatter.applyPattern(timeFormats[i]);
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
			int usedPattern = -1;

			for (int i = 0; i < dateFormats.length; i++)
			{
				try
				{
					this.formatter.applyPattern(timestampFormats[i]);
					result = this.formatter.parse(timestampInput);
					usedPattern = i;
					break;
				}
				catch (ParseException e)
				{
					result = null;
				}
			}

			if (usedPattern > -1)
			{
				LogMgr.logInfo("ValueConverter.parseTimestamp()", "Succeeded parsing '" + timestampInput + "' using the format: " + timestampFormats[usedPattern]);

				// use this pattern from now on to avoid multiple attempts for the next values
				defaultTimestampFormat = timestampFormats[usedPattern];
				timestampFormatter.applyPattern(this.defaultTimestampFormat);
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
				// Do not throw the exception yet as we will try the defaultTimestampFormat as well.
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
			for (int i = 0; i < dateFormats.length; i++)
			{
				try
				{
					this.formatter.applyPattern(dateFormats[i]);
					result = this.formatter.parse(dateInput);
					LogMgr.logInfo("ValueConverter.parseDate()", "Succeeded parsing [" + dateInput + "] using the format: " + dateFormats[i]);

					// use this format from now on...
					defaultDateFormat = dateFormats[i];
					dateFormatter.applyPattern(defaultDateFormat);
					break;
				}
				catch (Exception e)
				{
					result = null;
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

		if (decimalCharacter == '.')
		{
			// no need to search and replace the decimal character
			return input;
		}

		int len = input.length();
		if (len == 0)
		{
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

	private Boolean getBoolean(String value, int type)
		throws ConverterException
	{
		if (this.booleanFalseValues != null && this.booleanTrueValues != null)
		{
			if (booleanFalseValues.contains(value))
			{
				return Boolean.FALSE;
			}
			if (booleanTrueValues.contains(value))
			{
				return Boolean.TRUE;
			}
			throw new ConverterException("Input value [" + value + "] not in the list of defined true or false literals", type, null);
		}
		else
		{
			if ("false".equalsIgnoreCase(value))
			{
				return Boolean.FALSE;
			}
			if ("true".equalsIgnoreCase(value))
			{
				return Boolean.TRUE;
			}
		}
		return null;
	}
}
