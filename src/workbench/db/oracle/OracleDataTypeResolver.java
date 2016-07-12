/*
 * OracleDataTypeResolver.java
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

import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.DataTypeResolver;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleDataTypeResolver
  implements DataTypeResolver
{
  public static enum CharSemantics
  {
    Byte,
    Char;
  }

  private final WbConnection connection;
  private CharSemantics defaultCharSemantics = null;
  private boolean alwaysShowCharSemantics = false;

  /**
   * Only for testing purposes
   */
  OracleDataTypeResolver(CharSemantics defaultSemantics, boolean alwaysShowSemantics)
  {
    connection = null;
    defaultCharSemantics = defaultSemantics;
    alwaysShowCharSemantics = alwaysShowSemantics;
  }

  public OracleDataTypeResolver(WbConnection conn)
  {
    connection = conn;
    alwaysShowCharSemantics = Settings.getInstance().getBoolProperty("workbench.db.oracle.charsemantics.displayalways", true);

    if (!alwaysShowCharSemantics)
    {
      String sql
        = "-- SQL Workbench \n" +
        "SELECT value \n" +
        "FROM v$nls_parameters \n" +
        "WHERE parameter = 'NLS_LENGTH_SEMANTICS'";

      if (Settings.getInstance().getDebugMetadataSql())
      {
        LogMgr.logDebug("OracleDataTypeResolver.<init>", "Retrieving nls length semantics using:\n" + sql);
      }

      Statement stmt = null;
      ResultSet rs = null;
      try
      {
        stmt = this.connection.createStatement();

        rs = stmt.executeQuery(sql);
        if (rs.next())
        {
          String v = rs.getString(1);
          if ("BYTE".equalsIgnoreCase(v))
          {
            defaultCharSemantics = CharSemantics.Byte;
          }
          else if ("CHAR".equalsIgnoreCase(v))
          {
            defaultCharSemantics = CharSemantics.Char;
          }
          LogMgr.logInfo("OracleDataTypeResolver.<init>", "Default length semantics is: " + v);
        }
      }
      catch (Exception e)
      {
        defaultCharSemantics = CharSemantics.Byte;
        LogMgr.logWarning("OracleDataTypeResolver.<init>", "Could not retrieve NLS_LENGTH_SEMANTICS from v$nls_parameters. Assuming byte semantics. Using SQL:\n" + sql, e);
      }
      finally
      {
        SqlUtil.closeAll(rs, stmt);
      }
    }
  }


  @Override
  public int fixColumnType(int type, String dbmsType)
  {
    if (type == Types.DATE && OracleUtils.getMapDateToTimestamp(connection)) return Types.TIMESTAMP;

    // Oracle reports TIMESTAMP WITH TIMEZONE with the numeric value -101 and
    // TIMESTAMP WITH LOCAL TIMEZONE is reported as -102
    // neither of them are valid java.sql.Types values
    if (type == -101 || type == -102) return Types.TIMESTAMP_WITH_TIMEZONE;

    // The Oracle driver stupidly reports TIMESTAMP(n) columns as Types.OTHER
    if (type == Types.OTHER && dbmsType != null && dbmsType.startsWith("TIMESTAMP("))
    {
      return Types.TIMESTAMP;
    }

    return type;
  }

  @Override
  public String getSqlTypeDisplay(String dbmsName, int sqlType, int size, int digits)
  {
    return getSqlTypeDisplay(dbmsName, sqlType, size, digits, defaultCharSemantics);
  }

  public String getSqlTypeDisplay(String dbmsName, int sqlType, int size, int digits, CharSemantics byteOrChar)
  {
    String display;

    if (sqlType == Types.VARCHAR)
    {
      // Hack to get Oracle's VARCHAR2(xx Byte) or VARCHAR2(xxx Char) display correct
      // My own statement to retrieve column information in OracleMetaData
      // will pass the correct byte/char semantics to this method
      display = getVarcharType(dbmsName, size, byteOrChar);
    }
    else if ("NUMBER".equalsIgnoreCase(dbmsName))
    {
      if (digits < 0 || size == 0)
      {
        return "NUMBER";
      }
      else if (digits == 0)
      {
        return "NUMBER(" + size + ")";
      }
      else
      {
        return "NUMBER(" + size + "," + digits + ")";
      }
    }
    else if (sqlType == Types.VARBINARY && "RAW".equals(dbmsName))
    {
      return "RAW(" + size  + ")";
    }
    else
    {
      display = SqlUtil.getSqlTypeDisplay(dbmsName, sqlType, size, digits);
    }
    return display;
  }

  private String getVarcharType(String type, int size, CharSemantics semantics)
  {
    StringBuilder result = new StringBuilder(25);

    result.append(type);

    if (size <= 0)
    {
      // this can happen for procedure columns
      return result.toString();
    }

    result.append('(');
    result.append(size);

    // Only apply this logic on VARCHAR columns
    // NVARCHAR (which might have been reported as type == VARCHAR) does not allow Byte/Char semantics
    if (type.startsWith("VARCHAR"))
    {
      if (alwaysShowCharSemantics || semantics != defaultCharSemantics)
      {
        if (semantics == null) semantics = defaultCharSemantics;
        if (semantics == CharSemantics.Byte)
        {
          result.append(" Byte");
        }
        else if (semantics == CharSemantics.Char)
        {
          result.append(" Char");
        }
      }
    }
    result.append(')');
    return result.toString();
  }

  public CharSemantics getDefaultCharSemantics()
  {
    return defaultCharSemantics;
  }

  @Override
  public String getColumnClassName(int type, String dbmsType)
  {
    // use default
    return null;
  }

}
