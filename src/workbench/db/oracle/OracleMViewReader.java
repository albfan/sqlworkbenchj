/*
 * OracleMViewReader.java
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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.DropType;
import workbench.db.IndexDefinition;
import workbench.db.JdbcUtils;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.TableSourceBuilder;
import workbench.db.WbConnection;

import workbench.util.ExceptionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A class to retrieve the source of an Oracle materialized view
 *
 * @author Thomas Kellerer
 */
public class OracleMViewReader
{
  private String pkIndex;
  private String defaultTablespace = null;

  public OracleMViewReader()
  {
  }

  public CharSequence getMViewSource(WbConnection dbConnection, TableDefinition def, List<IndexDefinition> indexList, DropType dropType, boolean includeComments)
  {
    boolean useDbmsMeta = OracleUtils.getUseOracleDBMSMeta(OracleUtils.DbmsMetadataTypes.mview);

    TableIdentifier table = def.getTable();

    StringBuilder result = new StringBuilder(250);

    long start = System.currentTimeMillis();

    String mviewName = table.getTableExpression(dbConnection);

    if (dropType != DropType.none)
    {
      result.append("DROP MATERIALIZED VIEW ");
      result.append(mviewName);
      result.append(";\n\n");
    }

    boolean retrieved = false;
    pkIndex = null;

    if (useDbmsMeta)
    {
      try
      {
        String sql = DbmsMetadata.getDDL(dbConnection, "MATERIALIZED_VIEW", table.getObjectName(), table.getSchema());
        result.append(sql);
        retrieved = true;
      }
      catch (Exception sql)
      {
        LogMgr.logWarning("OracleMViewReader", "Could not retrieve source for MVIEW " + mviewName + " using dbms_metadata. Querying ALL_MVIEWS instead", sql);
      }
    }

    OracleTableSourceBuilder tsource = new OracleTableSourceBuilder(dbConnection);
    StringBuilder partitionSql = tsource.getPartitionSql(table, "  ", false);

    if (!retrieved)
    {
      StringBuilder query = new StringBuilder(500);
      StringBuilder options = new StringBuilder(150);

      try
      {
        //retrieveMViewDetails will store any defined primary key in pkIndex
        retrieveMViewDetails(dbConnection, table, query, options);
      }
      catch (SQLException sql)
      {
        return ExceptionUtil.getDisplay(sql);
      }

      result.append("CREATE MATERIALIZED VIEW ");
      result.append(mviewName);

      if (StringUtil.isNonEmpty(partitionSql))
      {
        result.append('\n');
        result.append(partitionSql);
      }
      if (options.length() > 0)
      {
        result.append(options);
      }
      result.append("\nAS\n");
      result.append(query);
      result.append('\n');

      if (includeComments)
      {
        TableSourceBuilder.appendComments(result, dbConnection, def);
      }
    }

    result.append('\n');

    if (indexList == null)
    {
      indexList = dbConnection.getMetadata().getIndexReader().getTableIndexList(table, true);
    }

    // The source for the auto-generated index that is created when using the WITH PRIMARY KEY option
    // does not need to be included in the generated SQL
    if (pkIndex != null)
    {
      Iterator<IndexDefinition> itr = indexList.iterator();
      while (itr.hasNext())
      {
        IndexDefinition index = itr.next();
        String name = index.getName();
        if (name.equals(pkIndex))
        {
          itr.remove();
          break;
        }
      }
    }

    StringBuilder indexSource = dbConnection.getMetadata().getIndexReader().getIndexSource(table, indexList);

    if (indexSource != null)
    {
      result.append("\n\n");
      result.append(indexSource);
    }

    long duration = System.currentTimeMillis() - start;
    LogMgr.logDebug("OracleMViewReader.getMViewSource()", "Building source for " + mviewName + " took " + duration + "ms");

    return result;
  }

  /**
   * Retrieve options for the given materialized view (like REFRESH type...).
   *
   * A call to this method will also store the name of the primary key index (if any)
   * in the instance variable pkIndex
   *
   * @param dbConnection
   * @param mview
   * @return a SQL string that can be used after the CREATE MATERIALIZED VIEW part
   * @see #pkIndex
   */
  private void retrieveMViewDetails(WbConnection dbConnection, TableIdentifier mview, StringBuilder query, StringBuilder options)
    throws SQLException
  {
    if (OracleUtils.checkDefaultTablespace() && defaultTablespace == null)
    {
      defaultTablespace = OracleUtils.getDefaultTablespace(dbConnection);
    }

    boolean supportsCompression = JdbcUtils.hasMinimumServerVersion(dbConnection, "11.1");
    boolean supportsUsingIndex = JdbcUtils.hasMinimumServerVersion(dbConnection, "9.0");

    String useNoIndexCol = "mv.use_no_index";
    if (!supportsUsingIndex)
    {
      useNoIndexCol = "null as use_no_index";
    }

    String sql =
      "-- SQL Workbench \n" +
      "select mv.query, \n" +
      "       mv.rewrite_enabled, \n" +
      "       mv.refresh_mode, \n" +
      "       mv.refresh_method, \n" +
      "       mv.build_mode, \n" +
      "       mv.fast_refreshable, \n" +
      "       " + useNoIndexCol + ", \n" +
      "       cons.constraint_name, \n" +
      "       cons.index_name, \n" +
      "       rc.interval, \n" +
      "       tb.tablespace_name, \n" +
      (supportsCompression ?
      "       tb.compression, \n"  +
      "       tb.compress_for \n" :
      "       null as compression, \n   null as compress_for \n") +
      "from all_mviews mv \n" +
      (supportsCompression ?
      "  join all_tables tb on tb.owner = mv.owner and tb.table_name = mv.container_name \n" :
      "") +
      "  left join all_constraints cons on cons.owner = mv.owner and cons.table_name = mv.mview_name and cons.constraint_type = 'P' \n" +
      "  left join all_refresh_children rc on rc.owner = mv.owner and rc.name = mv.mview_name \n" +
      "where mv.owner = ? \n" +
      " and mv.mview_name = ? ";

    PreparedStatement stmt = null;
    ResultSet rs = null;

    long start = System.currentTimeMillis();

    try
    {
      if (Settings.getInstance().getDebugMetadataSql())
      {
        LogMgr.logDebug("OracleMViewReader.retrieveMViewDetails()",
          "Retrieving MVIEW details using:\n" + SqlUtil.replaceParameters(sql, mview.getRawSchema(), mview.getRawTableName()));
      }

      stmt = dbConnection.getSqlConnection().prepareStatement(sql);
      stmt.setString(1, mview.getRawSchema());
      stmt.setString(2, mview.getRawTableName());

      rs = stmt.executeQuery();

      if (rs.next())
      {
        String queryString = rs.getString("query");
        query.append(cleanupQuery(queryString));

        String tsName = rs.getString("tablespace_name");
        if (StringUtil.isNonEmpty(tsName) && OracleUtils.shouldAppendTablespace(tsName, defaultTablespace, mview.getRawSchema(), dbConnection.getCurrentUser()))
        {
          options.append("\n  TABLESPACE ");
          options.append(tsName);
        }

        String compress = rs.getString("compression");
        if (StringUtil.equalStringIgnoreCase("enabled", compress))
        {
          String compressType = rs.getString("compress_for");
          if (StringUtil.isNonBlank(compressType))
          {
            options.append("\n  COMPRESS FOR ");
            options.append(compressType);
          }
        }

        String useNoIndex = rs.getString("USE_NO_INDEX");
        if ("Y".equals(useNoIndex))
        {
          options.append("\n  USING NO INDEX\n");
        }

        String buildMode = rs.getString("BUILD_MODE");
        if ("PREBUILT".equals(buildMode))
        {
          options.append("\n  ON PREBUILT TABLE ");
        }
        else
        {
          options.append("\n  BUILD ");
          options.append(buildMode);
        }

        String method = rs.getString("REFRESH_METHOD");
        options.append("\n  REFRESH ");
        options.append(method);

        String when = rs.getString("REFRESH_MODE");
        options.append(" ON ");
        options.append(when);

        String pk = rs.getString("constraint_name");
        if (pk != null)
        {
          options.append(" WITH PRIMARY KEY");
        }
        else
        {
          options.append(" WITH ROWID");
        }

        String next = rs.getString("INTERVAL");
        if (StringUtil.isNonBlank(next))
        {
          options.append("\n  NEXT ");
          options.append(next.trim());
        }

        String rewrite = rs.getString("REWRITE_ENABLED");
        if ("Y".equals(rewrite))
        {
          options.append("\n  ENABLE QUERY REWRITE");
        }
        else
        {
          options.append("\n  DISABLE QUERY REWRITE");
        }
        pkIndex = rs.getString("INDEX_NAME");
      }

      long duration = System.currentTimeMillis() - start;
      LogMgr.logDebug("OracleMViewReader.retrieveMViewDetails()",
        "Retrieving information from ALL_MVIEWS for " + mview.getRawSchema() + "." + mview.getRawTableName() + " took " + duration + "ms");
    }
    catch (SQLException e)
    {
      LogMgr.logWarning("OracleMetadata.retrieveMViewDetails()",
        "Could not retrieve MVIEW details using:\n" + SqlUtil.replaceParameters(sql, mview.getRawSchema(), mview.getRawTableName()), e);
      throw e;
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
  }

  private String cleanupQuery(String query)
  {
    if (query == null) return "";

    query = OracleDDLCleaner.cleanupQuotedIdentifiers(query);
    if (!query.endsWith(";"))
    {
      query += ";";
    }
    return query;
  }
}
