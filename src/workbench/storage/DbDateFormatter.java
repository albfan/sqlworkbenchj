/*
 * DbDateFormatter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import workbench.util.StringUtil;
import workbench.util.WbPersistence;

/**
 *
 * @author  support@sql-workbench.net
 */
public class DbDateFormatter
{
	public static final String DATE_PLACEHOLDER = "%formatted_date_literal%";
	public static final DbDateFormatter DEFAULT_FORMATTER = new DbDateFormatter("yyyy-MM-dd HH:mm:ss");

	private SimpleDateFormat formatter;
	private String format;

	private String functionCall;

	/** Creates a new instance of DbDateFormatter */
	public DbDateFormatter()
	{
	}

	public DbDateFormatter(String aFormat)
	{
		this.setFormat(aFormat);
		this.setFunctionCall(null);
	}

	public String getFormat()
	{
		return this.format;
	}
	public void setFormat(String aFormat)
	{
		this.format = aFormat;
		this.formatter = new SimpleDateFormat(aFormat);
	}

	public String getFunctionCall()
	{
		return this.functionCall;
	}

	public void setFunctionCall(String aCall)
	{
		this.functionCall = aCall;
	}

	public String getLiteral(Date aDate)
	{
		String dateStr;
		try
		{
			dateStr = "'" + this.formatter.format(aDate) + "'";
			if (this.functionCall != null)
			{
				dateStr = StringUtil.replace(this.functionCall, DATE_PLACEHOLDER, dateStr);
			}
		}
		catch (Throwable th)
		{
			dateStr = "'" + aDate.toString() + "'";
		}
		return dateStr;
	}

}
