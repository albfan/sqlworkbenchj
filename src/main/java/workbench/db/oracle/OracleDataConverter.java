/*
 * OracleDataConverter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.db.oracle;

import java.lang.reflect.Method;
import java.sql.Types;

import workbench.log.LogMgr;

import workbench.storage.DataConverter;

import workbench.util.NumberStringCache;

/**
 * A class to convert Oracle's RAW datatype to something readable.
 * <br/>
 * This is only used if enabled.
 *
 * @see workbench.resource.Settings#getConvertOracleTypes()
 *
 * @author Thomas Kellerer
 */
public class OracleDataConverter
	implements DataConverter
{
	private Method stringValueMethod;

	protected static class LazyInstanceHolder
	{
		protected static final OracleDataConverter instance = new OracleDataConverter();
	}

	public static OracleDataConverter getInstance()
	{
		return LazyInstanceHolder.instance;
	}

	private OracleDataConverter()
	{
	}

	@Override
	public Class getConvertedClass(int jdbcType, String dbmsType)
	{
		return String.class;
	}

	/**
	 * Two Oracle datatypes are supported
	 * <ul>
	 * <li>RAW (jdbcType == Types.VARBINARY && dbmsType == "RAW")</li>
	 * <li>ROWID (jdbcType = Types.ROWID)</li>
	 * </ul>
	 *
	 * @param jdbcType the jdbcType as returned by the driver
	 * @param dbmsType the name of the datatype for this value
	 */
	@Override
	public boolean convertsType(int jdbcType, String dbmsType)
	{
		return (jdbcType == Types.VARBINARY && dbmsType.startsWith("RAW") ||
			      jdbcType == Types.ROWID);
	}

	/**
	 * If the type of the originalValue is RAW, then
	 * the value is converted into a corresponding hex display, e.g. <br/>
	 * <tt>0x000000000001dc91</tt>
	 *
	 * If the type of the originalValue is ROWID, Oracles stringValue() method
	 * from the class oracle.sql.ROWID is used to convert the input value
	 *
	 * @param jdbcType the jdbcType as returned by the driver
	 * @param dbmsType the name of the datatype for this value
	 * @param inputValue the value to be converted (or not)
	 *
	 * @return the originalValue or a converted value if approriate
	 * @see #convertsType(int, java.lang.String)
	 */
	@Override
	public Object convertValue(int jdbcType, String dbmsType, Object inputValue)
	{
		if (inputValue == null) return null;
		if (!convertsType(jdbcType, dbmsType)) return inputValue;

		if (jdbcType == Types.ROWID)
		{
			return convertRowId(inputValue);
		}
		return convertRaw(inputValue);
	}

	private Object convertRaw(Object originalValue)
	{
		Object newValue;
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

	private Object convertRowId(Object value)
	{
		Method valueMethod = stringValueMethod(value);
		if (valueMethod == null) return value.toString();

		try
		{
			Object result = valueMethod.invoke(value);
			return result;
		}
		catch (Throwable th)
		{
			return value.toString();
		}
	}

	private synchronized Method stringValueMethod(Object value)
	{
		if (stringValueMethod == null)
		{
			try
			{
				stringValueMethod = value.getClass().getMethod("stringValue");
			}
			catch (Throwable th)
			{
				// ignore
			}
		}
		return stringValueMethod;
	}

}
