/*
 * JdbcTableDefinitionReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.derby.DerbyColumnEnhancer;
import workbench.db.firebird.FirebirdColumnEnhancer;
import workbench.db.h2database.H2ColumnEnhancer;
import workbench.db.hana.HanaColumnEnhancer;
import workbench.db.hsqldb.HsqlColumnEnhancer;
import workbench.db.ibm.Db2ColumnEnhancer;
import workbench.db.ibm.InformixColumnEnhancer;
import workbench.db.mssql.SqlServerColumnEnhancer;
import workbench.db.mssql.SqlServerUtil;
import workbench.db.mysql.MySQLColumnEnhancer;
import workbench.db.nuodb.NuoDbColumnEnhancer;
import workbench.db.postgres.PostgresColumnEnhancer;
import workbench.db.progress.OpenEdgeColumnEnhancer;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class JdbcTableDefinitionReader
  implements TableDefinitionReader
{
  protected final WbConnection dbConnection;

  public JdbcTableDefinitionReader(WbConnection conn)
  {
    dbConnection = conn;
  }

  /**
   * Return the definition of the given table.
   * <br/>
   * To display the columns for a table in a DataStore create an instance of {@link TableColumnsDatastore}.
   *
   * @param table The table for which the definition should be retrieved
   * @param typeResolver the DataTypeResolver to be used. If null, it will be taken from the connection
   *
   * @throws SQLException
   * @return the definition of the table.
   * @see TableColumnsDatastore
   */
  @Override
  public List<ColumnIdentifier> getTableColumns(TableIdentifier table, DataTypeResolver typeResolver)
    throws SQLException
  {
    DbSettings dbSettings = dbConnection.getDbSettings();
    DbMetadata dbmeta = dbConnection.getMetadata();

    // apparently some drivers (e.g. for DB2/HOST) do support column names in the ResultSet
    // other drivers (e.g. MonetDB) do not return the information when the column index is used.
    // Therefor we need a switch for this.
    boolean useColumnNames = dbSettings.useColumnNameForMetadata();

    String tablename = SqlUtil.removeObjectQuotes(table.getTableName());
    String schema = SqlUtil.removeObjectQuotes(table.getSchema());
    String catalog = SqlUtil.removeObjectQuotes(table.getCatalog());

    if (dbConnection.getDbSettings().supportsMetaDataWildcards())
    {
      tablename = SqlUtil.escapeUnderscore(tablename, dbConnection);
    }

    if (dbConnection.getDbSettings().supportsMetaDataSchemaWildcards())
    {
      schema = SqlUtil.escapeUnderscore(schema, dbConnection);
    }

    if (dbConnection.getDbSettings().supportsMetaDataCatalogWildcards())
    {
      catalog = SqlUtil.escapeUnderscore(catalog, dbConnection);
    }

    ResultSet rs = null;
    List<ColumnIdentifier> columns = new ArrayList<>();

    PkDefinition primaryKey = table.getPrimaryKey();
    Set<String> primaryKeyColumns = CollectionUtil.caseInsensitiveSet();

    if (primaryKey != null)
    {
      primaryKeyColumns.addAll(primaryKey.getColumns());
    }

    try
    {
			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logDebug("JdbcTableDefinitionReader.getTableColumns()",
          "Calling getColumns() using: catalog="+ catalog + ", schema=" + schema + ", table=" + tablename);
			}

      long start = System.currentTimeMillis();
      rs = dbmeta.getJdbcMetaData().getColumns(catalog, schema, tablename, "%");
      long duration = System.currentTimeMillis() - start;

      ResultSetMetaData rsmeta = rs.getMetaData();

      if (Settings.getInstance().getDebugMetadataSql())
      {
        LogMgr.logDebug("JdbcTableDefinitionReader.getTableColumns()", "Calling getColumns() took: " + duration + "ms");
        String fqn = SqlUtil.fullyQualifiedName(dbConnection, table);
        SqlUtil.dumpResultSetInfo("JdbcTableDefinitionReader.getColumns() for " + fqn, rsmeta);
      }

      boolean jdbc4 = false;
      boolean jdbc41 = false;
      if (rsmeta.getColumnCount() > 22)
      {
        String name = rsmeta.getColumnName(23);

        // HSQLDB 1.8 returns 23 columns, but is not JDBC4, so I need to check for the name as well.
        jdbc4 = name.equals("IS_AUTOINCREMENT");
      }

      if (rsmeta.getColumnCount() > 23)
      {
        String name = rsmeta.getColumnName(24);
        jdbc41 = name.equals("IS_GENERATEDCOLUMN");
      }

      while (rs.next())
      {
        String colName = StringUtil.trim(useColumnNames ? rs.getString("COLUMN_NAME") : rs.getString(4));
        int sqlType = useColumnNames ? rs.getInt("DATA_TYPE") : rs.getInt(5);
        String typeName = StringUtil.trim(useColumnNames ? rs.getString("TYPE_NAME") : rs.getString(6));

        sqlType = typeResolver.fixColumnType(sqlType, typeName);
        ColumnIdentifier col = new ColumnIdentifier(dbmeta.quoteObjectname(colName), sqlType);

        int size = useColumnNames ? rs.getInt("COLUMN_SIZE") : rs.getInt(7);
        int digits = -1;
        try
        {
          digits = useColumnNames ? rs.getInt("DECIMAL_DIGITS") : rs.getInt(9);
        }
        catch (Exception e)
        {
          digits = -1;
        }
        if (rs.wasNull()) digits = -1;

        String remarks = getString(rs, "REMARKS", 12, useColumnNames);
        String defaultValue = getString(rs, "COLUMN_DEF", 13, useColumnNames);

        if (defaultValue != null && dbSettings.trimDefaults())
        {
          defaultValue = defaultValue.trim();
        }

        int position = -1;
        try
        {
          position = useColumnNames ? rs.getInt("ORDINAL_POSITION") : rs.getInt(17);
        }
        catch (Exception e)
        {
          LogMgr.logWarning("JdbcTableDefinitionReader.getTableColumns()", "JDBC driver does not suport ORDINAL_POSITION column for getColumns()", e);
          position = -1;
        }

        String nullable = getString(rs, "IS_NULLABLE", 18, useColumnNames);

        String increment = "NO";
        if (jdbc4)
        {
          increment = useColumnNames ? rs.getString("IS_AUTOINCREMENT") : rs.getString(23);
        }
        boolean autoincrement = StringUtil.stringToBool(increment);

        String display = typeResolver.getSqlTypeDisplay(typeName, sqlType, size, digits);

        if (dbConnection.getMetadata().isSqlServer() && dbSettings.fixSqlServerAutoincrement())
        {
					// The Microsoft JDBC Driver does not return the autoincrement attribute correctly for identity columns.
          // (And they refuse to fix this: http://social.msdn.microsoft.com/Forums/en/sqldataaccess/thread/20df12f3-d1bf-4526-9daa-239a83a8e435)
          // This hack works around Microsoft's ignorance regarding Java and JDBC
          autoincrement = display.contains("identity");
        }

        boolean isGenerated = false;
        if (jdbc41)
        {
          String generated = useColumnNames ? rs.getString("IS_GENERATEDCOLUMN") : rs.getString(24);
          isGenerated = StringUtil.stringToBool(generated);
          col.setIsGenerated(Boolean.valueOf(isGenerated));
        }

        col.setDbmsType(display);
        col.setIsAutoincrement(autoincrement);
        col.setIsPkColumn(primaryKeyColumns.contains(colName));
        col.setIsNullable("YES".equalsIgnoreCase(nullable));
        col.setDefaultValue(defaultValue);
        col.setComment(remarks);
        col.setColumnSize(size);
        col.setDecimalDigits(digits);
        col.setPosition(position);
        columns.add(col);
      }
    }
    finally
    {
      SqlUtil.closeResult(rs);
    }

		// Some JDBC drivers (e.g. Ingres) do not return the columns in the correct order, so we need to make sure they are sorted correctly
    // for any DBMS returning them in the correct order, this shouldn't make a difference.
    ColumnIdentifier.sortByPosition(columns);

    return columns;
  }

  private String getString(ResultSet rs, String colName, int colIndex, boolean useColumnNames)
  {
    try
    {
      return useColumnNames ? rs.getString(colName) : rs.getString(colIndex);
    }
    catch (Exception ex)
    {
      LogMgr.logError("JdbcTableDefinitionReader.getString()", "Could not read column " + colName, ex);
    }
    return null;
  }

  /**
   * Return the definition of the given table.
   * <br/>
   * To display the columns for a table in a DataStore create an instance of {@link TableColumnsDatastore}.
   *
   * @param toRead         The table for which the definition should be retrieved
   * @param readPrimaryKey If true the PK information for the table will also be retrieved
   *
   * @throws SQLException
   * @return the definition of the table.
   * @see TableColumnsDatastore
   */
  @Override
  public TableDefinition getTableDefinition(TableIdentifier toRead, boolean readPrimaryKey)
    throws SQLException
  {
    if (toRead == null) return null;

    TableIdentifier table = toRead.createCopy();
    table.adjustCase(dbConnection);

    String catalog = SqlUtil.removeObjectQuotes(table.getCatalog());
    String schema = SqlUtil.removeObjectQuotes(table.getSchema());
    String tablename = SqlUtil.removeObjectQuotes(table.getTableName());

    DbMetadata meta = dbConnection.getMetadata();
    if (schema == null)
    {
      schema = meta.getCurrentSchema();
      table.setSchema(schema);
    }

    if (catalog == null)
    {
      catalog = meta.getCurrentCatalog();
      table.setCatalog(catalog);
    }

    TableIdentifier retrieve = table;

    if (dbConnection.getDbSettings().isSynonymType(table.getType()))
    {
      TableIdentifier id = table.getRealTable();
      if (id == null) id = meta.getSynonymTable(catalog, schema, tablename);

      if (id != null)
      {
        schema = id.getSchema();
        tablename = id.getTableName();
        catalog = null;
        retrieve = table.createCopy();
        retrieve.setSchema(schema);
        retrieve.parseTableIdentifier(tablename);
        retrieve.setCatalog(null);
      }
    }

    if (readPrimaryKey)
    {
      PkDefinition pk = meta.getIndexReader().getPrimaryKey(retrieve);
      retrieve.setPrimaryKey(pk);
    }

    List<ColumnIdentifier> columns = getTableColumns(retrieve, meta.getDataTypeResolver());

    retrieve.setNewTable(false);
    TableDefinition result = new TableDefinition(retrieve, columns);

    ColumnDefinitionEnhancer columnEnhancer = getColumnEnhancer(dbConnection);
    if (columnEnhancer != null)
    {
      columnEnhancer.updateColumnDefinition(result, dbConnection);
    }

    return result;
  }

  private ColumnDefinitionEnhancer getColumnEnhancer(WbConnection con)
  {
    if (con == null) return null;
    DbMetadata meta = con.getMetadata();
    if (meta == null) return null;

    if (meta.isPostgres())
    {
      return new PostgresColumnEnhancer();
    }
    if (meta.isH2())
    {
      return new H2ColumnEnhancer();
    }
    if (meta.isApacheDerby())
    {
      return new DerbyColumnEnhancer();
    }
    if (meta.isMySql())
    {
      return new MySQLColumnEnhancer();
    }
    if (con.getDbId().equals("db2"))
    {
      return new Db2ColumnEnhancer();
    }
    if (con.getDbId().equals("informix_dynamic_server"))
    {
      return new InformixColumnEnhancer();
    }
    if (meta.isSqlServer() && SqlServerUtil.isSqlServer2000(con))
    {
      return new SqlServerColumnEnhancer();
    }
    if (meta.isFirebird())
    {
      return new FirebirdColumnEnhancer();
    }
    if (con.getDbId().equals("nuodb"))
    {
      return new NuoDbColumnEnhancer();
    }
    if (meta.isHsql() && JdbcUtils.hasMinimumServerVersion(con, "2.0"))
    {
      return new HsqlColumnEnhancer();
    }
    if (meta.getDbId().equals(DbMetadata.DBID_OPENEDGE))
    {
      return new OpenEdgeColumnEnhancer();
    }
    if (meta.getDbId().equals(DbMetadata.DBID_HANA))
    {
      return new HanaColumnEnhancer();
    }
    return null;
  }
}
