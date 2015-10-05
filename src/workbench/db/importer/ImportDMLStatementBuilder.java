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
package workbench.db.importer;

import java.util.ArrayList;
import java.util.List;

import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.DmlExpressionBuilder;
import workbench.db.JdbcUtils;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.util.CollectionUtil;
import workbench.util.StringUtil;

/**
 * A class to build the INSERT statement for a DataImporter.
 *
 * By default this is a plain INSERT statement, if the DataImporter should do
 an INSERT/UPDATE and the underlying DBMS isModeSupported an "UPSERT", the insert
 statement will exploit this functionality.

 Currently implemented for Postgres 9.5, Firebird, H2 and MySQL.
 *
 * @author Thomas Kellerer
 */
public class ImportDMLStatementBuilder
{
  private final WbConnection dbConn;
  private final TableIdentifier targetTable;
	private final List<ColumnIdentifier> targetColumns;
  private List<ColumnIdentifier> keyColumns;

  ImportDMLStatementBuilder(WbConnection connection, TableIdentifier target, List<ColumnIdentifier> columns, ColumnFilter filter, boolean adjustColumnNameCase)
  {
    dbConn = connection;
    targetTable = target;
    targetColumns = createColumnList(columns, filter, adjustColumnNameCase);
  }

  public void setKeyColumns(List<ColumnIdentifier> keys)
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

	public String createInsertStatement(ConstantColumnValues columnConstants, String insertSqlStart)
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

  public String createInsertIgnore(ConstantColumnValues columnConstants, String insertSqlStart)
  {
    if (dbConn.getMetadata().isPostgres() && JdbcUtils.hasMinimumServerVersion(dbConn, "9.5"))
    {
      return createPostgresUpsert(columnConstants, insertSqlStart, true);
    }
    if (dbConn.getMetadata().isMySql())
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
      return createDB2Upsert(columnConstants, true);
    }
    return null;
  }

  public String createOracleInsertIgnore(ConstantColumnValues columnConstants)
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

  public String createUpsertStatement(ConstantColumnValues columnConstants, String insertSqlStart)
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
      return createDB2Upsert(columnConstants, false);
    }
    return null;
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

  private String createDB2Upsert(ConstantColumnValues columnConstants, boolean insertOnly)
  {
    return createStandardMerge(columnConstants, insertOnly, "USING TABLE");
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

    if (!insertOnly)
    {
      text.append("\nWHEN MATCHED THEN UPDATE\n  SET ");
      colIndex = 0;
      for (int i=0; i < getColCount(); i++)
      {
        ColumnIdentifier col = this.targetColumns.get(i);
        if (col.isPkColumn()) continue;

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
			ColumnIdentifier col = this.targetColumns.get(i);
      if (col.isPkColumn()) continue;

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

		return text.toString();

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

  public static boolean supportsInsertIgnore(WbConnection dbConn)
  {
    if (dbConn == null) return false;
    if (dbConn.getDbSettings().useUpsert() == false) return false;

    if (dbConn.getMetadata().isPostgres() && JdbcUtils.hasMinimumServerVersion(dbConn, "9.5")) return true;
    if (dbConn.getMetadata().isOracle() && JdbcUtils.hasMinimumServerVersion(dbConn, "11.2") ) return true;
    if (dbConn.getMetadata().isDB2LuW()) return true;
    if (dbConn.getMetadata().isHsql() && JdbcUtils.hasMinimumServerVersion(dbConn, "2.0")) return true;
    if (dbConn.getMetadata().isMySql()) return true;

    return false;
  }

  public boolean isModeSupported(ImportMode mode)
  {
    switch (mode)
    {
      case insertIgnore:
        if (dbConn.getMetadata().isMySql())
        {
          // MySQL supports an upsert if there is a real PK defined on the table
          return hasRealPK();
        }
        return supportsInsertIgnore(dbConn);
      case insertUpdate:
      case updateInsert:
      case upsert:
        return supportsUpsert();
    }
    return false;
  }

  public boolean hasRealPK()
  {
    if (CollectionUtil.isEmpty(keyColumns)) return false;
    for (ColumnIdentifier col : keyColumns)
    {
      if (!col.isPkColumn()) return false;
    }
    return true;
  }

  public boolean supportsUpsert()
  {
    if (dbConn.getMetadata().isH2() || dbConn.getMetadata().isMySql())
    {
      // MySQL and H2 only support an upsert if there is a PK defined on the table
      // the key columns to be used cannot be specified dynamically
      return hasRealPK();
    }
    return supportsUpsert(this.dbConn);
  }

  public static boolean supportsUpsert(WbConnection connection)
  {
    if (connection == null) return false;
    if (connection.getDbSettings().useUpsert() == false) return false;

    if (connection.getMetadata().isPostgres() && JdbcUtils.hasMinimumServerVersion(connection, "9.5")) return true;
    if (connection.getMetadata().isFirebird() && JdbcUtils.hasMinimumServerVersion(connection, "2.1")) return true;
    if (connection.getMetadata().isDB2LuW()) return true;
    if (connection.getMetadata().isHsql() && JdbcUtils.hasMinimumServerVersion(connection, "2.0")) return true;
    if (connection.getMetadata().isH2()) return true;
    if (connection.getMetadata().isMySql()) return true;

    return false;
  }

  private int getColCount()
  {
    if (targetColumns == null) return 0;
    return targetColumns.size();
  }

}

