/*
 * SqlSyntaxFormatter.java
 *
 * Created on February 6, 2003, 10:29 AM
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
 * @author  tkellerer
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
		try
		{
			// filename is for logging purposes only
			value = WbPersistence.readObject(in, aFilename);
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
			value = WbPersistence.readObject(aFilename);
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