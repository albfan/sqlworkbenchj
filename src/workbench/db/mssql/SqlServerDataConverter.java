/*
 * SqlServerDataConverter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.mssql;

import java.sql.Types;
import workbench.log.LogMgr;
import workbench.storage.DataConverter;
import workbench.util.NumberStringCache;

/**
 * A class to convert Microsofts strange binary "timestamp" to a readable display.
 *
 * @author Thomas Kellerer
 */
public class SqlServerDataConverter
	implements DataConverter
{

	protected static class LazyInstanceHolder
	{
		protected static final SqlServerDataConverter instance = new SqlServerDataConverter();
	}

	public static SqlServerDataConverter getInstance()
	{
		return LazyInstanceHolder.instance;
	}

	private SqlServerDataConverter()
	{
	}

	@Override
	public Class getConvertedClass(int jdbcType, String dbmsType)
	{
		return String.class;
	}

	/**
	 * Checks if jdbcType == Types.BINARY and if dbmsType == "timestamp"
	 *
	 * @param jdbcType the jdbcType as returned by the driver
	 * @param dbmsType the name of the datatype for this value
	 *
	 * @return true if Microsoft's "timestamp" type
	 */
	public boolean convertsType(int jdbcType, String dbmsType)
	{
		return (jdbcType == Types.BINARY && dbmsType.equals("timestamp"));
	}

	/**
	 * If the type of the originalValue is Microsoft's "timestamp", then
	 * the value is converted into a corresponding hex display, e.g. <br/>
	 * <tt>0x000000000001dc91</tt>
	 *
	 * @param jdbcType the jdbcType as returned by the driver
	 * @param dbmsType the name of the datatype for this value
	 * @param originalValue the value to be converted (or not)
	 *
	 * @return the originalValue or a converted value if approriate
	 * @see #convertsType(int, java.lang.String)
	 */
	public Object convertValue(int jdbcType, String dbmsType, Object originalValue)
	{
		if (originalValue == null) return null;
		if (!convertsType(jdbcType, dbmsType)) return originalValue;
		Object newValue;
		try
		{
			byte[] b = (byte[])originalValue;
			StringBuilder buffer = new StringBuilder(b.length * 2 + 2);
			buffer.append("0x");
			for (byte v : b)
			{
				int c = (v < 0 ? 256 + v : v);
				buffer.append(NumberStringCache.getHexString(c));
			}
			newValue = buffer.toString();
		}
		catch (Throwable th)
		{
			LogMgr.logWarning("SqlServerDataConverter.convertValue()", "Error converting value " + originalValue, th);
			newValue = originalValue;
		}
		return newValue;
	}

}
