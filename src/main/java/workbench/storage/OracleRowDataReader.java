/*
 * OracleRowDataReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
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

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Set;

import workbench.log.LogMgr;

import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;


/**
 * A class to properly read the value of a TIMESTAMP WITH TIME ZONE column.
 *
 * This code should actually be inside Oracle's JDBC driver's getTimestamp() method to properly adjust
 * the timestamp value.
 *
 * @author Thomas Kellerer
 */
public class OracleRowDataReader
	extends RowDataReader
{
	private Method stringValue;

	private Connection sqlConnection;
	private SimpleDateFormat tsParser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	private final Set<String> tsClasses = new HashSet<>(3);

	public OracleRowDataReader(ResultInfo info, WbConnection conn)
		throws ClassNotFoundException
	{
		super(info, conn);
		sqlConnection = conn.getSqlConnection();

		// TODO: do I also need to convert TIMESTAMP WITH LOCAL TIME ZONE (oracle.sql.TIMESTAMPTLZ)?
		tsClasses.add("oracle.sql.TIMESTAMPTZ");

		// we cannot have any "hardcoded" references to the Oracle classes
		// because that will throw a ClassNotFoundException as those classes were loaded through a different class loader.
		// Therefor I need to use reflection to access the stringValue() method
		try
		{
			Class oraDatum = ConnectionMgr.getInstance().loadClassFromDriverLib(conn.getProfile(), "oracle.sql.Datum");
			stringValue= oraDatum.getMethod("stringValue", java.sql.Connection.class);
		}
		catch (Throwable t)
		{
			LogMgr.logError("OracleRowDataReader.initialize()", "Could not access oracle.sql.Datum class", t);
			throw new ClassNotFoundException("TIMESTAMPTZ");
		}
	}

	@Override
	protected Object readTimestampValue(ResultSet rs, int column)
		throws SQLException
	{
		Object value = rs.getObject(column);

		if (value == null) return null;
		if (rs.wasNull()) return null;

		if (tsClasses.contains(value.getClass().getName()))
		{
			return adjustTIMESTAMP(value);
		}

		if (value instanceof java.sql.Timestamp)
		{
			return value;
		}
		return rs.getTimestamp(column);
	}

	private Object adjustTIMESTAMP(Object tz)
	{
		try
		{
			String tsValue = (String) stringValue.invoke(tz, sqlConnection);

			// SimpleDateFormat doesn't support more than 3 digits for milliseconds
			// Oracle returns 6 digits, e.g: 2015-01-26 11:42:46.894119 Europe/Berlin
			// apparently SimpleDateFormat does some strange rounding there and will return
			// the above timestamp as 11:57:40.119
			// so we need to strip the additional milliseconds
			// and the timezone name as Java can't handle that either.

			String cleanValue = cleanupTSValue(tsValue);

			// this loses the time zone information stored in Oracle's TIMESTAMPTZ or TIMESTAMPLTZ values
			// but otherwise the displayed time would be totally wrong.
			java.util.Date date = tsParser.parse(cleanValue);
			Timestamp ts = new java.sql.Timestamp(date.getTime());

      // TODO: extract the stripped nanoseconds from the passed object
      // and set them in the Timestamp instance using setNanos();
			return ts;
		}
		catch (Throwable ex)
		{
			LogMgr.logDebug("OracleRowDataReader.adjustTIMESTAMP()", "Could not read timestamp", ex);
		}
		return tz;
	}

	public static String cleanupTSValue(String tsValue)
	{
		int len = tsValue.length();
		if (len < 19)
		{
			return tsValue;
		}
		int msPos = tsValue.indexOf('.');
		int end = -1;
		if (msPos == 19)
		{
			end = tsValue.indexOf(' ', msPos);
		}
		else
		{
			// no milliseconds, find the timezone name
			end = tsValue.indexOf(' ', 12);
		}

		end = Math.min(len, 23);

		if (end < len)
		{
			tsValue = tsValue.substring(0, end);
		}
		return tsValue;
	}
}
