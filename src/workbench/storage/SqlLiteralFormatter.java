/*
 * SqlLiteralFormatter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage;

import java.io.BufferedInputStream;
import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import workbench.WbManager;

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

	private Map dateLiteralFormats;
	private DbDateFormatter defaultDateFormatter;
	
	public SqlLiteralFormatter(String product)
	{
		dateLiteralFormats = readStatementTemplates("DateLiteralFormats.xml");
		if (dateLiteralFormats == null) dateLiteralFormats = Collections.EMPTY_MAP;
		defaultDateFormatter = getDateLiteralFormatter(product);
	}

	private Map readStatementTemplates(String aFilename)
	{
		Map result = null;

		BufferedInputStream in = new BufferedInputStream(this.getClass().getResourceAsStream(aFilename));
		
		try
		{
			WbPersistence reader = new WbPersistence();
			Object value = reader.readObject(in);
			if (value != null && value instanceof Map)
			{
				result = (Map)value;
			}
		}
		catch (Exception e)
		{
			result = null;
			LogMgr.logError("SqlSyntaxFormatter.readStatementTemplates()", "Error reading template file " + aFilename,e);
		}
		
		Map customizedMap = null;
		
		// try to read additional definitions from local file
		try
		{
			File f = new File(WbManager.getInstance().getJarPath(), aFilename);
			if (f.exists())
			{
				WbPersistence reader = new WbPersistence(f.getAbsolutePath());
				customizedMap = (Map)reader.readObject();
			}
		}
		catch (Exception e)
		{
			customizedMap = null;
		}
		
		if (customizedMap != null)
		{
			HashMap m = (HashMap)customizedMap;
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

	public DbDateFormatter getDateLiteralFormatter(String aProductname)
	{
		DbDateFormatter format = (DbDateFormatter)dateLiteralFormats.get(aProductname == null ? GENERAL_SQL : aProductname);
		if (format == null)
		{
			format = (DbDateFormatter)dateLiteralFormats.get(GENERAL_SQL);
			
			// Just in case someone messed around with the XML file
			if (format == null) format = DbDateFormatter.DEFAULT_FORMATTER;
		}
		return format;
	}

	public String getDefaultLiteral(ColumnData data)
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
			return this.defaultDateFormatter.getLiteral((Date)value);
		}
		else if (value instanceof File)
		{
			return "{$blobfile='" + value.toString() + "'}";
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
			// This assumes that the JDBC driver returned a class
			// that implements the approriate toString() method!
			return value.toString();
		}
	}

}
