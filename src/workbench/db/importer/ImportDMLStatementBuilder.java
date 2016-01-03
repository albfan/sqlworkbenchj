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
package workbench.db.importer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import workbench.log.LogMgr;

import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.DmlExpressionBuilder;
import workbench.db.JdbcUtils;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.mssql.SqlServerUtil;

import workbench.util.CollectionUtil;
import workbench.util.StringUtil;

/**
 * A class to build the INSERT, "Upsert" or Insert/Ignore statements for a DataImporter.
 *
 * @author Thomas Kellerer
 */
class ImportDMLStatementBuilder
{
  private final WbConnection dbConn;
  private final TableIdentifier targetTable;
	private final List<ColumnIdentifier> targetColumns;
  private List<ColumnIdentifier> keyColumns;
  private final Set<String> upsertRequiresPK = CollectionUtil.caseInsensitiveSet(DbMetadata.DBID_CUBRID, DbMetadata.DBID_MYSQL, DbMetadata.DBID_HANA, DbMetadata.DBID_H2, DbMetadata.DBID_SQL_ANYWHERE, DbMetadata.DBID_SQLITE);
  private final Set<String> ignoreRequiresPK = CollectionUtil.caseInsensitiveSet(DbMetadata.DBID_CUBRID, DbMetadata.DBID_MYSQL, DbMetadata.DBID_SQL_ANYWHERE, DbMetadata.DBID_SQLITE);

  ImportDMLStatementBuilder(WbConnection connection, TableIdentifier target, List<ColumnIdentifier> columns, ColumnFilter filter, boolean adjustColumnNameCase)
  {
    dbConn = connection;
    targetTable = target;
    targetColumns = createColumnList(columns, filter, adjustColumnNameCase);
  }

  /**
   * Returns true if a "native" insert ignore is supported.
   *
   * For some DBMS (e.g. DB2 or HSQLDB) we use a MERGE statement without the "WHEN MATCHED" part to
   * simulate an insertIgnore mode.
   *
   * However when insert/update is used by the DataImporter it will try to use an insertIgnore statement
   * followed by an UPDATE statement if available.
   *
   * @return if the DBMS has a native insertIgnore mode (rather than simulating one using a MERGE)
   */
  boolean hasNativeInsertIgnore()
  {
    if (dbConn.getMetadata().isOracle() && JdbcUtils.hasMinimumServerVersion(dbConn, "11.2") ) return true;
    if (dbConn.getMetadata().isPostgres() && JdbcUtils.hasMinimumServerVersion(dbConn, "9.5")) return true;
    if (dbConn.getDbId().equals(DbMetadata.DBID_SQLITE)) return true;
    if (dbConn.getDbId().equals(DbMetadata.DBID_SQL_ANYWHERE) && JdbcUtils.hasMinimumServerVersion(dbConn, "10.0")) return true;

    return false;
  }

  /**
   * Returns true if the DBMS supports an "insert ignore" kind of statement.
   *
   * This is slightly different to {@link #hasNativeInsertIgnore()} which is a bit more restrictive.
   * For SQL Server or DB2 {@link #createInsertIgnore()} will create a <tt>MERGE</tt> statement
   * without an "WHEN MATCHED" clause, which might be less efficient than a "native" insert ignore statement.
   *
   * @param dbConn the DBMS to check
   * @return true if the DBMS supports some kine of "Insert but ignore unique key violations" statement.
   */
  static boolean supportsInsertIgnore(WbConnection dbConn)
  {
    if (dbConn == null) return false;

    if (dbConn.getMetadata().isPostgres() && JdbcUtils.hasMinimumServerVersion(dbConn, "9.5")) return true;
    if (dbConn.getMetadata().isOracle() && JdbcUtils.hasMinimumServerVersion(dbConn, "11.2") ) return true;
    if (dbConn.getMetadata().isDB2LuW()) return true;
    if (dbConn.getMetadata().isSqlServer() && SqlServerUtil.isSqlServer2008(dbConn)) return true;
    if (dbConn.getDbId().equals(DbMetadata.DBID_DB2_ZOS) && JdbcUtils.hasMinimumServerVersion(dbConn, "10.0")) return true;
    if (dbConn.getDbId().equals(DbMetadata.DBID_CUBRID)) return true;
    if (dbConn.getMetadata().isHsql() && JdbcUtils.hasMinimumServerVersion(dbConn, "2.0")) return true;
    if (dbConn.getMetadata().isMySql()) return true;
    if (dbConn.getDbId().equals(DbMetadata.DBID_SQLITE)) return true;
    if (dbConn.getDbId().equals(DbMetadata.DBID_SQL_ANYWHERE) && JdbcUtils.hasMinimumServerVersion(dbConn, "10.0")) return true;

    return false;
  }


  /**
   * Returns true if the DBMS supports an "UPSERT" (insert, if exists, then update) kind of statement.
   *
   * Some DBMS have a native "upsert" statement, for other DMBS (e.g. Oracle or SQL Server) a MERGE statement will be used.
   *
   * @param dbConn the DBMS to check
   * @return true if the DBMS supports some kine of "upsert" statement.
   */
  static boolean supportsUpsert(WbConnection connection)
  {
    if (connection == null) return false;

    if (connection.getMetadata().isPostgres() && JdbcUtils.hasMinimumServerVersion(connection, "9.5")) return true;
    if (connection.getMetadata().isFirebird() && JdbcUtils.hasMinimumServerVersion(connection, "2.1")) return true;
    if (connection.getMetadata().isOracle()) return true;
    if (connection.getMetadata().isDB2LuW()) return true;
    if (connection.getMetadata().isSqlServer() && SqlServerUtil.isSqlServer2008(connection)) return true;
    if (connection.getDbId().equals(DbMetadata.DBID_DB2_ZOS) && JdbcUtils.hasMinimumServerVersion(connection, "10.0")) return true;
    if (connection.getDbId().equals(DbMetadata.DBID_HANA)) return true;
    if (connection.getDbId().equals(DbMetadata.DBID_CUBRID)) return true;
    if (connection.getMetadata().isHsql() && JdbcUtils.hasMinimumServerVersion(connection, "2.0")) return true;
    if (connection.getMetadata().isH2()) return true;
    if (connection.getMetadata().isMySql()) return true;
    if (connection.getDbId().equals(DbMetadata.DBID_SQLITE)) return true;
    if (connection.getDbId().equals(DbMetadata.DBID_SQL_ANYWHERE) && JdbcUtils.hasMinimumServerVersion(connection, "10.0")) return true;

    return false;
  }


  /**
   * Returns true if the current DBMS supports an "UPSERT" statement.
   *
   * If no key columns have been defined, false is returned.
   *
   * Some DBMS als require a primary key constraint to be defined (rather than just a unique constraint).
   *
   * @return
   * @see #setKeyColumns(java.util.List)
   * @see #supportsUpsert(workbench.db.WbConnection)
   */
  boolean supportsUpsert()
  {
    if (upsertRequiresPK.contains(dbConn.getDbId()))
    {
      boolean hasPK = hasRealPK();
      if (!hasPK)
      {
        LogMgr.logInfo("ImportDMLStatementBuilder.supportsUpsert()", "Cannot use upsert without a primary key.");
      }
      return hasPK;
    }
    return CollectionUtil.isNonEmpty(keyColumns) && supportsUpsert(this.dbConn);
  }


  /**
   * Verifies if the given ImportMode is supported by the current DBMS.
   *
   * Some modes require a real primary key constraint. Any mode involving updating rows
   * will require the primary key (or unique) columns to be defined before calling this method.
   *
   * @param mode  the mode to check
   * @return true if the mode is supported
   *
   * @see #setKeyColumns(java.util.List)
   */
  boolean isModeSupported(ImportMode mode)
  {
    switch (mode)
    {
      case insertIgnore:
        if (ignoreRequiresPK.contains(dbConn.getDbId()))
        {
          boolean hasPK = hasRealPK();
          if (!hasPK)
          {
            LogMgr.logInfo("ImportDMLStatementBuilder.isModeSupported()", "Cannot use insertIgnore without a primary key.");
          }
          return hasPK;
        }
        return supportsInsertIgnore(dbConn);
      case insertUpdate:
      case updateInsert:
      case upsert:
        return supportsUpsert();
    }
    return false;
  }

  /**
   * Define the key columns to be used for updating rows.
   *
   * The columns do not have to match the primary key of the table, but for some DBMS and import modes this is required
   * (e.g. MySQL can not do an InsertIgnore if no primary key is defined)
   *
   * @param keys   the columns to be used as a unique key
   */
  void setKeyColumns(List<ColumnIdentifier> keys)
  {
    keyColumns = new ArrayList<>(2);
    if (keys == null)
    {
      return;
    }
    for (ColumnIdentifier col : keys)
    {
      keyColumns.add(col.createCopy());
    }
  }

  /**
   * Creates a plain INSERT statement.
   *
   * The alternate insert statement can be used to enable special DBMS features.
   * e.g. enabling direct path inserts for Oracle using: <tt>INSERT /&#42;+ append &#42;/ INTO</tt>
   *
   * @param columnConstants  constant value definitions for some columns
   * @param insertSqlStart   an alternate insert statement.
   * @return a SQL statement suitable used for a PreparedStatement
   */
	String createInsertStatement(ConstantColumnValues columnConstants, String insertSqlStart)
  {
    DmlExpressionBuilder builder = DmlExpressionBuilder.Factory.getBuilder(dbConn);
		StringBuilder text = new StringBuilder(targetColumns.size() * 50);
		StringBuilder parms = new StringBuilder(targetColumns.size() * 20);

		String sql = (insertSqlStart != null ? insertSqlStart : dbConn.getDbSettings().getInsertForImport());
		if (StringUtil.isNonBlank(sql))
		{
			text.append(sql);
			text.append(' ');
		}
		else
		{
			text.append("INSERT INTO ");
		}
		text.append(targetTable.getFullyQualifiedName(dbConn));
		text.append(" (");

		DbMetadata meta = dbConn.getMetadata();

		int colIndex = 0;
		for (int i=0; i < getColCount(); i++)
		{
			ColumnIdentifier col = this.targetColumns.get(i);

			if (colIndex > 0)
			{
				text.append(',');
				parms.append(',');
			}

			String colname = col.getDisplayName();
			colname = meta.quoteObjectname(colname);
			text.append(colname);

			parms.append(builder.getDmlExpression(col));
			colIndex ++;
		}

		if (columnConstants != null)
		{
			int cols = columnConstants.getColumnCount();
			for (int i=0; i < cols; i++)
			{
				text.append(',');
				text.append(columnConstants.getColumn(i).getColumnName());
				parms.append(',');
				if (columnConstants.isFunctionCall(i))
				{
					parms.append(columnConstants.getFunctionLiteral(i));
				}
				else
				{
					parms.append('?');
				}
			}
		}
		text.append(") VALUES (");
		text.append(parms);
		text.append(')');

		return text.toString();
  }

  String createInsertIgnore(ConstantColumnValues columnConstants, String insertSqlStart)
  {
    if (dbConn.getMetadata().isPostgres() && JdbcUtils.hasMinimumServerVersion(dbConn, "9.5"))
    {
      return createPostgresUpsert(columnConstants, insertSqlStart, true);
    }
    if (dbConn.getMetadata().isMySql())
    {
      return createMySQLUpsert(columnConstants, insertSqlStart, true);
    }
    if (dbConn.getDbId().equals(DbMetadata.DBID_CUBRID))
    {
      return createMySQLUpsert(columnConstants, insertSqlStart, true);
    }
    if (dbConn.getMetadata().isOracle())
    {
      return createOracleInsertIgnore(columnConstants);
    }
    if (dbConn.getMetadata().isHsql())
    {
      return createHSQLUpsert(columnConstants, true);
    }
    if (dbConn.getMetadata().isDB2LuW())
    {
      return createDB2LuWUpsert(columnConstants, true);
    }
    if (dbConn.getDbId().equals(DbMetadata.DBID_DB2_ZOS))
    {
      return createDB2zOSUpsert(columnConstants, true);
    }
    if (dbConn.getMetadata().isSqlServer())
    {
      return createSqlServerUpsert(columnConstants, true);
    }
    if (dbConn.getDbId().equals(DbMetadata.DBID_SQLITE))
    {
      return createInsertStatement(columnConstants, "INSERT OR IGNORE ");
    }
    if (dbConn.getDbId().equals(DbMetadata.DBID_SQL_ANYWHERE))
    {
      return createSQLAnywhereStatement(columnConstants, true);
    }
    return null;
  }

  String createUpsertStatement(ConstantColumnValues columnConstants, String insertSqlStart)
  {
    if (dbConn.getMetadata().isPostgres() && JdbcUtils.hasMinimumServerVersion(dbConn, "9.5"))
    {
      return createPostgresUpsert(columnConstants, insertSqlStart, false);
    }
    if (dbConn.getMetadata().isMySql())
    {
      return createMySQLUpsert(columnConstants, insertSqlStart, false);
    }
    if (dbConn.getMetadata().isH2())
    {
      return createH2Upsert(columnConstants);
    }
    if (dbConn.getMetadata().isHsql())
    {
      return createHSQLUpsert(columnConstants, false);
    }
    if (dbConn.getMetadata().isFirebird())
    {
      return createFirebirdUpsert(columnConstants);
    }
    if (dbConn.getMetadata().isDB2LuW())
    {
      return createDB2LuWUpsert(columnConstants, false);
    }
    if (dbConn.getDbId().equals(DbMetadata.DBID_DB2_ZOS))
    {
      return createDB2zOSUpsert(columnConstants, false);
    }
    if (dbConn.getDbId().equals(DbMetadata.DBID_HANA))
    {
      return createHanaUpsert(columnConstants);
    }
    if (dbConn.getDbId().equals(DbMetadata.DBID_CUBRID))
    {
      return createMySQLUpsert(columnConstants, null, false);
    }
    if (dbConn.getMetadata().isSqlServer())
    {
      return createSqlServerUpsert(columnConstants, false);
    }
    if (dbConn.getMetadata().isOracle())
    {
      return createOracleMerge(columnConstants);
    }
    if (dbConn.getDbId().equals(DbMetadata.DBID_SQLITE))
    {
      return createInsertStatement(columnConstants, "INSERT OR REPLACE ");
    }
    if (dbConn.getDbId().equals(DbMetadata.DBID_SQL_ANYWHERE))
    {
      return createSQLAnywhereStatement(columnConstants, false);
    }
    return null;
  }

  private String createSQLAnywhereStatement(ConstantColumnValues columnConstants, boolean useIgnore)
  {
    String insert = createInsertStatement(columnConstants, null);
    if (useIgnore)
    {
      insert = insert.replace(") VALUES (", ") ON EXISTING SKIP VALUES (");
    }
    else
    {
      insert = insert.replace(") VALUES (", ") ON EXISTING UPDATE VALUES (");
    }
    return insert;
  }

  private String createOracleInsertIgnore(ConstantColumnValues columnConstants)
  {
    String start = "INSERT /*+ IGNORE_ROW_ON_DUPKEY_INDEX (";
    start += targetTable.getRawTableName() + " (";
    for (int i=0; i < keyColumns.size(); i++)
    {
      if (i > 0) start += ",";
      String colname = keyColumns.get(i).getDisplayName();
      start += colname;
    }
    start += ")) */ INTO ";
    return createInsertStatement(columnConstants, start);
  }

  private String createHanaUpsert(ConstantColumnValues columnConstants)
  {
    if (CollectionUtil.isEmpty(keyColumns)) return null;

    String insert = createInsertStatement(columnConstants, "UPSERT ");
    insert += " WITH PRIMARY KEY";
    return insert;
  }

  private String createPostgresUpsert(ConstantColumnValues columnConstants, String insertSqlStart, boolean useIgnore)
  {
    if (CollectionUtil.isEmpty(keyColumns)) return null;

    String insert = createInsertStatement(columnConstants, insertSqlStart);
    insert += "\nON CONFLICT (";
		DbMetadata meta = dbConn.getMetadata();
    for (int i=0; i < keyColumns.size(); i++)
    {
      if (i > 0) insert += ",";
      String colname = keyColumns.get(i).getDisplayName();
      colname = meta.quoteObjectname(colname);
      insert += colname;
    }
    if (useIgnore)
    {
      insert += ")\nDO NOTHING";
    }
    else
    {
      insert += ")\nDO UPDATE\n  SET ";
      for (int i=0; i < targetColumns.size(); i++)
      {
        if (i > 0) insert += ",\n      ";
        String colname = targetColumns.get(i).getDisplayName();
        colname = meta.quoteObjectname(colname);
        insert += colname + " = EXCLUDED." + colname;
      }
    }
    return insert;
  }

  private String createH2Upsert(ConstantColumnValues columnConstants)
  {
    String insert = createInsertStatement(columnConstants, null);
    insert = insert.replace("INSERT INTO", "MERGE INTO");
    return insert;
  }

  private String createFirebirdUpsert(ConstantColumnValues columnConstants)
  {
    String insert = createInsertStatement(columnConstants, null);
    insert = insert.replace("INSERT INTO", "UPDATE OR INSERT INTO");

		DbMetadata meta = dbConn.getMetadata();
    if (CollectionUtil.isNonEmpty(keyColumns))
    {
      insert += "\nMATCHING (";
      for (int i = 0; i < keyColumns.size(); i++)
      {
        if (i > 0) insert += ",";
        String colname = keyColumns.get(i).getDisplayName();
        colname = meta.quoteObjectname(colname);
        insert += colname;
      }
      insert += ")";
    }
    return insert;
  }

  private String createHSQLUpsert(ConstantColumnValues columnConstants, boolean insertOnly)
  {
    return createStandardMerge(columnConstants, insertOnly, "USING ");
  }

  private String createDB2LuWUpsert(ConstantColumnValues columnConstants, boolean insertOnly)
  {
    return createStandardMerge(columnConstants, insertOnly, "USING TABLE");
  }

  private String createDB2zOSUpsert(ConstantColumnValues columnConstants, boolean insertOnly)
  {
    return createStandardMerge(columnConstants, insertOnly, "USING ");
  }

  private String createSqlServerUpsert(ConstantColumnValues columnConstants, boolean insertOnly)
  {
    return createStandardMerge(columnConstants, insertOnly, "USING ") + ";";
  }

  private String createStandardMerge(ConstantColumnValues columnConstants, boolean insertOnly, String usingKeyword)
  {
		StringBuilder text = new StringBuilder(targetColumns.size() * 50);

  	text.append("MERGE INTO ");
		text.append(targetTable.getFullyQualifiedName(dbConn));
		text.append(" AS tg\n" + usingKeyword + "(\n  VALUES (");

		DbMetadata meta = dbConn.getMetadata();

		int colIndex = 0;
		for (int i=0; i < getColCount(); i++)
		{
			if (colIndex > 0) text.append(',');

      if (columnConstants != null && columnConstants.isFunctionCall(i))
      {
        text.append(columnConstants.getFunctionLiteral(i));
      }
      else
      {
        text.append('?');
      }
			colIndex ++;
		}
    text.append(")\n) AS vals (");
		colIndex = 0;
		for (int i=0; i < getColCount(); i++)
		{
			if (colIndex > 0) text.append(',');
      String colname = targetColumns.get(i).getDisplayName();
      colname = meta.quoteObjectname(colname);
      text.append(colname);
      colIndex ++;
		}
    text.append(")\n  ON ");
    colIndex = 0;

    for (int i=0; i < keyColumns.size(); i++)
    {
      if (colIndex > 0) text.append(" AND ");
      String colname = keyColumns.get(i).getDisplayName();
      colname = meta.quoteObjectname(colname);
      text.append("tg.");
      text.append(colname);
      text.append(" = vals.");
      text.append(colname);
    }
    appendMergeMatchSection(text, insertOnly);
    return text.toString();
  }

  private void appendMergeMatchSection(StringBuilder text, boolean insertOnly)
  {
    DbMetadata meta = dbConn.getMetadata();
    int colIndex = 0;

    if (!insertOnly)
    {
      text.append("\nWHEN MATCHED THEN UPDATE\n  SET ");
      for (int i=0; i < getColCount(); i++)
      {
        ColumnIdentifier col = this.targetColumns.get(i);
        if (isKeyColumn(col)) continue;

        String colname = targetColumns.get(i).getDisplayName();
        colname = meta.quoteObjectname(colname);

        if (colIndex > 0) text.append(",\n      ");
        text.append("tg.");
        text.append(colname);
        text.append(" = vals.");
        text.append(colname);
        colIndex ++;
      }
    }

    StringBuilder insertCols = new StringBuilder(targetColumns.size() * 20);
    StringBuilder valueCols = new StringBuilder(targetColumns.size() * 20);

		colIndex = 0;
		for (int i=0; i < getColCount(); i++)
		{
      String colname = targetColumns.get(i).getDisplayName();
      colname = meta.quoteObjectname(colname);

			if (colIndex > 0)
      {
        insertCols.append(", ");
        valueCols.append(", ");
      }
      insertCols.append(colname);
      valueCols.append("vals.");
      valueCols.append(colname);
			colIndex ++;
		}
    text.append("\nWHEN NOT MATCHED THEN INSERT\n  (");
    text.append(insertCols);
    text.append(")\nVALUES\n  (");
    text.append(valueCols);
    text.append(")");
  }

  private String createOracleMerge(ConstantColumnValues columnConstants)
  {
		StringBuilder text = new StringBuilder(targetColumns.size() * 50);

  	text.append("MERGE INTO ");
		text.append(targetTable.getFullyQualifiedName(dbConn));
		text.append(" tg\n USING (\n  SELECT ");

		DbMetadata meta = dbConn.getMetadata();

		int colIndex = 0;
		for (int i=0; i < getColCount(); i++)
		{
			if (colIndex > 0) text.append(',');

      if (columnConstants != null && columnConstants.isFunctionCall(i))
      {
        text.append(columnConstants.getFunctionLiteral(i));
      }
      else
      {
        text.append('?');
      }
      String colname = targetColumns.get(i).getDisplayName();
      text.append(" AS ");
      text.append(meta.quoteObjectname(colname));
			colIndex ++;
		}
    text.append(" FROM DUAL\n) vals ON (");

    colIndex = 0;
    for (int i=0; i < keyColumns.size(); i++)
    {
      if (colIndex > 0) text.append(" AND ");
      String colname = keyColumns.get(i).getDisplayName();
      colname = meta.quoteObjectname(colname);
      text.append("tg.");
      text.append(colname);
      text.append(" = vals.");
      text.append(colname);
    }
    text.append(')');
    appendMergeMatchSection(text, false);
		return text.toString();
  }

  private boolean isKeyColumn(ColumnIdentifier col)
  {
    if (col.isPkColumn()) return true;
    if (this.keyColumns != null)
    {
      return keyColumns.contains(col);
    }
    return false;
  }

  private String createMySQLUpsert(ConstantColumnValues columnConstants, String insertSqlStart, boolean useIgnore)
  {
    String insert = createInsertStatement(columnConstants, insertSqlStart);
		DbMetadata meta = dbConn.getMetadata();

    insert += "\nON DUPLICATE KEY UPDATE \n  ";
    if (useIgnore)
    {
      // Just add a dummy update for one column
      // Apparently this is more efficient and stable than using insert ... ignore
      String colname = targetColumns.get(0).getDisplayName();
      colname = meta.quoteObjectname(colname);
      insert += " " + colname + " = " + colname;
    }
    else
    {
      for (int i=0; i < targetColumns.size(); i++)
      {
        if (i > 0) insert += ",\n  ";
        String colname = targetColumns.get(i).getDisplayName();
        colname = meta.quoteObjectname(colname);
        insert += colname + " = VALUES(" + colname + ")";
      }
    }
    return insert;
  }

  private boolean hasRealPK()
  {
    if (CollectionUtil.isEmpty(keyColumns)) return false;
    for (ColumnIdentifier col : keyColumns)
    {
      if (!col.isPkColumn()) return false;
    }
    return true;
  }

  private int getColCount()
  {
    if (targetColumns == null) return 0;
    return targetColumns.size();
  }

  private List<ColumnIdentifier> createColumnList(List<ColumnIdentifier> columns, ColumnFilter filter, boolean adjustCase)
  {
    DbMetadata meta = dbConn.getMetadata();

    List<ColumnIdentifier> newCols = new ArrayList<>(columns.size());
    for (ColumnIdentifier col : columns)
    {
      if (filter.ignoreColumn(col)) continue;
      ColumnIdentifier copy = col.createCopy();
      if (adjustCase)
      {
        String colname = meta.adjustObjectnameCase(meta.removeQuotes(copy.getColumnName()));
        copy.setColumnName(colname);
      }
      newCols.add(copy);
    }
    return newCols;
  }

}

