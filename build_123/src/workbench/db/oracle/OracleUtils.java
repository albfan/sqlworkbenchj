/*
 * OracleUtils.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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

import static workbench.db.DbMetadata.*;

/**
 * Utility methods for Oracle
 *
 * @author Thomas Kellerer
 */
public class OracleUtils
{
  public static final String PROP_KEY_TBLSPACE = "oracle_default_tablespace";
  public static final String PROP_FIX_TIMESTAMPTZ = "workbench.db.oracle.fix.timstamptz";
  public static final String PROP_FIX_PLSQL_RESULTSET = "workbench.db.oracle.fix.plsql.resultset";

  public static final String KEYWORD_EDITIONABLE = "EDITIONABLE";

  public static final Set<String> STANDARD_TYPES = CollectionUtil.caseInsensitiveSet("INTERVALDS", "INTERVALYM", "TIMESTAMP WITH LOCAL TIME ZONE", "TIMESTAMP WITH TIME ZONE",
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
    grant,
    constraint;
  };

  private OracleUtils()
  {
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
    return StringUtil.stringToBool(value);
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

    return StringUtil.stringToBool(value);
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
   *
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
    return StringUtil.stringToBool(value);
  }

  /**
   * Checks if the given connection enables the reporting of table comments in MySQL
   *
   * @param con the connection to test
   *
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
    return StringUtil.stringToBool(value);
  }

  public static boolean hasMultipleContainers(WbConnection conn)
  {
    if (JdbcUtils.hasMinimumServerVersion(conn, "12.1") == false) return false;

    int cdbCount = 0;
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      stmt = conn.createStatementForQuery();
      rs = stmt.executeQuery("select count(*) from v$pdbs");
      if (rs.next())
      {
        cdbCount = rs.getInt(1);
      }
    }
    catch (Exception ex)
    {
      // ignore
      cdbCount = 0;
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
    return cdbCount > 0;
  }

  public static String getCurrentContainer(WbConnection conn)
  {
    String sql
      = "-- SQL Workbench \n" +
      "select sys_context('userenv', 'CON_NAME') from dual";

    if (Settings.getInstance().getDebugMetadataSql())
    {
      LogMgr.logDebug("WbOraShow.getCurrentContainer()", "Retrieving current container name using:\n" + sql);
    }

    return getSingleResult(sql, conn);
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

    String sql
      = "-- SQL Workbench \n" +
      "select default_tablespace \n" +
      "from user_users";

    if (Settings.getInstance().getDebugMetadataSql())
    {
      LogMgr.logDebug("WbOraShow.readDefaultTableSpace()", "Retrieving default tablespace using:\n" + sql);
    }

    String tableSpace = getSingleResult(sql, conn);
    conn.setSessionProperty(PROP_KEY_TBLSPACE, tableSpace);
  }

  private static String getSingleResult(String query, WbConnection conn)
  {
    Statement stmt = null;
    ResultSet rs = null;
    String result = null;

    try
    {
      stmt = conn.createStatementForQuery();
      rs = stmt.executeQuery(query);
      if (rs.next())
      {
        result = rs.getString(1);
      }
    }
    catch (SQLException e)
    {
      LogMgr.logError("OracleUtils.getSingleResult()", "Error running query:\n" + query, e);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }

    return result;
  }

  public static String getCacheHint()
  {
    boolean useResultCache = Settings.getInstance().getBoolProperty("workbench.db.oracle.metadata.result_cache", false);
    return useResultCache ? "/*+ result_cache */ " : StringUtil.EMPTY_STRING;
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
    return Settings.getInstance().getBoolProperty("workbench.db.oracle.dbmsmeta.cleanup.quotes", false);
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

  public static boolean showContainerInfo()
  {
    return Settings.getInstance().getBoolProperty("workbench.db.oracle.conninfo.include.container", false);
  }

  public static boolean useInternalTimestampConversion()
  {
    return Settings.getInstance().getBoolProperty("workbench.db.oracle.timestamp.internal.conversion", false);
  }

  public static boolean fixTimestampTZ()
  {
    return Settings.getInstance().getBoolProperty(PROP_FIX_TIMESTAMPTZ, true);
  }

  public static boolean fixPLSQLResultSetBug()
  {
    return Settings.getInstance().getBoolProperty(PROP_FIX_PLSQL_RESULTSET, true);
  }

  public static String[] adjustTableTypes(WbConnection dbConnection, String[] types)
  {
    // When TABLE and MATERIALIZED VIEW is specified for getTables() the Oracle driver returns
    // materialized views twice, so we need to get rid of them.
    // As mviews are automatically returned when TABLE is specified we can remove the mview type
    if (typeIncluded("TABLE", types) && typeIncluded(MVIEW_NAME, types))
    {
      types = CollectionUtil.removeElement(types, MVIEW_NAME);
    }
    else if (typeIncluded(MVIEW_NAME, types) && JdbcUtils.hasMiniumDriverVersion(dbConnection, "12.1"))
    {
      // the 12.x driver does not accept MATERIALIZED VIEW as a type any more
      CollectionUtil.replaceElement(types, MVIEW_NAME, "TABLE");
    }
    return types;
  }
}
