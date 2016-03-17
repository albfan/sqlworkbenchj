/*
 * PostgresIndexReader.java
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
package workbench.db.postgres;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.DbMetadata;
import workbench.db.IndexDefinition;
import workbench.db.JdbcIndexReader;
import workbench.db.JdbcUtils;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.util.CollectionUtil;
import workbench.util.ExceptionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A extension to the JdbcIndexReader to construct the Postgres specific syntax
 * for indexes.
 *
 * This class does not actually construct the CREATE INDEX based on the information
 * available from the JDBC API, but retrieves the CREATE INDEX directly from the database
 * as Postgres stores the full command in the table <tt>pg_indexes</tt>.
 *
 * @author  Thomas Kellerer
 */
public class PostgresIndexReader
  extends JdbcIndexReader
{
  private static final String PROP_RETRIEVE_TABLESPACE = "workbench.db.postgres.tablespace.retrieve.index";

  public PostgresIndexReader(DbMetadata meta)
  {
    super(meta);
  }

  /**
   * Return the SQL for several indexes for one table.
   *
   * @param table      the table for which to retrieve the indexes
   * @param indexList  the indexes to retrieve
   *
   * @return The SQL statement for all indexes
   */
  @Override
  public StringBuilder getIndexSource(TableIdentifier table, List<IndexDefinition> indexList)
  {
    if (CollectionUtil.isEmpty(indexList)) return null;

    WbConnection con = this.metaData.getWbConnection();
    Statement stmt = null;
    ResultSet rs = null;

    // The full CREATE INDEX Statement is stored in pg_indexes for each
    // index. So all we need to do, is retrieve the indexdef value from there for all passed indexes.
    // For performance reasons I'm not calling getIndexSource(IndexDefinition) in a loop
    int count = indexList.size();
    String schema = "'" + table.getRawSchema() + "'";

    StringBuilder sql = new StringBuilder(50 + count * 20);
    if (JdbcUtils.hasMinimumServerVersion(con, "8.0"))
    {
      sql.append(
        "SELECT indexdef, indexname, tablespace, obj_description(indexname::regclass, 'pg_class') \n" +
        "FROM pg_indexes \n" +
        "WHERE (schemaname, indexname) IN (");
    }
    else
    {
      sql.append(
        "SELECT indexdef, indexname, null::text as tablespace, null::text as remarks \n" +
        "FROM pg_indexes \n" +
        "WHERE (schemaname, indexname) IN (");
    }

    String nl = Settings.getInstance().getInternalEditorLineEnding();

    StringBuilder source = new StringBuilder(count * 50);

    Savepoint sp = null;
    int indexCount = 0;
    try
    {
      for (IndexDefinition index : indexList)
      {
        String idxName = "'" + con.getMetadata().removeQuotes(index.getName()) + "'";

        if (index.isPrimaryKeyIndex()) continue;

        if (index.isUniqueConstraint())
        {
          String constraint = getUniqueConstraint(table, index);
          source.append(constraint);
          source.append(nl);
        }
        else
        {
          if (indexCount > 0) sql.append(',');
          sql.append('(');
          sql.append(schema);
          sql.append(',');
          sql.append(idxName);
          sql.append(')');
          indexCount++;
        }
      }
      sql.append(')');

      if (Settings.getInstance().getDebugMetadataSql())
      {
        LogMgr.logDebug("PostgresIndexReader.getIndexSource1()", "Using sql: " + sql.toString());
      }

      if (indexCount > 0)
      {
        sp = con.setSavepoint();
        stmt = con.createStatementForQuery();

        rs = stmt.executeQuery(sql.toString());
        while (rs.next())
        {
          source.append(rs.getString(1));

          String idxName = rs.getString(2);
          String tblSpace = rs.getString(3);
          if (StringUtil.isNonEmpty(tblSpace))
          {
            IndexDefinition idx = findIndexByName(indexList, idxName);
            idx.setTablespace(tblSpace);
            source.append(" TABLESPACE ");
            source.append(tblSpace);
          }
          source.append(';');
          source.append(nl);
          String remarks = rs.getString(4);
          if (StringUtil.isNonBlank(remarks))
          {
            source.append("COMMENT ON INDEX " + SqlUtil.quoteObjectname(idxName) + " IS '" + SqlUtil.escapeQuotes(remarks) + "'");
          }
        }
        con.releaseSavepoint(sp);
      }
    }
    catch (Exception e)
    {
      con.rollback(sp);
      LogMgr.logError("PostgresIndexReader.getIndexSource1()", "Error retrieving source", e);
      source = new StringBuilder(ExceptionUtil.getDisplay(e));
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }

    if (source.length() > 0) source.append(nl);

    return source;
  }

  /**
   * Return the SQL to re-create any (non default) options for the index.
   *
   * The returned String has to be structured so that it can be appended
   * after the DBMS specific basic CREATE INDEX statement.
   *
   * @param table   the table for which ot retrieve the index options
   * @param index   the table's index for which to retrieve the options
   *
   * @return null if not options are applicable
   *         a SQL fragment to be appended at the end of the create index statement if an option is available.
   */
  @Override
  public String getIndexOptions(TableIdentifier table, IndexDefinition index)
  {
    if (index != null && StringUtil.isNonEmpty(index.getTablespace()))
    {
      return "\n   TABLESPACE " + index.getTablespace();
    }
    return null;
  }

  @Override
  public boolean supportsTableSpaces()
  {
    // only make this dependent on the property to actually retrieve the tablespace, because
    // we know that Postgres supports table spaces
    return Settings.getInstance().getBoolProperty(PROP_RETRIEVE_TABLESPACE, true);
  }

  /**
   * Enhance the retrieved indexes with additional information.
   *
   * Currently this only reads the tablespace for each index.
   * Reading the tablespace information can be turned off using the config setting {@link PROP_RETRIEVE_TABLESPACE}
   *
   * @param tbl        the table for which the indexes were retrieved
   * @param indexDefs  the list of retrieved indexes
   *
   * @see IndexDefinition#setTablespace(java.lang.String)
   * @see PostgresIndexReader#PROP_RETRIEVE_TABLESPACE
   */
  @Override
  public void processIndexList(Collection<IndexDefinition> indexDefs)
  {
    if (!Settings.getInstance().getBoolProperty(PROP_RETRIEVE_TABLESPACE, true)) return;

    if (CollectionUtil.isEmpty(indexDefs)) return;

    WbConnection con = this.metaData.getWbConnection();
    if (!JdbcUtils.hasMinimumServerVersion(con, "8.0")) return;

    Statement stmt = null;
    ResultSet rs = null;

    int count = indexDefs.size();

    StringBuilder sql = new StringBuilder(50 + count * 20);
    sql.append(
      "SELECT indexname, tablespace, obj_description(indexname::regclass, 'pg_class') as remarks \n" +
      "FROM pg_indexes \n" +
      "WHERE (schemaname, indexname) IN (");

    int indexCount = 0;
    for (IndexDefinition index : indexDefs)
    {
      String idxName = con.getMetadata().removeQuotes(index.getName());
      String schema = con.getMetadata().removeQuotes(index.getSchema());
      if (indexCount > 0) sql.append(',');
      sql.append("('");
      sql.append(schema);
      sql.append("','");
      sql.append(idxName);
      sql.append("')");
      indexCount++;
    }
    sql.append(')');

    if (Settings.getInstance().getDebugMetadataSql())
    {
      LogMgr.logDebug("PostgresIndexReader.processIndexList()", "Retrieving index tablespace information using:\n" + sql);
    }

    Savepoint sp = null;

    try
    {
      sp = con.setSavepoint();
      stmt = con.createStatementForQuery();

      rs = stmt.executeQuery(sql.toString());
      while (rs.next())
      {
        String idxName = rs.getString(1);
        String tblSpace = rs.getString(2);
        String remarks = rs.getString(3);
        IndexDefinition idx = findIndexByName(indexDefs, idxName);
        if (StringUtil.isNonEmpty(tblSpace))
        {
          idx.setTablespace(tblSpace);
        }
        idx.setComment(remarks);
      }
      con.releaseSavepoint(sp);
    }
    catch (Exception e)
    {
      con.rollback(sp);
      LogMgr.logError("PostgresIndexReader.processIndexList()", "Error retrieving index tablespace using:\n" + sql, e);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
  }

  @Override
  public CharSequence getIndexSource(TableIdentifier table, IndexDefinition indexDefinition)
  {
    if (indexDefinition == null) return null;
    if (table == null) return null;

    // This allows to use a statement configured through workbench.settings
    // see getNativeIndexSource()
    if (Settings.getInstance().getBoolProperty("workbench.db.postgresql.default.indexsource", false))
    {
      return super.getIndexSource(table, indexDefinition);
    }

    if (indexDefinition.isUniqueConstraint())
    {
      return getUniqueConstraint(table, indexDefinition);
    }

    WbConnection con = this.metaData.getWbConnection();

    PreparedStatement stmt = null;
    ResultSet rs = null;

    // The full CREATE INDEX Statement is stored in pg_indexes for each
    // index. So all we need to do, is retrieve the indexdef value from that view

    String result = null;

    StringBuilder sql = new StringBuilder(100);
    if (JdbcUtils.hasMinimumServerVersion(con, "8.0"))
    {
      sql.append(
        "SELECT indexdef, tablespace, obj_description(indexname::regclass, 'pg_class') \n" +
        "FROM pg_indexes \n" +
        "WHERE indexname = ? and schemaname = ? ");
    }
    else
    {
      sql.append(
        "SELECT indexdef, null::text as tablespace, null::text as remarks \n" +
        "FROM pg_indexes \n" +
        "WHERE indexname = ? and schemaname = ? ");
    }

    if (Settings.getInstance().getDebugMetadataSql())
    {
      LogMgr.logDebug("PostgresIndexReader.getIndexSource2()", "Retrieving index source using:\n " +
        SqlUtil.replaceParameters(sql, indexDefinition.getName(), table.getSchema()));
    }

    Savepoint sp = null;
    try
    {
      sp = con.setSavepoint();
      stmt = con.getSqlConnection().prepareStatement(sql.toString());
      stmt.setString(1, indexDefinition.getName());
      stmt.setString(2, table.getSchema());
      rs = stmt.executeQuery();
      if (rs.next())
      {
        result = rs.getString(1);
        String tblSpace = rs.getString(2);
        String remarks = rs.getString(3);
        indexDefinition.setTablespace(tblSpace);
        indexDefinition.setComment(result);

        if (StringUtil.isNonBlank(remarks))
        {
          result += "\nCOMMENT ON INDEX " + SqlUtil.quoteObjectname(indexDefinition.getName()) + " IS '" + SqlUtil.escapeQuotes(remarks) + "';\n";
        }
      }
      con.releaseSavepoint(sp);
    }
    catch (Exception e)
    {
      con.rollback(sp);
      LogMgr.logError("PostgresIndexReader.getIndexSource2()", "Error when retrieving index information using:\n" +
          SqlUtil.replaceParameters(sql, indexDefinition.getName(), table.getSchema()), e);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
    return result;
  }

  @Override
  public boolean supportsIndexComments()
  {
    return true;
  }

}
