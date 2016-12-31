/*
 * DerbySynonymReader.java
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
package workbench.db.progress;


import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.SynonymReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * Retrieve synonyms and their definition from a Progress (OpenEdge) database.
 *
 * @author Thomas Kellerer
 */
public class OpenEdgeSynonymReader
  implements SynonymReader
{
  public OpenEdgeSynonymReader()
  {
  }

  @Override
  public List<TableIdentifier> getSynonymList(WbConnection con, String catalog, String owner, String namePattern)
    throws SQLException
  {
    List<TableIdentifier> result = new ArrayList<>();
    String sql =
      "SELECT sowner,\n" +
      "       sname \n" +
      "FROM sysprogress.syssynonyms \n";

    namePattern = StringUtil.trimToNull(SqlUtil.removeObjectQuotes(namePattern));
    owner = StringUtil.trimToNull(SqlUtil.removeObjectQuotes(owner));

    boolean whereAdded = false;

    if (StringUtil.isNonBlank(namePattern))
    {
      if (namePattern.contains("%"))
      {
        sql += " WHERE sname LIKE '";
      }
      else
      {
        sql += " WHERE sname = '";
      }
      sql += SqlUtil.escapeQuotes(namePattern);
      sql += "' \n";
      whereAdded = true;
    }

    if (StringUtil.isNonBlank(owner))
    {
      if (whereAdded)
      {
        sql += " AND sowner = '" + owner + "'";
      }
      else
      {
        sql += "WHERE sowner = '" + owner + "'";
      }
    }

    if (Settings.getInstance().getDebugMetadataSql())
    {
      LogMgr.logInfo(getClass().getName() + ".getSynonymList()", "Retrieving synonym list using:\n" + sql);
    }

    PreparedStatement stmt = null;
    ResultSet rs = null;
    try
    {
      stmt = con.getSqlConnection().prepareStatement(sql);
      stmt.setString(1, owner);
      if (StringUtil.isNonBlank(namePattern)) stmt.setString(2, namePattern);

      rs = stmt.executeQuery();
      while (rs.next())
      {
        String schema = rs.getString(1);
        String synonym = rs.getString(2);
        if (!rs.wasNull())
        {
          TableIdentifier tbl = new TableIdentifier(null, schema, synonym, false);
          tbl.setType(SYN_TYPE_NAME);
          tbl.setNeverAdjustCase(true);
          result.add(tbl);
        }
      }
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }

    return result;
  }

  @Override
  public TableIdentifier getSynonymTable(WbConnection con, String catalog, String owner, String synonym)
    throws SQLException
  {
    String sql =
      "SELECT stbl,\n" +
      "       stblowner\n" +
      "FROM sysprogress.syssynonyms \n" +
      "WHERE sname = ?\n" +
      "  AND sowner = ?";

    if (Settings.getInstance().getDebugMetadataSql())
    {
      LogMgr.logInfo(getClass().getName() + ".getSynonymTable()", "Retrieving synonym table using:\n" + SqlUtil.replaceParameters(sql, synonym, owner));
    }

    PreparedStatement stmt = null;
    ResultSet rs = null;
    TableIdentifier result = null;

    try
    {
      stmt = con.getSqlConnection().prepareStatement(sql);
      stmt.setString(1, synonym);
      stmt.setString(2, owner);
      rs = stmt.executeQuery();

      if (rs.next())
      {
        String targetTable = rs.getString(1);
        String targetSchema = rs.getString(2);
        if (targetTable != null)
        {
          result = new TableIdentifier(null, targetSchema, targetTable, false);
          result.setNeverAdjustCase(true);
          result.setType(SYN_TYPE_NAME);
        }
      }
    }
    finally
    {
      SqlUtil.closeAll(rs,stmt);
    }

    return result;
  }

  @Override
  public boolean supportsReplace(WbConnection con)
  {
    return false;
  }

}
