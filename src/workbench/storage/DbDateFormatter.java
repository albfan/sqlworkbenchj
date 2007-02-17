/*
 * DbDateFormatter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage;

import java.text.SimpleDateFormat;
import java.util.Date;

import workbench.util.StringUtil;

/**
 * A serializable Bean to create literals for Date and Timestamp values that 
 * can be used in a SQL statement.
 * 
 * @author  support@sql-workbench.net
 */
public class DbDateFormatter
{
	/**
	 * A default formatter that creates quoted literals in ISO format.
	 */
	public static final DbDateFormatter DEFAULT_FORMATTER = new DbDateFormatter("''yyyy-MM-dd HH:mm:ss''");
	
	/**
	 * The placeholder for the function call template that is replaced
	 * with the formatted value.
	 */
	public static final String DATE_PLACEHOLDER = "%formatted_date_literal%";

	private SimpleDateFormat formatter;
	private String format;

	private String functionCall;

	/**
	 * Create a default formatter with no format defined. 
	 * This default constructor is only needed to be able to serialize
	 * the Bean into an XML file and should not be used.
	 */
	public DbDateFormatter()
	{
	}

	/**
	 * Create a new formatter with the given format string.
	 * The format string is used to initialize a SimpleDateFormat
	 * 
	 * @param aFormat the format for SimpleDateFormat
	 * @see #setFormat(String)
	 */
	private DbDateFormatter(String aFormat)
	{
		this.setFormat(aFormat);
		this.setFunctionCall(null);
	}

	/**
	 * Return the date format used by this Formatter
	 * @return the format string
	 */
	public String getFormat()
	{
		return this.format;
	}
	
	/**
	 * Define a new format for this formatter.
	 * @param aFormat the format for SimpleDateFormat
	 */ 
	public void setFormat(String aFormat)
	{
		this.format = aFormat;
		this.formatter = new SimpleDateFormat(aFormat);
	}

	/**
	 * Return the function call (template) to be used
	 * by this formatter.
	 * @return the function call template currently used, may be null
	 */
	public String getFunctionCall()
	{
		return this.functionCall;
	}

	/**
	 * Define this formatter to use a function call template.
	 * {@link #getLiteral(java.util.Date)} will replace
	 * the placeholder in this string with the formatted 
	 * date value
	 * @param aCall the function call template. It should contain the placeholder.
	 * @see #getLiteral(java.util.Date)
	 */
	public void setFunctionCall(String aCall)
	{
		this.functionCall = aCall;
	}

	/**
	 * Return a literal useable in SQL statements.
	 * 
	 * This method basically formats the given according to the defined format.
	 * If a function call is defined, the placeholder is replaced in that 
	 * tmeplate and the modified template is returned. Otherwise the 
	 * formatted value is returned.
	 * 
	 * @param aDate the date value to be formatted. 
	 * @return the literal to be used in a SQL statement or null if the input value was null
	 * 
	 * @see #setFunctionCall(String)
	 * @see #setFormat(String)
	 */
	public String getLiteral(Date aDate)
	{
		if (aDate == null) return null;
		
		StringBuilder dateStr = new StringBuilder(format.length());
		try
		{
			synchronized (this.formatter)
			{
        dateStr.append(this.formatter.format(aDate));
			}
      
			if (this.functionCall != null)
			{
				return StringUtil.replace(this.functionCall, DATE_PLACEHOLDER, dateStr.toString());
			}
		}
		catch (Throwable th)
		{
      return aDate.toString();
		}
		return dateStr.toString();
	}

}
