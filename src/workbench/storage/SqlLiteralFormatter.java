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
public class SqlLiteralFormatter
{
	public static final String GENERAL_SQL = "All";

	private HashMap dateLiteralFormats;
	private DbDateFormatter defaultDateFormatter;

	public SqlLiteralFormatter()
	{
		dateLiteralFormats = readStatementTemplates("DateLiteralFormats.xml");
	}

	private HashMap readStatementTemplates(String aFilename)
	{
		HashMap result = null;

		BufferedInputStream in = new BufferedInputStream(this.getClass().getResourceAsStream(aFilename));
		Object value;
		
		try
		{
			// filename is for logging purposes only
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

	public DbDateFormatter getDateLiteralFormatter()
	{
		if (defaultDateFormatter == null)
		{
			defaultDateFormatter = getDateLiteralFormatter(GENERAL_SQL);
		}
		return defaultDateFormatter;
	}

	public DbDateFormatter getDateLiteralFormatter(String aProductname)
	{
		Object value = dateLiteralFormats.get(aProductname);
		if (value == null)
		{
			value = dateLiteralFormats.get(GENERAL_SQL);
		}
		DbDateFormatter format = (DbDateFormatter)value;
		return format;
	}

	public String getDefaultLiteral(ColumnData data, DbDateFormatter formatter)
	{
		Object value = data.getValue();
		if (value == null) return "NULL";
		int type = data.getIdentifier().getDataType();
		
		if (value instanceof String || value instanceof OracleLongType)
		{
			String t = (String)value;
			StringBuffer realValue = new StringBuffer(t.length() + 10);
			// Single quotes in a String must be "quoted"...
			realValue.append('\'');
			// replace to Buffer writes the result of into the passed buffer
			// so this appends the correct literal to realValue
			StringUtil.replaceToBuffer(realValue, t, "'", "''");
			realValue.append("'");
			return realValue.toString();
		}
		else if (value instanceof Date)
		{
			if (formatter == null) formatter = DbDateFormatter.DEFAULT_FORMATTER;
			return formatter.getLiteral((Date)value);
		}
		else if (value instanceof NullValue)
		{
			return "NULL";
		}
		else if (type == java.sql.Types.BIT && "bit".equalsIgnoreCase(data.getIdentifier().getDbmsType()))
		{
			// this is for MS SQL Server
			// we cannot convert all values denoted as Types.BIT to 0/1 as
			// e.g. Postgres only accepts the literals true/false for boolean columns
			// which are reported as Types.BIT as well.
			// that's why I compare to the DBMS data type bit (hoping that 
			// other DBMS's that are also using 'bit' work the same way
			boolean flag = ((java.lang.Boolean)value).booleanValue();
			return (flag ? "1" : "0");
		}
		else
		{
			return value.toString();
		}
	}

}
