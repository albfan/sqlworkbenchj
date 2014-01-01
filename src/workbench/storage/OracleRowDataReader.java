/*
 * OracleRowDataReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.storage;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.TimeZone;

import workbench.log.LogMgr;

import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;
import workbench.db.oracle.OracleTimeZoneInfo;

import workbench.util.StringUtil;


/**
 * A class to properly read the value of a TIMESTAMP WITH TIME ZONE column.
 *
 * This code should actually be inside Oracle's JDBC trigger's getTimestamp() method to properly adjust
 * the timestamp value.
 *
 * @author Thomas Kellerer
 */
public class OracleRowDataReader
	extends RowDataReader
{
	private Method getTsValue;
	private Method getBytes;

	private Connection sqlConnection;

	public OracleRowDataReader(ResultInfo info, WbConnection conn)
		throws ClassNotFoundException
	{
		super(info, conn);
		sqlConnection = conn.getSqlConnection();

		// we cannot have any "hardcoded" references to the Oracle classes
		// because that will throw a ClassNotFoundException as those classes
		// were loaded through a different class loader.
		// Therefor I need to use reflection to access the methods
		try
		{
			Class tzClass = ConnectionMgr.getInstance().loadClassFromDriverLib(conn.getProfile(), "oracle.sql.TIMESTAMPTZ");
			getBytes = tzClass.getMethod("getBytes", (Class[]) null);
			getTsValue = tzClass.getMethod("timestampValue", java.sql.Connection.class);
		}
		catch (Throwable t)
		{
			LogMgr.logError("OracleRowDataReader.initialize()", "Could not access TIMESTAMPTZ class", t);
			throw new ClassNotFoundException("TIMESTAMPTZ");
		}
	}

	@Override
	protected Object readTimestampValue(ResultSet rs, int column)
		throws SQLException
	{
		String type = this.resultInfo.getDbmsTypeName(column-1);
		if (!type.equalsIgnoreCase("TIMESTAMP WITH TIME ZONE"))
		{
			return rs.getTimestamp(column);
		}

		Object value = rs.getObject(column);
		if (value == null) return null;
		if (rs.wasNull()) return null;

		if (value.getClass().getName().equals("oracle.sql.TIMESTAMPTZ"))
		{
			try
			{
				return adjustTimezone(value);
			}
			catch (Exception ex)
			{
				LogMgr.logDebug("OracleRowDataReader.readTimestampValue()", "Could not read timestamp", ex);
			}
		}
		return rs.getTimestamp(column);
	}

	private Object adjustTimezone(Object tz)
		throws SQLException, IllegalAccessException, InvocationTargetException
	{
		Timestamp oraTs = (Timestamp)getTsValue.invoke(tz, sqlConnection);
		long tzValue = oraTs.getTime();

		byte[] bytes = (byte[])getBytes.invoke(tz, (Object[]) null);
		TimeZone zone;
		if ((bytes[11] & -128) != 0)
		{
			int regionCode = (bytes[11] & 127) << 6;
			regionCode += ((bytes[12] & 252) >> 2);
			String regionName = OracleTimeZoneInfo.ORA_TIMEZONES.get(regionCode);
			if (regionName != null)
			{
				zone = TimeZone.getTimeZone(regionName);
			}
			else
			{
				LogMgr.logWarning("OracleRowDataReader.adjustTimezone", "No timezone for ID=" + regionCode + " found. Using default timezone!");
				zone = TimeZone.getDefault();
			}
		}
		else
		{
			int hourOffset = bytes[11] - 20;
			int minuteOffset = bytes[12] - 60;
			String region = StringUtil.formatInt(hourOffset, 2) + ":" + StringUtil.formatInt(minuteOffset, 2);
			zone = TimeZone.getTimeZone(region);
		}
		Calendar cal = Calendar.getInstance(zone);
		cal.setTimeInMillis(tzValue);

		// I have to use the deprecated consctructor, because Calendar.get() correctly deals with DST and timezone information
		// which is not done when using getTimeMillis()
		Timestamp ts = new java.sql.Timestamp(
			cal.get(Calendar.YEAR) - 1900, cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH),
			cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND), cal.get(Calendar.MILLISECOND) * 1000000
		);
		return ts;
	}

}
