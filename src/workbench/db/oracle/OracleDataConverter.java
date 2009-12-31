/*
 * OracleDataConverter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.oracle;

import java.sql.Types;
import workbench.log.LogMgr;
import workbench.storage.DataConverter;
import workbench.util.NumberStringCache;

/**
 * A class to convert Oracle's RAW datatype to something readable.
 *
 * @author Thomas Kellerer
 */
public class OracleDataConverter
	implements DataConverter
{

	protected static class LazyInstanceHolder
	{
		protected static final OracleDataConverter instance = new OracleDataConverter();
	}

	public static final OracleDataConverter getInstance()
	{
		return LazyInstanceHolder.instance;
	}

	private OracleDataConverter()
	{
	}

	/**
	 * Checks if jdbcType == Types.VARBINARY and if dbmsType == "RAW"
	 *
	 * @param jdbcType the jdbcType as returned by the driver
	 * @param dbmsType the name of the datatype for this value
	 *
	 */
	public boolean convertsType(int jdbcType, String dbmsType)
	{
		return (jdbcType == Types.VARBINARY && dbmsType.equals("RAW"));
	}

	/**
	 * If the type of the originalValue is RAW, then
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
		Object newValue = null;
		try
		{
			byte[] b = (byte[])originalValue;
			StringBuilder buffer = new StringBuilder(b.length * 2);
			for (byte v : b)
			{
				int c = (v < 0 ? 256 + v : v);
				buffer.append(NumberStringCache.getHexString(c));
			}
			newValue = buffer.toString();
		}
		catch (Throwable th)
		{
			LogMgr.logWarning("OracleDataConverter.convertValue()", "Error converting value " + originalValue, th);
			newValue = originalValue;
		}
		return newValue;
	}

}
