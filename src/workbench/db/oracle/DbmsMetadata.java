/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015 Thomas Kellerer.
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

import static workbench.db.oracle.OracleUtils.*;

/**
 *
 * @author Thomas Kellerer
 */
public class DbmsMetadata
{
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
