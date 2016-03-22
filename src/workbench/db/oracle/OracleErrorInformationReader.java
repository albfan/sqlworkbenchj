/*
 * OracleErrorInformationReader.java
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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.ErrorInformationReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.sql.ErrorDescriptor;

import workbench.util.CollectionUtil;
import workbench.util.SqlParsingUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * An ErrorInformationReader for Oracle that retrieves the errors for the last DDL command.
 *
 * This is basically what SQL*Plus' "show errors" does.
 *
 * @author Thomas Kellerer
 */
public class OracleErrorInformationReader
  implements ErrorInformationReader
{
  private WbConnection connection;
  private Set<String> validTypes = CollectionUtil.caseInsensitiveSet(
    "VIEW", "PROCEDURE", "FUNCTION", "PACKAGE", "PACKAGE BODY", "TRIGGER", "TYPE",
    "TYPE BODY", "LIBRARY", "JAVA SOURCE", "JAVA CLASS", "DIMENSION");

  public OracleErrorInformationReader(WbConnection conn)
  {
    this.connection = conn;
  }

  /**
   *  Return the errors reported in the all_errors table for Oracle.
   *  This method can be used to obtain error information after a CREATE PROCEDURE
   *  or CREATE TRIGGER statement has been executed.
   *
   *  @return extended error information if available
   */
  @Override
  public ErrorDescriptor getErrorInfo(String originalSql, String schema, String objectName, String objectType, boolean showObjectHeaders)
  {
    if (StringUtil.isEmptyString(objectName))
    {
      return null;
    }

    if (objectType != null && !validTypes.contains(objectType))
    {
      return null;
    }

    String query =
      "-- SQL Workbench \n" +
      "SELECT line, position, text, name, type \n" +
      "FROM all_errors \n" +
      "WHERE owner = ? \n";

    int typeIndex = -1;
    int nameIndex = -1;

    if (objectType != null)
    {
      query += " and type = ? \n";
      typeIndex = 2;
    }

    if (objectName != null)
    {
      query += "  and name = ? \n";
      nameIndex = typeIndex == -1 ? 2 : 3;
    }

    query += " ORDER BY type, name, line, position";

    PreparedStatement stmt = null;
    ResultSet rs = null;

    StringBuilder msg = new StringBuilder(250);
    ErrorDescriptor result = null;

    try
    {
      TableIdentifier tbl = new TableIdentifier(objectName);

      tbl.adjustCase(connection);
      String oschema = schema == null ? tbl.getRawSchema() : schema;
      String oname = tbl.getRawTableName();

      if (oschema == null)
      {
        oschema = connection.getMetadata().getCurrentSchema();
      }
      stmt = this.connection.getSqlConnection().prepareStatement(query);

      String otype = objectType == null ? null : objectType.toUpperCase().trim();

      stmt.setString(1, oschema);

      if (typeIndex > -1)
      {
        stmt.setString(typeIndex, otype);
      }
      if (nameIndex > -1)
      {
        stmt.setString(nameIndex, oname);
      }

      if (Settings.getInstance().getDebugMetadataSql())
      {
        LogMgr.logDebug("OracleErrorInformationReader.getErrorInfo()", "Retrieving error information using:\n" + SqlUtil.replaceParameters(query, oschema, otype, oname));
      }

      rs = stmt.executeQuery();
      int count = 0;
      String currentName = null;

      boolean firstHeading = true;
      int indentLength = 15;

      String indent = StringUtil.padRight("", indentLength);
      while (rs.next())
      {
        if (count == 0)
        {
          result = new ErrorDescriptor();
        }
        else
        {
          msg.append("\n");
        }

        int line = rs.getInt(1);
        int pos = rs.getInt(2);

        String message = rs.getString(3);
        String name = rs.getString(4);
        String type = rs.getString(5);

        // do not report the line number of the "ignored" error, because the real error occurs after that
        // (and there is always another error if a "PL/SQL: SQL Statement ignored" is reported)
        if (firstHeading && !message.contains("PL/SQL: SQL Statement ignored"))
        {
          firstHeading = false;

          // only report the first error position
          // normalize to zero based values
          result.setErrorPosition(line - 1, pos - 1);
          result.setInStatementOffset(getRealStart(type, originalSql));
        }

        if (showObjectHeaders && (currentName == null || !currentName.equals(name)))
        {
          String heading = ResourceMgr.getFormattedString("ErrForObject", type, name);
          String divide = StringUtil.padRight("", heading.length(), '-');
          msg.append(heading);
          msg.append('\n');
          msg.append(divide);
          msg.append("\n");
          currentName = name;
        }

        String lineInfo = ResourceMgr.getFormattedString("ErrAtLinePos", Integer.valueOf(line), Integer.valueOf(pos));
        lineInfo = StringUtil.padRight(lineInfo, indentLength);
        msg.append(lineInfo);
        message = message.trim().replace("\n\n", "\n"); // remove duplicate newlines
        // indent all lines of the message
        message = message.replaceAll(StringUtil.REGEX_CRLF, "\n" + indent);
        msg.append(message);
        count++;
      }
    }
    catch (SQLException e)
    {
      LogMgr.logError("OracleErrorInformationReader.getErrorInfo()", "Error retrieving error information", e);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }

    if (result != null)
    {
      result.setErrorMessage(msg.toString());
    }

    return result;
  }

  private int getRealStart(String type, String sql)
  {
    if (sql == null) return 0;
    if (!"trigger".equalsIgnoreCase(type)) return 0;

    SqlParsingUtil util = SqlParsingUtil.getInstance(connection);
    int pos = util.getKeywordPosition("BEGIN", sql);
    if (pos > 0) return pos;
    return 0;
  }
}
