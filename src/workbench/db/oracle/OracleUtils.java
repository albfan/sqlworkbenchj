/*
 * OracleUtils.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.ConnectionProfile;
import workbench.db.JdbcUtils;
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
	public static final String KEYWORD_EDITIONABLE = "EDITIONABLE";

	public static final Set<String> STANDARD_TYPES = CollectionUtil.caseInsensitiveSet
		("INTERVALDS", "INTERVALYM", "TIMESTAMP WITH LOCAL TIME ZONE", "TIMESTAMP WITH TIME ZONE",
		 "NUMBER", "NUMBER", "NUMBER", "LONG RAW", "RAW", "LONG", "CHAR", "NUMBER", "NUMBER", "NUMBER",
		 "FLOAT", "REAL", "VARCHAR2", "DATE", "DATE", "TIMESTAMP", "STRUCT", "ARRAY", "BLOB", "CLOB", "ROWID",
		 "XMLType", "SDO_GEOMETRY", "SDO_TOPO_GEOMETRY", "SDO_GEORASTER", "ANYTYPE", "ANYDATA");

  public static enum DbmsMetadataTypes
  {
    procedure,
    trigger,
    index,
    table,
    mview,
    view,
    sequence,
    synonym,
    grant;
  };

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

		// this is what the driver does: it assumes true if nothing was specified
		if (value == null) return true;

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
    if (conn == null) return "";
		readDefaultTableSpace(conn);
		return conn.getSessionProperty(PROP_KEY_TBLSPACE);
	}

	private static synchronized void readDefaultTableSpace(final WbConnection conn)
	{
		if (conn.getSessionProperty(PROP_KEY_TBLSPACE) != null) return;

		Statement stmt = null;
		ResultSet rs = null;
		String sql =
      "-- SQL Workbench \n" +
      "select default_tablespace \n" +
      "from user_users";

		try
		{
			stmt = conn.createStatementForQuery();
			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logDebug("OracleUtils.readDefaultTableSpace()", "Retrieving default tablespace using:\n" + sql);
			}

			rs = stmt.executeQuery(sql);
			if (rs.next())
			{
				conn.setSessionProperty(PROP_KEY_TBLSPACE, rs.getString(1));
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("OracleUtils.readDefaultTableSpace()", "Error retrieving table options using:\n" + sql, e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
	}

  public static String getCacheHint()
  {
    boolean useResultCache = Settings.getInstance().getBoolProperty("workbench.db.oracle.metadata.result_cache", false);
    return useResultCache ? "/*+ result_cache */" : StringUtil.EMPTY_STRING;
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

	public static boolean showSetServeroutputFeedback()
	{
		return Settings.getInstance().getBoolProperty("workbench.db.oracle.set_serveroutput.feedback", false);
	}

	public static boolean is12_1_0_2(WbConnection conn)
	{
		if (conn == null) return false;
		if (!JdbcUtils.hasMinimumServerVersion(conn, "12.1")) return false;

		try
		{
			String release = conn.getSqlConnection().getMetaData().getDatabaseProductVersion();
			return is12_1_0_2(release);
		}
		catch (Throwable th)
		{
			return false;
		}
	}

	public static boolean is12_1_0_2(String release)
	{
		int pos = release.indexOf("Release ");
		if (pos < 0) return false;
		int pos2 = release.indexOf(" - ", pos);
		String version = release.substring(pos + "Release".length() + 1, pos2);
		if (!version.startsWith("12")) return false;
		// "12.1.0.2.0"
		String[] elements = version.split("\\.");
		if (elements == null || elements.length < 5) return false;
		try
		{
			int major = Integer.parseInt(elements[0]);
			int minor = Integer.parseInt(elements[1]);
			int first = Integer.parseInt(elements[2]);
			int second = Integer.parseInt(elements[3]);
			if (major < 12) return false;
			if (minor > 1) return true;

			return (first >= 0 && second >= 2);
		}
		catch (Throwable th)
		{
			return false;
		}
	}

  public static boolean cleanupDDLQuotedIdentifiers()
  {
    return Settings.getInstance().getBoolProperty("workbench.db.oracle.dbmsmeta.cleanup.quotes", true);
  }

	public static boolean getUseOracleDBMSMeta(DbmsMetadataTypes type)
	{
    boolean global = Settings.getInstance().getBoolProperty("workbench.db.oracle.use.dbmsmeta", false);
		return Settings.getInstance().getBoolProperty("workbench.db.oracle.use.dbmsmeta." + type.name(), global);
	}

	public static void setUseOracleDBMSMeta(DbmsMetadataTypes type, boolean flag)
	{
		Settings.getInstance().setProperty("workbench.db.oracle.use.dbmsmeta." + type.name(), flag);
	}

  private static final String CALL_SET_TRANSFORM = "{call dbms_metadata.set_transform_param(dbms_metadata.session_transform, ?, true)}";

  /**
   * Calls dbms_metadata.set_transform_param to turn on the use of a SQLTERMINATOR
   *
   * See: http://docs.oracle.com/database/121/ARPLS/d_metada.htm#BGBJBFGE
   *
   * Use {@link #resetDBMSMetadata(workbench.db.WbConnection)} to reset the dbms_metadata configuration.
   *
   * @param con  the connection on which to invoke the procedure.
   */
  public static void initDBMSMetadata(WbConnection con)
  {
    CallableStatement stmt = null;
    try
    {
      stmt = con.getSqlConnection().prepareCall(CALL_SET_TRANSFORM);
      stmt.setString(1, "SQLTERMINATOR");
      stmt.execute();

      stmt.setString(1, "PRETTY");
      stmt.execute();
    }
    catch (Throwable th)
    {
      SqlUtil.closeStatement(stmt);
      LogMgr.logDebug("OracleUtils.initDBMSMetadata()", "Could not set transform parameter", th);
    }
  }

  /**
   * Calls dbms_metadata.set_transform_param to reset the transformations to the default.
   *
   * See: http://docs.oracle.com/database/121/ARPLS/d_metada.htm#BGBJBFGE
   *
   * @param con  the connection on which to invoke the procedure.
   */
  public static void resetDBMSMetadata(WbConnection con)
  {
    CallableStatement stmt = null;
    try
    {
      stmt = con.getSqlConnection().prepareCall(CALL_SET_TRANSFORM);
      stmt.setString(1, "DEFAULT");
    }
    catch (Throwable th)
    {
      SqlUtil.closeStatement(stmt);
      LogMgr.logDebug("OracleUtils.initDBMSMetadata()", "Could not reset transform parameters", th);
    }
  }

  public static String getDependentDDL(WbConnection conn, String dependentType, String name, String owner)
  {
    try
    {
      return getDDL(conn, dependentType, name, owner, true);
    }
    catch (SQLException ex)
    {
      // ignore. This simply means that the dependent DDL is not valid
      return null;
    }
  }

  /**
   * Utility function to call Oracle's dbms_metadata.get_ddl function.
   * See: http://docs.oracle.com/database/121/ARPLS/d_metada.htm#ARPLS66885
   *
   * Before calling the function, set_transform_param is called so that the SQLTERMINATOR is added
   * to the generated source (see: http://docs.oracle.com/database/121/ARPLS/d_metada.htm#ARPLS66910)
   *
   * @param conn   the connection on which to call GET_DDL
   * @param type   the object type for which to retrieve the DDL
   * @param name   the name of the object
   * @param owner  the owner of the object
   *
   * @return the source code as returned by GET_DDL (trimmed)
   *
   * @throws SQLException
   * @see #initDBMSMetadata(workbench.db.WbConnection)
   * @see #resetDBMSMetadata(workbench.db.WbConnection)
   */
  public static String getDDL(WbConnection conn, String type, String name, String owner)
    throws SQLException
  {
    return getDDL(conn, type, name, owner, false);
  }

  private static String getDDL(WbConnection conn, String type, String name, String owner, boolean dependent)
    throws SQLException
  {
    ResultSet rs = null;
    PreparedStatement stmt = null;
    String source = null;

    long start = System.currentTimeMillis();

    String sql = null;
    if (dependent)
    {
      sql = "select dbms_metadata.get_dependent_ddl(?, ?, ?) from dual";
    }
    else
    {
      sql = "select dbms_metadata.get_ddl(?, ?, ?) from dual";
    }
    try
    {
      initDBMSMetadata(conn);

      if (Settings.getInstance().getDebugMetadataSql())
      {
        LogMgr.logDebug("OracleUtils.getDDL()", "Calling dbms_metadata using:\n" + SqlUtil.replaceParameters(sql, type, name, owner));
      }
      stmt = conn.getSqlConnection().prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
      stmt.setString(1, type);
      stmt.setString(2, SqlUtil.removeObjectQuotes(name));
      stmt.setString(3, SqlUtil.removeObjectQuotes(owner));

      rs = stmt.executeQuery();
      if (rs.next())
      {
        source = StringUtil.trim(rs.getString(1));
      }

      if (cleanupDDLQuotedIdentifiers())
      {
        source = OracleDDLCleaner.cleanupQuotedIdentifiers(source);
      }
    }
    catch (SQLException ex)
    {
      LogMgr.logError("OracleUtils.getDDL()", "Could not procedure source using:\n" + SqlUtil.replaceParameters(sql, type, name, owner), ex);
      throw ex;
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
      resetDBMSMetadata(conn);
    }

    long duration = System.currentTimeMillis() - start;
    LogMgr.logDebug("OracleUtils.getDDL()", "Retrieving DDL using dbms_metadata for " + type + " " + owner + "." + name + " took: " + duration + "ms");
    return source;
  }


}
