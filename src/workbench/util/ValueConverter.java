/*
 * ValueConverter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Types;
import java.text.SimpleDateFormat;
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
	private final SimpleDateFormat timeFormatter = new SimpleDateFormat();
	
	private String defaultDateFormat;
	private String defaultTimestampFormat;
	private char decimalCharacter = '.';
	private SimpleDateFormat dateFormatter;
	private SimpleDateFormat timestampFormatter;
	private SimpleDateFormat formatter = new SimpleDateFormat();
	
	public ValueConverter()
	{
		Settings sett = Settings.getInstance();
		this.setDefaultDateFormat(sett.getDefaultDateFormat());
		this.setDefaultTimestampFormat(sett.getDefaultTimestampFormat());
	}

	public ValueConverter(String aDateFormat, String aTimeStampFormat)
	{
		this();
		this.setDefaultDateFormat(aDateFormat);
		this.setDefaultTimestampFormat(aTimeStampFormat);
	}

	public void setDefaultDateFormat(String aFormat)
	{
		if (!StringUtil.isEmptyString(aFormat))
		{
			this.defaultDateFormat = aFormat;
			this.dateFormatter = new SimpleDateFormat(aFormat);
		}
	}

	public void setDefaultTimestampFormat(String aFormat)
	{
		if (!StringUtil.isEmptyString(aFormat))
		{
			this.defaultTimestampFormat = aFormat;
			this.timestampFormatter = new SimpleDateFormat(aFormat);
		}
	}

	public void setDecimalCharacter(char aChar)
	{
		this.decimalCharacter = aChar;
	}

	private static final Integer INT_TRUE = new Integer(1);
	private static final Integer INT_FALSE = new Integer(0);
	
	private static final Integer LONG_TRUE = new Integer(1);
	private static final Integer LONG_FALSE = new Integer(0);
	
	/**
	 * Convert the given input value to a class instance
	 * according to the given type (from java.sql.Types)
	 * If the value is a blob file parameter as defined by {@link workbench.util.LobFileParameter}
	 * then a File object is returned that points to the data file (as passed in the
	 * blob file parameter)
	 * @see workbench.storage.DataStore#convertCellValue(Object, int)
	 */
	public Object convertValue(Object aValue, int type)
		throws Exception
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
					if ("false".equalsIgnoreCase(v)) return LONG_FALSE;
					if ("true".equalsIgnoreCase(v)) return LONG_TRUE;
					throw e;
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
					if ("false".equalsIgnoreCase(v)) return INT_FALSE;
					if ("true".equalsIgnoreCase(v)) return INT_TRUE;
					throw e;
				}
				
			case Types.NUMERIC:
			case Types.DECIMAL:
			case Types.DOUBLE:
			case Types.REAL:
			case Types.FLOAT:
				if (v.length() == 0) return null;
				return new BigDecimal(this.adjustDecimalString(aValue.toString()));
			case Types.CHAR:
			case Types.VARCHAR:
			case Types.LONGVARCHAR:
					return aValue.toString();
			case Types.DATE:
				if (v.length() == 0) return null;
				return this.parseDate((String)aValue);
			case Types.TIMESTAMP:
				if (v.length() == 0) return null;
				return this.parseTimestamp((String)aValue);
			case Types.TIME:
				if (v.length() == 0) return null;
				return this.parseTime((String)aValue);
			case Types.BLOB:
			case Types.BINARY:
			case Types.LONGVARBINARY:
			case Types.VARBINARY:
				if (aValue instanceof String)
				{
					LobFileParameterParser p = new LobFileParameterParser(aValue.toString());
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
				return null;
				
			case Types.BIT:
			case Types.BOOLEAN:
				try
				{
					if (aValue instanceof String)
					{
						return Boolean.valueOf(StringUtil.stringToBool((String)aValue));
					}
					else if (aValue instanceof Number)
					{
						return Boolean.valueOf(((Number)aValue).intValue() == 1);
					}
					else
					{
						return aValue;
					}
				}
				catch (Exception e)
				{
					LogMgr.logError("ValueConverter.convertValue()", "Could not convert [" + aValue + "] to Boolean",e);
					return null;
				}
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
	{
		java.sql.Time result = null;
		java.util.Date parsed = null;
		synchronized (this.formatter)
		{
			for (int i=0; i < timeFormats.length; i++)
			{
				try
				{
					this.formatter.applyPattern(timeFormats[i]);
					parsed = this.formatter.parse(time);
					break;
				}
				catch (Exception e)
				{
					result = null;
				}
			}
		}
		
		if (parsed == null) 
		{
			LogMgr.logWarning("ValueConverter.parseTime()", "Could not parse time value '" + time + "'");
			return null;
		}
		
		result = new java.sql.Time(parsed.getTime());
		return result;
	}
	
  public java.sql.Timestamp parseTimestamp(String aDate)
  {
		java.util.Date result = null;
		
		if (this.defaultTimestampFormat != null)
		{
			try
			{
				synchronized (this.timestampFormatter)
				{
					result = this.timestampFormatter.parse(aDate);
				}
			}
			catch (Exception e)
			{
				LogMgr.logWarning("ValueConverter.parseTimestamp()", "Could not parse '" + aDate + "' using " + this.timestampFormatter.toPattern() + ". Trying to recognize the format", null);
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
					catch (Exception e)
					{
						result = null;
					}
				}
			}
			if (usedPattern > -1)
			{
				LogMgr.logWarning("ValueConverter.parseTimestamp()", "Succeeded parsing '" + aDate + "' using the format: " + timestampFormats[usedPattern]);
			}
		}
		
		if (result != null)
		{
			return new java.sql.Timestamp(result.getTime());
		}
		return null;
		
	}
	
  public java.sql.Date parseDate(String aDate)
  {
		java.util.Date result = null;
		
		if (this.defaultDateFormat != null)
		{
			try
			{
				synchronized (this.dateFormatter)
				{
					result = this.dateFormatter.parse(aDate);
				}
			}
			catch (Exception e)
			{
				LogMgr.logWarning("ValueConverter.parseDate()", "Could not parse '" + aDate + "' using " + this.dateFormatter.toPattern() + ". Trying to recognize the format...", null);
				result = null;
			}
		}

		if (result == null && this.defaultTimestampFormat != null)
		{
			try
			{
				result = this.timestampFormatter.parse(aDate);
			}
			catch (Exception e)
			{
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
						this.formatter.applyPattern(dateFormats[i]);
						result = this.formatter.parse(aDate);
						usedPattern = i;
						break;
					}
					catch (Exception e)
					{
						result = null;
					}
				}
			}
			if (usedPattern > -1)
			{
				LogMgr.logWarning("ValueConverter.parseDate()", "Succeeded parsing '" + aDate + "' using the format: " + dateFormats[usedPattern]);
			}
		}

		if (result != null)
		{
			return new java.sql.Date(result.getTime());
		}
		return null;
  }

	private String adjustDecimalString(String input)
	{
		if (input == null)  return input;
		String value = input.trim();
		int len = value.length();
		if (len == 0) return value;
		StringBuffer result = new StringBuffer(len);
		int pos = value.lastIndexOf(this.decimalCharacter);
		for (int i=0; i < len; i++)
		{
			char c = value.charAt(i);
			if (i == pos)
			{
				// replace the decimal char with a . as that is required by BigDecimal(String)
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

}
