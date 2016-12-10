/*
 * JdbcIndexReader.java
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
package workbench.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.sqltemplates.TemplateHandler;

import workbench.storage.DataStore;
import workbench.storage.SortDefinition;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * An implementation of the IndexReader interface that uses the standard JDBC API
 * to get the index information.
 *
 * @author Thomas Kellerer
 */
public class JdbcIndexReader
  implements IndexReader
{
  protected DbMetadata metaData;
  protected String pkIndexNameColumn;
  protected String pkStatusColumn;
  protected String partitionedFlagColumn;

  private static final String COL_NAME_REMARKS = "REMARKS";
  private static final String COL_NAME_STATUS = "STATUS";
  private static final String COL_NAME_TABLESPACE = "TABLESPACE";

  private UniqueConstraintReader uniqueConstraintReader;

  public JdbcIndexReader(DbMetadata meta)
  {
    this.metaData = meta;
    if (meta != null && meta.getWbConnection() != null)
    {
      uniqueConstraintReader = ReaderFactory.getUniqueConstraintReader(meta.getWbConnection());
    }
  }

  /**
   * Returns false.
   *
   * Needs to be overriden by a specialized IndexReader for each DBMS
   */
  @Override
  public boolean supportsTableSpaces()
  {
    // needs to be implemented by a specialized Index reader that
    // either retrieves tablespace information directly in getTableIndexList()
    // or enhances the index list in processIndexList()
    return false;
  }

  public boolean supportsIndexStatus()
  {
    return false;
  }

  public boolean supportsIndexComments()
  {
    return false;
  }

  /**
   * This method is called after the ResultSet obtained from getIndexInfo() has been processed.
   *
   * This is a hook for sub-classes that overwrite getIndexInfo() and need to close the
   * returned result set.
   *
   * @see #getIndexInfo(workbench.db.TableIdentifier, boolean)
   */
  @Override
  public void indexInfoProcessed()
  {
    // nothing to do, as we are using the driver's call
  }

  /**
   * Return information about the indexes defined for the given table.
   * If the TableIdentifier's type is VIEW null will be returned unless
   * the current DBMS supports indexed views.
   * <br/>
   * This is a performance optimization when retrieving a large number
   * of objects (such as for WbSchemaReport or WbGrepSource) in order
   * to minimized the roundtrips to the database.
   *
   * @throws java.sql.SQLException
   * @see DbSettings#supportsIndexedViews()
  */
  @Override
  public ResultSet getIndexInfo(TableIdentifier table, boolean unique)
    throws SQLException
  {
    if (Settings.getInstance().getDebugMetadataSql())
    {
      LogMgr.logDebug("JdbcIndexReader.getIndexInfo()",
        "Calling getIndexInfo() using: catalog="+ table.getCatalog() +
        ", schema=" + table.getSchema() +
        ", name=" + table.getTableName() +
        ", unique=" + unique +
        ", approximate=true");
    }

    ResultSet rs = null;
    Savepoint sp = null;
    try
    {
      if (metaData.getDbSettings().useSavePointForDML())
      {
        sp = metaData.getWbConnection().setSavepoint();
      }
      rs = this.metaData.getSqlConnection().getMetaData().getIndexInfo(table.getRawCatalog(), table.getRawSchema(), table.getRawTableName(), unique, true);
    }
    catch (Exception sql)
    {
      metaData.getWbConnection().rollback(sp);
      LogMgr.logWarning("JdbcIndexReader.getIndexInfo()", "Error calling DatabaseMetaData.getIndexInfo()", sql);
    }
    finally
    {
      metaData.getWbConnection().releaseSavepoint(sp);
    }
    return rs;
  }

  /**
   * Return the primary key definition for the table.
   *
   * The definition contains the name of the primary key constraint,
   * and optionally the name of the index supporting the primary key. It also contains
   * all the columns that make up the primary key.
   *
   * @param tbl the table for which the PK should be retrieved
   */
  @Override
  public PkDefinition getPrimaryKey(TableIdentifier tbl)
  {
    // Views don't have primary keys...
    if (metaData.getDbSettings().isViewType(tbl.getType())) return null;

    // extended objects don't have primary keys either...
    if (metaData.isExtendedObject(tbl)) return null;

    // Retrieve the name of the PK index
    String pkName = null;
    String pkIndexName = null;
    PkDefinition pk = null;

    // apparently some drivers (e.g. for DB2/HOST) do support column names in the ResultSet
    // other drivers (e.g. MonetDB) do not return the information when the column index is used.
    // Therefor we need a switch for this.
    boolean useColumnNames = metaData.getDbSettings().useColumnNameForMetadata();

    if (this.metaData.getDbSettings().supportsGetPrimaryKeys())
    {
      String catalog = tbl.getCatalog();
      String schema = tbl.getSchema();
      List<IndexColumn> cols = new ArrayList<>();

      ResultSet keysRs = null;
      String pkStatus = null;

      long start = System.currentTimeMillis();

      Savepoint sp = null;

      try
      {
        if (metaData.getDbSettings().useSavePointForDML())
        {
          sp = metaData.getWbConnection().setSavepoint();
        }

        keysRs = getPrimaryKeyInfo(catalog, schema, tbl.getRawTableName());
        while (keysRs.next())
        {
          if (pkName == null)
          {
            pkName = useColumnNames ? keysRs.getString("PK_NAME") : keysRs.getString(6);
          }
          if (pkIndexNameColumn != null && pkIndexName == null)
          {
            // this is supplied by our own statement that is used
            // by the OracleIndexReader
            pkIndexName = keysRs.getString(pkIndexNameColumn);
          }
          if (pkStatusColumn != null && pkStatus == null)
          {
            pkStatus = keysRs.getString(pkStatusColumn);
          }
          String colName = useColumnNames ? keysRs.getString("COLUMN_NAME") : keysRs.getString(4);
          int sequence = useColumnNames ? keysRs.getInt("KEY_SEQ") : keysRs.getInt(5);
          if (sequence < 1)
          {
            LogMgr.logWarning("JdbcIndexReader.getPrimaryKey()", "Invalid column sequence '" + sequence + "' for key column " + tbl.getTableName() + "." + colName + " received!");
          }

          cols.add(new IndexColumn(quoteIndexColumn(colName), sequence));
        }
      }
      catch (Exception e)
      {
        metaData.getWbConnection().rollback(sp);
        LogMgr.logWarning("JdbcIndexReader.getPrimaryKey()", "Error retrieving PK information", e);
      }
      finally
      {
        metaData.getWbConnection().releaseSavepoint(sp);
        SqlUtil.closeResult(keysRs);
        primaryKeysResultDone();
      }

      long duration = System.currentTimeMillis() - start;
      LogMgr.logDebug("JdbcIndexreader.getPrimaryKey()", "PK Information for " + tbl.getTableName() + ", PK Name=" + pkName + ", PK Index=" + pkIndexName + ", columns=" + cols + " (" + duration + "ms)");

      if (cols.size() > 0)
      {
        pk = new PkDefinition(getPkName(pkName, pkIndexName, tbl), cols);
        pk.setPkIndexName(pkIndexName);
        if (pkStatus != null)
        {
          pk.setEnabled(isStatusEnabled(pkStatus));
        }
      }
    }

    if (pk == null && metaData.getDbSettings().pkIndexHasTableName())
    {
      LogMgr.logDebug("JdbcIndexreader.getPrimaryKey()", "No primary key returned from the driver, checking the unique indexes");
      pk = findPKFromIndexList(tbl);
    }

    tbl.setPkInitialized(true);
    if (pk != null && tbl.getPrimaryKey() == null)
    {
      tbl.setPrimaryKey(pk);
    }

    return pk;
  }

  protected String quoteIndexColumn(String colName)
  {
    if (colName == null) return null;
    if (metaData.getDbSettings().quoteIndexColumnNames())
    {
      colName = metaData.quoteObjectname(colName);
    }
    return colName;
  }

  protected Boolean isStatusEnabled(String status)
  {
    return null;
  }

  private PkDefinition findPKFromIndexList(TableIdentifier tbl)
  {
    List<IndexDefinition> unique = getTableIndexList(tbl, true, false, false);
    if (CollectionUtil.isEmpty(unique)) return null;

    // see DbSettings.pkIndexHasTableName()
    // this will be checked in processIndexResult
    for (IndexDefinition idx : unique)
    {
      if (idx.isPrimaryKeyIndex())
      {
        LogMgr.logInfo("JdbcIndexreader.findPKFromIndexList()", "Using unique index " + idx.getObjectName() + " as a primary key");
        PkDefinition pk = new PkDefinition(idx.getObjectName(), idx.getColumns());
        pk.setPkIndexDefinition(idx);
        return pk;
      }
    }

    return null;
  }

  private String getPkName(String pkName, String indexName, TableIdentifier tbl)
  {
    if (pkName != null) return pkName;
    if (indexName != null) return indexName;
    String name = "pk_" + SqlUtil.cleanupIdentifier(tbl.getRawTableName()).toLowerCase();
    LogMgr.logInfo("JdbcIndexReader.getPkName()","Using generated PK name " + name + " for " + tbl.getTableName());
    return name;
  }

  protected void primaryKeysResultDone()
  {
  }

  protected ResultSet getPrimaryKeyInfo(String catalog, String schema, String tableName)
    throws SQLException
  {
    return this.metaData.getJdbcMetaData().getPrimaryKeys(catalog, schema, tableName);
  }

  /**
   * Return the SQL to re-create the indexes defined for the table.
   *
   * @param table
   * @param indexList

   * @return SQL Script to create indexes
   */
  @Override
  public StringBuilder getIndexSource(TableIdentifier table, List<IndexDefinition> indexList)
  {
    if (CollectionUtil.isEmpty(indexList)) return null;
    StringBuilder result = new StringBuilder(indexList.size() * 100);

    for (IndexDefinition definition : indexList)
    {
      // Only add non-PK Indexes here. The indexes related to the PK constraints
      // are usually auto-created when the PK is defined, so there is no need
      // to re-create a CREATE INDEX statement for them
      if (definition != null && !definition.isPrimaryKeyIndex() && !definition.isAutoGenerated())
      {
        CharSequence idx = getIndexSource(table, definition);
        if (idx != null)
        {
          result.append(idx);
          result.append('\n');
        }
      }
    }
    return result;
  }

  protected String getUniqueConstraint(TableIdentifier table, IndexDefinition indexDefinition)
  {
    String template = this.metaData.getDbSettings().getCreateUniqeConstraintSQL();
    String sql = TemplateHandler.replaceTablePlaceholder(template, table, metaData.getWbConnection());
    sql = StringUtil.replace(sql, MetaDataSqlManager.COLUMN_LIST_PLACEHOLDER, indexDefinition.getColumnList());
    sql = StringUtil.replace(sql, MetaDataSqlManager.CONSTRAINT_NAME_PLACEHOLDER, this.metaData.quoteObjectname(indexDefinition.getUniqueConstraintName()));

    ConstraintDefinition constraint = indexDefinition.getUniqueConstraint();

    Boolean deferred = constraint == null ? Boolean.FALSE : constraint.isInitiallyDeferred();
    Boolean deferrable = constraint == null ? Boolean.FALSE : constraint.isDeferrable();

    if (deferrable == null)
    {
      sql = StringUtil.replace(sql, MetaDataSqlManager.DEFERRABLE, "");
      sql = StringUtil.replace(sql, MetaDataSqlManager.DEFERRED, "");
    }
    else
    {
      if (Boolean.TRUE.equals(deferrable))
      {
        sql = StringUtil.replace(sql, MetaDataSqlManager.DEFERRABLE, "DEFERRABLE");
        if (Boolean.TRUE.equals(deferred))
        {
          sql = StringUtil.replace(sql, MetaDataSqlManager.DEFERRED, "INITIALLY DEFERRED");
        }
        else
        {
          sql = StringUtil.replace(sql, MetaDataSqlManager.DEFERRED, "INITIALLY IMMEDIATE");
        }
      }
      else
      {
        sql = StringUtil.replace(sql, MetaDataSqlManager.DEFERRABLE, "");
        sql = StringUtil.replace(sql, MetaDataSqlManager.DEFERRED, "");
      }
    }
    sql = sql.trim();

    if (constraint != null)
    {
      // currently this is only implemented for Oracle
      String disabled = metaData.getDbSettings().getDisabledConstraintKeyword();
      String novalidate = metaData.getDbSettings().getNoValidateConstraintKeyword();

      sql = TemplateHandler.replacePlaceholder(sql, MetaDataSqlManager.CONS_ENABLED, constraint.isEnabled() ? "" : disabled, true);
      sql = TemplateHandler.replacePlaceholder(sql, MetaDataSqlManager.CONS_VALIDATED, constraint.isValid() ? "" : novalidate, true);

      if (metaData.isOracle())
      {
        // deal with the "using index" part of a unique constraint
        if (!constraint.getConstraintName().equals(indexDefinition.getName()))
        {
          sql += "\n   USING INDEX ";
          sql +=indexDefinition.getObjectExpression(this.metaData.getWbConnection());
        }
      }
    }

    if (!sql.endsWith(";"))
    {
      sql += ";";
    }
    sql += "\n";
    return sql;
  }

  private String getNativeIndexSource(TableIdentifier table, IndexDefinition index)
  {
    String sql = metaData.getDbSettings().getRetrieveIndexSourceSql();
    if (sql == null) return null;

    StringBuilder result = new StringBuilder(250);

    int colIndex = metaData.getDbSettings().getRetrieveIndexSourceCol();
    boolean needQuotes = metaData.getDbSettings().getRetrieveTableSourceNeedsQuotes();

    WbConnection conn = metaData.getWbConnection();

    sql = TableSourceBuilder.replacePlaceHolder(sql, TableSourceBuilder.SCHEMA_PLACEHOLDER, index.getSchema(), needQuotes, metaData);
    sql = TableSourceBuilder.replacePlaceHolder(sql, TableSourceBuilder.CATALOG_PLACEHOLDER, index.getCatalog(), needQuotes, metaData);
    sql = TableSourceBuilder.replacePlaceHolder(sql, MetaDataSqlManager.INDEX_NAME_PLACEHOLDER, index.getName(), needQuotes, metaData);
    sql = TableSourceBuilder.replacePlaceHolder(sql, MetaDataSqlManager.FQ_INDEX_NAME_PLACEHOLDER, index.getFullyQualifiedName(conn), false, metaData);
    sql = TableSourceBuilder.replacePlaceHolder(sql, MetaDataSqlManager.TABLE_EXPRESSION_PLACEHOLDER, table.getTableExpression(conn), false, metaData);
    sql = TableSourceBuilder.replacePlaceHolder(sql, MetaDataSqlManager.TABLE_NAME_PLACEHOLDER, table.getTableName(), needQuotes, metaData);
    sql = TableSourceBuilder.replacePlaceHolder(sql, MetaDataSqlManager.FQ_TABLE_NAME_PLACEHOLDER, table.getFullyQualifiedName(conn), false, metaData);

    if (Settings.getInstance().getDebugMetadataSql())
    {
      LogMgr.logDebug("JdbcIndexReader.getNativeIndexSource()", "Using query to retrieve index definition=" + sql);
    }
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      stmt = conn.createStatementForQuery();
      rs = stmt.executeQuery(sql);
      while (rs.next())
      {
        result.append(rs.getString(colIndex));
      }
      result.append('\n');
    }
    catch (Exception se)
    {
      LogMgr.logError("JdbcIndexReader.getNativeIndexSource()", "Error retrieving table source using query: " + sql + "\n", se);
      return null;
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
    StringUtil.trimTrailingWhitespace(result);
    if (result.charAt(result.length() -1 ) != ';')
    {
      result.append(";\n");
    }

    return result.toString();
  }

  @Override
  public CharSequence getIndexSource(TableIdentifier table, IndexDefinition indexDefinition)
  {
    if (indexDefinition == null) return null;
    String nativeSource = getNativeIndexSource(table, indexDefinition);
    if (nativeSource != null) return nativeSource;

    String uniqueConstraint = null;
    if (indexDefinition.isUniqueConstraint())
    {
      uniqueConstraint = getUniqueConstraint(table, indexDefinition);
      if (indexDefinition.getUniqueConstraintName().equals(indexDefinition.getName()))
      {
        return uniqueConstraint;
      }
    }
    StringBuilder idx = processCreateIndexTemplate(table, indexDefinition);
    if (uniqueConstraint != null)
    {
      idx.append('\n');
      idx.append(uniqueConstraint);
    }
    return idx;
  }

  protected StringBuilder processCreateIndexTemplate(TableIdentifier table, IndexDefinition indexDefinition)
  {
    StringBuilder idx = new StringBuilder(100);
    String template = this.metaData.getDbSettings().getCreateIndexSQL();
    String type = indexDefinition.getIndexType();
    type = getSQLKeywordForType(type);

    String options = getIndexOptions(table, indexDefinition);

    WbConnection conn = metaData.getWbConnection();
    String sql = template;

    sql = StringUtil.replace(sql, MetaDataSqlManager.FQ_TABLE_NAME_PLACEHOLDER, table.getFullyQualifiedName(conn));
    sql = StringUtil.replace(sql, MetaDataSqlManager.TABLE_EXPRESSION_PLACEHOLDER, table.getTableExpression(conn));
    sql = StringUtil.replace(sql, MetaDataSqlManager.TABLE_NAME_PLACEHOLDER, table.getTableName());

    if (indexDefinition.isUnique())
    {
      sql = StringUtil.replace(sql, MetaDataSqlManager.UNIQUE_PLACEHOLDER, "UNIQUE");
      if ("unique".equalsIgnoreCase(type)) type = "";
    }
    else
    {
      sql = TemplateHandler.removePlaceholder(sql, MetaDataSqlManager.UNIQUE_PLACEHOLDER, true);
    }

    if (StringUtil.isEmptyString(type))
    {
      sql = TemplateHandler.removePlaceholder(sql, MetaDataSqlManager.INDEX_TYPE_PLACEHOLDER, true);
    }
    else
    {
      sql = TemplateHandler.replacePlaceholder(sql, MetaDataSqlManager.INDEX_TYPE_PLACEHOLDER, type, true);
    }

    String expr = indexDefinition.getExpression(conn);
    if (indexDefinition.isNonStandardExpression()) // currently only Firebird
    {
      sql = StringUtil.replace(sql, "(" + MetaDataSqlManager.COLUMN_LIST_PLACEHOLDER + ")", expr);
    }
    else
    {
      sql = TemplateHandler.replacePlaceholder(sql, MetaDataSqlManager.COLUMN_LIST_PLACEHOLDER, expr, true);
    }

    if (!StringUtil.equalStringIgnoreCase("ASC", indexDefinition.getDirection()))
    {
      sql = StringUtil.replace(sql, MetaDataSqlManager.IDX_DIRECTION_PLACEHOLDER, indexDefinition.getDirection());
    }
    else
    {
      sql = TemplateHandler.removePlaceholder(sql, MetaDataSqlManager.IDX_DIRECTION_PLACEHOLDER, true);
    }

    sql = StringUtil.replace(sql, MetaDataSqlManager.FQ_INDEX_NAME_PLACEHOLDER, indexDefinition.getObjectExpression(metaData.getWbConnection()));
    sql = StringUtil.replace(sql, MetaDataSqlManager.INDEX_NAME_PLACEHOLDER, indexDefinition.getObjectName());
    idx.append(sql);

    if (StringUtil.isNonBlank(options))
    {
      idx.append(options);
    }
    idx.append(";\n");

    CommentSqlManager mgr = new CommentSqlManager(metaData.getDbId());
    String commentTemplate = mgr.getCommentSqlTemplate("INDEX", null);
    if (StringUtil.isNonBlank(indexDefinition.getComment()) && commentTemplate != null)
    {
      commentTemplate = StringUtil.replace(commentTemplate, MetaDataSqlManager.FQ_INDEX_NAME_PLACEHOLDER, indexDefinition.getObjectExpression(metaData.getWbConnection()));
      commentTemplate = StringUtil.replace(commentTemplate, MetaDataSqlManager.INDEX_NAME_PLACEHOLDER, indexDefinition.getObjectName());
      commentTemplate = commentTemplate.replace(CommentSqlManager.COMMENT_PLACEHOLDER, indexDefinition.getComment());
      idx.append(commentTemplate);
      idx.append(";\n");
    }
    return idx;
  }

  /**
   * Return the SQL to re-create any (non default) options for the index.
   *
   * The returned String has to be structured so that it can be appended
   * after the DBMS specific basic CREATE INDEX statement
   *
   * @return null if not options are applicable
   *         a SQL "fragment" to be appended at the end of the create index statement if an option is available.
   */
  @Override
  public String getIndexOptions(TableIdentifier table, IndexDefinition type)
  {
    return null;
  }

  public String getSQLKeywordForType(String type)
  {
    // fix some Oracle types that are informational only
    if (type == null || type.startsWith("NORMAL") || "IOT - TOP".equals(type)) return "";
    return type;
  }


  /**
   *  Build the SQL statement to create an Index on the given table.
   *
   *  @param aTable      The table name for which the index should be constructed
   *  @param indexName   The name of the Index
   *  @param unique      unique index yes/no
   *  @param columnList  The columns that should build the index
   *
   *  @return the SQL statement to create the index
   */
  @Override
  public String buildCreateIndexSql(TableIdentifier aTable, String indexName, boolean unique, List<IndexColumn> columnList)
  {
    if (columnList == null) return StringUtil.EMPTY_STRING;
    int count = columnList.size();
    if (count == 0) return StringUtil.EMPTY_STRING;
    String template = this.metaData.getDbSettings().getCreateIndexSQL();
    StringBuilder cols = new StringBuilder(count * 25);

    for (int i=0; i < count; i++)
    {
      IndexColumn col = columnList.get(i);
      if (col == null) continue;
      if (cols.length() > 0) cols.append(',');
      cols.append(col.getExpression());
    }

    String sql = StringUtil.replace(template, MetaDataSqlManager.TABLE_NAME_PLACEHOLDER, aTable.getTableName());
    sql = sql.replace(MetaDataSqlManager.TABLE_EXPRESSION_PLACEHOLDER, aTable.getTableExpression(metaData.getWbConnection()));
    sql = sql.replace(MetaDataSqlManager.FQ_TABLE_NAME_PLACEHOLDER, aTable.getFullyQualifiedName(metaData.getWbConnection()));

    sql = TemplateHandler.removePlaceholder(sql, MetaDataSqlManager.INDEX_TYPE_PLACEHOLDER, true);
    if (unique)
    {
      sql = TemplateHandler.replacePlaceholder(sql, MetaDataSqlManager.UNIQUE_PLACEHOLDER, "UNIQUE", true);
    }
    else
    {
      sql = TemplateHandler.removePlaceholder(sql, MetaDataSqlManager.UNIQUE_PLACEHOLDER, true);
    }
    sql = StringUtil.replace(sql, MetaDataSqlManager.COLUMN_LIST_PLACEHOLDER, cols.toString());
    sql = StringUtil.replace(sql, MetaDataSqlManager.INDEX_NAME_PLACEHOLDER, indexName);
    sql = StringUtil.replace(sql, MetaDataSqlManager.FQ_INDEX_NAME_PLACEHOLDER, indexName);
    return sql;
  }

  /**
   * A hook to post-process the index definitions after they are full retrieved.
   *
   * @param table     the table for which the indexlist was retrieved (never null)
   * @param indexList the indexes retrieved (never null)
   */
  @Override
  public void processIndexList(Collection<IndexDefinition> indexList)
  {
    // Nothing implemented
  }

  private DataStore createIndexListDataStore(boolean includeTableName)
  {
    List<String> columnList = CollectionUtil.arrayList("INDEX_NAME", "UNIQUE", "PK", "DEFINITION", "TYPE");
    List<Integer> typeList = CollectionUtil.arrayList(Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR);
    List<Integer> sizeList = CollectionUtil.arrayList(30, 7, 6, 40, 10, 15);

    if (this.supportsTableSpaces())
    {
      columnList.add(COL_NAME_TABLESPACE);
      typeList.add(Types.VARCHAR);
      sizeList.add(15);
    }

    if (this.supportsIndexStatus())
    {
      columnList.add(COL_NAME_STATUS);
      typeList.add(Types.VARCHAR);
      sizeList.add(15);
    }

    if (this.supportsIndexComments())
    {
      columnList.add(COL_NAME_REMARKS);
      typeList.add(Types.VARCHAR);
      sizeList.add(15);
    }

    if (includeTableName)
    {
      columnList.add(0, "TABLE_SCHEMA");
      typeList.add(0, Types.VARCHAR);
      sizeList.add(0, 30);
      columnList.add(1, "TABLENAME");
      typeList.add(1, Types.VARCHAR);
      sizeList.add(1, 30);
    }

    String[] cols = new String[columnList.size()];
    cols = columnList.toArray(cols);

    final int[] types = new int[typeList.size()];
    for (int i=0; i < typeList.size(); i++)
    {
      types[i] = typeList.get(i).intValue();
    }

    final int[] sizes = new int[sizeList.size()];
    for (int i=0; i < sizeList.size(); i++)
    {
      sizes[i] = sizeList.get(i).intValue();
    }

    return new DataStore(cols, types, sizes);
  }

  /**
   * Return the index information for a table as a DataStore.
   *
   * This is delegated to getTableIndexList() and from the resulting collection
   * the datastore is created.
   *
   * @param table the table to get the indexes for
   * @see #getTableIndexList(TableIdentifier)
   */
  @Override
  public DataStore getTableIndexInformation(TableIdentifier table)
  {
    Collection<IndexDefinition> indexes = getTableIndexList(table, false);
    return fillDataStore(indexes, false);
  }

  @Override
  public DataStore fillDataStore(Collection<IndexDefinition> indexes, boolean includeTableName)
  {
    DataStore idxData = createIndexListDataStore(includeTableName);
    int offset = includeTableName ? 2 : 0;

    int statusIndex = idxData.getColumnIndex(COL_NAME_STATUS);
    int tableSpaceIndex = idxData.getColumnIndex(COL_NAME_TABLESPACE);
    int remarksIndex = idxData.getColumnIndex(COL_NAME_REMARKS);

    for (IndexDefinition idx : indexes)
    {
      int row = idxData.addRow();
      if (includeTableName)
      {
        idxData.setValue(row, 0, idx.getBaseTable().getSchema());
        idxData.setValue(row, 1, idx.getBaseTable().getTableName());
      }
      idxData.setValue(row, offset + COLUMN_IDX_TABLE_INDEXLIST_INDEX_NAME, idx.getName());
      idxData.setValue(row, offset + COLUMN_IDX_TABLE_INDEXLIST_UNIQUE_FLAG, (idx.isUnique() ? "YES" : "NO"));
      idxData.setValue(row, offset + COLUMN_IDX_TABLE_INDEXLIST_PK_FLAG, (idx.isPrimaryKeyIndex() ? "YES" : "NO"));
      idxData.setValue(row, offset + COLUMN_IDX_TABLE_INDEXLIST_COL_DEF, idx.getExpression());
      idxData.setValue(row, offset + COLUMN_IDX_TABLE_INDEXLIST_TYPE, idx.getIndexType());

      if (tableSpaceIndex > -1)
      {
        idxData.setValue(row, tableSpaceIndex, idx.getTablespace());
      }
      if (statusIndex > -1)
      {
        idxData.setValue(row, statusIndex, idx.getStatus());
      }
      if (remarksIndex > -1)
      {
        idxData.setValue(row, remarksIndex, idx.getComment());
      }
      idxData.getRow(row).setUserObject(idx);
    }
    SortDefinition sort = new SortDefinition(new int[] {0,1}, new boolean[] {true, true});
    idxData.sort(sort);
    idxData.resetStatus();
    return idxData;
  }

  /**
   * Returns a list of indexes defined for the given table.
   *
   * @param table the table to get the indexes for
   * @see #getTableIndexList(workbench.db.TableIdentifier, boolean, boolean)
   */
  @Override
  public List<IndexDefinition> getTableIndexList(TableIdentifier table, boolean includeUniqueConstraints)
  {
    return getTableIndexList(table, false, true, includeUniqueConstraints);
  }

  /**
   * Returns a list of unique indexes defined for the given table.
   *
   * @param table the table to get the indexes for
   * @see #getTableIndexList(workbench.db.TableIdentifier, boolean, boolean)
   */
  @Override
  public List<IndexDefinition> getUniqueIndexes(TableIdentifier table)
  {
    return getTableIndexList(table, true, true, false);
  }

  /**
   * Return a list of indexes for the given table.
   *
   * if the parameter checkPk is true, the primary key of the table is determined by
   * calling {@link #getPrimaryKey(workbench.db.TableIdentifier)}. If a PkDefinition is
   * found the corresponding index will be marked as such
   *
   * @param table         the table for which to return the indexes
   * @param uniqueOnly    if true only unique indexes are returned
   * @param checkPK       if true, the PK index will be searched and identified (if possible)
   *
   * @return the list of indexes
   * @see #getIndexInfo(workbench.db.TableIdentifier, boolean)
   * @see #processIndexResult(java.sql.ResultSet, workbench.db.PkDefinition, workbench.db.TableIdentifier)
   * @see #getPrimaryKey(workbench.db.TableIdentifier)
   * @see IndexDefinition#isPrimaryKeyIndex()
   */
  public List<IndexDefinition> getTableIndexList(TableIdentifier table, boolean uniqueOnly, boolean checkPK, boolean includeUniqueConstraints)
  {
    if (table == null) return new ArrayList<>();

    ResultSet idxRs = null;
    TableIdentifier tbl = table.createCopy();

    WbConnection conn = metaData.getWbConnection();
    tbl.adjustCase(conn);

    List<IndexDefinition> result = null;

    Savepoint sp = null;

    try
    {
      if (metaData.getDbSettings().useSavePointForDML())
      {
        sp = conn.setSavepoint();
      }

      PkDefinition pk = tbl.getPrimaryKey();
      if (pk == null && checkPK && !tbl.isPkInitialized())
      {
        pk = getPrimaryKey(tbl);
      }

      long start = System.currentTimeMillis();
      idxRs = getIndexInfo(tbl, uniqueOnly);
      result = processIndexResult(idxRs, pk, tbl);
      long duration = System.currentTimeMillis() - start;
      LogMgr.logDebug("JdbcIndexReader.getTableIndexList()", "Retrieving index information for table " + table.getTableExpression() + " took: " + duration + "ms");
    }
    catch (Exception e)
    {
      conn.rollback(sp);
      LogMgr.logWarning("JdbcIndexReader.getTableIndexInformation()", "Could not retrieve indexes", e);
      result = new ArrayList<>(0);
    }
    finally
    {
      conn.releaseSavepoint(sp);
      SqlUtil.closeResult(idxRs);
      indexInfoProcessed();
    }

    if (includeUniqueConstraints && uniqueConstraintReader != null)
    {
      uniqueConstraintReader.readUniqueConstraints(tbl, result, metaData.getWbConnection());
    }

    return result;
  }

  private boolean isSameTable(TableIdentifier tbl, String cat, String schema, String name)
  {
    if (tbl == null) return name == null;
    if (StringUtil.stringsAreNotEqual(name, tbl.getRawTableName())) return false;
    if (tbl.getCatalog() != null && cat != null)
    {
      if (StringUtil.stringsAreNotEqual(cat, tbl.getRawCatalog())) return false;
    }
    if (tbl.getSchema() != null && schema != null)
    {
      if (StringUtil.stringsAreNotEqual(schema, tbl.getRawSchema())) return false;
    }
    return true;
  }

  protected List<IndexDefinition> processIndexResult(ResultSet idxRs, PkDefinition pkIndex, TableIdentifier tbl)
    throws SQLException
  {
    // This will map an indexname to an IndexDefinition object
    // getIndexInfo() returns one row for each column
    // so the columns of the index are collected in the IndexDefinition
    HashMap<String, IndexDefinition> defs = new HashMap<>();

    boolean supportsDirection = metaData.getDbSettings().supportsSortedIndex();
    boolean ignoreZeroOrdinalPos = metaData.getDbSettings().ignoreIndexColumnWithOrdinalZero();

    boolean isPartitioned = false;

    boolean useColumnNames = metaData.getDbSettings().useColumnNameForMetadata();
    boolean checkTable = metaData.getDbSettings().checkIndexTable();

    Set<String> ignoredIndexes = CollectionUtil.caseInsensitiveSet();

    if (idxRs != null && Settings.getInstance().getDebugMetadataSql())
    {
      SqlUtil.dumpResultSetInfo("DatabaseMetaData.processIndexResult()", idxRs.getMetaData());
    }

    while (idxRs != null && idxRs.next())
    {
      String tableCat = useColumnNames ? idxRs.getString("TABLE_CAT"): idxRs.getString(1);
      String tableSchema = useColumnNames ? idxRs.getString("TABLE_SCHEM"): idxRs.getString(2);
      String tableName = useColumnNames ? idxRs.getString("TABLE_NAME"): idxRs.getString(3);

      boolean nonUniqueFlag = useColumnNames ? idxRs.getBoolean("NON_UNIQUE") : idxRs.getBoolean(4);
      String indexName = useColumnNames ? idxRs.getString("INDEX_NAME"): idxRs.getString(6);

      if (ignoredIndexes.contains(indexName))
      {
        continue;
      }

      if (checkTable && !isSameTable(tbl, tableCat, tableSchema, tableName))
      {
        ignoredIndexes.add(indexName);
        TableIdentifier owner = new TableIdentifier(tableCat, tableSchema, tableName);
        LogMgr.logInfo("JdbcIndexReader.processIndexResult()", "Ignoring index " + indexName + " because it belongs to " + owner.getFullyQualifiedName(metaData.getWbConnection()) + " and not to " + tbl.getFullyQualifiedName(metaData.getWbConnection()));
        continue;
      }

      if (idxRs.wasNull() || indexName == null) continue;

      int ordinal = useColumnNames ? idxRs.getInt("ORDINAL_POSITION") : idxRs.getInt(8);
      if (idxRs.wasNull())
      {
        ordinal = -1;
      }

      String colName = useColumnNames ? idxRs.getString("COLUMN_NAME") : idxRs.getString(9);

      if (ignoreZeroOrdinalPos && ordinal < 1)
      {
        LogMgr.logDebug("JdbcIndexReader.processIndexResult()", "Ignoring column " + colName + " for index " + indexName + " because ordinal_position was: " + ordinal);
        continue;
      }

      String dir = null;
      if (supportsDirection)
      {
        dir = useColumnNames ? idxRs.getString("ASC_OR_DESC") : idxRs.getString(10);
      }

      IndexDefinition def = defs.get(indexName);
      if (def == null)
      {
        if (partitionedFlagColumn != null)
        {
          isPartitioned = StringUtil.stringToBool(StringUtil.trim(idxRs.getString(partitionedFlagColumn)));
        }
        def = new IndexDefinition(tbl, indexName);
        def.setUnique(!nonUniqueFlag);
        def.setPartitioned(isPartitioned);
        if (metaData.getDbSettings().pkIndexHasTableName())
        {
          def.setPrimaryKeyIndex(indexName.equals(tbl.getRawTableName()));
        }
        defs.put(indexName, def);

        // OracleIndexReader returns the index type directly
        // so we need to check the type of the column
        int type = idxRs.getMetaData().getColumnType(7);
        if (type == Types.VARCHAR)
        {
          String typeName = idxRs.getString("TYPE");

          // this is a fix for Informix which claims the column is of type VARCHAR
          // but in reality it contains a numeric value
          int typeVal = StringUtil.getIntValue(typeName, Integer.MIN_VALUE);
          if (typeVal != Integer.MIN_VALUE)
          {
            def.setIndexType(metaData.getDbSettings().mapIndexType(type));
          }
          else
          {
            def.setIndexType(typeName);
          }
        }
        else if (SqlUtil.isNumberType(type))
        {
          int typeNr = useColumnNames ? idxRs.getInt("TYPE") : idxRs.getInt(7);
          if (!idxRs.wasNull())
          {
            def.setIndexType(metaData.getDbSettings().mapIndexType(typeNr));
          }
        }
      }
      def.addColumn(quoteIndexColumn(colName), dir);
      processIndexResultRow(idxRs, def, tbl);
    }

    Collection<IndexDefinition> indexes = defs.values();

    // first try to find the PK index by only checking the PkDefinition
    boolean pkFound = false;

    // Because isPkIndex() will check if the PK columns are part of the index
    // expression, that should only be called if the real PK index cannot be identified
    // through the passed PkDefinition.
    // Otherwise additional indexes defined on the PK columns will be reported as PK index
    // as well, and will be left out of the table's source
    // only one index should be marked as the PK index!
    if (pkIndex != null)
    {
      for (IndexDefinition index : indexes)
      {
        if (!index.isPrimaryKeyIndex())
        {
          boolean isPK = index.getName().equals(pkIndex.getPkIndexName());
          if (isPK)
          {
            index.setPrimaryKeyIndex(true);
            pkFound = true;
            break; // don't look any further. There can only be one PK index
          }
        }
      }
    }

    if (!pkFound)
    {
      for (IndexDefinition index : indexes)
      {
        if (!index.isPrimaryKeyIndex())
        {
          boolean isPK = isPkIndex(index, pkIndex);
          index.setPrimaryKeyIndex(isPK);
          if (isPK && pkIndex != null)
          {
            pkIndex.setPkIndexDefinition(index);
          }
          if (isPK) break;
        }
      }
    }

    processIndexList(indexes);
    return new ArrayList<>(indexes);
  }

  /**
   * Callback function for specialized index readers to process a single row
   * while retrieving the index information.
   *
   * This will be called by {@link #processIndexResult(ResultSet, PkDefinition, TableIdentifier)} for each row
   * of the passed result set.
   *
   * This is an empty definition, to be overwritten by descendant classes.
   *
   * @param rs       the index information ResultSet being processed. This is already positioned to the "current" row.
   * @param index    the index for which the information is processed
   * @param tbl      the table for which the indexes are retrieved.
   *
   * @throws SQLException
   */
  protected void processIndexResultRow(ResultSet rs, IndexDefinition index, TableIdentifier tbl)
    throws SQLException
  {
    // nothing to do
  }

  private boolean isPkIndex(IndexDefinition toCheck, PkDefinition pkIndex)
  {
    if (toCheck == null || pkIndex == null) return false;
    if (toCheck.getName().equals(pkIndex.getPkIndexName())) return true;

    // not the same name, check if they have the same definition (same columns at the same position)
    // note that this test will yield false positives for DBMS that allow multiple identical index expressions
    List<IndexColumn> checkCols = toCheck.getColumns();
    List<String> pkCols = pkIndex.getColumns();
    int count = pkCols.size();
    if (checkCols.size() != count) return false;
    for (int col=0; col<count; col++)
    {
      if (!checkCols.get(col).getColumn().equals(pkCols.get(col))) return false;
    }
    return true;
  }

  protected IndexDefinition findIndexByName(Collection<IndexDefinition> indexList, String toFind)
  {
    if (StringUtil.isEmptyString(toFind)) return null;
    if (CollectionUtil.isEmpty(indexList)) return null;
    for (IndexDefinition index : indexList)
    {
      if (index != null && index.getName().equalsIgnoreCase(toFind)) return index;
    }
    return null;
  }

  /**
   * Checks if the current DBMS supports returning a list of indexes.
   *
   * This method checks if a SQL was configured using {@link MetaDataSqlManager#getListIndexesSql()}
   *
   * @return true if such a list can be retrieved.
   */
  @Override
  public boolean supportsIndexList()
  {
    GetMetaDataSql sql = metaData.getMetaDataSQLMgr().getListIndexesSql();
    return sql !=null;
  }

  /**
   * Return all indexes defined for the given catalog and/or schema.
   *
   * The method checks for a configured SQL statement to retrieve the indexes.
   * If nothing is configured, an empty result is returned.
   * <br/><br/>
   * The configured SQL must return the following columns (in that order):<br/>
   *
   * <ol>
   * <li>index_name</li>
   * <li>index_schema</li>
   * <li>table_name</li>
   * <li>table_sxhema</li>
   * <li>table_catalog</li>
   * <li>index_type</li>
   * <li>is_unique</li>
   * <li>is_pk</li>
   * <li>index_def</li>
   * </ol>
   *
   * @param catalogPattern    the catalog to search, may be null.
   * @param schemaPattern     the schema to search, may be null.
   * @param tablePattern      the table for which indexes should be returned, may be null
   * @param indexNamePattern  the index name pattern to look for, may be null
   *
   * @return a list of indexes in the catalog/schema
   *
   * @see MetaDataSqlManager#getListIndexesSql()
   */
  @Override
  public List<IndexDefinition> getIndexes(String catalogPattern, String schemaPattern, String tablePattern, String indexNamePattern)
  {
    GetMetaDataSql sqlDef = metaData.getMetaDataSQLMgr().getListIndexesSql();
    if (sqlDef == null) return Collections.emptyList();

    catalogPattern = DbMetadata.cleanupWildcards(metaData.adjustSchemaNameCase(catalogPattern));
    schemaPattern = DbMetadata.cleanupWildcards(metaData.adjustSchemaNameCase(schemaPattern));
    tablePattern = DbMetadata.cleanupWildcards(metaData.adjustObjectnameCase(tablePattern));
    indexNamePattern = DbMetadata.cleanupWildcards(metaData.adjustObjectnameCase(indexNamePattern));

    sqlDef.setCatalog(catalogPattern);
    sqlDef.setSchema(schemaPattern);
    sqlDef.setBaseObjectName(tablePattern);
    sqlDef.setObjectName(indexNamePattern);

    String sql = sqlDef.getSql();
    if (Settings.getInstance().getDebugMetadataSql())
    {
      LogMgr.logInfo("JdbcIndexReader.getIndexes()", "Retrieving index list using:\n" + sql);
    }

    List<IndexDefinition> result = new ArrayList<>();
    Statement stmt = null;
    ResultSet rs = null;

    boolean hasStatus = supportsIndexStatus();

    Savepoint sp = null;
    try
    {
      if (metaData.getDbSettings().useSavePointForDML())
      {
        sp = metaData.getWbConnection().setSavepoint();
      }
      stmt = this.metaData.getWbConnection().createStatementForQuery();
      rs = stmt.executeQuery(sql);
      while (rs.next())
      {
        String idxName = StringUtil.rtrim(rs.getString("index_name"));
        String idxSchema = StringUtil.rtrim(rs.getString("index_schema"));
        String tableName = StringUtil.rtrim(rs.getString("table_name"));
        String tableSchema = StringUtil.rtrim(rs.getString("table_schema"));
        String tableCatalog = StringUtil.rtrim(rs.getString("table_catalog"));
        String idxType = StringUtil.rtrim(rs.getString("index_type"));
        String isUnique = StringUtil.rtrim(rs.getString("is_unique"));
        String isPK = StringUtil.rtrim(rs.getString("is_pk"));
        String def = rs.getString("index_def");
        String tbs = rs.getString("index_tablespace");
        String status = (hasStatus ? rs.getString("index_status") : null);

        TableIdentifier tbl = new TableIdentifier(tableCatalog, tableSchema, tableName);
        IndexDefinition idx = new IndexDefinition(tbl, idxName.trim());
        idx.setSchema(idxSchema);
        idx.setIndexType(idxType);
        idx.setTablespace(tbs);
        idx.setStatus(status);

        if (isUnique != null)
        {
          idx.setUnique(StringUtil.stringToBool(isUnique));
        }
        if (isPK != null)
        {
          idx.setPrimaryKeyIndex(StringUtil.stringToBool(isPK));
        }
        if (def != null)
        {
          def = StringUtil.removeBrackets(def);
          List<String> colNames = StringUtil.stringToList(def, ",", true, true, false, true);

          for (String name : colNames)
          {
            idx.addColumn(name, null);
          }
        }
        result.add(idx);
      }
    }
    catch (Exception e)
    {
      metaData.getWbConnection().rollback(sp);
      LogMgr.logError("JdbcIndexReader.getIndexes()", "Error retrieving index list using: " + sql, e);
    }
    finally
    {
      metaData.getWbConnection().releaseSavepoint(sp);
      SqlUtil.closeAll(rs, stmt);
    }
    processIndexList(result);
    Collections.sort(result, IndexDefinition.getNameSorter());
    return result;
  }

}
