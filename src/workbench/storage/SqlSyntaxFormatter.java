/*
 * SqlSyntaxFormatter.java
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

import java.io.BufferedInputStream;
import java.util.Date;
import java.util.HashMap;

import workbench.log.LogMgr;
import workbench.util.StringUtil;
import workbench.util.WbPersistence;

/**
 *
 * @author  support@sql-workbench.net
 */
public class SqlSyntaxFormatter
{

	public static final String GENERAL_SQL = "All";

	private static HashMap dateLiteralFormatter;
	private static DbDateFormatter defaultDateFormatter;

	private SqlSyntaxFormatter()
	{
	}

	static
	{
		dateLiteralFormatter = readStatementTemplates("DateLiteralFormats.xml");
	}

	static HashMap readStatementTemplates(String aFilename)
	{
		HashMap result = null;

		BufferedInputStream in = new BufferedInputStream(SqlSyntaxFormatter.class.getResourceAsStream(aFilename));
		Object value;
		// filename is for logging purposes only
		try
		{
			WbPersistence reader = new WbPersistence(aFilename);
			value = reader.readObject(in);
		}
		catch (Exception e)
		{
			value = null;
			LogMgr.logError("SqlSyntaxFormatter.readStatementTemplates()", "Error reading template file " + aFilename,e);
		}
		if (value != null && value instanceof HashMap)
		{
			result = (HashMap)value;
		}

		// try to read additional definitions from local file
		try
		{
			WbPersistence reader = new WbPersistence(aFilename);
			value = reader.readObject();
		}
		catch (Exception e)
		{
			value = null;
		}
		if (value != null && value instanceof HashMap)
		{
			HashMap m = (HashMap)value;
			if (result != null)
			{
				result.putAll(m);
			}
			else
			{
				result = m;
			}
		}
		return result;
	}

	public static DbDateFormatter getDateLiteralFormatter()
	{
		if (defaultDateFormatter == null)
		{
			defaultDateFormatter = getDateLiteralFormatter(GENERAL_SQL);
		}
		return defaultDateFormatter;
	}

	public static DbDateFormatter getDateLiteralFormatter(String aProductname)
	{
		Object value = dateLiteralFormatter.get(aProductname);
		if (value == null)
		{
			value = dateLiteralFormatter.get(GENERAL_SQL);
		}
		DbDateFormatter format = (DbDateFormatter)value;
		return format;
	}

	public static String getDefaultLiteral(Object aValue)
	{
		return getDefaultLiteral(aValue, getDateLiteralFormatter());
	}

	public static String getDefaultLiteral(Object aValue, DbDateFormatter formatter)
	{
		if (aValue == null) return "NULL";

		if (aValue instanceof String || aValue instanceof OracleLongType)
		{
			// Single quotes in a String must be "quoted"...
			String t = aValue.toString();
			StringBuffer realValue = new StringBuffer(t.length() + 10);
			realValue.append('\'');
			StringUtil.replaceToBuffer(realValue, t, "'", "''");
			realValue.append("'");
			return realValue.toString();
		}
		else if (aValue instanceof Date)
		{
			if (formatter == null) formatter = DbDateFormatter.DEFAULT_FORMATTER;
			return formatter.getLiteral((Date)aValue);
		}
		else if (aValue instanceof NullValue)
		{
			return "NULL";
		}
		else
		{
			return aValue.toString();
		}
	}

}
