/*
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
package workbench.db.oracle;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.WbConnection;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class DbmsMetadata
{
  private static final String ENABLE_TRANSFORM = "{call dbms_metadata.set_transform_param(dbms_metadata.session_transform, ?, true)}";
  private static final String DISABLE_TRANSFORM = "{call dbms_metadata.set_transform_param(dbms_metadata.session_transform, ?, false)}";

  /**
   * Calls dbms_metadata.set_transform_param to reset the transformations to the default.
   *
   * See: http://docs.oracle.com/database/121/ARPLS/d_metada.htm#ARPLS66885
   *
   * @param con  the connection on which to invoke the procedure.
   *
   * @see #getDDL(WbConnection, String, String, String, boolean)
   */
  public static String getDependentDDL(WbConnection conn, String dependentType, String name, String owner)
  {
    try
    {
      return getDDL(conn, dependentType, name, owner, true);
    }
    catch (SQLException ex)
    {
      // ignore. This simply means that there is no dependent DDL
      return null;
    }
  }

  /**
   * Utility function to call Oracle's dbms_metadata.get_ddl function.
   *
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
   *
   * @see #initTransforms(WbConnection)
   * @see #resetSessionTransforms(WbConnection)
   * @see #getDependentDDL(WbConnection, String, String, String)
   */
  public static String getDDL(WbConnection conn, String type, String name, String owner)
    throws SQLException
  {
    return getDDL(conn, type, name, owner, false);
  }

  public static String getTableDDL(WbConnection conn, String name, String owner, boolean inlinePK, boolean inlineFK)
    throws SQLException
  {
    if (!inlineFK)
    {
      disableTransformParam(conn, "REF_CONSTRAINTS");
    }
    if (!inlinePK)
    {
      disableTransformParam(conn, "CONSTRAINTS");
    }
    return getDDL(conn, "TABLE", name, owner, false);
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
      initTransforms(conn);

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

      if (OracleUtils.cleanupDDLQuotedIdentifiers())
      {
        source = OracleDDLCleaner.cleanupQuotedIdentifiers(source);
      }
    }
    catch (SQLException ex)
    {
      LogMgr.logError("OracleUtils.getDDL()", "Could not read source using:\n" + SqlUtil.replaceParameters(sql, type, name, owner), ex);
      throw ex;
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
      resetSessionTransforms(conn);
    }

    long duration = System.currentTimeMillis() - start;
    LogMgr.logDebug("OracleUtils.getDDL()", "Retrieving DDL using dbms_metadata for " + type + " " + owner + "." + name + " took: " + duration + "ms");
    return source;
  }

  /**
   * Calls dbms_metadata.set_transform_param to turn on the use of a SQLTERMINATOR
   *
   * See: http://docs.oracle.com/database/121/ARPLS/d_metada.htm#BGBJBFGE
   *
   * Use {@link #resetSessionTransforms(WbConnection)} to reset the dbms_metadata configuration.
   *
   * @param con  the connection on which to invoke the procedure.
   */
  private static void initTransforms(WbConnection con)
  {
    CallableStatement stmt = null;
    try
    {
      stmt = con.getSqlConnection().prepareCall(ENABLE_TRANSFORM);
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
   *
   * @see #initTransforms(WbConnection)
   */
  private static void resetSessionTransforms(WbConnection con)
  {
    CallableStatement stmt = null;
    try
    {
      stmt = con.getSqlConnection().prepareCall(ENABLE_TRANSFORM);
      stmt.setString(1, "DEFAULT");
    }
    catch (Throwable th)
    {
      SqlUtil.closeStatement(stmt);
      LogMgr.logDebug("OracleUtils.initDBMSMetadata()", "Could not reset transform parameters", th);
    }
  }

  private static void disableTransformParam(WbConnection con, String transform)
  {
    CallableStatement stmt = null;
    try
    {
      stmt = con.getSqlConnection().prepareCall(DISABLE_TRANSFORM);
      stmt.setString(1, transform);
      stmt.execute();
    }
    catch (Throwable th)
    {
      SqlUtil.closeStatement(stmt);
      LogMgr.logDebug("OracleUtils.disableTransformParam()", "Could not disable transform parameter: " + transform, th);
    }
  }

}
