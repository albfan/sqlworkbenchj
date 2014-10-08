/*
 * SqlServerDataConverter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.mssql;

import java.sql.Types;

import workbench.log.LogMgr;
import workbench.resource.Settings;
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
	private boolean convertVarbinary;

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
		convertVarbinary = Settings.getInstance().getBoolProperty("workbench.db.microsoft_sql_server.converter.varbinary", false);
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
	@Override
	public boolean convertsType(int jdbcType, String dbmsType)
	{
		if (jdbcType == Types.BINARY)
		{
			return dbmsType.equals("timestamp");
		}
		if (jdbcType == Types.VARBINARY)
		{
			// don't convert really large blobs
			// only convert varbinary(x) - assuming that they are sufficiently small
			return convertVarbinary && !dbmsType.equalsIgnoreCase("varbinary(max)");
		}
		return false;
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
	@Override
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
