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
 * an INSERT/UPDATE and the underlying DBMS supports an "UPSERT", the insert
 * statement will exploit this functionality.
 *
 * Currently implemented for Postgres 9.5, Firebird, H2 and MySQL.
 *
 * @author Thomas Kellerer
 */
public class ImportDMLStatementBuilder
{
  private final WbConnection dbConn;
  private final TableIdentifier targetTable;
	private final List<ColumnIdentifier> targetColumns;
  private final ColumnFilter colFilter;

  public ImportDMLStatementBuilder(WbConnection connection, TableIdentifier target, List<ColumnIdentifier> columns, ColumnFilter filter)
  {
    dbConn = connection;
    targetTable = target;
    targetColumns = columns;
    colFilter = filter;
  }

	public String createInsertStatement(ConstantColumnValues columnConstants, String insertSqlStart, boolean adjustColumnNameCase)
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
			if (colFilter.ignoreColumn(col)) continue;

			if (colIndex > 0)
			{
				text.append(',');
				parms.append(',');
			}

			String colname = col.getDisplayName();
			if (adjustColumnNameCase)
			{
				colname = meta.adjustObjectnameCase(meta.removeQuotes(colname));
			}
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

  public String createUpsertStatement(ConstantColumnValues columnConstants, String insertSqlStart, List<ColumnIdentifier> keyColumns, boolean adjustColumnNameCase)
  {
    if (dbConn.getMetadata().isPostgres() && JdbcUtils.hasMinimumServerVersion(dbConn, "9.5"))
    {
      return createPostgresUpsert(columnConstants, insertSqlStart, keyColumns, adjustColumnNameCase);
    }
    if (dbConn.getMetadata().isMySql())
    {
      return createMySQLUpsert(columnConstants, insertSqlStart, adjustColumnNameCase);
    }
    if (dbConn.getMetadata().isH2())
    {
      return createH2Upsert(columnConstants, adjustColumnNameCase);
    }
    if (dbConn.getMetadata().isFirebird())
    {
      return createFirebirdUpsert(columnConstants, keyColumns, adjustColumnNameCase);
    }
    return null;
  }

  private String createPostgresUpsert(ConstantColumnValues columnConstants, String insertSqlStart, List<ColumnIdentifier> keyColumns, boolean adjustColumnNameCase)
  {
    if (CollectionUtil.isEmpty(keyColumns)) return null;

    String insert = createInsertStatement(columnConstants, insertSqlStart, adjustColumnNameCase);
    insert += "\nON CONFLICT (";
		DbMetadata meta = dbConn.getMetadata();
    for (int i=0; i < keyColumns.size(); i++)
    {
      if (i > 0) insert += ",";
      String colname = keyColumns.get(i).getDisplayName();
      colname = meta.quoteObjectname(colname);
      insert += colname;
    }
    insert += ")\nDO UPDATE\n  SET ";
    for (int i=0; i < targetColumns.size(); i++)
    {
      if (i > 0) insert += ",\n      ";
      String colname = targetColumns.get(i).getDisplayName();
      colname = meta.quoteObjectname(colname);
      insert += colname + " = EXCLUDED." + colname;
    }
    return insert;
  }

  private String createH2Upsert(ConstantColumnValues columnConstants, boolean adjustColumnNameCase)
  {
    String insert = createInsertStatement(columnConstants, null, adjustColumnNameCase);
    insert = insert.replace("INSERT INTO", "MERGE INTO");
    return insert;
  }

  private String createFirebirdUpsert(ConstantColumnValues columnConstants, List<ColumnIdentifier> keyColumns, boolean adjustColumnNameCase)
  {
    String insert = createInsertStatement(columnConstants, null, adjustColumnNameCase);
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

  private String createMySQLUpsert(ConstantColumnValues columnConstants, String insertSqlStart, boolean adjustColumnNameCase)
  {
    String insert = createInsertStatement(columnConstants, insertSqlStart, adjustColumnNameCase);
		DbMetadata meta = dbConn.getMetadata();

    insert += "\nON DUPLICATE KEY UPDATE \n  ";
    for (int i=0; i < targetColumns.size(); i++)
    {
      if (i > 0) insert += ",\n  ";
      String colname = targetColumns.get(i).getDisplayName();
      colname = meta.quoteObjectname(colname);
      insert += colname + " = VALUES(" + colname + ")";
    }
    return insert;
  }

  public boolean supportsUpsert()
  {
    if (dbConn == null) return false;

    if (dbConn.getMetadata().isMySql()) return true;
    if (dbConn.getMetadata().isH2()) return true;
    if (dbConn.getMetadata().isFirebird() && JdbcUtils.hasMinimumServerVersion(dbConn, "2.1")) return true;
    if (dbConn.getMetadata().isPostgres() && JdbcUtils.hasMinimumServerVersion(dbConn, "9.5")) return true;

    return false;
  }

  private int getColCount()
  {
    if (targetColumns == null) return 0;
    return targetColumns.size();
  }

}

