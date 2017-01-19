/*
 * Db2SynonymReader.java
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
package workbench.db.ibm;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.DBID;
import workbench.db.SynonymReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;

/**
 * A class to retrieve synonym definitions from a DB2 database.
 *
 * @author Thomas Kellerer
 */
public class Db2SynonymReader
  implements SynonymReader
{

  /**
   * Returns an empty list, as the standard JDBC driver
   * alread returns synonyms in the getObjects() method.
   *
   * @return an empty list
   */
  @Override
  public List<TableIdentifier> getSynonymList(WbConnection con, String catalog, String owner, String namePattern)
    throws SQLException
  {
    return Collections.emptyList();
  }

  @Override
  public String getSynonymTypeName()
  {
    return "ALIAS";
  }

  @Override
  public TableIdentifier getSynonymTable(WbConnection con, String catalog, String schemaPattern, String namePattern)
    throws SQLException
  {
    String sql = "";

    boolean isHostDB2 = DBID.DB2_ZOS.isDB(con);
    boolean isISeries = DBID.DB2_ISERIES.isDB(con);

    if (isISeries)
    {
      char catalogSeparator = con.getMetadata().getCatalogSeparator();
      sql =
        "SELECT base_table_schema, base_table_name \n" +
        "FROM qsys2" + catalogSeparator + "systables \n" +
        " WHERE table_type = 'A' \n" +
        "   AND table_name = ? \n" +
        "   AND table_owner = ?";
    }
    else if (isHostDB2)
    {
      sql =
        "SELECT tbcreator, tbname \n" +
        "FROM sysibm.syssynonyms \n" +
        "WHERE name = ? \n" +
        "  AND creator = ?";
    }
    else
    {
      sql =
        "SELECT base_tabschema, base_tabname \n" +
        "FROM syscat.tables \n" +
        "WHERE type = 'A' \n" +
        "  and tabname = ? \n" +
        "  and tabschema = ?";
    }

    if (Settings.getInstance().getDebugMetadataSql())
    {
      LogMgr.logInfo("Db2SynonymReader.getSynonymTable()", "Query to retrieve synonyms:\n" + SqlUtil.replaceParameters(sql, namePattern, schemaPattern));
    }

    PreparedStatement stmt = con.getSqlConnection().prepareStatement(sql);
    stmt.setString(1, namePattern);
    stmt.setString(2, schemaPattern);

    ResultSet rs = stmt.executeQuery();
    String table = null;
    String owner = null;
    TableIdentifier result = null;
    try
    {
      if (rs.next())
      {
        owner = rs.getString(1);
        table = rs.getString(2);
        if (table != null)
        {
          result = new TableIdentifier(null, owner, table, false);
          result.setNeverAdjustCase(true);
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
  public String getSynonymSource(WbConnection con, String catalog, String synonymSchema, String synonymName)
    throws SQLException
  {
    TableIdentifier id = getSynonymTable(con, catalog, synonymSchema, synonymName);
    StringBuilder result = new StringBuilder(00);
    String nl = Settings.getInstance().getInternalEditorLineEnding();
    result.append("CREATE OR REPLACE ALIAS ");
    result.append(SqlUtil.buildExpression(con, null, synonymSchema, synonymName));
    result.append(nl + "   FOR ");
    result.append(id.getTableExpression());
    result.append(';');
    result.append(nl);

    return result.toString();
}

  @Override
  public boolean supportsReplace(WbConnection con)
  {
    return true;
  }
}
