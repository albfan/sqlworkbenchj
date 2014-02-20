/*
 * OracleConstraintReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
package workbench.db.oracle;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import workbench.log.LogMgr;

import workbench.db.AbstractConstraintReader;
import workbench.db.ColumnIdentifier;
import workbench.db.TableConstraint;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;

/**
 * A class to read column and table constraints from Oracle.
 *
 * @author Thomas Kellerer
 */
public class OracleConstraintReader
	extends AbstractConstraintReader
{
	private final String TABLE_SQL =
		 "SELECT constraint_name, search_condition, status, validated \n" +
		 "FROM all_constraints cons   \n" +
		 "WHERE constraint_type = 'C' \n" +
		 " and owner = ? \n" +
		 " and table_name = ?  \n";

	public OracleConstraintReader(String dbId)
	{
		super(dbId);
	}

	@Override
	public int getIndexForSchemaParameter()
	{
		return 1;
	}

	@Override
	public int getIndexForCatalogParameter()
	{
		return -1;
	}

	@Override
	public int getIndexForTableNameParameter()
	{
		return 2;
	}

	@Override
	public String getColumnConstraintSql()
	{
		return null;
	}

	@Override
	public String getTableConstraintSql()
	{
		return TABLE_SQL;
	}

	@Override
	public List<TableConstraint> getTableConstraints(WbConnection dbConnection, TableDefinition def)
	{
		String sql = this.getTableConstraintSql();
		if (sql == null) return null;
		List<TableConstraint> result = CollectionUtil.arrayList();

		TableIdentifier table = def.getTable();

		ResultSet rs = null;
		PreparedStatement stmt = null;

		try
		{
			stmt = dbConnection.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, table.getSchema());
			stmt.setString(2, table.getTableName());

			rs = stmt.executeQuery();
			while (rs.next())
			{
				String name = rs.getString(1);
				String constraint = rs.getString(2);
				String status = rs.getString(3);
				String valid = rs.getString(4);

				if (constraint != null)
				{
					if (isImplicitConstraint(name, constraint, def.getColumns())) continue;

					String expression = "(" + constraint + ")";
					if ("DISABLED".equalsIgnoreCase(status))
					{
						expression += " DISABLE";
					}
					if ("NOT VALIDATED".equalsIgnoreCase(valid))
					{
						expression += " NOVALIDATE";
					}
					TableConstraint c = new TableConstraint(name, expression);
					c.setIsSystemName(isSystemConstraintName(name));
					result.add(c);
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("OracleConstraintReader", "Error when reading column constraints", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}

	/**
	 * Checks if the constraint definition is a valid Not null definition that should be displayed.
	 */
	protected boolean isImplicitConstraint(String name, String definition, List<ColumnIdentifier> columns)
	{
		// Not null constrainst can also be defined as check constraints.
		// Tn that case the constraint will have a name that is not system generated
		// and it needs to be included because those columns are not reported as NOT NULL
		// e.g. create table foo (id integer constraint id_not_null check (id is not null));
		if (!isSystemConstraintName(name)) return false;

		try
		{
			SQLLexer lexer = new SQLLexer(definition);
			SQLToken tok = lexer.getNextToken(false, false);
			if (tok == null) return false;

			if (!tok.isIdentifier()) return false;
			String colName = SqlUtil.removeObjectQuotes(tok.getText());

			// If no further tokens exist, this cannot be a not null constraint
			tok = lexer.getNextToken(false, false);
			if (tok == null) return false;

			SQLToken tok2 = lexer.getNextToken(false, false);
			if (tok2 == null)
			{
				if ("IS NOT NULL".equalsIgnoreCase(tok.getContents()))
				{
					if (isNullable(columns, colName))
					{
						// the colum is marked as nullable but a not null check constraint exists
						// this constraint should be displayed
						return false;
					}
					// the column is already marked as not null
					// so the constraint can be ignored
					return true;
				}
				return false;
			}
			else
			{
				return false;
			}
		}
		catch (Exception e)
		{
			return false;
		}
	}

	private boolean isNullable(List<ColumnIdentifier> columns, String colname)
	{
		for (ColumnIdentifier col : columns)
		{
			if (col.getColumnName().equalsIgnoreCase(colname))
			{
				return col.isNullable();
			}
		}
		return true;
	}
}
