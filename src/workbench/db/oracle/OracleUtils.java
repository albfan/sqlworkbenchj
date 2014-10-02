/*
 * OracleUtils.java
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
package workbench.db.oracle;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * Utility methods for Oracle
 *
 * @author Thomas Kellerer
 */
public class OracleUtils
{
	public static final int BYTE_SEMANTICS = 1;
	public static final int CHAR_SEMANTICS = 2;
	public static final String PROP_KEY_TBLSPACE = "oracle_default_tablespace";
	private static final String CACHE_HINT = "/*+ RESULT_CACHE */";

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

	/**
	 * Checks if the given connection enables the reporting of table comments in MySQL
	 *
	 * @param con the connection to test
	 * @return true if the driver returns comments for tables
	 */
	public static boolean remarksEnabledMySQL(WbConnection con)
	{
		if (con == null) return false;
		ConnectionProfile prof = con.getProfile();
		Properties props = prof.getConnectionProperties();
		String value = "false";
		if (props != null)
		{
			value = props.getProperty("useInformationSchema", "false");
		}
		return "true".equals(value);
	}


	public static String getDefaultTablespace(WbConnection conn)
	{
		readDefaultTableSpace(conn);
		return conn.getSessionProperty(PROP_KEY_TBLSPACE);
	}

	public static synchronized void readDefaultTableSpace(final WbConnection conn)
	{
		if (conn.getSessionProperty(PROP_KEY_TBLSPACE) != null) return;

		Statement stmt = null;
		ResultSet rs = null;
		String sql = "select /* SQLWorkbench */ default_tablespace from user_users";

		try
		{
			stmt = conn.createStatementForQuery();
			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logDebug("OracleTableSourceBuilder.readDefaultTableSpace()", "Using sql: " + sql);
			}

			rs = stmt.executeQuery(sql);
			if (rs.next())
			{
				conn.setSessionProperty(PROP_KEY_TBLSPACE, rs.getString(1));
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("OracleTableSourceBuilder.readDefaultTableSpace()", "Error retrieving table options", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
	}

	public static boolean checkDefaultTablespace()
	{
		return Settings.getInstance().getBoolProperty("workbench.db.oracle.check_default_tablespace", false);
	}

	public static boolean retrieveTablespaceInfo()
	{
		return Settings.getInstance().getBoolProperty("workbench.db.oracle.retrieve_tablespace", true);
	}

	public static boolean shouldAppendTablespace(String tablespace, String defaultTablespace, String objectOwner, String currentUser)
	{
		// no tablespace given --> nothing to append
		if (StringUtil.isEmptyString(tablespace)) return false;

		// different owner than the current user --> always append
		if (!StringUtil.equalStringIgnoreCase(StringUtil.trimQuotes(objectOwner), currentUser)) return true;

		// current user's table --> dependent on the system setting
		if (!retrieveTablespaceInfo()) return false;

		if (StringUtil.isEmptyString(defaultTablespace) && StringUtil.isNonEmpty(tablespace)) return true;
		return (!tablespace.equals(defaultTablespace));
	}

	public static String trimSQLPlusLineContinuation(String input)
	{
		if (StringUtil.isEmptyString(input)) return input;
		List<String> lines = StringUtil.getLines(input);
		StringBuilder result = new StringBuilder(input.length());
		for (String line : lines)
		{
			String clean = StringUtil.rtrim(line);
			if (clean.endsWith("-"))
			{
				result.append(clean.substring(0, clean.length() - 1));
			}
			else
			{
				result.append(line);
			}
			result.append('\n');
		}
		return result.toString();
	}

	public static boolean shouldTrimContinuationCharacter(WbConnection conn)
	{
		if (conn == null) return false;
		if (conn.getMetadata().isOracle())
		{
			return Settings.getInstance().getBoolProperty("workbench.db.oracle.trim.sqlplus.continuation", false);
		}
		return false;
	}

	public static boolean optimizeCatalogQueries()
	{
		return Settings.getInstance().getBoolProperty("workbench.db.oracle.prefer_user_catalog_tables", true);
	}
}
