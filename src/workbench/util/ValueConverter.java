/*
 * ValueConverter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
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
 * to a type from java.sql.Types.
 *
 * @author  support@sql-workbench.net
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
	private SimpleDateFormat dateFormatter;
	private SimpleDateFormat timestampFormatter;
	private SimpleDateFormat formatter = new SimpleDateFormat();
	private boolean autoConvertBooleanNumbers = true;
	private Collection<String> booleanTrueValues = null;
	private Collection<String> booleanFalseValues = null;

	private static final String FORMAT_MILLIS = "millis";
	
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

	public void setDefaultDateFormat(String aFormat)
		throws IllegalArgumentException
	{
		if (!StringUtil.isEmptyString(aFormat))
		{
      if (aFormat.equalsIgnoreCase(FORMAT_MILLIS))
			{
        this.defaultTimestampFormat = FORMAT_MILLIS;
				this.dateFormatter = null;
			}
			else
			{
				this.defaultDateFormat = aFormat;
				this.dateFormatter = new SimpleDateFormat(aFormat);
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
				this.dateFormatter = null;
      }
      else
      {
        this.defaultTimestampFormat = aFormat;
        this.timestampFormatter = new SimpleDateFormat(aFormat);
      }
		}
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
		if (trueValues == null || falseValues == null || trueValues.size() == 0 || falseValues.size() == 0)
		{
			this.booleanFalseValues = null;
			this.booleanTrueValues = null;
		}
		else
		{
			this.booleanFalseValues = new HashSet<String>(falseValues);
			this.booleanTrueValues = new HashSet<String>(trueValues);
		}
	}
	
	private final Integer INT_TRUE = Integer.valueOf(1);
	private final Integer INT_FALSE = Integer.valueOf(0);
	
	private final Long LONG_TRUE = Long.valueOf(1);
	private final Long LONG_FALSE = Long.valueOf(0);

	private final BigDecimal BIG_TRUE = BigDecimal.valueOf(1);
	private final BigDecimal BIG_FALSE = BigDecimal.valueOf(0);
	
	/**
	 * Convert the given input value to a class instance
	 * according to the given type (from java.sql.Types)
	 * If the value is a blob file parameter as defined by {@link workbench.util.LobFileParameter}
	 * then a File object is returned that points to the data file (as passed in the
	 * blob file parameter)
	 * @see workbench.storage.DataStore#convertCellValue(Object, int)
	 */
	public Object convertValue(Object aValue, int type)
		throws ConverterException
	{
		if (aValue == null) return null;
		String v = aValue.toString().trim();

		switch (type)
		{
			case Types.BIGINT:
				if (v.length() == 0) return null;
				try
				{
					return new Long(v);
				}
				catch (NumberFormatException e)
				{
					// When exporting from a database that supports the boolean datatype
					// into a database that maps this to an integer, we assume that
					// true/false should be 1/0
					if (autoConvertBooleanNumbers)
					{
						Boolean b = getBoolean(v); 
						if (b != null) 
						{
							if (b.booleanValue()) return LONG_TRUE;
							else return LONG_FALSE;
						}
					}
					throw new ConverterException(aValue, type, e);
				}
				
			case Types.INTEGER:
			case Types.SMALLINT:
			case Types.TINYINT:
				if (v.length() == 0) return null;
				try
				{
					return new Integer(v);
				}
				catch (NumberFormatException e)
				{
					// When exporting from a database that supports the boolean datatype
					// into a database that maps this to an integer, we assume that
					// true/false should be 1/0
					if (autoConvertBooleanNumbers)
					{
						Boolean b = getBoolean(v); 
						if (b != null) 
						{
							if (b.booleanValue()) return INT_TRUE;
							else return INT_FALSE;
						}
					}
					throw new ConverterException(aValue, type, e);
				}
				
			case Types.NUMERIC:
			case Types.DECIMAL:
			case Types.DOUBLE:
			case Types.REAL:
			case Types.FLOAT:
				if (v.length() == 0) return null;
				try
				{
					return new BigDecimal(this.adjustDecimalString(aValue.toString()));
				}
				catch (NumberFormatException e)
				{
					// When exporting from a database that supports the boolean datatype
					// into a database that maps this to an integer, we assume that
					// true/false should be 1/0
					if (autoConvertBooleanNumbers)
					{
						Boolean b = getBoolean(v); 
						if (b != null) 
						{
							if (b.booleanValue()) return BIG_TRUE;
							else return BIG_FALSE;
						}
					}
					throw new ConverterException(aValue, type, e);
				}
				
			case Types.CHAR:
			case Types.VARCHAR:
			case Types.LONGVARCHAR:
					return aValue.toString();
					
			case Types.DATE:
				if (v.length() == 0) return null;
				try
				{
					return this.parseDate((String)aValue);
				}
				catch (Exception e)
				{
					throw new ConverterException(aValue, type, e);
				}
				
			case Types.TIMESTAMP:
				if (v.length() == 0) return null;
				try
				{
					return this.parseTimestamp((String)aValue);
				}
				catch (Exception e)
				{
					throw new ConverterException(aValue, type, e);
				}
				
			case Types.TIME:
				if (v.length() == 0) return null;
				try
				{
					return this.parseTime((String)aValue);
				}
				catch (Exception e)
				{
					throw new ConverterException(aValue, type, e);
				}
				
			case Types.BLOB:
			case Types.BINARY:
			case Types.LONGVARBINARY:
			case Types.VARBINARY:
				if (aValue instanceof String)
				{
					LobFileParameterParser p = null;
					try
					{
						p = new LobFileParameterParser(aValue.toString());
					}
					catch (Exception e)
					{
						throw new ConverterException(aValue, type, e);
					}
					LobFileParameter[] parms = p.getParameters();
					if (parms == null) return null;
					String fname = parms[0].getFilename();
					if (fname == null) return null;
					return new File(fname);
				}
				else if (aValue instanceof File)
				{
					return aValue;
				}
				else if (aValue instanceof byte[])
				{
					return aValue;
				}
				return null;
				
			case Types.BIT:
			case Types.BOOLEAN:
				return convertBool(v, type);

			default:
				return aValue;
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
		
		synchronized (this.formatter)
		{
			for (int i=0; i < timeFormats.length; i++)
			{
				try
				{
					this.formatter.applyPattern(timeFormats[i]);
					parsed = this.formatter.parse(time);
					LogMgr.logInfo("ValueConverter.parseTime()", "Succeeded parsing the time string [" + time + "] using the format: " + formatter.toPattern());
					break;
				}
				catch (Exception e)
				{
					parsed = null;
				}
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
	
  public java.sql.Timestamp parseTimestamp(String aDate)
		throws ParseException, NumberFormatException
  {
		if (isCurrentTimestamp(aDate))
		{
			java.sql.Timestamp ts = new java.sql.Timestamp(System.currentTimeMillis());
			return ts;
		}
		
		if (isCurrentDate(aDate))
		{
			return new java.sql.Timestamp(getToday().getTime());
		}
		
		java.util.Date result = null;
		
		if (this.defaultTimestampFormat != null)
		{
			try
			{
				if (FORMAT_MILLIS.equalsIgnoreCase(defaultTimestampFormat))
				{
					long value = Long.parseLong(aDate);
          result = new java.util.Date(value);
				}
				else
				{
					synchronized (this.timestampFormatter)
					{
						result = this.timestampFormatter.parse(aDate);
					}
				}
			}
			catch (Exception e)
			{
				LogMgr.logWarning("ValueConverter.parseTimestamp()", "Could not parse '" + aDate + "' using default format " + this.timestampFormatter.toPattern() + ". Trying to recognize the format...", null);
				result = null;
			}
		}
		
		if (result == null)
		{
			int usedPattern = -1;
			synchronized (this.formatter)
			{
				for (int i=0; i < dateFormats.length; i++)
				{
					try
					{
						this.formatter.applyPattern(timestampFormats[i]);
						result = this.formatter.parse(aDate);
						usedPattern = i;
						break;
					}
					catch (ParseException e)
					{
						result = null;
					}
				}
			}
			if (usedPattern > -1)
			{
				LogMgr.logInfo("ValueConverter.parseTimestamp()", "Succeeded parsing '" + aDate + "' using the format: " + timestampFormats[usedPattern]);
			}
		}
		
		if (result != null)
		{
			return new java.sql.Timestamp(result.getTime());
		}
		throw new ParseException("Could not convert [" + aDate + "] to a timestamp value!",0);
	}

  public java.sql.Date parseDate(String aDate)
		throws ParseException
  {
		if (isCurrentDate(aDate))
		{
			return getToday();
		}
		
		if (isCurrentTimestamp(aDate))
		{
			return new java.sql.Date(System.currentTimeMillis());
		}
		
		java.util.Date result = null;
		
		if (this.defaultDateFormat != null)
		{
			try
			{
				if (FORMAT_MILLIS.equalsIgnoreCase(defaultTimestampFormat))
				{
					long value = Long.parseLong(aDate);
          result = new java.util.Date(value);
				}
				else
				{
					synchronized (this.dateFormatter)
					{
						result = this.dateFormatter.parse(aDate);
					}
				}
			}
			catch (Exception e)
			{
				LogMgr.logWarning("ValueConverter.parseDate()", "Could not parse [" + aDate + "] using: " + this.dateFormatter.toPattern(), null);
				// Do not throw the exception yet as we will try the defaultTimestampFormat as well.
				result = null;
			}
		}

		if (result == null && this.defaultTimestampFormat != null)
		{
			try
			{
				result = this.timestampFormatter.parse(aDate);
			}
			catch (ParseException e)
			{
				LogMgr.logWarning("ValueConverter.parseDate()", "Could not parse [" + aDate + "] using: " + this.timestampFormatter.toPattern() + ". Trying to recognize the format...", null);
			}
		}

		if (result == null)
		{
			synchronized (this.formatter)
			{
				for (int i=0; i < dateFormats.length; i++)
				{
					try
					{
						this.formatter.applyPattern(dateFormats[i]);
						result = this.formatter.parse(aDate);
						LogMgr.logInfo("ValueConverter.parseDate()", "Succeeded parsing [" + aDate + "] using the format: " + dateFormats[i]);
						break;
					}
					catch (Exception e)
					{
						result = null;
					}
				}
			}
		}

		if (result != null)
		{
			return new java.sql.Date(result.getTime());
		}
		
		throw new ParseException("Could not convert [" + aDate + "] to a date", 0);
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
		if (StringUtil.isEmptyString(arg)) return false;
		
		List<String> keywords = Settings.getInstance().getListProperty("workbench.db.keyword." + type, true);
		return keywords.contains(arg.toLowerCase());
	}
	
//	private Collection<String> getCurrentKeywords()
//	{
//		Set<String> allKeywords = new HashSet<String>(7);
//		allKeywords.addAll(Settings.getInstance().getListProperty("workbench.db.keyword.current_time", true));
//		allKeywords.addAll(Settings.getInstance().getListProperty("workbench.db.keyword.current_date", true));
//		allKeywords.addAll(Settings.getInstance().getListProperty("workbench.db.keyword.current_timestamp", true));
//		return allKeywords;
//	}
		
	private String adjustDecimalString(String input)
	{
		if (input == null)  return input;
		String value = input.trim();
		int len = value.length();
		if (len == 0) return value;
		StringBuilder result = new StringBuilder(len);
		int pos = value.lastIndexOf(this.decimalCharacter);
		for (int i=0; i < len; i++)
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
	
	private Boolean getBoolean(Object value)
		throws ConverterException
	{
		if (this.booleanFalseValues != null && this.booleanTrueValues != null)
		{
			if (booleanFalseValues.contains(value)) return Boolean.FALSE;
			if (booleanTrueValues.contains(value)) return Boolean.TRUE;
			throw new ConverterException("Input value [" + value + "] not in the list of defined true or false literals");
		}
		else if (value instanceof String)
		{
			if ("false".equalsIgnoreCase((String)value)) return Boolean.FALSE;
			if ("true".equalsIgnoreCase((String)value)) return Boolean.TRUE;
		}
		return null;
	}
	
	private Boolean convertBool(Object value, int type)
		throws ConverterException
	{
		Boolean b = getBoolean(value);
		if (b != null) return b;

		throw new ConverterException(value, type, null);
	}
}
