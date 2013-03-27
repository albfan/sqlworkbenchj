/*
 * OracleUtils.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
package workbench.db.oracle;

import java.util.Properties;
import java.util.Set;
import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;
import workbench.resource.Settings;
import workbench.util.CollectionUtil;

/**
 * Utility methods for Oracle
 *
 * @author Thomas Kellerer
 */
public class OracleUtils
{
	public static final int BYTE_SEMANTICS = 1;
	public static final int CHAR_SEMANTICS = 2;

	public static final Set<String> STANDARD_TYPES = CollectionUtil.caseInsensitiveSet
		("INTERVALDS", "INTERVALYM", "TIMESTAMP WITH LOCAL TIME ZONE", "TIMESTAMP WITH TIME ZONE",
		 "NUMBER", "NUMBER", "NUMBER", "LONG RAW", "RAW", "LONG", "CHAR", "NUMBER", "NUMBER", "NUMBER",
		 "FLOAT", "REAL", "VARCHAR2", "DATE", "DATE", "TIMESTAMP", "STRUCT", "ARRAY", "BLOB", "CLOB", "ROWID",
		 "XMLType", "SDO_GEOMETRY", "SDO_TOPO_GEOMETRY", "SDO_GEORASTER", "ANYTYPE", "ANYDATA");

	private OracleUtils()
	{
	}

	protected final boolean fixNVARCHARSemantics()
	{
		return Settings.getInstance().getBoolProperty("workbench.db.oracle.fixnvarchartype", true);
	}

	static boolean getRemarksReporting(WbConnection conn)
	{
		// The old "remarksReporting" property should not be taken from the
		// System properties as a fall-back
		String value = getDriverProperty(conn, "remarksReporting", false);
		if (value == null)
		{
			// Only the new oracle.jdbc.remarksReporting should also be
			// checked in the system properties
			value = getDriverProperty(conn, "oracle.jdbc.remarksReporting", true);
		}
		return "true".equalsIgnoreCase(value == null ? "false" : value.trim());
	}

	static boolean getMapDateToTimestamp(WbConnection conn)
	{
		if (Settings.getInstance().fixOracleDateType()) return true;
		// if the mapping hasn't been enabled globally, then check the driver property

		// Newer Oracle drivers support a connection property to automatically
		// return DATE columns as Types.TIMESTAMP. We have to mimic that
		// when using our own statement to retrieve column definitions
		String value = getDriverProperty(conn, "oracle.jdbc.mapDateToTimestamp", true);
		return "true".equalsIgnoreCase(value);
	}

	static String getDriverProperty(WbConnection con, String property, boolean includeSystemProperty)
	{
		if (con == null) return "false";
		String value = null;
		ConnectionProfile profile = con.getProfile();
		if (profile != null)
		{
			Properties props = profile.getConnectionProperties();
			value = (props != null ? props.getProperty(property, null) : null);
			if (value == null && includeSystemProperty)
			{
				value = System.getProperty(property, null);
			}
		}
		return value;
	}

	/**
	 * Checks if the property "remarksReporting" is enabled for the given connection.
	 *
	 * @param con the connection to test
	 * @return true if the driver returns comments for tables and columns
	 */
	public static boolean remarksEnabled(WbConnection con)
	{
		if (con == null) return false;
		ConnectionProfile prof = con.getProfile();
		Properties props = prof.getConnectionProperties();
		String value = "false";
		if (props != null)
		{
			value = props.getProperty("remarksReporting", "false");
		}
		return "true".equals(value);
	}
}
